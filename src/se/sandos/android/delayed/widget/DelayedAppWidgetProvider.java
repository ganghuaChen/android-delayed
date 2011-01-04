package se.sandos.android.delayed.widget;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import se.sandos.android.delayed.Delayed;
import se.sandos.android.delayed.R;
import se.sandos.android.delayed.StationActivity;
import se.sandos.android.delayed.StationListActivity;
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
import android.view.View;
import android.widget.RemoteViews;

public class DelayedAppWidgetProvider extends AppWidgetProvider
{
    private static String Tag = "DelayedAppWidgetProvider";

    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds)
    {
        RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.widget);

        List<Favorite> favorites = Prefs.getFavorites(context);
        List<TrainEvent> events = new ArrayList<TrainEvent>(40);
        DBAdapter db = Delayed.getDb(context);
        String name = ""; 
        for(Favorite f : favorites) {
//            Log.v(Tag, "Favorite: " + f.getName());
            if(f.isActive()) {
                for(TrainEvent te : db.getStationEvents(f.getName())) {
//                    Log.v(Tag, "TE: " + te);
                    te.setStation(new Station(f.getName(), null));
                    if(Favorite.isFavoriteTarget(favorites, te, db)) {
                        events.add(te);
                    }
                }
                //XXX: Just use a random favorite for the click-links for now
                name = f.getName();
            }
        }
        Collections.sort(events);
        
        int index = 0;
        for (TrainEvent te : events) {
            //Log.v(Tag, "Got te: " + te);
            if (index <= 4) {
                //Log.v(Tag, "Setting text: " + index);
                
                String extra = te.getExtra();
               
                if(extra != null && !extra.equals(""))
                {
                    rv.setViewVisibility(getWidgetId(index, "WidgetStrike"), View.VISIBLE);
                }
                else
                {
                    rv.setViewVisibility(getWidgetId(index, "WidgetStrike"), View.GONE);
                }
                
                String delay = te.getDelayed();
                if(delay != null && !delay.equals("")) {
                    //Log.v(Tag, "Adding delay info");
                    rv.setTextViewText(getWidgetId(index, "WidgetDelay"), delay);
                } else {
                    rv.setTextViewText(getWidgetId(index, "WidgetDelay"), "");
                }
                rv.setTextViewText(getWidgetId(index, "WidgetTime"), te.toString() + "|" + te.getTrack() + " "+ te.getStation().getName() + "->" + te.getDestination());
                rv.setViewVisibility(getWidgetId(index, "WidgetTime"), View.VISIBLE);
                rv.setViewVisibility(getWidgetId(index, "WidgetDelay"), View.VISIBLE);
                index++;
            }
        }
        
        if(index == 0) {
            for(;index<=5;index++) {
                rv.setViewVisibility(getWidgetId(index, "WidgetDelay"), View.GONE);
                rv.setViewVisibility(getWidgetId(index, "WidgetTime"), View.GONE);
            }
            //Empty widget, please set sth
            rv.setViewVisibility(R.id.EmptyPlaceHolder, View.VISIBLE);
            rv.setTextViewText(R.id.EmptyPlaceHolder, "Inga tågtider\n" + TrainEvent.df.format(new Date()));
        } else {
            rv.setViewVisibility(R.id.EmptyPlaceHolder, View.GONE);
            for(;index<=5;index++) {
                rv.setViewVisibility(getWidgetId(index, "WidgetDelay"), View.GONE);
                rv.setViewVisibility(getWidgetId(index, "WidgetTime"), View.GONE);
            }
        }

        Intent mainIntent = new Intent("android.intent.action.MAIN", null, context, StationListActivity.class);
        PendingIntent mainPi = PendingIntent.getActivity(context, 1, mainIntent, 0);
        rv.setOnClickPendingIntent(R.id.EmptyPlaceHolder, mainPi);
        
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
            int id = appWidgetIds[i];
            Log.v(Tag, "Updating id " + id);
            Prefs.addWidget(context, id);
            
            appWidgetManager.updateAppWidget(id, rv);
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
    
    public void onReceive(Context context, Intent intent)
    {
        super.onReceive(context, intent);

        Log.v(Tag, "" + intent.getAction() + " " + intent.getDataString());
        
        if(intent.getAction().equals("se.sandos.android.delayed.widgetUpdate")) {
            onUpdate(context, AppWidgetManager.getInstance(context), intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS)); 
        }
        
        //Handle deletions ourselfs due to bug in 1.5/1.6
        if(intent.getAction().equals(AppWidgetManager.ACTION_APPWIDGET_DELETED)) {
            int id = intent.getIntExtra("appWidgetId", -1);
            
            Log.v(Tag, "Id: " + id);
            
            Prefs.removeWidget(context, id);
        }
    }
}
