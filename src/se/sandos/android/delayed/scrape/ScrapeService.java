package se.sandos.android.delayed.scrape;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.PowerManager.WakeLock;
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
        PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Delayed service");
        wl.acquire();
        
        try {
            Log.v(Tag, "onStart, missed: " + intent.getIntExtra(Intent.EXTRA_ALARM_COUNT, 0));
            
            setAlarm(getApplicationContext());
            
            stopSelf();
        } finally {
            wl.release();
        }
    }

    public static void setAlarm(Context ctx)
    {
        Log.v(Tag, "Setting up alarm");
        AlarmManager mgr = (AlarmManager) ctx.getSystemService(ALARM_SERVICE);
        
        //Wake us up after 15 minutes
        Intent i = new Intent();
        i.setAction("se.sandos.android.delayed.scrape.Service");
        PendingIntent pi = PendingIntent.getService(ctx, 1, i, PendingIntent.FLAG_ONE_SHOT);
        mgr.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + 60000 * 15, pi);
    }
}
