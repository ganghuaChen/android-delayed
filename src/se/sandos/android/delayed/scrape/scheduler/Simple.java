package se.sandos.android.delayed.scrape.scheduler;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

/**
 * Trivial Scheduler that always wants to run 5 minutes into the future.
 * @author sandos
 *
 */
public class Simple extends BroadcastReceiver {
    @Override
    public void onReceive(Context arg0, Intent arg1) {
        //Default to 5 minute delay
        Bundle b = new Bundle();
        b.putInt("delay", 300);
        setResult(0, "majs", b);
    }
}
