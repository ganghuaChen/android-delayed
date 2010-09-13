package se.sandos.android.delayed.db;

/**
 * Trains are identified by url
 * @author John Bäckstrand
 *
 */
public class Train {
	private String url;
	private String number;
	
	public Train(String number, String url)
	{
		this.url = url;
		this.number = number;
	}
}
