package se.sandos.android.delayed.scrape;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLDecoder;
import java.util.ArrayList;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import se.sandos.android.delayed.db.Station;
import android.util.Log;

public class StationListScraper extends Scraper<Station, ArrayList<Station>> {
	private static final String Tag = "StationListScraper";
	
	
	@Override
	public void scrapeImpl() throws Exception {
		HttpClient hc = new DefaultHttpClient();
		// HttpGet("http://m.banverket.se/trafik");
		// HttpGet("http://m.banverket.se/trafik/(111111111111111111111111)/WapPages/StationSearch.aspx?JG=-1");
		// HttpGet("http://m.banverket.se/trafik/(111111111111111111111111)/WapPages/TrainSearch.aspx?JG=-1");
		String base = "http://m.banverket.se/trafik/(111111111111111111111111)/WapPages/";
		String stationSearch = base + "StationSearch.aspx?JG=-1&stationlink=";
		HttpGet hg = new HttpGet(stationSearch + "66");

		// JF=-1&stationlink=1 A-D
		// JF=-1&stationlink=2 E-H
		// JF=-1&stationlink=3 I-L
		// JF=-1&stationlink=4 M-P
		// JF=-1&stationlink=5 Q-Ö

		ArrayList<Station> stations = null;

		Log.i(Tag, "fetching http");
		HttpResponse hr = hc.execute(hg);
		Log.i(Tag, "Got page: " + hr.getStatusLine());
		InputStream is = hr.getEntity().getContent();
		InputStreamReader isr = new InputStreamReader(is);
		BufferedReader br = new BufferedReader(isr);
		String s = null;
		stations = new ArrayList<Station>();
		while ((s = br.readLine()) != null) {
			if (s.contains("station=")) {
				try {
					// We should probably store the entire URL?
					String station = s.substring(s.indexOf("\">") + 2, s
							.indexOf("</a>"));
					station = StringEscapeUtils.unescapeHtml(station);
					String urlid = s.substring(s.indexOf("href=\"") + 6, s
							.indexOf("\">"));
					urlid = URLDecoder.decode(urlid);
					Log.i(Tag, "Stationurl: " + urlid + " name: " + station);
					//XXX use scrapelistener
					//db.addStation(station, urlid);
					if(mListener != null) {
						mListener.onPartialResult(new Station(station, urlid));
					}
					stations.add(new Station(station, urlid));
				} catch (Throwable e) {
					Log.w(Tag, "Could not decode: " + s + " " + e.getMessage());
				}
			}
		}
		//XXX use scrapelistener
		// Done, signal this
		//Message done = Message.obtain(mHandler, 0, stations);
		//mHandler.sendMessage(done);
		if(mListener != null) {
			mListener.onFinished(stations);
		}
	}

}
