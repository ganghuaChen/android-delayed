package se.sandos.android.delayed.scrape;

import java.util.ArrayList;
import java.util.List;

import se.sandos.android.delayed.Delayed;
import se.sandos.android.delayed.TrainEvent;
import se.sandos.android.delayed.prefs.Prefs;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.Log;

public class ScrapeService extends Service {
    private static final String Tag = "ScrapeService";
    
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
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        final PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Delayed service");
        wl.acquire();
        
        try {
            Log.v(Tag, "onStart, missed: " + intent.getIntExtra(Intent.EXTRA_ALARM_COUNT, 0));
            
            String favName = Prefs.getSetting(getApplicationContext(), Prefs.PREFS_FAV_NAME);
            String favURL = Prefs.getSetting(getApplicationContext(), Prefs.PREFS_FAV_URL);
            
            if(favName != null && favURL != null) {
                //This will spawn a new thread so that we can return quickly
                Log.v(Tag, "Starting scrape from background service for " + favName);
                ScraperHelper.scrapeStationReleaseWakelock(favURL, favName, new ScrapeListener<TrainEvent, Object[]>(){
                    private List<TrainEvent> trainevents = new ArrayList<TrainEvent>();
                    
                    public void onFinished(Object[] result) {
                        if (result == null) {
                            // this actually means finished!
                            Delayed.getDb(getApplicationContext()).addTrainEvents(trainevents);
                        } else {
                            //This is a fixup (destination) message
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
                }, wl);
            }
            
            if(Prefs.isSet(getApplicationContext(), Prefs.PREFS_SERVICE_ENABLED, false)) {
                setAlarm(getApplicationContext(), Prefs.getIntSetting(getApplicationContext(), Prefs.PREFS_INTERVAL, 120));
            }
        } finally {
            stopSelf();
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
    
    public static void removeAlarm(Context ctx)
    {
        Log.v(Tag, "Removing alarm");
        AlarmManager mgr = (AlarmManager) ctx.getSystemService(ALARM_SERVICE);
     
        Intent i = new Intent(ctx, ScrapeService.class);
        PendingIntent pi = PendingIntent.getService(ctx, 1, i, PendingIntent.FLAG_UPDATE_CURRENT);
        mgr.cancel(pi);
    }
}
