package se.sandos.android.delayed.scrape;

public interface ScrapeListener<T, U> {
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
}
