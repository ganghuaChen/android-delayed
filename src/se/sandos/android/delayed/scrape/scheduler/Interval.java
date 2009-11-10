package se.sandos.android.delayed.scrape.scheduler;

import java.util.Calendar;

import se.sandos.android.delayed.scrape.IntentTest;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

/**
 * Support for a number of hour:minute intervals to poll in
 * @author John Bäckstrand
 *
 */
public class Interval extends BroadcastReceiver {
    private static final String PREFS_KEY = "se.sandos.android.delayed.scape.scheduler.Interval";
    
    @Override
    public void onReceive(Context context, Intent intent) {

        if(intent.getAction().equals(IntentTest.SCHEDULER_ACTION)) {
            schedule(context);
            return;
        }
        
        if(intent.getAction().equals(IntentTest.SCHEDULER_SETTINGS)) {
            Bundle b = new Bundle();
            b.putString("activity", "se.sandos.android.delayed.scrape.schedule.IntervalActivity");
            setResult(0, "Success", b);
            return;
        }
        
    }

    private void schedule(Context context) {
        Calendar c = Calendar.getInstance();
        int hour = c.get(Calendar.HOUR_OF_DAY);
        int minute = c.get(Calendar.MINUTE);
        
        int shortestDistance = -1;
        
        SharedPreferences sp = context.getSharedPreferences(PREFS_KEY, Context.MODE_PRIVATE);
        
        int index = 0;
        while(true) {
            if(sp.contains("intervalStart" + index) && sp.contains("intervalEnd" + index)) {
                String start = sp.getString("intervalStart" + index, null);
                int startH = getHour(start);
                int startM = getMinute(start);
                
                String end = sp.getString("intervalEnd" + index, null);
                int endH = getHour(end);
                int endM = getHour(end);
                
                if(hour <= endH && hour >= startH && minute <= endM && minute >= startM) {
                    //Default to 2 minute delay
                    Bundle b = new Bundle();
                    b.putInt("delay", 120);
                    setResult(0, "Success", b);
                    return;
                } else {
                    //Compute shortest distance, if this is in the future
                    int dayMinute = hour * 60 + minute;
                    int intervalDayMinute = startH * 60 + startM;
                    int diff = intervalDayMinute - dayMinute;
                    if(shortestDistance == -1 || shortestDistance > diff) {
                        shortestDistance = diff;
                    }
                }
            } else {
                break;
            }
        }

        if(shortestDistance != -1) {
            Bundle b = new Bundle();
            b.putInt("delay", shortestDistance);
            setResult(0, "Success", b);
        } else {
            //No settings at all
            //Default to 10 minute delay
            Bundle b = new Bundle();
            b.putInt("delay", 600);
            setResult(0, "Success", b);
        }
    }

    private int getHour(String start) {
        return Integer.valueOf(start.substring(0, 1));
    }

    private int getMinute(String start) {
        return Integer.valueOf(start.substring(2, 3));
    }
}
