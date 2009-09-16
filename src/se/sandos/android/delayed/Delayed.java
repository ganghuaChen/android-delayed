package se.sandos.android.delayed;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLDecoder;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import se.sandos.android.delayed.db.DBAdapter;
import se.sandos.android.delayed.db.Station;
import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcel;
import android.util.Log;

public class Delayed extends Activity {
	private static final String Tag = "Delayed";

	protected DBAdapter db = null;
	
	private Handler mHandler = new Handler() {
		public void handleMessage(Message msg) {
			
		}
	};
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		db = new DBAdapter(this);
		db.open();
		
		//Create db
		Thread t = new Thread() {
			public void run() {
				HttpClient hc = new DefaultHttpClient();
				// HttpGet("http://m.banverket.se/trafik");
				// HttpGet("http://m.banverket.se/trafik/(111111111111111111111111)/WapPages/StationSearch.aspx?JG=-1");
				//HttpGet("http://m.banverket.se/trafik/(111111111111111111111111)/WapPages/TrainSearch.aspx?JG=-1");
				String base = "http://m.banverket.se/trafik/(111111111111111111111111)/WapPages/";
				String stationSearch = base
						+ "StationSearch.aspx?JG=-1&stationlink=";
				HttpGet hg = new HttpGet(stationSearch + "66");

				// JF=-1&stationlink=1 A-D
				// JF=-1&stationlink=2 E-H
				// JF=-1&stationlink=3 I-L
				// JF=-1&stationlink=4 M-P
				// JF=-1&stationlink=5 Q-Ö

				try {
					HttpResponse hr = hc.execute(hg);
					Log.i(Tag, "Got page: " + hr.getStatusLine());
					InputStream is = hr.getEntity().getContent();
					InputStreamReader isr = new InputStreamReader(is);
					BufferedReader br = new BufferedReader(isr);
					String s = null;
					List<Station> stations = new LinkedList<Station>();
					while ((s = br.readLine()) != null) {
						if(s.contains("station=")) {
							try {
								//We should probably store the entire URL?
								String station = s.substring(s.indexOf("\">") + 2, s.indexOf("</a>"));
								station = StringEscapeUtils.unescapeHtml(station);
								String urlid = s.substring(s.indexOf("href=\"") + 6, s.indexOf("\">"));
								urlid = URLDecoder.decode(urlid);
								db.addStation(station, urlid);
								stations.add(new Station(station, urlid));
							} catch(Throwable e) {
								Log.w(Tag, "Could not decode: " + s + " " + e.getMessage());
							}
						}
					}
					//Done, signal this
					Message done = Message.obtain();
					Bundle b = new Bundle();
					Parcel p = new Parcel();
					done.setData();
					mHandler.sendMessage(null);
				} catch (ClientProtocolException e) {
					Log.i(Tag, "Something happened: " + e);
				} catch (IOException e) {
					Log.i(Tag, "IOExc: " + e);
				}
			}
		};
		if(db.getNumberOfStations() == 0) {
			t.start();
		}
	}
}