package se.sandos.android.delayed.db;

public class Station {
	private String mName;
	private String mUrl;
	
	public Station(String name, String url)
	{
		mName = name;
		mUrl = url;
	}	
	
	public String getmName() {
		return mName;
	}

	public String getmUrl() {
		return mUrl;
	}
}
