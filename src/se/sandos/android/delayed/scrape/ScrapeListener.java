package se.sandos.android.delayed.scrape;

public interface ScrapeListener<T, U> {
	public static final int MSG_STATUS = 10;
	
	/**
	 * Partial result from scrape
	 * @param result
	 */
	public void onPartialResult(T result);
	
	/**
	 * This means all the partial results should be discarded
	 * @param result
	 */
	public void onRestart();
	
	/**
	 * Scrape was successfully finished
	 * @param result
	 */
	public void onFinished(U result);
	
	/**
	 * A status message about progress
	 * @param status
	 */
	public void onStatus(String status);
	
	/**
	 * Called when the scrape runs out of retries, and fails completely
	 */
	public void onFail();
}
