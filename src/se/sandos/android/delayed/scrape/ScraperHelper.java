package se.sandos.android.delayed.scrape;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import se.sandos.android.delayed.TrainEvent;
import se.sandos.android.delayed.db.Station;
import android.os.PowerManager;
import android.util.Log;

/**
 * Helpers for scraping simple HTML pages.
 * @author John Bäckstrand
 *
 */
public class ScraperHelper {
	public final static String BASE_URL = "http://m.banverket.se/trafik/(111111111111111111111111)/WapPages/";
	
	public final static String Tag = "Scraper";
	
	public static class Nameurl {
		public String name;
		public String url;
		public Nameurl(String n, String u) { name = n; url = u; }
	}

	public abstract static class Job<T> implements Runnable
	{
		protected T value;
		
		public abstract void run();
	}
	
	public static void queueForParse(final String relativeUrl, final Job<List<Nameurl>> job)
	{
		ScrapePool.addJob(new Job<List<Nameurl>>(){
			@Override
			public void run() {
				job.value = parseTrainPage(relativeUrl);
				job.run();
			}
		});
	}
	
	/**
	 * Parse stations. End station specifically.
	 * @param relativeUrl
	 */
	private static List<Nameurl> parseTrainPage(String relativeUrl)
	{
		String cut = relativeUrl.substring(relativeUrl.indexOf("WapPages/") + 9);

		HttpClient hc = new DefaultHttpClient();
		String url = BASE_URL + cut;
		Log.i(Tag, "Parsing trainpage, full url: " + url);

		HttpGet hg = new HttpGet(url);
		
		try {
			HttpResponse hr = hc.execute(hg);
			Log.i(Tag, "Got page: " + hr.getStatusLine());
			InputStream is = hr.getEntity().getContent();
			InputStreamReader isr = new InputStreamReader(is);
			BufferedReader br = new BufferedReader(isr);

			
			List<Nameurl> names = new ArrayList<Nameurl>(40);
			while (true) {
				String s = br.readLine();
				if(s == null) {
					break;
				}
				if(s.indexOf("showallstations") != -1) {
					String uri = s.substring(9, s.lastIndexOf("\">"));
					Log.i(Tag, "Url: " + uri);
					
					String stationName = s.substring(s.lastIndexOf("\">") + 2, s.length() - 8);
					stationName = StringEscapeUtils.unescapeHtml(stationName);
					names.add(new Nameurl(stationName, uri));
				} else {
					Log.i(Tag, s);
				}
			}
			Log.i(Tag, "Got names of all stations for " + cut);

			return names;
		} catch (Throwable e) {
			String msg = e.getMessage();
			if(msg == null) {
				msg = "Something happened: " + e;
			}
			Log.w(Tag, msg);
		}

		return new LinkedList<Nameurl>();
	}

	public static void scrapeStation(String url, String name, final ScrapeListener<TrainEvent, Object[]> listener)
	{
		final Scraper<TrainEvent, Object[]> s = new StationScraper(url, name);
		
		ScrapePool.addJob(new Job<Object>(){
			@Override
			public void run() {
				s.setScrapeListener(listener);
				s.scrape();
			}
		});
	}

	public static void scrapeStations(final ScrapeListener<Station, ArrayList<Station>> notify)
	{
		final Scraper<Station, ArrayList<Station>> s = new StationListScraper();
		
		ScrapePool.addJob(new Job<Object>(){
			@Override
			public void run() {
				s.setScrapeListener(notify);
				s.scrape();
			}
		});
	}
	
}
