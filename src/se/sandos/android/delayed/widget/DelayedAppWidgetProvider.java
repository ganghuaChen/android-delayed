package se.sandos.android.delayed.widget;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import se.sandos.android.delayed.Delayed;
import se.sandos.android.delayed.R;
import se.sandos.android.delayed.StationActivity;
import se.sandos.android.delayed.StationListActivity;
import se.sandos.android.delayed.TrainEvent;
import se.sandos.android.delayed.db.DBAdapter;
import se.sandos.android.delayed.db.Station;
import se.sandos.android.delayed.prefs.Favorite;
import se.sandos.android.delayed.prefs.Prefs;
import se.sandos.android.delayed.prefs.Widget;
import se.sandos.android.delayed.scrape.ScrapeService;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

abstract public class DelayedAppWidgetProvider extends AppWidgetProvider
{
    private static String Tag = "DelayedAppWidgetProvider";

    abstract protected int ourLayout();
    
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds)
    {
        int[] ourIds = appWidgetManager.getAppWidgetIds(new ComponentName(context, this.getClass()));
        Set<Integer> ids = new HashSet<Integer>();
        for (int i = 0; i < ourIds.length; i++) {
            ids.add(ourIds[i]);
        }
        boolean hasSomethingTodo = false;
        for(int i=0; i<appWidgetIds.length; i++)
        {
            if(!ids.contains(appWidgetIds[i]))
            {
                Log.v(Tag, "Id is not for us: " + appWidgetIds[i]);
                appWidgetIds[i] = -1;
            }
            else
            {
                hasSomethingTodo = true;
            }
        }
        
        if(!hasSomethingTodo)
        {
            return;
        }
        
        RemoteViews rv = new RemoteViews(context.getPackageName(), ourLayout());

        List<TrainEvent> events = new ArrayList<TrainEvent>(40);
        String name = "";
        DBAdapter db = Delayed.getDb(context);
        List<Favorite> favorites = Prefs.getFavorites(context);
        for(Favorite f : favorites) {
//            Log.v(Tag, "Favorite: " + f.getName());
            if(f.isActive()) {
                Log.v(Tag, "Calling DB from widget " + appWidgetIds[0] + " for fav " + f.getName());
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
                rv.setViewVisibility(getWidgetId(index, "WidgetStrike"), View.GONE);
            }
            //Empty widget, please set sth
            rv.setViewVisibility(R.id.EmptyPlaceHolder, View.VISIBLE);
            rv.setTextViewText(R.id.EmptyPlaceHolder, "Inga tågtider\n" + TrainEvent.df.format(new Date()));
        } else {
            rv.setViewVisibility(R.id.EmptyPlaceHolder, View.GONE);
            for(;index<=5;index++) {
                rv.setViewVisibility(getWidgetId(index, "WidgetDelay"), View.GONE);
                rv.setViewVisibility(getWidgetId(index, "WidgetTime"), View.GONE);
                rv.setViewVisibility(getWidgetId(index, "WidgetStrike"), View.GONE);
            }
        }

        for (int i = 0; i < appWidgetIds.length; i++) {
            int id = appWidgetIds[i];
            
            removeButtons(context, id);
            
            if(id != -1)
            {
                Log.v(Tag, "Updating id " + id);
                
                Widget w = Prefs.findWidget(context, id);
                
                if(getControlsPrefix().equals("") && w.getClickSetting() != Widget.CLICK_SHOW)
                {
                    Intent intent = new Intent("widgetclick", null, context, DelayedAppWidgetProvider41.class);
                    intent.putExtra("widgetid", id);
                    intent.setData(Uri.fromParts("delayed", "aSA", "frag"));
                    PendingIntent pi = PendingIntent.getBroadcast(context, id, intent, 0);
                    
                    //Here we do re-route to enable other functionality
                    rv.setOnClickPendingIntent(getWidgetId("WidgetLayout"), pi);
                    
                    Log.v(Tag, "Set onclick rerouted");
                }
                else
                {
                    Intent intent = new Intent("se.sandos.android.delayed.Station", null, context, StationActivity.class);
                    intent.setData(Uri.fromParts("delayed", "trainstation", name));
                    intent.putExtra("name", (String)null);
                    intent.putExtra("url", (String)null);
                    
                    PendingIntent pi = PendingIntent.getActivity(context, id, intent, 0);
                    
                    rv.setOnClickPendingIntent(getWidgetId("WidgetLayout"), pi);
                }
                
                Prefs.addWidget(context, id);
                
                appWidgetManager.updateAppWidget(id, rv);
            }
        }
    }

    protected int getWidgetId(int index, String prefix)
    {
        int id = 0;
        try {
            Field f = R.id.class.getDeclaredField(getControlsPrefix() + prefix + index);
            Integer i = (Integer) f.get(null);
            id = i.intValue();
        } catch (Exception e) {
            Log.w(Tag, "Something went wrong: " + e);
        }
        return id;
    }

    protected int getWidgetId(String prefix)
    {
        int id = 0;
        try {
            Field f = R.id.class.getDeclaredField(getControlsPrefix() + prefix);
            Integer i = (Integer) f.get(null);
            id = i.intValue();
        } catch (Exception e) {
            Log.w(Tag, "Something went wrong: " + e);
        }
        return id;
    }
    
    
    abstract public String getControlsPrefix();
    
    public void onReceive(Context context, Intent intent)
    {
        super.onReceive(context, intent);

        Log.v(Tag, "" + intent.getAction() + " " + intent.getDataString() + " " + intent);
        
        if(intent.getAction().equals("se.sandos.android.delayed.widgetUpdate")) {
            Log.v(Tag, "Our own widget update " + this.getClass());
            onUpdate(context, AppWidgetManager.getInstance(context), intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS)); 
        }

        if(intent.getAction().equals("widgetrefresh"))
        {
            refresh(context, intent.getExtras().getInt("widgetid"));
        }
        
        if(intent.getAction().equals("widgetclick"))
        {
            Log.v(Tag, "Someone clicked the widget! " );
            
            onWidgetClick(context, intent.getExtras().getInt("widgetid"));
        }
        
        //Handle deletions ourselves due to bug in 1.5/1.6
        if(intent.getAction().equals(AppWidgetManager.ACTION_APPWIDGET_DELETED)) {
            int id = intent.getIntExtra("appWidgetId", -1);
            
            Log.v(Tag, "Id: " + id);
            
            Prefs.removeWidget(context, id);
        }
    }

    private void refresh(Context context, int widgetId)
    {
        removeButtons(context, widgetId);
        ScrapeService.runOnceNowForced(context);
    }

    private void removeButtons(Context ctx, int widgetId)
    {
        RemoteViews rv = new RemoteViews(ctx.getPackageName(), ourLayout());
        rv.setViewVisibility(R.id.refresh, View.INVISIBLE);
        rv.setViewVisibility(R.id.config, View.INVISIBLE);
        rv.setViewVisibility(R.id.open, View.INVISIBLE);
        
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(ctx);                       
        appWidgetManager.updateAppWidget(widgetId, rv);
    }

    private void onWidgetClick(Context ctx, int widgetId)
    {
        List<Widget> widgets = Prefs.getWidgets(ctx);
        for (Widget widget : widgets)
        {
            if(widget.getId() == widgetId)
            {
                int click = widget.getClickSetting();
                if(click == Widget.CLICK_BUTTONS)
                {
                    //Toggle buttons
                    if(getControlsPrefix().equals(""))
                    {
                        RemoteViews rv = new RemoteViews(ctx.getPackageName(), ourLayout());
                        rv.setViewVisibility(R.id.refresh, View.VISIBLE);
                        rv.setViewVisibility(R.id.config, View.VISIBLE);
                        rv.setViewVisibility(R.id.open, View.VISIBLE);
                        
                        Intent refresh = new Intent("widgetrefresh", null, ctx, DelayedAppWidgetProvider41.class);
                        refresh.putExtra("widgetid", widgetId);
                        refresh.setData(Uri.fromParts("delayed", "refresh", "frag"));
                    
                        PendingIntent pi = PendingIntent.getBroadcast(ctx, 0, refresh, 0);
                        rv.setOnClickPendingIntent(R.id.refresh, pi);

                        Intent open = new Intent("se.sandos.android.delayed.Station", null, ctx, StationActivity.class);
                        
                        String stationName = "";
                        List<Favorite> favorites = Prefs.getFavorites(ctx);
                        for(Favorite f : favorites) {
                            if(f.isActive()) {
                                stationName = f.getName();
                            }
                        }
                        
                        open.setData(Uri.fromParts("delayed", "trainstation", stationName));
                        open.putExtra("name", (String)null);
                        open.putExtra("url", (String)null);
                        
                        PendingIntent pi2 = PendingIntent.getActivity(ctx, widgetId, open, 0);
                        rv.setOnClickPendingIntent(R.id.open, pi2);

                        Intent cfg = new Intent("cfg", null, ctx, WidgetConfigActivity.class);
                        cfg.setData(Uri.fromParts("delayed", "trainstation", ""));
                        cfg.putExtra("widgetId", widgetId);

                        PendingIntent pi3 = PendingIntent.getActivity(ctx, widgetId, cfg, 0);
                        rv.setOnClickPendingIntent(R.id.config, pi3);
                        
                        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(ctx);                       
                        appWidgetManager.updateAppWidget(widgetId, rv);
                    }
                }
                else if(click == Widget.CLICK_REFRESH)
                {
                    refresh(ctx, widgetId);
                }
                else if(click == Widget.CLICK_SHOW)
                {
                    String station = null;
                    List<Favorite> favorites = Prefs.getFavorites(ctx);
                    for(Favorite f : favorites) {
                        if(f.isActive()) {
                            station = f.getName();
                        }
                    }
                    
                    Intent intent = new Intent("se.sandos.android.delayed.Station", null, ctx, StationActivity.class);
                    intent.setData(Uri.fromParts("delayed", "trainstation", station));
                    intent.putExtra("name", (String)null);
                    intent.putExtra("url", (String)null);
                    
                    ctx.startActivity(intent);
                }
            }
        }
    }
}
