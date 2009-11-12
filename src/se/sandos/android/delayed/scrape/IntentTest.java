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
import android.os.Handler;
import android.os.Message;
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
    
    public static void startSchedulerActivity(final Context context, final Handler handler, String pkg, String className)
    {
        BroadcastReceiver br = new BroadcastReceiver() {
            
            @Override
            public void onReceive(Context context, Intent intent) {
                final String cl = getResultExtras(true).getString("activity");
                final String pkg = getResultExtras(true).getString("pkg");
                Log.v(Tag, "ACTIVITY: " + cl);
                if(cl != null) {
                    Intent in = new Intent();
                    in.setComponent(new ComponentName(pkg, cl));
                    handler.dispatchMessage(Message.obtain(handler, 0, in));
                }
            }
        };

        IntentTest.sendBroadcast(context, pkg, className, IntentTest.SCHEDULER_SETTINGS, br);

    }
    
    public static void test(final Context ctx)
    {
        List<ResolveInfo> l = getSchedulerList(ctx);
        
        for(ResolveInfo ri : l) {
            if(ri.activityInfo != null) {
                final ActivityInfo ai = ri.activityInfo;
                
                getLabel(ctx, ai);
                if(ai.name.indexOf("sandos") != -1) {
                    BroadcastReceiver br = new BroadcastReceiver() {
                        @Override
                        public void onReceive(Context context, Intent intent) {
                            Log.v(Tag, "" + getResultExtras(true).getInt("delay"));
                        }
                    };

                    sendBroadcast(ctx, ai, SCHEDULER_ACTION, br);

                    BroadcastReceiver br2 = new BroadcastReceiver() {
                        @Override
                        public void onReceive(Context context, Intent intent) {
                            String cl = getResultExtras(true).getString("activity");
                            Log.v(Tag, "ACTIVITY: " + cl);
                            if(cl != null) {
                                Intent in = new Intent();
                                in.setComponent(new ComponentName(ai.packageName, cl));
                                ctx.startActivity(in);
                            }
                        }
                    };
                    sendBroadcast(ctx, ai.packageName, ai.name, SCHEDULER_SETTINGS, br2);
                }
            }
        }
    }

    public static String getLabel(final Context ctx, final ActivityInfo ai) {
        try {
            ComponentName cn = new ComponentName(ai.packageName, ai.name);
            ActivityInfo aii = ctx.getPackageManager().getReceiverInfo(cn, PackageManager.GET_META_DATA);
            if(aii.metaData != null) {
                Log.v(Tag, "Got more info: " + aii.metaData.getString("label"));
                return aii.metaData.getString("label");
            }
        } catch (NameNotFoundException e) {
            Log.v(Tag, e.toString());
        }
        
        return null;
    }

    public static void sendBroadcast(Context ctx, ActivityInfo ai, String action, BroadcastReceiver receiver) {
        Intent in = new Intent();
        in.setAction(action);
        in.setComponent(new ComponentName(ai.packageName, ai.name));

        ctx.sendOrderedBroadcast(in, null, receiver, null, Activity.RESULT_OK, null, null);
    }
    
    public static void sendBroadcast(Context ctx, String pkg, String className, String action, BroadcastReceiver receiver) {
        Log.v(Tag, "Pkg:" + pkg +" " + className);
        Intent in = new Intent();
        in.setAction(action);
        in.setComponent(new ComponentName(pkg, className));

        ctx.sendOrderedBroadcast(in, null, receiver, null, Activity.RESULT_OK, null, null);
    }

}
