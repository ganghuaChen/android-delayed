package se.sandos.android.delayed.scrape;

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
        Log.v(Tag, "Starting service on boot");
        
        Intent intent = new Intent();
        intent.setAction("se.sandos.android.delayed.scrape.ScrapeService");
        context.startService(intent);
    }

}
