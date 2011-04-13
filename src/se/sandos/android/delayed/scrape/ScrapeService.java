package se.sandos.android.delayed.scrape;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import se.sandos.android.delayed.Delayed;
import se.sandos.android.delayed.StationActivity;
import se.sandos.android.delayed.TrainEvent;
import se.sandos.android.delayed.db.DBAdapter;
import se.sandos.android.delayed.prefs.Favorite;
import se.sandos.android.delayed.prefs.Prefs;
import se.sandos.android.delayed.prefs.Widget;
import android.R;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

public class ScrapeService extends Service {
    private static final String Tag = "ScrapeService";
    private static final int SERVICE_NOTIFICATION_ID = 1;
    private static boolean forceRun = true;
    
    @Override
    public IBinder onBind(Intent arg0)
    {
        //We don't really support binding
        Log.v(Tag, "onBind");
        return null;
    }
    
    @Override
    public void onCreate()
    {
        Log.v(Tag, "onCreate");
    }

    @Override
    public void onStart(Intent intent, int startid)
    {
    	ConnectivityManager cm = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        if(!forceRun && (!cm.getBackgroundDataSetting() || !Prefs.isSet(getApplicationContext(), Prefs.PREFS_SERVICE_ENABLED, false))) {
            stopSelf();
            removeAlarm(getApplicationContext());
            return;
        }

        if(forceRun)
        {
            forceRun = false;
        }
        
        showNotification();
        
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        final PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Delayed service");
        wl.acquire();
        
        Log.v(Tag, "onStart, missed: " + intent.getIntExtra(Intent.EXTRA_ALARM_COUNT, 0));
        
        DBAdapter db = Delayed.getDb(getApplicationContext());
        
        List<Favorite> favorites = Prefs.getFavorites(getApplicationContext());
        Set<Favorite> toScrape = new HashSet<Favorite>();
        for(Favorite f : favorites)
        {
            if(f.isActive()) {
                toScrape.add(f);
                
                List<Favorite> toAdd = new LinkedList<Favorite>();
                if(f.targetOtherFavorites())
                {
                    toAdd = favorites;
                }
                else
                {
                    for(Favorite innerFav : favorites)
                    {
                        if(f.getTargetSet().contains(innerFav.getName()))
                        {
                            toAdd.add(innerFav);
                        }
                    }
                }
                
                for(Favorite innerFav : toAdd)
                {
                    // We do not check activeness here. We want to see trains toward inactive favorites. (This kinda
                    // sucks)
                    if(!innerFav.equals(f))
                    {
                        toScrape.add(innerFav);
                    }
                }
            }
        }
        
        for(Favorite f : toScrape)
        {
            String url = db.getUrl(f.getName());
            scrape(f.getName(), url);
        }

        // We need to add this to the thread-pool unfortunately, since the DB update is
        // done using the same threadpool.
        // This job should be "just behind" the db-update jobs (one per scraped favorite)
        ScrapePool.addJob(new DelayRunnable(DelayRunnable.Importance.NORMAL) {
            public void run() {
                scheduleAllWidgetUpdate(getApplicationContext());
            }
        });
        
        // Schedule wakelock-release and alarm setting, this will run after any scrapes are done
        // We want to set the alarm after the scrapes are done, to avoid over-runs from previous
        // triggers
        final ScrapeService t = this;
        
        ScrapePool.addJob(new DelayRunnable(DelayRunnable.Importance.HIGH){
            public void run(){
                Log.v(Tag, "Running service clean-up");
                wl.release();
                
                if(Prefs.isSet(getApplicationContext(), Prefs.PREFS_SERVICE_ENABLED, false)) {
                    setAlarm(getApplicationContext(), Prefs.getIntSetting(getApplicationContext(), Prefs.PREFS_INTERVAL, 120));
                }
                
                stopSelf();
                
                t.cancelNotification();
            }
        });
    }

    private void cancelNotification()
    {
        String ns = Context.NOTIFICATION_SERVICE;
        NotificationManager notificationMgr = (NotificationManager) getSystemService(ns);
        
        notificationMgr.cancel(SERVICE_NOTIFICATION_ID);
    }
    
