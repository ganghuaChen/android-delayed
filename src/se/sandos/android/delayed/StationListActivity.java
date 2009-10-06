package se.sandos.android.delayed;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import se.sandos.android.delayed.db.Station;
import se.sandos.android.delayed.db.StationList;
import se.sandos.android.delayed.scrape.ScrapeListener;
import se.sandos.android.delayed.scrape.ScraperHelper;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.ContextMenu;
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
	
	private ProgressDialog dialog = null;
	
	Handler mHandler = new Handler() {
		public void handleMessage(Message msg) {
			switch(msg.what) {
			case MSG_COMPLETELIST:
				setListData((StationList) msg.obj);
				break;
			case MSG_PARTIAL_RESULT:
				addRow((Station)msg.obj);
				break;
			}
		}
	};
	
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.liststations);

		Log.v(Tag, "Created StationList");

		//Register context menu
		registerForContextMenu(getListView());
		
		Bundle extras = getIntent().getExtras();
		if(extras == null) {
			Log.i(Tag, "Extras are null!");
			dialog = ProgressDialog.show(this, "Progress", "Downloading list of stations");
			ScraperHelper.scrapeStations(new ScrapeListener<Station, ArrayList<Station>>(){
				public void onFinished(ArrayList<Station> sl) {
					mHandler.dispatchMessage(Message.obtain(mHandler, MSG_COMPLETELIST, new StationList(sl)));
				}

				public void onPartialResult(Station result) {
					mHandler.dispatchMessage(Message.obtain(mHandler, MSG_PARTIAL_RESULT, result));
				}

				public void onRestart() {
				}
			});
			return;
		}
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
					sa = new SimpleAdapter(getApplicationContext(), content, R.layout.stationrow, new String[]{"name"}, new int[]{R.id.TextView01});
					setListAdapter(sa);
				}
				
				sa.notifyDataSetChanged();
			}
		});
	}
	private void setListData(final StationList sl) {
		runOnUiThread(new Runnable(){
			public void run() {
				if(content == null) {
					content = new ArrayList<Map<String, String>>();
				}
				
				content.clear();
				for(Station s : sl.getList()) {
					Map<String, String> m = new HashMap<String, String>();
					m.put("name", s.getName());
					content.add(m);
					Delayed.getDb(getApplicationContext()).addStation(s.getName(), s.getUrl());
				}
				
				if(sa == null) {
					sa = new SimpleAdapter(getApplicationContext(), content, R.layout.stationrow, new String[]{"name"}, new int[]{R.id.TextView01});
					setListAdapter(sa);
					
				}
				
				sa.notifyDataSetChanged();
				
				dialog.dismiss();
			}
		});
		
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
		if(mi.getTitle().equals("Kolla tidtabell")) {
			
			AdapterContextMenuInfo cmi = (AdapterContextMenuInfo) mi.getMenuInfo();
			Map<String, String> m = (Map) getListView().getAdapter().getItem(cmi.position);
			String stationName = m.get("name");
			String url = null;
			
			if(Delayed.db != null) {
				url = Delayed.db.getUrl(stationName);
			}
			
			Intent i = new Intent("se.sandos.android.delayed.Station", null, getApplicationContext(), StationActivity.class);
			i.putExtra("stationname", stationName);
			i.putExtra("url", url);
			Log.i(Tag, "Url: " + url + " Name: " + stationName);
			startActivity(i);
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

		Intent i = new Intent("se.sandos.android.delayed.Station", null, getApplicationContext(), StationActivity.class);
		i.putExtra("stationname", stationName);
		i.putExtra("url", url);
		Log.i(Tag, "Url: " + url + " Name: " + stationName);
		startActivity(i);

	}
}
