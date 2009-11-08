package se.sandos.android.delayed.prefs;

import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.util.Log;

public class IntentTest
{
    private static final String Tag = "IntentTest";
    
    public static void test(Context ctx)
    {
        PackageManager pm = ctx.getPackageManager();
        
        
        Intent intent = new Intent();
        intent.setAction("android.intent.action.BOOT_COMPLETED");
        
        List<ResolveInfo> l = pm.queryBroadcastReceivers(intent, 0);
        
        Log.v(Tag, "Got num " + l.size());
        
        for(ResolveInfo ri : l) {
            Log.v(Tag, ri.toString() + " " + ri.getClass() + " " + ri.activityInfo + " " + ri.serviceInfo);
        }
        
    }
}
