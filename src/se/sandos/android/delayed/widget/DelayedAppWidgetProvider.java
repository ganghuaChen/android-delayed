package se.sandos.android.delayed.widget;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import se.sandos.android.delayed.Delayed;
import se.sandos.android.delayed.R;
import se.sandos.android.delayed.StationActivity;
import se.sandos.android.delayed.TrainEvent;
import se.sandos.android.delayed.db.DBAdapter;
import se.sandos.android.delayed.db.Station;
import se.sandos.android.delayed.prefs.Favorite;
import se.sandos.android.delayed.prefs.Prefs;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.widget.RemoteViews;

public class DelayedAppWidgetProvider extends AppWidgetProvider
{
    private static String Tag = "DelayedAppWidgetProvider";

    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds)
    {
        updateWidget(context, appWidgetManager, appWidgetIds);
    }

    public static void updateWidget(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        Log.v(Tag, "onUpdate " + context + " " + appWidgetManager);

        RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.widget);

        Log.v(Tag, "Updating due to outdatedness");
        
        List<Favorite> favorites = Prefs.getFavorites(context);
        List<TrainEvent> events = new ArrayList<TrainEvent>(40);
        DBAdapter db = Delayed.getDb(context);
        String name = ""; 
        for(Favorite f : favorites) {
            if(f.isActive()) {
                for(TrainEvent te : db.getStationEvents(f.getName())) {
                    te.setStation(new Station(f.getName(), null));
                    events.add(te);
                }
                //Just use a random favorite for the click-links for now
                name = f.getName();
            }
        }
        Collections.sort(events);
        
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
                rv.setTextViewText(getWidgetId(index, "WidgetTime"), te.toString() + "|" + te.getTrack() + " "+ te.getStation().getName() + "->" + te.getDestination());
            }
            index++;
        }
        
        for(;index<=5;index++) {
            rv.setTextViewText(getWidgetId(index, "WidgetDelay"), "");
            rv.setTextViewText(getWidgetId(index, "WidgetTime"), "");
        }

        Intent intent = new Intent("se.sandos.android.delayed.Station", null, context, StationActivity.class);
        intent.setData(Uri.fromParts("delayed", "trainstation", name));
        intent.putExtra("name", (String)null);
        intent.putExtra("url", (String)null);
        
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

    private static int getWidgetId(int index, String prefix)
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
