package se.sandos.android.delayed.widget;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;

public class DelayedAppWidgetProvider41 extends AppWidgetProvider
{
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds)
    {
        DelayedAppWidgetProvider.updateWidget(context, appWidgetManager, appWidgetIds);
    }
}
