package se.sandos.android.delayed.scrape;

import android.util.Log;

/**
 * Base class for scrapers
 * @author John Bäckstrand
 *
 */
public abstract class Scraper<T, U> {
	private static final int MAX_DELAY = 1000*60*5;
	private static final int MIN_DELAY = 1000;
	private int retryDelay = MIN_DELAY;

	private static final int DEFAULT_RETRIES = 2;
	
	private static final String Tag = "Scraper"; 
	
	protected ScrapeListener<T, U> mListener = null;
	
	/**
	 * Handler to be run on any result
	 */
	public void setScrapeListener(ScrapeListener<T, U> listener)
	{
		this.mListener = listener;
	}
	
	public abstract void scrapeImpl() throws Exception;
	
	public void scrape()
	{
		scrape(DEFAULT_RETRIES);
	}
	
	/**
	 * Scrape, with retries
	 */
	public void scrape(int retries)
	{
		Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
		int r = 0;
		retryDelay = MIN_DELAY;
		while (true) {
			try {
				Log.v(Tag, "Trying to download, we are " + getClass());
				if(r++ > retries) {
					Log.v(Tag, "Retries exhausted, giving up: " + r);
					break;
				}
			
				scrapeImpl();
				break;
			} catch (Exception e) {
				Log.d(Tag, "Failed scrape: " + e.getMessage(), e);
				retryDelay();
				if(mListener != null) {
					mListener.onRestart();
				}
			}
		}
	}
	
	private void retryDelay() {
		try {
			Thread.sleep(retryDelay);
		} catch (InterruptedException e1) {
		}
		retryDelay *= 2;
		if (retryDelay > MAX_DELAY) {
			retryDelay = MAX_DELAY;
		}
	}

}
