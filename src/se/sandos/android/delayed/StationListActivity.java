package se.sandos.android.delayed;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import se.sandos.android.delayed.db.DBAdapter;
import se.sandos.android.delayed.db.Station;
import se.sandos.android.delayed.db.StationList;
import se.sandos.android.delayed.prefs.PreferencesActivity;
import se.sandos.android.delayed.scrape.ScrapeListener;
import se.sandos.android.delayed.scrape.ScraperHelper;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.AdapterView.AdapterContextMenuInfo;

public class StationListActivity extends ListActivity {
	private final static String Tag = "StationListActivity";

	private final int MSG_COMPLETELIST = 1; 
	private final int MSG_PARTIAL_RESULT = 2; 
	
	private List<Map<String, String>> content = null;
	private SimpleAdapter sa = null;
	
	private boolean hasSeenIncomplete = false;
	
	//Choose station only, for home screen shortcuts
	private boolean chooser = false;
	
	Handler mHandler = new Handler() {
		public void handleMessage(Message msg) {
			switch(msg.what) {
			case MSG_COMPLETELIST:
				Log.v(Tag, "Got complete list");
				//dialog.dismiss();
				if(!hasSeenIncomplete) {
					setList((StationList) msg.obj);
				}
				break;
			case MSG_PARTIAL_RESULT:
				hasSeenIncomplete = true;
				Log.v(Tag, "Got incomplete list");
				Station s = (Station)msg.obj;
				addRow(s);
				DBAdapter db = Delayed.getDb(getApplicationContext());
				long status = db.addStation(s.getName(), s.getUrl());
				if(status == -1) {
					Log.v(Tag, "Error inserting");
				} else {
					Log.v(Tag, "Success inserting " + db.getNumberOfStations() + " " + db);
				}
				break;
			}
		}
	};
	
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		if(getIntent().getExtras() != null && getIntent().getExtras().containsKey("chooser")) {
		    chooser = true;
		}
		
		setContentView(R.layout.liststations);

		Log.v(Tag, "Created StationList ");

		//Register context menu
		if(!chooser) {
		    registerForContextMenu(getListView());
		}
		
