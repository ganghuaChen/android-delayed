package se.sandos.android.delayed.scrape;

import java.util.List;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.util.Log;

public class IntentTest
{
    public static final String SCHEDULER_ACTION = "se.sandos.android.delayed.Scheduler";
    public static final String SCHEDULER_SETTINGS = "se.sandos.android.delayed.SchedulerSettings";
    
    private static final String Tag = "IntentTest";

    public static List<ResolveInfo> getSchedulerList(Context ctx)
    {
        PackageManager pm = ctx.getPackageManager();
        
        Intent intent = new Intent();
        intent.setAction(SCHEDULER_ACTION);
        
        List<ResolveInfo> l = pm.queryBroadcastReceivers(intent, 0);
        
        return l;
    }
    
    public static void test(Context ctx)
    {
        List<ResolveInfo> l = getSchedulerList(ctx);
        
        for(ResolveInfo ri : l) {
            if(ri.activityInfo != null) {
                ActivityInfo ai = ri.activityInfo;
                
                try {
                    ComponentName cn = new ComponentName(ai.packageName, ai.name);
                    ActivityInfo aii = ctx.getPackageManager().getReceiverInfo(cn, PackageManager.GET_META_DATA);
                    if(aii.metaData != null) {
                        Log.v(Tag, "Got more info: " + aii.metaData.getString("label"));
                    }
                } catch (NameNotFoundException e) {
                    Log.v(Tag, e.toString());
                }
                if(ai.name.indexOf("sandos") != -1) {
                    Intent in = new Intent();
                    in.setAction(SCHEDULER_ACTION);
                    in.setComponent(new ComponentName(ai.packageName, ai.name));

                    BroadcastReceiver br = new BroadcastReceiver() {
                        @Override
                        public void onReceive(Context context, Intent intent) {
                            Log.v(Tag, "" + getResultExtras(true).getInt("delay"));
                        }
                    };
                    ctx.sendOrderedBroadcast(in, null, br, null, Activity.RESULT_OK, null, null);
                }
            }
        }
    }
}
