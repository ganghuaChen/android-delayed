package se.sandos.android.delayed.prefs.locale;

import se.sandos.android.delayed.prefs.Prefs;
import se.sandos.android.delayed.scrape.ScrapeService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class LocaleBroadcastReceiver extends BroadcastReceiver
{
    private static final String Tag = LocaleBroadcastReceiver.class.getName();

    @Override
    public void onReceive(Context context, Intent intent)
    {
        if ("com.twofortyfouram.locale.intent.action.FIRE_SETTING".equals(intent.getAction()))
        {
            Log.v(Tag, "Got fired");
            
            boolean enabled = intent.getBooleanExtra(Prefs.PREFS_SERVICE_ENABLED, false);
            
            if(enabled) {
                Prefs.setBooleanSetting(context, Prefs.PREFS_SERVICE_ENABLED, true);
                ScrapeService.setAlarmWithDefaults(context);
                ScrapeService.runOnceNow(context);
            } else {
                Prefs.setBooleanSetting(context, Prefs.PREFS_SERVICE_ENABLED, false);
                ScrapeService.removeAlarm(context);
            }
        }
    }
}
