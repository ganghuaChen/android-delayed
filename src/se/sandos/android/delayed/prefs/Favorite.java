package se.sandos.android.delayed.prefs;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;

public class Favorite {

    private String name;
    private int index;
    private boolean active;
    private boolean otherFavorites;
    
    List<String> targets = new ArrayList<String>();
    
    public List<String> getTargets() {
        return targets;
    }
    public void setTargets(List<String> targets) {
        this.targets = targets;
    }
    public boolean isOtherFavorites() {
        return otherFavorites;
    }
    public void setOtherFavorites(boolean otherFavorites) {
        this.otherFavorites = otherFavorites;
    }
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
        Prefs.setBooleanSetting(ctx, fav + ".otherfavs", otherFavorites);
        
        int index = 0;
        for(String favorite : targets) {
            Prefs.setSetting(ctx, fav + ".targets." + index++, favorite);
        }
        Prefs.removeSetting(ctx, fav + ".targets." + index);
    }
}
