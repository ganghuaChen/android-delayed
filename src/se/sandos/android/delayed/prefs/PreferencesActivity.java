package se.sandos.android.delayed.prefs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import se.sandos.android.delayed.R;
import se.sandos.android.delayed.scrape.IntentTest;
import se.sandos.android.delayed.scrape.ScrapeService;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CheckBox;
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

    private Handler mHandler = new Handler() {
        public void handleMessage(final Message msg) {
            startActivity((Intent)msg.obj);
        }
    };
    
    @Override
    public void onResume()
    {
        super.onResume();
        
        CheckBox automatic = (CheckBox) findViewById(R.id.AutomaticUpdate);

        automatic.setChecked(Prefs.isSet(getApplicationContext(), Prefs.PREFS_SERVICE_ENABLED, false));
    }
    
    @Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.prefs);
		
		setList();
	}

    @Override
    public void onPause()
    {
        super.onPause();
        
        CheckBox automatic = (CheckBox) findViewById(R.id.AutomaticUpdate);
        
        boolean isCheckedNow = automatic.isChecked();
        
        if(Prefs.isSet(getApplicationContext(), Prefs.PREFS_SERVICE_ENABLED, false) != isCheckedNow) {
            if(isCheckedNow) {
                //Need to start service
                ScrapeService.setAlarmWithDefaults(getApplicationContext());
                ScrapeService.runOnceNow(getApplicationContext());
            }
        }
        
        Prefs.setBooleanSetting(getApplicationContext(), Prefs.PREFS_SERVICE_ENABLED, automatic.isChecked());
    }
    
    private void setList() {
        final ListView lv = (ListView) findViewById(R.id.FavoriteList);

        lv.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        
        List<HashMap<String, Object>> listContent = new ArrayList<HashMap<String, Object>>(10);
        
        List<Favorite> favs = Prefs.getFavorites(getApplicationContext());
        
        for(Favorite f : favs) {
            HashMap<String, Object> m = new HashMap<String, Object>();
            m.put("name", f.getName());
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
                new String[] { "name"}, new int[] { R.id.SchedulerName});

        //sa.setViewBinder(vb);
        sa.notifyDataSetInvalidated();
		lv.setAdapter(sa);
		lv.setOnItemClickListener(new OnItemClickListener(){

            @SuppressWarnings("unchecked")
            public void onItemClick(AdapterView<?> l, View view, int pos, long id) {
                HashMap<String, String> m = (HashMap) sa.getItem(pos);

                //Start activity for configuring the favorite
                Intent intent = new Intent(getApplicationContext(), FavoriteActivity.class);
                intent.setData(Uri.fromParts("delayed", "favorite", m.get("name")));
                startActivity(intent);
//                CheckedTextView child = (CheckedTextView) view.findViewById(R.id.SchedulerName);
//                Log.v(Tag, "Child: " + child + " " + pos + " " + lv.getCheckedItemPosition() + " " + lv.getChoiceMode() + " " + view + " " + id);
//                child.toggle();
//                //sa.notifyDataSetChanged();
//                //lv.invalidate();
//                IntentTest.startSchedulerActivity(getApplicationContext(), mHandler, m.get("pkg"), m.get("class"));
            }
		});
    }
}
