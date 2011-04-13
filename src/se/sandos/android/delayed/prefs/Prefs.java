package se.sandos.android.delayed.prefs;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.Log;

public class Prefs {
    private static final String Tag = "Prefs";
    
    public static final String PREFS_FAV_PREFIX = "favorite";
    public static final String PREFS_KEY = "delayedfilter";
    
    public static final String PREFS_INTERVAL = "defaultscheduleinterval";

    public static final String PREFS_WIDGET_PREFIX = "widget";
    
    public static final String PREFS_SERVICE_ENABLED = "se.sandos.android.delayed.serviceEnabled";

    
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
//        Log.v(Tag, "Setting setting: " + setting + " to " + value);
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
//        Log.v(Tag, "Returning long setting: " + setting + " " + r);
        return r;
    }

    public static int getIntSetting(Context ctx, String setting, int def)
    {
        SharedPreferences sp = ctx.getSharedPreferences(PREFS_KEY, Context.MODE_PRIVATE);
        int r = sp.getInt(setting, def);
//        Log.v(Tag, "Returning int setting: " + setting + " " + r);
        return r;
    }
    
    public static void setBooleanSetting(Context ctx, String setting, boolean value)
    {
        SharedPreferences sp = ctx.getSharedPreferences(PREFS_KEY, Context.MODE_PRIVATE);
        Editor editor = sp.edit();
        editor.putBoolean(setting, value);
        if(!editor.commit()) {
            Log.w(Tag, "Failed to commit!");
        }
//        Log.v(Tag, "Setting boolean setting: " + setting + " " + value);
    }

    public static void setIntSetting(Context ctx, String setting, int value)
    {
        SharedPreferences sp = ctx.getSharedPreferences(PREFS_KEY, Context.MODE_PRIVATE);
        Editor editor = sp.edit();
        editor.putInt(setting, value);
        if(!editor.commit()) {
            Log.w(Tag, "Failed to commit!");
        }
//        Log.v(Tag, "Setting int setting: " + setting + " " + value);
    }

    
    public static void removeSetting(Context ctx, String setting)
    {
//        Log.v(Tag, "Removing setting " + setting);
        
        SharedPreferences sp = ctx.getSharedPreferences(PREFS_KEY, Context.MODE_PRIVATE);
        Editor editor = sp.edit();
        editor.remove(setting);
        editor.commit();
    }
    
    public static boolean isSet(Context ctx, String setting, boolean def)
    {
        SharedPreferences sp = ctx.getSharedPreferences(PREFS_KEY, Context.MODE_PRIVATE);
        boolean r = sp.getBoolean(setting, def);
//        Log.v(Tag, "Returning boolean setting: " + setting + " " + r);
        return r;
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
//        Log.v(Tag, "Returning setting: " + setting + " " + r);
        return r;
    }

    public static boolean contains(Context ctx, String setting)
    {
        SharedPreferences sp = ctx.getSharedPreferences(PREFS_KEY, Context.MODE_PRIVATE);
        return sp.contains(setting);
    }
    
    public static boolean hasWidget(Context ctx, int id)
    {
        for(Widget w : getWidgets(ctx)) {
            if(w.getId() == id) {
                return true;
            }
        }
        return false;
    }

    public static List<Widget> getWidgets(Context ctx)
    {
        List<Widget> res = new ArrayList<Widget>(4);
        
        int index = 0;
        while(true) {
            final String wdgt = PREFS_WIDGET_PREFIX + index;
            if(contains(ctx, wdgt)) {
                Widget w = new Widget(getIntSetting(ctx, wdgt, -1));
                w.setIndex(index++);
                w.setClickSetting(getIntSetting(ctx, wdgt + ".click", 0));
                res.add(w);
            } else {
                break;
            }
        }
        
        return res;
    }
    
    public static Widget findWidget(Context context, int id)
    {
        List<Widget> widgets = Prefs.getWidgets(context);
        for (Widget widget : widgets)
        {
            if(widget.getId() == id)
            {
                return widget;
            }
        }
        
        return null;
    }    
    
    public static void saveWidget(Context ctx, Widget w)
    {
        if(!hasWidget(ctx, w.getId())) {
            Log.e(Tag, "Could not find widget with id " + w.getId());
            return;
        }
        
        final String wdgt = PREFS_WIDGET_PREFIX + w.getIndex();
        setIntSetting(ctx, wdgt, w.getId());
        setIntSetting(ctx, wdgt + ".click", w.getClickSetting());
    }
    
    public static void addWidget(Context ctx, int id)
    {
        if(hasWidget(ctx, id)) {
            return;
        }
        
        int index = getWidgets(ctx).size();
        final String wdgt = PREFS_WIDGET_PREFIX + index;
            
        Log.v(Tag, "Added new widget: " + index + " " + id);
        
        setIntSetting(ctx, wdgt, id);
    }
    
    public static void removeWidget(Context ctx, int id)
    {
        if(!hasWidget(ctx, id)) {
            Log.v(Tag, "Did not find widget to delete: " + id);
            return;
        }
        
        List<Widget> widgets = getWidgets(ctx);
        
        boolean hasDeleted = false;
        int highestIndex = -1;
        for(Widget f : widgets) {
            if(!hasDeleted) {
                if(f.getId() == id) {
                    highestIndex = f.getIndex();
                    Log.v(Tag, "Removing widget: " + f.getId() + " " + f.getIndex());
                    hasDeleted = true;
                    final String wdgt = PREFS_WIDGET_PREFIX + f.getIndex();
                    removeSetting(ctx, wdgt);
                }
            } else {
                highestIndex = f.getIndex();
                Log.v(Tag, "Subtracting: " + f.getId() + " " + f.getIndex());
                //Subtract one from index
                f.setIndex(f.getIndex()-1);
                final String wdgt = PREFS_WIDGET_PREFIX + f.getIndex();
                setIntSetting(ctx, wdgt, f.getId());
            }
        }
        
        //Remove the last one
        if(highestIndex != -1) {
            final String wdgt = PREFS_WIDGET_PREFIX + highestIndex;
            removeSetting(ctx, wdgt);
        }
    }
    
    public static List<Favorite> getFavorites(Context ctx)
    {
        List<Favorite> res = new ArrayList<Favorite>(6);
        
        int index = 0;
        while(true) {
            final String fav = PREFS_FAV_PREFIX + index++;
            if(contains(ctx, fav)) {
                Favorite f = new Favorite();
                f.setActive(isSet(ctx, fav, false));
                f.setIndex(index-1);
                f.setName(getSetting(ctx, fav + ".name"));
                if(f.getName() != null) {
                    res.add(f);
                }
                f.setOtherFavorites(isSet(ctx, fav + ".otherfavs", false));
                int targetIndex = 0;
                
                while(contains(ctx, fav + ".targets." + targetIndex)) {
                    String targetName = getSetting(ctx, fav + ".targets." + targetIndex++); 
                    if(targetName != null && !targetName.equals("")) {
                        f.addTarget(targetName);
                    }
                }
            } else {
                break;
            }
        }
        
        return res;
    }
    
    public static Favorite getFavorite(Context ctx, String name)
    {
        for(Favorite f : getFavorites(ctx)) {
            if(f.getName().equals(name)) {
                return f;
            }
        }
        
        return null;
    }
    
    private static boolean hasFavorite(Context ctx, String name)
    {
        for(Favorite f : getFavorites(ctx)) {
            if(f.getName() != null && f.getName().equals(name)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Add new favorite station.
     * @param stationName
     */
    public static void addFavorite(Context ctx, String stationName)
    {
        //Do not allow re-adding
        if(hasFavorite(ctx, stationName)) {
            return;
        }
        
        int index = getFavorites(ctx).size();
        final String fav = PREFS_FAV_PREFIX + index;
        
        setBooleanSetting(ctx, fav, true);
        setSetting(ctx, fav + ".name", stationName);
    }

    public static void removeFavorite(Context context, String name)
    {
        if(!hasFavorite(context, name)) {
            return;
        }
        
        List<Favorite> favorites = getFavorites(context);
        
        boolean hasDeleted = false;
        int highestIndex = -1;
        for(Favorite f : favorites) {
            if(!hasDeleted) {
                if(f.getName().equals(name)) {
                    highestIndex = f.getIndex();
                    Log.v(Tag, "Removing favorite: " + f.getName() + " " + f.getIndex());
                    hasDeleted = true;
                    final String fav = PREFS_FAV_PREFIX + f.getIndex();
                    removeSetting(context, fav);
                    removeSetting(context, fav + ".name");
                }
            } else {
                highestIndex = f.getIndex();
                Log.v(Tag, "Subtracting: " + f.getName() + " " + f.getIndex());
                //Subtract one from index
                f.setIndex(f.getIndex()-1);
                f.persist(context);
            }
        }
        
        //Remove the last one
        if(highestIndex != -1) {
            final String fav = PREFS_FAV_PREFIX + highestIndex;
            removeSetting(context, fav);
            removeSetting(context, fav + ".name");
        }
    }
}
