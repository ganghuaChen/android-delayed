package se.sandos.android.delayed.scrape;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Class for handling stuff in threads.
 * @author John Bäckstrand
 *
 */
public class ScrapePool {
	private static ExecutorService e = Executors.newFixedThreadPool(1);
	
	public static void addJob(Runnable r)
	{
		e.execute(r);
	}
	
	
}
