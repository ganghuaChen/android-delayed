package se.sandos.android.delayed;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import se.sandos.android.delayed.db.Station;
import android.util.Log;

/**
 * Arrival/departure from one particular station
 * @author johba
 *
 */
public class TrainEvent implements Comparable<TrainEvent> {
	private final static String Tag = "TrainEvent";
	
	private String altDest;
	private Station destination;
	private Station station;

	private String number = "";
	private String track;
	private Date departure;
	private Date delayed;
	//private Date arrival;
	public static DateFormat df = SimpleDateFormat.getTimeInstance(SimpleDateFormat.SHORT, java.util.Locale.GERMANY);
	private String url;
	private StringBuffer sb;
	
	public StringBuffer getStringBuffer()
	{
		if(sb == null) {
			sb = new StringBuffer();
		}
		return sb;
	}
	
	public void setStation(Station s)
	{
	    station = s;
	}
	
	public String getNumber()
	{
		return number;
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
	
	public boolean hasTrack()
	{
		return track != null;
	}
	
	public boolean hasExtra()
	{
		return (sb != null && sb.toString().length() > 0);
	}
	
	public String getExtra()
	{
		if(sb == null) {
			sb = new StringBuffer();
		}
		
		return sb.toString();
	}
	
	public String getTrack()
	{
		if(track != null) {
			return track;
		}
		
		return "-";
	}
	
	public Date getDelayedDate() {
		return this.delayed;
	}
	
	public Date getDepartureDate() {
		return this.departure;
	}
	
	public String getDelayed()
	{
		StringBuffer sb = new StringBuffer();
		if(delayed != null) {
			sb.append(df.format(delayed));
		}
		return sb.toString();
	}
	
	public TrainEvent(Station s)
	{
		station = s;
	}
	
	public String getUrl()
	{
		return url;
	}

	public void setDeparture(Long departure) {
		this.departure = new Date(departure);
	}

    public void setDeparture(Date departure)
    {
        this.departure = departure;
    }
	
	public void setDelayed(Date delayed) {
		this.delayed = delayed;
	}

    public void setDelayed(Long delayed)
    {
        this.delayed = new Date(delayed);
    }
	
	public void setDestination(Station dest) {
		destination = dest;
	}
	
	public void setNumber(String id)
	{
		this.number = id;
	}
	
	public void setUrl(String url)
	{
		this.url = url;
	}
	
	public void setTrack(String track)
	{
		this.track = track;
	}
	
	public String toString()
	{
		StringBuffer sb = new StringBuffer();
		if(departure != null) {
			sb.append(df.format(departure));
		}
		return sb.toString();
	}

	public Station getStation() {
		return station;
	}

	public void setDestinationFromString(String dest) {
		String url = Delayed.db.getUrl(dest);
		if(url == null) {
			//Try adding " C"
			url = Delayed.db.getUrl(dest + " C");
			if(url == null) {
				Log.w(Tag, "Could not find " + dest);
				setAltDest(dest);
			} else {
				setDestination(new Station(dest + " C", url));
			}
		} else {
			setDestination(new Station(dest, url));
		}
	}

    public int compareTo(TrainEvent another) {
        long ours = departure.getTime();
        long theirs = another.getDepartureDate().getTime();
        
        if(ours < theirs) {
            return -1;
        }
        
        if(ours > theirs) {
            return 1;
        }
        
        return 0;
    }
}
