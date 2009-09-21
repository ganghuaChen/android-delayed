package se.sandos.android.delayed;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import se.sandos.android.delayed.db.Station;
import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.ListAdapter;
import android.widget.SimpleAdapter;

public class StationActivity extends ListActivity {
	private static final String Tag = "StationActivity";

	private Handler mHandler = new Handler() {
		@SuppressWarnings("unchecked")
		public void handleMessage(Message msg) {
			List<TrainEvent> l = (List<TrainEvent>) msg.obj;
			
			List<Map<String, String>> content = new ArrayList<Map<String, String>>();
			for(TrainEvent te : l) {
				Map<String, String> m = new HashMap<String, String>();
				Log.i(Tag, "Adding " + te.toString());
				m.put("name", te.toString());
				m.put("track", "Track: " + te.getTrack());
				m.put("number", "  Train #: " + Integer.toString(te.getNumber()));
				m.put("destination", te.getDestination());
				content.add(m);
			}
			
			ListAdapter la = new SimpleAdapter(getApplicationContext(), content, R.layout.traineventrow, 
					new String[]{"name", "destination", "track", "number"},
					new int[]{R.id.TeTime, R.id.TeDestination, R.id.TeTrack, R.id.TeNumber});
			
			setListAdapter(la);
		}
	};
	
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.liststations);


		Intent i = getIntent();
		final String url = i.getStringExtra("url");
		final String name = i.getStringExtra("name");
		
		new Thread(){
			public void run(){
				try {
					HttpClient hc = new DefaultHttpClient();
					
					String base = "http://m.banverket.se/";
					String stationSearch = base + url;
					Log.i(Tag, "Dull url: " + stationSearch);
					HttpGet hg = new HttpGet(stationSearch);
			
					Log.i(Tag, "fetching http");
					HttpResponse hr = hc.execute(hg);
					Log.i(Tag, "Got page: " + hr.getStatusLine());
					InputStream is = hr.getEntity().getContent();
					InputStreamReader isr = new InputStreamReader(is);
					BufferedReader br = new BufferedReader(isr);
					String s = null;
					List<String> stations = new ArrayList<String>();
					List<TrainEvent> events = new ArrayList<TrainEvent>(20);
					TrainEvent te = new TrainEvent(new Station(name, url));
					while ((s = br.readLine()) != null) {
						String unescaped = StringEscapeUtils.unescapeHtml(s);
						te.parse(unescaped);
						if(te.isParsed()) {
							events.add(te);
							Log.i(Tag, "Added trainevent: " + te);
							te = new TrainEvent();
						}
						stations.add(unescaped);
					}
					mHandler.sendMessage(Message.obtain(mHandler, 0, events));
				} catch(Throwable e) {
					Log.w(Tag, "Error when fetching: " + e.getMessage());
				}
			}
		}.start();
	}
}
