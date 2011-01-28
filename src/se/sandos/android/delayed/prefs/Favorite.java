package se.sandos.android.delayed.prefs;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Delayed;

import se.sandos.android.delayed.TrainEvent;
import se.sandos.android.delayed.db.DBAdapter;

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
    
    public Set<String> getTargetSet()
    {
        return targetSet;
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
    
    public boolean filter(DBAdapter db, TrainEvent te)
    {
        // Check if train passes any of this favorites' targets
        for(String s : this.targetSet)
        {
            if (db.checkIfPasses(s, te.getNumber(), te.getDepartureDate()))
            {
                Log.v(Tag, "Adding train " + te.getNumber() + " since it passes " + getName());
                return true;
            }
        }
        
        return targetSet.contains(te.getDestination());
    }
    
    public static boolean isFavoriteTarget(List<Favorite> favorites, TrainEvent te, DBAdapter db) {
        for(Favorite f : favorites) {
            if(!f.isActive())
            {
                continue;
            }
            
            if(te.getStation().getName().equals(f.getName())) {
                if(f.filter(db, te)) {
                    return true;
                }
                
                if(f.targetOtherFavorites()) {
                    //Just assume we got all favorites
                    for(Favorite fav : favorites) {
                        //exclude ourselves, equals will work here
                        if(fav.equals(f))
                        {
                            continue;
                        }
                        
                        if(te.getDestination().equals(fav.getName())) {
                            return true;
                        }
                        
                        // Check if train passes this favorite!
                        if (db.checkIfPasses(fav.getName(), te.getNumber(), te.getDepartureDate()))
                        {
                            Log.v(Tag, "Adding train " + te.getNumber() + " since it passes " + fav.getName());
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
                
//                Log.v(Tag, "" + f.getName() + " did not like");
            }
        }
        
        return false;
    }
}
