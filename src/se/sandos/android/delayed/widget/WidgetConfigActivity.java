package se.sandos.android.delayed.widget;

import java.util.List;

import se.sandos.android.delayed.R;
import se.sandos.android.delayed.prefs.Prefs;
import se.sandos.android.delayed.prefs.Widget;
import se.sandos.android.delayed.scrape.ScrapeService;
import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.widget.RadioButton;

public class WidgetConfigActivity extends Activity
{
    private static final String Tag = "WidgetConfigActivity";

    private Widget widget;
    
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        Log.v(Tag, "Widget:" + getIntent().getExtras().getInt("widgetId"));

        // Need to refresh widgets, to remove any buttons from them!
        ScrapeService.scheduleAllWidgetUpdate(getApplicationContext());

        setContentView(R.layout.widgetcfg);

        List<Widget> widgets = Prefs.getWidgets(getApplicationContext());
        for (Widget widget : widgets)
        {
            if(widget.getId() == getIntent().getExtras().getInt("widgetId"))
            {
                this.widget = widget;
                
                if(widget.getClickSetting() == Widget.CLICK_BUTTONS)
                {
                    RadioButton rb = (RadioButton) findViewById(R.id.rdioButtons);
                    rb.setChecked(true);
                }
                if(widget.getClickSetting() == Widget.CLICK_REFRESH)
                {
                    RadioButton rb = (RadioButton) findViewById(R.id.rdioRefresh);
                    rb.setChecked(true);
                }
                if(widget.getClickSetting() == Widget.CLICK_SHOW)
                {
                    RadioButton rb = (RadioButton) findViewById(R.id.rdioOpen);
                    rb.setChecked(true);
                }
            }
        }
    }
    
    public void onPause()
    {
        super.onPause();
        
        RadioButton open = (RadioButton) findViewById(R.id.rdioOpen);
        RadioButton refresh = (RadioButton) findViewById(R.id.rdioRefresh);
        
        if(open.isChecked())
        {
            widget.setClickSetting(Widget.CLICK_SHOW);
        }
        else if(refresh.isChecked())
        {
            widget.setClickSetting(Widget.CLICK_REFRESH);
        }
        else
        {
            widget.setClickSetting(Widget.CLICK_BUTTONS);
        }
        
        Prefs.saveWidget(getApplicationContext(), widget);
    }
}
