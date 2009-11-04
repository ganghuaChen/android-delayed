package se.sandos.android.delayed;	

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import se.sandos.android.delayed.db.DBAdapter;
import se.sandos.android.delayed.scrape.ScrapeListener;
import se.sandos.android.delayed.scrape.ScraperHelper;
import se.sandos.android.delayed.scrape.StationScraper;
import se.sandos.android.delayed.scrape.ScraperHelper.Nameurl;
import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Debug;
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
	
	private List<TrainEvent> trainevents = new ArrayList<TrainEvent>();
	
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
			if(msg.what == StationScraper.MSG_DEST) {
				//Got a proper destination for some particular train. Mend.
				Object[] vals = (Object[]) msg.obj;
				List<Nameurl> stations = (List<Nameurl>) vals[1];
				if(stations.size() > 0) {
					Nameurl nu  = stations.get(stations.size()-1);

					Log.i(Tag, "We got an end destination with value " + nu.name + " and name " + vals[0]);

					for(Map<String, String> v : listContent) {
						String name = v.get("destination");
						if(name != null && name.equals(vals[0])) {
							Log.i(Tag, "found match: " + vals[0] + " " + nu.name);
							v.put("destination", nu.name);
							sa.notifyDataSetChanged();
						} 
					}
				}
				
				return;
			} 
			
			if(msg.what == ScrapeListener.MSG_STATUS) {
				StringBuffer sb = new StringBuffer();
				sb.append("Delayed: ").append(name).append(" ");
				sb.append(msg.obj);
				setTitle(sb.toString());
				
				return;
			}
			
			TrainEvent te  = (TrainEvent) msg.obj;
			
			List<TrainEvent> evs = new ArrayList<TrainEvent>();
			evs.add(te);
			addEvents(evs);
		}

	};
	
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.liststations);

		Intent i = getIntent();
		this.url = i.getStringExtra("url");
		this.name = i.getStringExtra("name");

		setTitle("Delayed: " + name);
		
		Log.v(Tag, "Created stationact: " + name + " " + url + " " + this);
		
		fetchList();
	}

	private void addEvents(List<TrainEvent> events) {
		
		if(listContent == null) {
			listContent = new ArrayList<Map<String, String>>();
		}
		
		boolean needInvalidate = false;
		for(TrainEvent te : events) {
			if(existsAndUpdate(te)) {
				needInvalidate = true;
				continue;
			}
			
			trainevents.add(te);
			Map<String, String> m = new HashMap<String, String>();
			m.put("name", te.toString());
			m.put("track", "Track: " + te.getTrack());
			m.put("number", "Train #: " + Integer.toString(te.getNumber()));
			m.put("destination", te.getDestination());
			m.put("url", te.getUrl());
			m.put("delayed", te.getDelayed());
			m.put("extra", te.getExtra());
			listContent.add(m);
		}
		
		if(sa == null) {
			needInvalidate = true;
			SimpleAdapter.ViewBinder vb = new SimpleAdapter.ViewBinder(){

				public boolean setViewValue(View view, Object data,
						String textRepresentation) {
					TextView tv = (TextView) view;
					
					if(tv.getId() == R.id.Extra) {
						if(((String)data).length() == 0) {
							tv.setVisibility(View.GONE);
						} else {
                            tv.setText((String)data);
                            tv.setVisibility(View.VISIBLE);
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
	}

	
	private boolean existsAndUpdate(TrainEvent te) {
		if(listContent == null || listContent.size() == 0) {
			return false;
		}
		
		for(Map<String, String> m : listContent) {
			if(m.get("number").equals("Train #: " + Integer.toString(te.getNumber()))) {
				//Update
				String delayed = m.get("delayed");
				if(!delayed.equals(te.getDelayed())) {
					m.put("delayed", te.getDelayed());
				}
				
				String extra = m.get("extra");
				if(!extra.equals(te.getExtra())) {
					m.put("extra", te.getExtra());
				}
				
				return true;
			} 
		}
		
		return false;
	}

	@Override
	public void onNewIntent(Intent intent)
	{
	    //This probably means the user used a launcher shortcut
		//Only if we set ourselves to singletask/singletop
		//We want one activity per station, really.
	    Log.v(Tag, "Reintenting");
	}
	
	private void fetchList() {
		DBAdapter db = Delayed.getDb(getApplicationContext());
		
		Log.v(Tag, "Name of station: " + name + " " + url);
		final String url = this.url;
		final String name = this.name;

		if(listContent == null || listContent.size() == 0) {
			//Fetch from db
			List<TrainEvent> events = db.getStationEvents(name);
			
			addEvents(events);
			trainevents.clear();
		}
		
		
		ScraperHelper.scrapeStation(url, name, new ScrapeListener<TrainEvent, Object[]>(){
			public void onStatus(String status) {
				mHandler.dispatchMessage(Message.obtain(mHandler, ScrapeListener.MSG_STATUS, status));
			}
			
			public void onFinished(Object[] result) {
				if(result == null) {
					//this actually means finished!
					Delayed.getDb(getApplicationContext()).addTrainEvents(trainevents);

					//Send ourselves a status-message
					mHandler.dispatchMessage(Message.obtain(mHandler, ScrapeListener.MSG_STATUS, "done"));
				} else {
					//In this case, we "abuse" this method and use it for mending previously unknown destinations
					mHandler.dispatchMessage(Message.obtain(mHandler, StationScraper.MSG_DEST, result));
				}
			}

			public void onPartialResult(TrainEvent result) {
				mHandler.dispatchMessage(Message.obtain(mHandler, 0, result));
			}

			public void onRestart() {
				trainevents.clear();
			}
		});
	}
	
	public boolean onCreateOptionsMenu(Menu menu)
	{
        menu.add(0, 1, 0, "Ladda om");
        menu.add(0, 2, 0, "Välj station");
		
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
			return true;
		}
		
        if(mi.getItemId() == 2) {
            Intent intent = new Intent("se.sandos.android.delayed.StationList", null, getApplicationContext(), StationListActivity.class);
            startActivity(intent);
        }
		
		return true;
	}

}
