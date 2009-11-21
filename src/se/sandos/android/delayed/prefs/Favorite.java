package se.sandos.android.delayed.prefs;

import android.content.Context;

public class Favorite {

    private String name;
    private int index;
    private boolean active;
    
    public boolean isActive() {
        return active;
    }
    public void setActive(boolean active) {
        this.active = active;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public int getIndex() {
        return index;
    }
    public void setIndex(int index) {
        this.index = index;
    }
    
    public void persist(Context ctx) {
        final String fav = Prefs.PREFS_FAV_PREFIX + index;
        Prefs.setBooleanSetting(ctx, fav, active);
        Prefs.setSetting(ctx, fav + ".name", name);
    }
}
