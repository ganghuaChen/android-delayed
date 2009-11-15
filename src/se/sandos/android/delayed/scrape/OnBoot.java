package se.sandos.android.delayed.scrape;

import se.sandos.android.delayed.prefs.Prefs;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class OnBoot extends BroadcastReceiver
{
    private static final String Tag = "OnBoot";
    
    @Override
    public void onReceive(Context context, Intent i)
    {
        if(Prefs.isSet(context, Prefs.PREFS_SERVICE_ENABLED, false)) {
            Log.v(Tag, "Starting delayed service on boot");
            
            Intent intent = new Intent();
            intent.setAction("se.sandos.android.delayed.scrape.ScrapeService");
            context.startService(intent);
        }
    }

}
