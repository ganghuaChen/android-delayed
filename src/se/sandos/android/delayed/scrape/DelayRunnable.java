package se.sandos.android.delayed.scrape;

/**
 * This class is used to "tag" background tasks that are important. We try not killing these.
 * @author John Bäckstrand
 *
 */
public abstract class DelayRunnable implements Runnable
{
    public enum Importance
    {
        NORMAL, HIGH;
    }
    
    private Importance importance;
    
    public DelayRunnable(Importance imp)
    {
        importance = imp;
    }
    
    public Importance getImportance()
    {
        return importance;
    }
}
