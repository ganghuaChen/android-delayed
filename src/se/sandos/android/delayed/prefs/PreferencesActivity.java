package se.sandos.android.delayed.prefs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import se.sandos.android.delayed.R;
import se.sandos.android.delayed.scrape.IntentTest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CheckedTextView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

/**
 * Set preferences, filters etc
 * @author John Bäckstrand
 *
 */
public class PreferencesActivity extends Activity {
    private final static String Tag = "PreferencesActivity";

    public static final String PREFS_FAV_NAME = "favoriteName";
    public static final String PREFS_FAV_URL = "favoriteUrl";
    public static final String PREFS_KEY = "delayedfilter";

    private Handler mHandler = new Handler() {
        public void handleMessage(final Message msg) {
            startActivity((Intent)msg.obj);
        }
    };
    
    @Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.prefs);
		
		SharedPreferences sp = getApplicationContext().getSharedPreferences(PREFS_KEY, Context.MODE_PRIVATE);
		TextView tv = (TextView) findViewById(R.id.FavoriteStation);
		tv.setText(sp.getString(PREFS_FAV_NAME, "NA"));
		
		
		setList();
		
	}

    private void setList() {
        final ListView lv = (ListView) findViewById(R.id.SchedulerList);

        lv.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        
        List<HashMap<String, Object>> listContent = new ArrayList<HashMap<String, Object>>(10);
        
        List<ResolveInfo> l = IntentTest.getSchedulerList(getApplicationContext());
        
        for(ResolveInfo ri : l) {
            ActivityInfo ai = ri.activityInfo;
            HashMap<String, Object> m = new HashMap<String, Object>();
            m.put("name", IntentTest.getLabel(getApplicationContext(), ai));
            m.put("pkg", ai.packageName);
            m.put("class", ai.name);
            m.put("enabled", new Boolean(false));
            listContent.add(m);
        }

        for(ResolveInfo ri : l) {
            ActivityInfo ai = ri.activityInfo;
            HashMap<String, Object> m = new HashMap<String, Object>();
            m.put("name", IntentTest.getLabel(getApplicationContext(), ai));
            m.put("pkg", ai.packageName);
            m.put("class", ai.name);
            m.put("enabled", new Boolean(false));
            listContent.add(m);
        }

        
        final List<HashMap<String, Object>> ll = listContent;
        
        SimpleAdapter.ViewBinder vb = new SimpleAdapter.ViewBinder() {
            public boolean setViewValue(View view, Object data, String textRepresentation) {
                CheckedTextView tv = (CheckedTextView) view;
                if (tv.getId() == R.id.SchedulerName) {
                    String t = "x";
                    for(HashMap<String, Object> m : ll) {
                        if(m.get("enabled") == data) {
                            t = (String) m.get("name");
                        }
                    }
                    tv.setText(t);
                    if (tv.isChecked()) {
                        tv.setBackgroundColor(0xff333333);
                        tv.setTextColor(0xff334455);
                    } else {
                        tv.setBackgroundColor(0xff000000);
                        tv.setTextColor(0xffffffff);
                    }
                } else {
                    tv.setText((String) data);
                }
                return true;
            }
        };
        
        final SimpleAdapter sa = new SimpleAdapter(getApplicationContext(), listContent, R.layout.schedulerrow,
                new String[] { "enabled"}, new int[] { R.id.SchedulerName});

        sa.setViewBinder(vb);
        sa.notifyDataSetInvalidated();
		lv.setAdapter(sa);
		lv.setOnItemClickListener(new OnItemClickListener(){

            @SuppressWarnings("unchecked")
            public void onItemClick(AdapterView<?> l, View view, int pos, long id) {
                HashMap<String, String> m = (HashMap) sa.getItem(pos);

                CheckedTextView child = (CheckedTextView) view.findViewById(R.id.SchedulerName);
                Log.v(Tag, "Child: " + child + " " + pos + " " + lv.getCheckedItemPosition() + " " + lv.getChoiceMode() + " " + view + " " + id);
                child.toggle();
                //sa.notifyDataSetChanged();
                //lv.invalidate();
                IntentTest.startSchedulerActivity(getApplicationContext(), mHandler, m.get("pkg"), m.get("class"));
            }
		});
    }
}
