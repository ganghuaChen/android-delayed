package se.sandos.android.delayed.scrape;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import se.sandos.android.delayed.TrainEvent;
import se.sandos.android.delayed.db.Station;
import se.sandos.android.delayed.scrape.ScraperHelper.Job;
import se.sandos.android.delayed.scrape.ScraperHelper.Nameurl;
import android.util.Log;

public class StationScraper extends Scraper<TrainEvent, Object[]> {
	private static final String Tag = "StationScraper";
	
	private String mName;
	private String mUrl;
	
    private static final DateFormat df = SimpleDateFormat.getTimeInstance(SimpleDateFormat.SHORT, java.util.Locale.GERMANY);

    private static final Map<String, String> nameMap = new HashMap<String, String>();
    
	private boolean delimiterSeen = false;

	public final static int MSG_DEST = 1;

	public StationScraper(String url, String name)
	{
		mName = name;
		mUrl = url;
	}
	
	@Override
	public void scrapeImpl() throws Exception {
		HttpClient hc = new DefaultHttpClient();
		
		String base = "http://m.banverket.se/";
		String stationSearch = base + mUrl;
		Log.i(Tag, "Full url: " + stationSearch);
		HttpGet hg = new HttpGet(stationSearch);

		Log.i(Tag, "fetching http");
		HttpResponse hr = hc.execute(hg);
		Log.i(Tag, "Got page: " + hr.getStatusLine());
		mListener.onStatus("got page, status " + hr.getStatusLine());
		InputStream is = hr.getEntity().getContent();
		InputStreamReader isr = new InputStreamReader(is);
		BufferedReader br = new BufferedReader(isr);
		String s = null;
		List<TrainEvent> events = new ArrayList<TrainEvent>(20);
		//XXX: Move this out to scraper
		TrainEvent te = new TrainEvent(new Station(mName, mUrl));
		delimiterSeen = false;
		int line = 0;
		while ((s = br.readLine()) != null) {
			mListener.onStatus("pl: " + line++);
			String unescaped = StringEscapeUtils.unescapeHtml(s);
			if(parse(te, unescaped)) {
				events.add(te);
				
				if(mListener != null) {
	 				Log.v(Tag, "Sending trainevent: " + te + " " + te.getStation().getName());
					mListener.onPartialResult(te);
				} else {
					Log.w(Tag, "Parsing, but nobody listening?");
				}
				
				te = new TrainEvent(new Station(mName, mUrl));
			}
		}
		if(mListener != null) {
			mListener.onFinished(null);
		}
	}
	
	public boolean parse(TrainEvent te, final String html)
	{
		boolean hasHandled = false;
		if(html.indexOf(" till ") != -1) {
			hasHandled = true;
			delimiterSeen = true;
			String arrival = html.substring(0, html.indexOf(" "));
			Date dd = null;
			dd = parseTime(arrival);
			te.setDeparture(dd);
			//Log.i(Tag, "Header for train time: " + dd);
			
			//also parse destination
			String dest = html.substring(html.indexOf(" till ") + 6);
			dest = dest.substring(0, dest.indexOf("<br>"));
			Log.v(Tag, "Dest: " + dest);
			
			//Try to find it in db
			te.setDestinationFromString(dest);
			return false;
		}
		
		if(html.endsWith("-<br>")) {
			if(delimiterSeen) {
				StringBuffer sb = te.getStringBuffer();
				if(sb != null && sb.length() != 0) {
					Log.v(Tag, "EXTRA DATA: " + te.getStringBuffer().toString());
				}
				return true;
			}
			delimiterSeen = true;
			return false;
		}
		
		if(!delimiterSeen) {
			Log.v(Tag, "Has not seen delimiter yet, ignoring: " + html);
			return false;
		}
		
		if(html.startsWith("Tåg nr ")) {
			int startNr = html.indexOf("\">") + 2;
			String d = html.substring(startNr);
			int endNr = d.indexOf("</a>");
			d = d.substring(0, endNr);
			te.setNumber(Integer.valueOf(d));
			
			String url = html.substring(html.indexOf("href=\"") + 6);
			final String finalUrl = url.substring(0, url.indexOf("\""));
			te.setUrl(finalUrl);
			final String name = te.getDestination();
			//XXX use listener if !foundDest
			if(!te.hasProperDest()) {
				if(nameMap.containsKey(name)) {
					//No need to redo this...
					Log.v(Tag, "Not redoing namemap scraping");

					if(nameMap.get(name) != null) {
						te.setDestinationFromString(nameMap.get(name));
					}
				} else {
					Log.v(Tag, "Destination is not set, finding it");
					nameMap.put(name, null);
					ScraperHelper.queueForParse(finalUrl, new Job<List<Nameurl>>(){
						public void run() {
							mListener.onFinished(new Object[] {name, value});
							//handler.sendMessage(Message.obtain(handler, MSG_DEST, new Object[] {finalUrl, value}));

							if(value.size() > 0) {
								nameMap.put(name, value.get(value.size()-1).name);
							}
						}
					});
				}
			} else {
				Log.v(Tag, "Destination was set");
			}
			return false;
		}
		
		if(html.startsWith("Spår ")) {
			te.setTrack(html.substring(5, html.indexOf("<br>")));
			return false;
		}

		if(html.startsWith("Beräknas ")) {
			String delayed = html.substring(9);
			Date dd = parseTime(delayed);
			te.setDelayed(dd);
			//Log.v(Tag, "Setting delayed arrival to " + dd);
			return false;
		}
		
		if(!hasHandled) {
			Log.v(Tag, "not handled: " + html);
		}
		
		if(!te.hasTrack() && delimiterSeen) {
			te.getStringBuffer().append(html);
		}
		
		return false;
	}

	/**
	 * Parse time on the format "07:28" with some fuzz: assume all times are +/- 2 hours, including around midnight!  
	 * @param arrival
	 * @return
	 */
	public static Date parseTime(String arrival) {
        Calendar now = Calendar.getInstance();
        Calendar correctitem = Calendar.getInstance();
		Date dd = null;
		try {
			dd = df.parse(arrival);
		} catch (ParseException e) {
			Log.i(Tag, "Exception parsing date: " + e.getMessage());
		}

		{
    		Calendar item = Calendar.getInstance();
    		item.setTime(dd);
    		
    		correctitem.set(Calendar.HOUR_OF_DAY, item.get(Calendar.HOUR_OF_DAY));
            correctitem.set(Calendar.MINUTE, item.get(Calendar.MINUTE));
		}
		
        //Compare
        long diff = correctitem.getTimeInMillis() - now.getTimeInMillis(); 
        
        //Log.v(Tag, "Now: " + DBAdapter.SIMPLE_DATEFORMATTER.format(now.getTime()));
        if(diff > 3600000 * 2) {
            //Need to add one day
            now.add(Calendar.DAY_OF_YEAR, 1);
            //Log.v(Tag, "Fixed day of time, too big diff: " + DBAdapter.SIMPLE_DATEFORMATTER.format(correctitem.getTime()));
        } else if(diff < -(3600000 * 2)) {
            now.add(Calendar.DAY_OF_YEAR, -1);
            //Log.v(Tag, "Fixed day of time, too small diff: " + DBAdapter.SIMPLE_DATEFORMATTER.format(correctitem.getTime()));
        }
		
        Date ret = correctitem.getTime();
        
		return ret;
	}

}
