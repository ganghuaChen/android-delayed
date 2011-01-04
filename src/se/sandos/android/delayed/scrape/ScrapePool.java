package se.sandos.android.delayed.scrape;

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
	
	final static ArrayBlockingQueue<Runnable> queue = new ArrayBlockingQueue<Runnable>(5);
	
	private static ThreadPoolExecutor e = new ThreadPoolExecutor(1,1, 1000, TimeUnit.MILLISECONDS, queue);
	
	public static void addJob(Runnable r)
	{
		e.execute(r);
	}
	
	
}
