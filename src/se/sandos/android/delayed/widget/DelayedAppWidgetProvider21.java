package se.sandos.android.delayed.widget;

import se.sandos.android.delayed.R;

/**
 * 2x1 widget
 */
public class DelayedAppWidgetProvider21 extends DelayedAppWidgetProvider
{
    @Override
    protected int ourLayout()
    {
        return R.layout.widget;
    }

    @Override
    public String getControlsPrefix()
    {
        return "";
    }    
}
