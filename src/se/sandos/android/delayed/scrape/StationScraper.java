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
import java.util.List;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import se.sandos.android.delayed.Delayed;
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
		InputStream is = hr.getEntity().getContent();
		InputStreamReader isr = new InputStreamReader(is);
		BufferedReader br = new BufferedReader(isr);
		String s = null;
		List<TrainEvent> events = new ArrayList<TrainEvent>(20);
		//XXX: Move this out to scraper
		TrainEvent te = new TrainEvent(new Station(mName, mUrl));
		delimiterSeen = false;
		while ((s = br.readLine()) != null) {
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
			String url = Delayed.db.getUrl(dest);
			if(url == null) {
				//Try adding " C"
				url = Delayed.db.getUrl(dest + " C");
				if(url == null) {
					Log.w(Tag, "Could not find " + dest);
					te.setAltDest(dest);
				} else {
					te.setDestination(new Station(dest + " C", url));
				}
			} else {
				te.setDestination(new Station(dest, url));
			}
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
			//XXX use listener if !foundDest
			if(!te.hasProperDest()) {
				Log.v(Tag, "Destination is not set, finding it");
				ScraperHelper.queueForParse(finalUrl, new Job<List<Nameurl>>(){
					public void run() {
						mListener.onFinished(new Object[] {finalUrl, value});
						//handler.sendMessage(Message.obtain(handler, MSG_DEST, new Object[] {finalUrl, value}));
					}
				});
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
			Log.v(Tag, "Setting delayed arrival to " + dd);
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
	    long l = System.currentTimeMillis();
		Calendar cal = Calendar.getInstance();
		Date dd = null;
		try {
			dd = df.parse(arrival);
		} catch (ParseException e) {
			Log.i(Tag, "Exception parsing date: " + e.getMessage());
		}
		long s = System.currentTimeMillis();
		Log.v(Tag, "Took1: " + (s-l));
		
		Calendar d = Calendar.getInstance();
		d.setTime(dd);
        cal.set(Calendar.HOUR_OF_DAY, d.get(Calendar.HOUR_OF_DAY));
        cal.set(Calendar.MINUTE, d.get(Calendar.MINUTE));
        l = System.currentTimeMillis();
        Log.v(Tag, "Took2: " + (l-s));
        
        //Compare
        long diff = d.getTimeInMillis() - cal.getTimeInMillis(); 
        
        if(diff > 3600000 * 2) {
            //Need to add or subtract one day
            if(diff < 0) { 
                cal.add(Calendar.DAY_OF_YEAR, -1);
            } else {
                cal.add(Calendar.DAY_OF_YEAR, 1);
            }
            Log.v(Tag, "Fixed day of time: " + cal.toString());
        }
		
        s=System.currentTimeMillis();

        //EXTREMELY SLOW. Do not uncomment?
        //Log.w(Tag, "parsed time: " + cal.getTime());
        
        Date ret = cal.getTime();
        Log.v(Tag, "Took3: " + (s-l));
        
		return ret;
	}

}
