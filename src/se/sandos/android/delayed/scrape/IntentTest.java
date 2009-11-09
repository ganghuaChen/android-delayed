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
    private static final String SCHEDULER_ACTION = "se.sandos.android.delayed.Scheduler";
    private static final String Tag = "IntentTest";

    public static List<ResolveInfo> getSchedulerList(Context ctx)
    {
        PackageManager pm = ctx.getPackageManager();
        
        Intent intent = new Intent();
        intent.setAction(SCHEDULER_ACTION);
        
        List<ResolveInfo> l = pm.queryBroadcastReceivers(intent, 0);
        
        Log.v(Tag, "Got num " + l.size());

        return l;
    }
    
    public static void test(Context ctx)
    {
        List<ResolveInfo> l = getSchedulerList(ctx);
        
        for(ResolveInfo ri : l) {
            Log.v(Tag, ri.toString() + " " + ri.getClass() + " " + ri.activityInfo + " " + ri.serviceInfo);
            if(ri.activityInfo != null) {
                ActivityInfo ai = ri.activityInfo;
                Log.v(Tag, "" + ai.name + " " + ai.processName + " " + ai.packageName);
                Log.v(Tag, "Label: " + ctx.getResources().getString(ai.labelRes));
                
                try {
                    //ComponentName cn = new ComponentName(ai.packageName, ai.name);
                    ComponentName cn = new ComponentName(ctx, OnBoot.class);
                    ComponentName cn2 = new ComponentName("se.sandos.android.delayed", "se.sandos.android.delayed.scrape.OnBoot");
                    Log.v(Tag, "Cool: " + cn + " " + cn2);
                    ActivityInfo aii = ctx.getPackageManager().getReceiverInfo(cn, PackageManager.GET_META_DATA);
                    Log.v(Tag, "Got more info: " + aii.metaData.getString("Label"));
                } catch (NameNotFoundException e) {
                    Log.v(Tag, e.toString());
                }
                if(ai.name.indexOf("sandos") != -1) {
                    Intent in = new Intent();
                    in.setAction(SCHEDULER_ACTION);
                    //in.setComponent(new ComponentName(ctx, OnBoot.class));
                    //ComponentName cn = new ComponentName("se.sandos.android.delayed", "se.sandos.android.delayed.scrape.OnBoot");
                    //ComponentName cn2 = new ComponentName(ctx, OnBoot.class);
                    //Log.v(Tag, "Cn: " + cn + " " + cn2);
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