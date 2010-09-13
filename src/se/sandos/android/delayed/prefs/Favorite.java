package se.sandos.android.delayed.prefs;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import se.sandos.android.delayed.TrainEvent;

import android.content.Context;
import android.util.Log;

public class Favorite {
    private final static String Tag = "Favorite";
    
    private String name;
    private int index;
    private boolean active;
    private boolean otherFavorites;
    
    private List<String> targets = new ArrayList<String>();
    private Set<String> targetSet = new HashSet<String>();
    
    public List<String> getTargets() {
        return new ArrayList<String>(targets);
    }
    
    public void addTarget(String target)
    {
        targets.add(target);
        targetSet.add(target);
    }
    
    public void setTargets(List<String> targets) {
        this.targets = targets;
        
        Log.v(Tag, "New target list has " + targets.size());
        targetSet.clear();
        for(String t : targets) {
            targetSet.add(t);
        }
        Log.v(Tag, "New target set has " + targetSet.size());
    }
    public boolean targetOtherFavorites() {
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
    
    public boolean filter(String name)
    {
        if(!targetSet.contains(name)) {
            Log.v(Tag ,"Did not contain " + name + "<");
        }
        for(String s : targets) {
            Log.v(Tag, "Contains " + s + "<");
        }
        for(Iterator<String> i = targetSet.iterator(); i.hasNext();) {
            Log.v(Tag, "Contains also " + i.next());
        }
        return targetSet.contains(name);
    }
    
    public static boolean isFavoriteTarget(List<Favorite> favorites, TrainEvent te) {
        for(Favorite f : favorites) {
            if(te.getStation().getName().equals(f.getName())) {
                if(f.isActive() && f.filter(te.getDestination())) {
                    return true;
                }
                
                if(f.targetOtherFavorites()) {
                    //Just assume we got all favorites
                    for(Favorite fav : favorites) {
                        if(te.getDestination().equals(fav.getName())) {
                            return true;
                        }
                    }
                } else {
                    //If nothing set at all, accept anything
                    if(f.getTargets().size() == 0) {
                        Log.v(Tag, "Returning true since nothing is set for " + f.getName());
                        return true;
                    }
                }
                
                Log.v(Tag, "" + f.getName() + " did not like");
            }
        }
        
        return false;
    }
}