    private void showNotification()
    {
        String ns = Context.NOTIFICATION_SERVICE;
        NotificationManager notificationMgr = (NotificationManager) getSystemService(ns);
        
        int icon = R.drawable.ic_dialog_alert;
        CharSequence tickerText = "Delayed";
        long when = System.currentTimeMillis();

        Notification notification = new Notification(icon, tickerText, when);
        
        Context context = getApplicationContext();
        CharSequence contentTitle = "Delayed uppdatering";
        CharSequence contentText = "Uppdatering görs i bakgrunden";
        Intent notificationIntent = new Intent(this, StationActivity.class);
        
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
        
        notification.setLatestEventInfo(context, contentTitle, contentText, contentIntent);
        
        notification.flags = Notification.FLAG_AUTO_CANCEL;
        
        notificationMgr.notify(SERVICE_NOTIFICATION_ID, notification);
    }

    public static void scheduleAllWidgetUpdate(Context ctx)
    {
        Log.v(Tag, "Sending update intents to all widgets");
        
        for(Widget w : Prefs.getWidgets(ctx)) {
            Log.v(Tag, "Updating widget with id " + w.getId());
            Intent update = new Intent();
            update.setAction("se.sandos.android.delayed.widgetUpdate");
            //update.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
            update.setData(Uri.fromParts("delayed", "widgetupdate", String.valueOf(w.getId())));
            update.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, new int[] {w.getId()});
            ctx.sendBroadcast(update);
        }
    }

    private void scrape(final String favName, final String favURL)
    {
        //This will spawn a new thread so that we can return quickly
        Log.v(Tag, "Starting scrape from background service for " + favName);
        ScraperHelper.scrapeStation(favURL, favName, new ScrapeListener<TrainEvent, Object[]>(){
            private List<TrainEvent> trainevents = new ArrayList<TrainEvent>();
            
            public void onFinished(Object[] result) {
                if (result == null) {
                    // this actually means finished!
                    Log.v(Tag, "Station: " + favName + " " + trainevents.size());
                    Delayed.getDb(getApplicationContext()).addTrainEvents(trainevents, favName);
                } else {
                    //This is a fixup (destination) message
                    //XXX: we should do something here
                }
            }

            public void onPartialResult(TrainEvent result) {
                trainevents.add(result);
            }

            public void onRestart() {
                trainevents.clear();
            }

            public void onStatus(String status) {}
            public void onFail(){}
        });
    }


    public static void startIfNeeded(Context ctx)
    {
        if(Prefs.isSet(ctx, Prefs.PREFS_SERVICE_ENABLED, false)) {
            runOnceNow(ctx);
            ScrapeService.setAlarm(ctx, Prefs.getIntSetting(ctx, Prefs.PREFS_INTERVAL, 120));
        }
    }
    
    /**
     * 
     * @param ctx
     * @param delay Delay in seconds until next wakeup
     */
    public static void setAlarm(Context ctx, int delay)
    {
        Log.v(Tag, "Setting up alarm");
        AlarmManager mgr = (AlarmManager) ctx.getSystemService(ALARM_SERVICE);
        
        Intent i = new Intent(ctx, ScrapeService.class);
        PendingIntent pi = PendingIntent.getService(ctx, 1, i, PendingIntent.FLAG_UPDATE_CURRENT);
        mgr.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + (delay*1000), pi);
    }
    
    public static void setAlarmWithDefaults(Context ctx) 
    {
        setAlarm(ctx, Prefs.getIntSetting(ctx, Prefs.PREFS_INTERVAL, 120));
    }
    
    public static void runOnceNow(Context ctx)
    {
        Intent i = new Intent(ctx, ScrapeService.class);
        ctx.startService(i);
    }

    public static void runOnceNowForced(Context ctx)
    {
        forceRun = true;
        Intent i = new Intent(ctx, ScrapeService.class);
        ctx.startService(i);
    }
    
    public static void removeAlarm(Context ctx)
    {
        Log.v(Tag, "Removing alarm");
        AlarmManager mgr = (AlarmManager) ctx.getSystemService(ALARM_SERVICE);
     
        Intent i = new Intent(ctx, ScrapeService.class);
        PendingIntent pi = PendingIntent.getService(ctx, 1, i, PendingIntent.FLAG_UPDATE_CURRENT);
        mgr.cancel(pi);
    }
}
