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

    @Override
    public String getControlsPrefix()
    {
        return "Small";
    }
    
    @Override
    protected int ourLayout()
    {
        return R.layout.widget11;
    }
}
