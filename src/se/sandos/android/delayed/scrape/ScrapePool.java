package se.sandos.android.delayed.scrape;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import android.util.Log;

/**
 * Class for handling stuff in threads.
 * @author John Bäckstrand
 *
 */
public class ScrapePool {
	private static final String TAG = "ScrapePool";

    static ArrayBlockingQueue<Runnable> queue = new ArrayBlockingQueue<Runnable>(10);
	
	private static ThreadPoolExecutor e = new ThreadPoolExecutor(1,1, 1000, TimeUnit.MILLISECONDS, queue);
	
	public static void addJob(DelayRunnable r)
	{
	    if(e.getQueue().remainingCapacity() < 5)
	    {
	        for(Runnable rr : e.getQueue())
	        {
	            DelayRunnable dr = (DelayRunnable) rr;
	            if(dr.getImportance().equals(DelayRunnable.Importance.NORMAL))
	            {
	                Log.v(TAG, "Removed task");
	                e.remove(dr);
	            }
	        }
	    }
	    
		e.execute(r);
	}
}
