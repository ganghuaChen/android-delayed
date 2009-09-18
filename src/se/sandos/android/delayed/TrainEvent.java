package se.sandos.android.delayed;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import se.sandos.android.delayed.db.Station;
import android.util.Log;

/**
 * Arrival/departure from one particular station
 * @author johba
 *
 */
public class TrainEvent {
	private final static String Tag = "TrainEvent";
	
	private Station station;
	private int id;
	private Date arrival;
	private Date departure;
	private boolean delimiterSeen = false;
	private boolean done = false;
	
	public TrainEvent()
	{
	}
	
	public boolean isParsed()
	{
		return done;
	}
	
	public String toString()
	{
		StringBuffer sb = new StringBuffer();
		sb.append("TrainEvent: ").append(arrival);
		sb.append(" ").append(id);
		return sb.toString();
	}
	
	public void parse(String html)
	{
		if(html.indexOf(" till ") != -1) {
			delimiterSeen = true;
			String arrival = html.substring(0, html.indexOf(" "));
			DateFormat df = SimpleDateFormat.getTimeInstance(SimpleDateFormat.SHORT, java.util.Locale.GERMANY);
			Date dd = null;
			try {
				dd = df.parse(arrival);
			} catch (ParseException e) {
			}
			Log.i(Tag, "Header for train time: " + dd);
			//parse date
			
		}
		
		if(html.endsWith("-<br>")) {
			Log.i(Tag, "Delimiter");
			if(delimiterSeen) {
				done = true;
				Log.i(Tag, "Done!");
			}
			delimiterSeen = true;
			return;
		}
		
		if(!delimiterSeen) {
			Log.i(Tag, "Has not seen delimiter yet, ignoring: " + html);
			return;
		}
		
	}
}
