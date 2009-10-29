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
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.SimpleAdapter;
import android.widget.TextView;

public class StationActivity extends ListActivity {
	private static final String Tag = "StationActivity";

	private List<Map<String, String>> listContent = null;
	private SimpleAdapter sa = null;
	
	//These are passed to use on creation
	private String url;
	private String name;
	
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
			m.put("number", "Train #: " + Integer.toString(te.getNumber()));
			m.put("destination", te.getDestination());
			m.put("url", te.getUrl());
			m.put("delayed", te.getDelayed());
			m.put("extra", te.getExtra());
			Log.v(Tag, "Te: " + m);
			listContent.add(m);
			
			Delayed.getDb(getApplicationContext()).addTrainEvent(te.getStation().getName(), te.getArrivalDate(), te.getTrack(), te.getNumber(), te.getDelayedDate(), te.getExtra());
			
			boolean needInvalidate = false;
			if(sa == null) {
				needInvalidate = true;
				SimpleAdapter.ViewBinder vb = new SimpleAdapter.ViewBinder(){

					public boolean setViewValue(View view, Object data,
							String textRepresentation) {
						TextView tv = (TextView) view;
						
						if(tv.getId() == R.id.Extra) {
							if(((String)data).length() == 0) {
								tv.setVisibility(View.GONE);
							}
						} else {
							tv.setText((String)data);
						}
						return true;
					}
				};
				
				sa = new SimpleAdapter(getApplicationContext(), listContent, R.layout.eventrow, 
						new String[]{"name", "destination", "track", "number", "delayed", "extra"},
						new int[]{R.id.Time, R.id.Destination, R.id.Track, R.id.TNumber, R.id.Delayed, R.id.Extra});

				sa.setViewBinder(vb);
				
				setListAdapter(sa);
			}
			
			if(!needInvalidate) {
				sa.notifyDataSetChanged();
			} else {
				sa.notifyDataSetInvalidated();
			}
			Log.i(Tag, "got mesg");
		}
	};
	
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.liststations);

		Intent i = getIntent();
		this.url = i.getStringExtra("url");
		this.name = i.getStringExtra("name");
		
		fetchList();
	}

	private void fetchList() {
		Log.v(Tag, "Name of station: " + name);
		final String url = this.url;
		final String name = this.name;

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
	
	public boolean onCreateOptionsMenu(Menu menu)
	{
		menu.add(0, 1, 0, "Ladda på nytt");
		
		return true;
	}
	
	private void clearList()
	{
		if(listContent != null) {
			runOnUiThread(new Runnable(){
				public void run() {
					listContent.clear();
					
					if(sa == null) {
						sa = new SimpleAdapter(getApplicationContext(), listContent, R.layout.stationrow, new String[]{"name"}, new int[]{R.id.StationName});
						setListAdapter(sa);
					}
					
					sa.notifyDataSetInvalidated();
				}
			});
		}
	}

	
	public boolean onOptionsItemSelected(MenuItem mi)
	{
		if(mi.getItemId() == 1) {
			clearList();
			fetchList();
		}
		
		return true;
	}

}
