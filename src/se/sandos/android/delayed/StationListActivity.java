package se.sandos.android.delayed;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.ListActivity;
import android.os.Bundle;
import android.widget.ListAdapter;
import android.widget.SimpleAdapter;

public class StationListActivity extends ListActivity {
	
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.liststations);

		List<Map<String, String>> content = new ArrayList<Map<String, String>>();
		for(int i = 0; i < 10; i++) {
			Map<String, String> m = new HashMap<String, String>();
			m.put("name", "Västerås");
			content.add(m);
		}
		
		ListAdapter la = new SimpleAdapter(this.getBaseContext(), content, R.layout.row, new String[]{"name"}, new int[]{R.id.TextView01});
		
		setListAdapter(la);
	}
}
