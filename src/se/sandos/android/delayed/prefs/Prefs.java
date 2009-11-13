package se.sandos.android.delayed.prefs;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.Log;

public class Prefs {
    private static final String Tag = "Prefs";
    
    public static final String PREFS_FAV_NAME = "favoriteName";
    public static final String PREFS_FAV_URL = "favoriteUrl";
    public static final String PREFS_KEY = "delayedfilter";
    
    public static final String PREFS_INTERVAL = "defaultscheduleinterval";

    public static final String PREFS_SERVICE_ENABLED = "serviceEnabled";

    
    /**
     * Get a setting. Default value if not found is null
     * @param setting
     * @return
     */
    public static String getSetting(Context ctx, String setting)
    {
        return getSetting(ctx, setting, null);
    }
    
    public static void setSetting(Context ctx, String setting, String value) 
    {
        SharedPreferences sp = ctx.getSharedPreferences(PREFS_KEY, Context.MODE_PRIVATE);
        Log.v(Tag, "Setting setting: " + setting + " to " + value);
        Editor editor = sp.edit();
        editor.putString(setting, value);
        if(!editor.commit()) {
            Log.w(Tag, "Failed to commit!");
        }
    }

    public static long getLongSetting(Context ctx, String setting, long def)
    {
        SharedPreferences sp = ctx.getSharedPreferences(PREFS_KEY, Context.MODE_PRIVATE);
        long r = sp.getLong(setting, def);
        Log.v(Tag, "Returning long setting: " + setting + " " + r);
        return r;
    }

    public static int getIntSetting(Context ctx, String setting, int def)
    {
        SharedPreferences sp = ctx.getSharedPreferences(PREFS_KEY, Context.MODE_PRIVATE);
        int r = sp.getInt(setting, def);
        Log.v(Tag, "Returning int setting: " + setting + " " + r);
        return r;
    }

    
    public static boolean isSet(Context ctx, String setting)
    {
        String s = getSetting(ctx, setting, null);
        return (s != null && s.equalsIgnoreCase("true"));
    }
    
    /**
     * Get a setting. 
     * @param setting Name of setting to fetch
     * @param def Default value if not found
     * @return
     */
    public static String getSetting(Context ctx, String setting, String def)
    {
        SharedPreferences sp = ctx.getSharedPreferences(PREFS_KEY, Context.MODE_PRIVATE);
        String r = sp.getString(setting, def);
        Log.v(Tag, "Returning setting: " + setting + " " + r);
        return r;
    }

}
