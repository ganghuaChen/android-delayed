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
	private int id = -1;
	private String track;
	private Date arrival;
	private Date departure;
	private DateFormat df;
	
	
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
		if(df != null && arrival != null) {
			sb.append(df.format(arrival));
		}
		sb.append(" nr:").append(id);
		if(track != null) {
			sb.append(" track:").append(track);
		}
		return sb.toString();
	}
	
	public void parse(String html)
	{
		if(html.indexOf(" till ") != -1) {
			delimiterSeen = true;
			String arrival = html.substring(0, html.indexOf(" "));
			df = SimpleDateFormat.getTimeInstance(SimpleDateFormat.SHORT, java.util.Locale.GERMANY);
			Date dd = null;
			try {
				dd = df.parse(arrival);
			} catch (ParseException e) {
				Log.i(Tag, "Exception parsing date: " + e.getMessage());
			}
			this.arrival = dd;
			Log.i(Tag, "Header for train time: " + dd);
			
			//also parse destination
			String dest = html.substring(html.indexOf(" till ")+5);
			Log.i(Tag, "Dest: " + dest);
			
			return;
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
		
		if(html.startsWith("Tåg nr ")) {
			int startNr = html.indexOf("\">") + 2;
			Log.i(Tag, "start: " + startNr);
			String d = html.substring(startNr);
			int endNr = d.indexOf("</a>");
			Log.i(Tag, "end: " + endNr);
			d = d.substring(0, endNr);
			id = Integer.valueOf(d);
			return;
		}
		
		if(html.startsWith("Spår ")) {
			track = html.substring(5, html.indexOf("<br>"));
			return;
		}

		Log.i(Tag, "not handled: " + html);
	}
}
