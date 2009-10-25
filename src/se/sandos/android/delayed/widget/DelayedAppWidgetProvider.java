package se.sandos.android.delayed.widget;

import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class DelayedAppWidgetProvider extends BroadcastReceiver {
	private static String Tag = "DelayedAppWidgetProvider";
	
	 public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		 Log.v(Tag, "asds");
	 }

	@Override
	public void onReceive(Context arg0, Intent arg1) {
		 Log.v(Tag, "onReceive " + arg0 + " " + arg1);
		
	}
}
