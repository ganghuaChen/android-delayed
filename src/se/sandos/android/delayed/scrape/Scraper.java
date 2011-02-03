package se.sandos.android.delayed.scrape;

import java.net.UnknownHostException;
import java.util.Random;

import android.util.Log;

/**
 * Base class for scrapers
 * @author John Bäckstrand
 *
 */
public abstract class Scraper<T, U> {
	private static final double GLOBAL_FAILURE_EXPONENT = 1.4;
    private static final int BASE_GLOBAL_FAILURE_DELAY = 1000;
    private static final int MAX_DELAY = 1000*60*5;
	private static final int MIN_DELAY = 1000;
	private int retryDelay = MIN_DELAY;

	//Limit total load on servers that we poll in case of failures
    // This does NOT limit load in non-error situations, although the built-in delay is started only after a scrape is
    // done
	private static long globalNumberOfFailures = 0;
	private static long lastFailureTime = -1;
	
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
				if(++r >= retries) {
					Log.v(Tag, "Retries exhausted, giving up: " + r);
					if(mListener != null)
					{
					    mListener.onFail();
					}
					break;
				}
			
				//Limit load before even trying
				if(globalNumberOfFailures > 0)
				{
				    globalDelay();
				}
				scrapeImpl();
				globalNumberOfFailures = 0;
				break;
			} catch (Exception e) {
			    //Here, we ignore EVERY error that did not "hit" the webserver
			    if(!(e instanceof UnknownHostException))
			    {
    			    globalNumberOfFailures++;
    			    lastFailureTime = System.currentTimeMillis();
			    }
			    else
			    {
			        globalNumberOfFailures = 0;
			        lastFailureTime = -1;
			    }
				Log.d(Tag, "Failed scrape: " + e.getMessage(), e);
				retryDelay();
				if(mListener != null) {
					mListener.onStatus(e.getMessage() + " retry:" + r + "/" + retries);
					mListener.onRestart();
				}
			}
		}
	}
	
	private void globalDelay()
    {
	    Log.v(Tag, "Failures " + globalNumberOfFailures);
	    long diff = -1;
	    if(lastFailureTime != -1)
	    {
	        diff = System.currentTimeMillis() - lastFailureTime;
	    }
	    //delay is now a "multiplier" for delays, we convert it to milliseconds last
	    float delay = (float) Math.pow(GLOBAL_FAILURE_EXPONENT, globalNumberOfFailures);
	    Random r = new Random();
	    //Randomization
	    delay += r.nextFloat() * delay;
	    
	    //Convert multiplier to milliseconds
	    delay *= BASE_GLOBAL_FAILURE_DELAY;
	    
	    //Take the elapsed time since into account
	    if(diff != -1)
	    {
    	    if(diff >= delay)
    	    {
    	        delay = 0;
    	    }
    	    else
    	    {
    	        delay -= diff;
    	    }
	    }
	    
	    Log.v(Tag, "Sleeping " + delay + " ms for global load limiting");
	    try {
	        Thread.sleep((long) delay);
	    }
	    catch(InterruptedException e){}
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
