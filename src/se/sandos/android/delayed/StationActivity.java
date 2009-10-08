package se.sandos.android.delayed;	

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import se.sandos.android.delayed.scrape.ScrapeListener;
import se.sandos.android.delayed.scrape.ScraperHelper;
import se.sandos.android.delayed.scrape.StationScraper;
import se.sandos.android.delayed.scrape.ScraperHelper.Nameurl;
import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.SimpleAdapter;

public class StationActivity extends ListActivity {
	private static final String Tag = "StationActivity";

	private List<Map<String, String>> listContent = null;
	private SimpleAdapter sa = null;
	
	private Handler mHandler = new Handler() {
		public void handleMessage(final Message msg) {
			runOnUiThread(new Runnable(){
				public void run() {
					handle(msg);
				}
			});
		}

		@SuppressWarnings("unchecked")
		private void handle(Message msg) {
			Log.v(Tag, "Got message: " + msg.what);
			if(msg.what == StationScraper.MSG_DEST) {
				
				//Got a proper destination for some particular train. Mend.
				Object[] vals = (Object[]) msg.obj;
				List<Nameurl> stations = (List<Nameurl>) vals[1];
				if(stations.size() > 0) {
					Nameurl nu  = stations.get(stations.size()-1);

					Log.i(Tag, "We got an end destination with value " + nu.name);

					for(Map<String, String> v : listContent) {
						String url = v.get("url");
						if(url != null && url.equals(vals[0])) {
							Log.i(Tag, "found match: " + vals[0]);
							v.put("destination", nu.name);
							sa.notifyDataSetChanged();
						}
					}
				}
				
				return;
			}
			
			TrainEvent te  = (TrainEvent) msg.obj;
			
			if(listContent == null) {
				listContent = new ArrayList<Map<String, String>>();
			}
			
			Map<String, String> m = new HashMap<String, String>();
			Log.i(Tag, "Adding " + te.toString());
			m.put("name", te.toString());
			m.put("track", "Track: " + te.getTrack());
			m.put("number", "  Train #: " + Integer.toString(te.getNumber()));
			m.put("destination", te.getDestination());
			m.put("url", te.getUrl());
			Log.v(Tag, "Te: " + m);
			listContent.add(m);
			
			if(sa == null) {
				sa = new SimpleAdapter(getApplicationContext(), listContent, R.layout.traineventrow, 
						new String[]{"name", "destination", "track", "number"},
						new int[]{R.id.TeTime, R.id.TeDestination, R.id.TeTrack, R.id.TeNumber});

				setListAdapter(sa);
			}
			
			sa.notifyDataSetChanged();
			Log.i(Tag, "got mesg");
		}
	};

	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.liststations);

		Intent i = getIntent();
		final String url = i.getStringExtra("url");
		final String name = i.getStringExtra("name");

		ScraperHelper.scrapeStation(url, name, new ScrapeListener<TrainEvent, Object[]>(){

			public void onFinished(Object[] result) {
				//In this case, we "abuse" this method and use it for mending previously unknown destinations
				mHandler.dispatchMessage(Message.obtain(mHandler, StationScraper.MSG_DEST, result));
			}

			public void onPartialResult(TrainEvent result) {
				mHandler.dispatchMessage(Message.obtain(mHandler, 0, result));
			}

			public void onRestart() {
				// TODO Auto-generated method stub
			}
		});
	}
}
