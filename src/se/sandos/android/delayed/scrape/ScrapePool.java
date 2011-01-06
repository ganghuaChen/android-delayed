package se.sandos.android.delayed.scrape;

import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Class for handling stuff in threads.
 * @author John Bäckstrand
 *
 */
public class ScrapePool {
//	private static ExecutorService e = Executors.newFixedThreadPool(1);
	
	static ArrayBlockingQueue<Runnable> queue = new ArrayBlockingQueue<Runnable>(15);
	
	private static ThreadPoolExecutor e = new ThreadPoolExecutor(1,1, 1000, TimeUnit.MILLISECONDS, queue);
	
	public static void addJob(DelayRunnable r)
	{
		e.execute(r);
	}

	
	public static void clearOldJobs()
	{
        // XXX Must take real care here. Remember wake-lock releases AND service-killing is done in background threads!
	    List<Runnable> shutdownNow = e.shutdownNow();
	    queue = new ArrayBlockingQueue<Runnable>(5);
	    e = new ThreadPoolExecutor(1,1, 1000, TimeUnit.MILLISECONDS, queue);
	}
	
	
}
