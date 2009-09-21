package se.sandos.android.delayed;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import se.sandos.android.delayed.db.Station;
import se.sandos.android.delayed.db.StationList;
import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.AdapterView.AdapterContextMenuInfo;

public class StationListActivity extends ListActivity {
	private final static String Tag = "StationListActivity";
	
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		Bundle extras = getIntent().getExtras();
		if(extras == null) {
			return;
		}
		StationList sl = extras.getParcelable("se.sandos.android.delayed.StationList");
		
		setContentView(R.layout.liststations);
		Log.i(Tag, "Number of stations: " + sl.getList().size());
		List<Map<String, String>> content = new ArrayList<Map<String, String>>();
		for(Station s : sl.getList()) {
			Map<String, String> m = new HashMap<String, String>();
			m.put("name", s.getName());
			content.add(m);
		}
		
		ListAdapter la = new SimpleAdapter(this.getBaseContext(), content, R.layout.row, new String[]{"name"}, new int[]{R.id.TextView01});
		
		setListAdapter(la);
		
		//Register context menu
		registerForContextMenu(getListView());
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
			String url = Delayed.db.getUrl(stationName);
			
			Intent i = new Intent("se.sandos.android.delayed.Station", null, getApplicationContext(), StationActivity.class);
			i.putExtra("stationname", stationName);
			i.putExtra("url", url);
			startActivity(i);
			return true;
		}
		return false;
	}
	
	protected void onListItemClick(ListView l, View v, int position, long id)
	{
		Object o = l.getAdapter().getItem(position);
		Log.i(Tag, "Click: " + o + " " + o.getClass());
	}
}
