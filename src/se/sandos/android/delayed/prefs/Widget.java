package se.sandos.android.delayed.prefs;

public class Widget {

    private int index;
    //Setting for what to do when clicking this widget
    private int clickSetting;
    public static int CLICK_BUTTONS = 0;
    public static int CLICK_REFRESH = 1;
    public static int CLICK_SHOW = 2;
    
    public int getClickSetting()
    {
        return clickSetting;
    }

    public void setClickSetting(int clickSetting)
    {
        this.clickSetting = clickSetting;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }
    
    private int id;
    
    public int getId() {
        return id;
    }

    public Widget(int id)
    {
        this.id = id;
    }
    
}
