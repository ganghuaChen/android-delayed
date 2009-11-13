package se.sandos.android.delayed.widget;

import java.lang.reflect.Field;
import java.util.List;

import se.sandos.android.delayed.Delayed;
import se.sandos.android.delayed.R;
import se.sandos.android.delayed.StationActivity;
import se.sandos.android.delayed.TrainEvent;
import se.sandos.android.delayed.prefs.Prefs;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.RemoteViews;

public class DelayedAppWidgetProvider extends AppWidgetProvider
{
    private static String Tag = "DelayedAppWidgetProvider";

    private static long lastUpdate = -1;

    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds)
    {
        Log.v(Tag, "onUpdate " + context + " " + appWidgetManager);

        RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.widget);

        String url = Prefs.getSetting(context, Prefs.PREFS_FAV_URL, null);
        String name = Prefs.getSetting(context, Prefs.PREFS_FAV_NAME, null);

        Log.v(Tag, "URl/name: " + name + "/" + url);
        
        if(name == null || url == null) {
            return;
        }
        
        long lastUp = Prefs.getLongSetting(context, "lastUpdateForStation" + name, -1);
        // Update from DB, if it is updated recently enough
        if (lastUpdate == -1 || lastUp > lastUpdate) {
            lastUpdate = lastUp;

            Log.v(Tag, "Updating due to outdatedness");
            
            List<TrainEvent> events = Delayed.getDb(context).getStationEvents(name);
            int index = 0;
            for (TrainEvent te : events) {
                Log.v(Tag, "Got te: " + te);
                if (index <= 4) {
                    Log.v(Tag, "Setting text: " + index);
                    
                    String delay = te.getDelayed();
                    if(delay != null && !delay.equals("")) {
                        Log.v(Tag, "Adding delay info");
                        rv.setTextViewText(getWidgetId(index, "WidgetDelay"), delay);
                    } else {
                        rv.setTextViewText(getWidgetId(index, "WidgetDelay"), "");
                    }
                    rv.setTextViewText(getWidgetId(index, "WidgetTime"), te.toString());
                }
                index++;
            }
            
            for(;index<=5;index++) {
                rv.setTextViewText(getWidgetId(index, "WidgetDelay"), "");
                rv.setTextViewText(getWidgetId(index, "WidgetTime"), "");
            }

        }

        Intent intent = new Intent("se.sandos.android.delayed.Station", null, context, StationActivity.class);
        intent.putExtra("name", name);
        intent.putExtra("url", url);
        
        Log.v(Tag, "Setting pending intent to name" + name);
        
        PendingIntent pi = PendingIntent.getActivity(context, 0, intent, 0);
        for(int j=0; j<=5; j++) {
            rv.setOnClickPendingIntent(getWidgetId(j, "WidgetTime"), pi);
            rv.setOnClickPendingIntent(getWidgetId(j, "WidgetDelay"), pi);
        }
        
        for (int i = 0; i < appWidgetIds.length; i++) {
            Log.v(Tag, "Updating id " + appWidgetIds[i]);
            
            appWidgetManager.updateAppWidget(appWidgetIds[i], rv);
        }
    }

    private int getWidgetId(int index, String prefix)
    {
        int id = 0;
        try {
            Field f = R.id.class.getDeclaredField(prefix + index);
            Integer i = (Integer) f.get(null);
            id = i.intValue();
        } catch (Exception e) {
            Log.w(Tag, "Something went wrong: " + e);
        }
        return id;
    }
    
}
