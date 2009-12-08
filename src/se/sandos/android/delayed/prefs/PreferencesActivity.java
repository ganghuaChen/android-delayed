package se.sandos.android.delayed.prefs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import se.sandos.android.delayed.R;
import se.sandos.android.delayed.scrape.ScrapeService;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;

/**
 * Set preferences, filters etc
 * @author John Bäckstrand
 *
 */
public class PreferencesActivity extends Activity {
    private final static String Tag = "PreferencesActivity";

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
                ScrapeService.runOnceNow(getApplicationContext());
                //Need to start service
                ScrapeService.setAlarmWithDefaults(getApplicationContext());
            }
        } else {
            if(!isCheckedNow) {
                //Terminate alarm
                ScrapeService.removeAlarm(getApplicationContext());
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
        
        final List<HashMap<String, Object>> content = listContent;
        
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
            }
		});
		
		lv.setOnItemLongClickListener(new OnItemLongClickListener() {
            @SuppressWarnings("unchecked")
            public boolean onItemLongClick(AdapterView<?> adapter, View view, int pos, long id)
            {
                HashMap<String, Object> m = (HashMap<String, Object>) adapter.getAdapter().getItem(pos);

                Prefs.removeFavorite(getApplicationContext(), (String)m.get("name"));
                List<HashMap<String, Object>> toRemove = new ArrayList<HashMap<String, Object>>(10);
                for(HashMap<String, Object> orig : content) {
                    if(orig.get("name").equals(m.get("name"))) {
                        toRemove.add(orig);
                    }
                }
                for(HashMap<String, Object> map : toRemove) {
                    content.remove(map);
                }
                sa.notifyDataSetChanged();
                return true;
            }
        });
    }
}
