package se.sandos.android.delayed;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import se.sandos.android.delayed.db.StationList;
import android.app.ListActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ListAdapter;
import android.widget.SimpleAdapter;

public class StationListActivity extends ListActivity {
	private final static String Tag = "StationListActivity";
	
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		Bundle extras = getIntent().getExtras();
		StationList sl = extras.getParcelable("se.sandos.android.delayed.StationList");
		//Object o = extras.getParcelableArrayList("se.sandos.android.delayed.StationList");
		Log.i(Tag, "Obj: " + sl);
		
		for(String k : extras.keySet()) {
			Log.i(Tag, "Key: " + k + " " + extras.get(k));
		}
		
		
		setContentView(R.layout.liststations);

		List<Map<String, String>> content = new ArrayList<Map<String, String>>();
		for(int i = 0; i < 10; i++) {
			Map<String, String> m = new HashMap<String, String>();
			m.put("name", "V�ster�s");
			content.add(m);
		}
		
		ListAdapter la = new SimpleAdapter(this.getBaseContext(), content, R.layout.row, new String[]{"name"}, new int[]{R.id.TextView01});
		
		setListAdapter(la);
	}
}
