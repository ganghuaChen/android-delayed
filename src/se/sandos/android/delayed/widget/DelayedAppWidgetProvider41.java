package se.sandos.android.delayed.widget;

import se.sandos.android.delayed.R;

/**
 * 4x1 widget
 *
 */
public class DelayedAppWidgetProvider41 extends DelayedAppWidgetProvider
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
