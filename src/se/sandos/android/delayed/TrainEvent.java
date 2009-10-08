package se.sandos.android.delayed;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import se.sandos.android.delayed.db.Station;

/**
 * Arrival/departure from one particular station
 * @author johba
 *
 */
public class TrainEvent {
	private final static String Tag = "TrainEvent";
	
	private String altDest;
	private Station destination;
	private Station station;
	private int id = -1;
	private String track;
	private Date arrival;
	private Date departure;
	private DateFormat df = SimpleDateFormat.getTimeInstance(SimpleDateFormat.SHORT, java.util.Locale.GERMANY);
	private String url;
	
	private boolean delimiterSeen = false;
	private boolean done = false;
	
	public TrainEvent()
	{
	}
	
	public int getNumber()
	{
		return id;
	}
	
	public boolean hasProperDest()
	{
		return destination != null;
	}
	
	public String getDestination()
	{
		if(destination != null) {
			return destination.getName();
		}
		
		if(altDest != null) {
			return altDest;
		}
		
		return " -- ";
	}
	
	public void setAltDest(String name)
	{
		altDest = name;
	}
	
	public String getTrack()
	{
		if(track != null) {
			return track;
		}
		
		return "-";
	}
	
	public TrainEvent(Station s)
	{
		station = s;
	}
	
	public String getUrl()
	{
		return url;
	}

	public void setArrival(Date arrival) {
		this.arrival = arrival;
	}
	
	public void setDestination(Station dest) {
		destination = dest;
	}
	
	public void setId(int id)
	{
		this.id = id;
	}
	
	public void setUrl(String url)
	{
		this.url = url;
	}
	
	public void setTrack(String track)
	{
		this.track = track;
	}
	
	public boolean isParsed()
	{
		return done;
	}
	
	public String toString()
	{
		StringBuffer sb = new StringBuffer();
		if(arrival != null) {
			sb.append(df.format(arrival));
		}
		return sb.toString();
	}

}
