package se.sandos.android.delayed.widget;

import se.sandos.android.delayed.R;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.util.Log;
import android.widget.RemoteViews;

public class DelayedAppWidgetProvider extends AppWidgetProvider {
	private static String Tag = "DelayedAppWidgetProvider";
	
	 public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		 Log.v(Tag, "onUpdate " + context + " " + appWidgetManager);

		 RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.widget);
		 rv.setTextViewText(R.id.WidgetText, "majs " + System.currentTimeMillis());

		 for(int i=0; i<appWidgetIds.length; i++) {
			 Log.v(Tag, "Updating id " + appWidgetIds[i]);
			 appWidgetManager.updateAppWidget(appWidgetIds[i], rv);
		 }
	 }
}
