package se.sandos.android.delayed.widget;

import java.lang.reflect.Field;

import se.sandos.android.delayed.R;
import android.util.Log;

/**
 * 1x1 widget
 *
 */
public class DelayedAppWidgetProvider11 extends DelayedAppWidgetProvider
{
    private static final String Tag = "DelayedAppWidgetProvider";

    protected int ourLayout()
    {
        return R.layout.widget11;
    }
    
    protected static int getWidgetId(int index, String prefix)
    {
        int id = 0;
        try {
            Field f = R.id.class.getDeclaredField("Small" + prefix + index);
            Integer i = (Integer) f.get(null);
            id = i.intValue();
        } catch (Exception e) {
            Log.w(Tag, "Something went wrong: " + e);
        }
        return id;
    }
    
}