		fetchStations();
	}

	private void fetchStations() {
		DBAdapter db = Delayed.getDb(getApplicationContext());
		
		if(db.getNumberOfStations() == 0) {
			Log.v(Tag, "Downloading");
			//dialog = ProgressDialog.show(this, "Progress", "Downloading list of stations");
			ScraperHelper.scrapeStations(new ScrapeListener<Station, ArrayList<Station>>(){
				public void onStatus(String status) {
					
				}
				
				public void onFinished(ArrayList<Station> sl) {
					mHandler.dispatchMessage(Message.obtain(mHandler, MSG_COMPLETELIST, new StationList(sl)));
				}
	
				public void onPartialResult(Station result) {
					mHandler.dispatchMessage(Message.obtain(mHandler, MSG_PARTIAL_RESULT, result));
				}
	
				public void onRestart() {
					clearList();
				}
			});
		} else {
			Log.v(Tag, "Fetching from db");
			StationList sl = db.getStations();
			mHandler.dispatchMessage(Message.obtain(mHandler, MSG_COMPLETELIST, sl));
		}
	}

	private void setList(final StationList sl)
	{
		runOnUiThread(new Runnable(){
			public void run() {
				if(content == null) {
					content = new ArrayList<Map<String, String>>();
				}

				
				for(Station station : sl.getList()) {
					Map<String, String> m = new HashMap<String, String>();
					m.put("name", station.getName());
					content.add(m);
				}
				
				if(sa == null) {
					sa = new SimpleAdapter(getApplicationContext(), content, R.layout.stationrow, new String[]{"name"}, new int[]{R.id.StationName});
					setListAdapter(sa);
				}
				
				sa.notifyDataSetChanged();
			}
		});
	}
	
	private void clearList()
	{
		runOnUiThread(new Runnable(){
			public void run() {
				content.clear();
				
				if(sa == null) {
					sa = new SimpleAdapter(getApplicationContext(), content, R.layout.stationrow, new String[]{"name"}, new int[]{R.id.StationName});
					setListAdapter(sa);
				}
				
				sa.notifyDataSetInvalidated();
			}
		});
	}
	
	private void addRow(final Station station)
	{
		runOnUiThread(new Runnable(){
			public void run() {
				if(content == null) {
					content = new ArrayList<Map<String, String>>();
				}

				Map<String, String> m = new HashMap<String, String>();
				m.put("name", station.getName());
				content.add(m);
				
				if(sa == null) {
					sa = new SimpleAdapter(getApplicationContext(), content, R.layout.stationrow, new String[]{"name"}, new int[]{R.id.StationName});
					setListAdapter(sa);
				}
				
				sa.notifyDataSetChanged();
			}
		});
	}
	
	public boolean onCreateOptionsMenu(Menu menu)
	{
		menu.add(0, 1, 0, "Ladda på nytt");
		menu.add(0, 2, 0, "Inställningar");
		
		return true;
	}
	
	public boolean onOptionsItemSelected(MenuItem mi)
	{
		if(mi.getItemId() == 1) {
			Log.v(Tag, "Clearing db");
			Delayed.getDb(getApplicationContext()).clearStations();
			clearList();
			fetchStations();
			
			return true;
		}
		
		if(mi.getItemId() == 2) {
			Intent i = new Intent("se.sandos.android.delayed.Prefs", null, getApplicationContext(), PreferencesActivity.class);
			startActivity(i);
		}
		
		return true;
	}
	
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo cmi)
	{
		menu.setHeaderTitle("Stationsgrejer");
		menu.add("Lägg till som favorit");
		menu.add("Kolla tidtabell");
		menu.add("Öppna URL i webläsare");
	}
	
	@SuppressWarnings("unchecked")
	public boolean onContextItemSelected(MenuItem mi)
	{
		if(mi.getTitle().equals("Lägg till som favorit")) {
			AdapterContextMenuInfo cmi = (AdapterContextMenuInfo) mi.getMenuInfo();
			Map<String, String> m = (Map) getListView().getAdapter().getItem(cmi.position);

			String stationName = m.get("name");
			String url = null;
			if(Delayed.db != null) {
				url = Delayed.db.getUrl(stationName);
			}
			
			SharedPreferences sp = getApplicationContext().getSharedPreferences(PreferencesActivity.PREFS_KEY, Context.MODE_APPEND | Context.MODE_PRIVATE);
			Log.v(Tag, "Setting favorite: " + sp);
			Editor editor = sp.edit();
			editor.putString(PreferencesActivity.PREFS_FAV_URL, url);
			editor.putString(PreferencesActivity.PREFS_FAV_NAME, stationName);
			if(!editor.commit()) {
			    Log.w(Tag, "Failed to commit!");
			}
		}
		
		if(mi.getTitle().equals("Kolla tidtabell")) {
			
			AdapterContextMenuInfo cmi = (AdapterContextMenuInfo) mi.getMenuInfo();
			Map<String, String> m = (Map) getListView().getAdapter().getItem(cmi.position);
			String stationName = m.get("name");
			String url = null;
			
			if(Delayed.db != null) {
				url = Delayed.db.getUrl(stationName);
			}
			
			gotoStation(stationName, url);
			return true;
		}
		return false;
	}
	
	@SuppressWarnings("unchecked")
	protected void onListItemClick(ListView l, View v, int position, long id)
	{
		Object o = l.getAdapter().getItem(position);
		Map<String, String> m = (Map) o;
		Log.i(Tag, "Click: " + o + " " + o.getClass());

		String stationName = m.get("name");
		String url = null;
		
		if(Delayed.db != null) {
			url = Delayed.db.getUrl(stationName);
		}

        if (chooser) {
            Intent i = new Intent("se.sandos.android.delayed.Station", null, getApplicationContext(), StationActivity.class);
            i.putExtra("name", stationName);
            i.putExtra("url", url);
            setResult(1, i);
            finish();
            return;
        } else {	        
            gotoStation(stationName, url);
        }

	}

	private void gotoStation(String stationName, String url) {
		Intent i = new Intent("se.sandos.android.delayed.Station", null, getApplicationContext(), StationActivity.class);
		i.putExtra("name", stationName);
		i.putExtra("url", url);
		Log.i(Tag, "Url: " + url + " Name: " + stationName);
		startActivity(i);
	}
}
