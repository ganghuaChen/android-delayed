package se.sandos.android.delayed.prefs.locale;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import se.sandos.android.delayed.R;
import se.sandos.android.delayed.prefs.FavoriteActivity;
import se.sandos.android.delayed.prefs.FavoritesDialog;
import se.sandos.android.delayed.prefs.Prefs;
import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.AdapterView.OnItemClickListener;

public class LocaleActivity extends Activity 
{
    private static final String Tag = "LocaleActivity";
    
    List<HashMap<String, Object>> content = new ArrayList<HashMap<String, Object>>(10);
    
    SimpleAdapter sa;
    
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.localesettings);
        
        CheckBox cb = (CheckBox) findViewById(R.id.LocaleEnabled);
        Intent intent = getIntent();
        cb.setChecked(intent.getBooleanExtra(Prefs.PREFS_SERVICE_ENABLED, false));
        
        int index = 0;
        while(true) {
            String name = intent.getStringExtra("favorite" + index); 
            if(name != null) {
                addFavorite(name, intent.getBooleanExtra("favoriteEnabled" + index++, false));
            } else {
                break;
            }
        }
        
        Button b = (Button) findViewById(R.id.AddFavorite);
        b.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                showDialog(1);
            }
        });
        
        setTitle("Locale > Edit situation > Delayed");
    } 
   
    @Override
    public Dialog onCreateDialog(int id)
    {
        if(id == 1) {
            FavoritesDialog fd = new FavoritesDialog(this);
            
            fd.setClickListener(new DialogInterface.OnClickListener() {
                
                public void onClick(DialogInterface dialog, int which) {
                    FavoritesDialog fd = (FavoritesDialog) dialog;
                    addFavorite(fd.getSelected(), fd.getEnabled());
                    dismissDialog(1);
                    editFavorite(fd.getEnabled(), fd.getSelected());
                }
            });
            
            return fd;
        }
        
        return null;
    }

    private void addFavorite(String selected, boolean enabled) {
        if(exists(selected)) {
            return;
        }
        
        HashMap<String, Object> m = new HashMap<String, Object>();
        m.put("name", selected);
        m.put("enabled", enabled);
        content.add(m);
        
        if(sa == null) {
            sa = new SimpleAdapter(getApplicationContext(), content, R.layout.schedulerrow, 
                    new String[]{"name"}, new int[]{R.id.SchedulerName});
            
            ListView lv = (ListView) findViewById(R.id.SelectedFavorites);
            lv.setAdapter(sa);
            
            //On click, you get to configure the favorite in question...
            lv.setOnItemClickListener(new OnItemClickListener() {
                @SuppressWarnings("unchecked")
                public void onItemClick(AdapterView<?> adapter, View view, int pos, long id) {
                    HashMap<String, Object> m = (HashMap<String, Object>) adapter.getAdapter().getItem(pos);
                    
                    boolean enabled = ((Boolean)m.get("enabled")).booleanValue();
                    String name = (String) m.get("name"); 
                    editFavorite(enabled, name);
                }
            });
        }
        
        sa.notifyDataSetChanged();
    }

    private void editFavorite(boolean enabled, String name)
    {
        Intent intent = new Intent(getApplicationContext(), FavoriteActivity.class);
        intent.putExtra("enabled", enabled);
        intent.setData(Uri.fromParts("delayed", "favoriteFuture", name));
        startActivityForResult(intent, 1);
    }
    
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if(requestCode == 1 && data != null) {
            Log.v(Tag, "Saving result from FavoriteActivity");
            String name = data.getStringExtra("name");
            for(HashMap<String, Object> m : content) {
                if(m.get("name").equals(name)) {
                    m.put("enabled", data.getBooleanExtra("enabled", false));
                    Log.v(Tag, "E: " + m.get("enabled"));
                }
            }
        }
    }
    
    private boolean exists(String selected) {
        for(HashMap<String, Object> m : content) {
            if(m.get("name").equals(selected)) {
                return true;
            }
        }
        
        return false;
    }

    public boolean onCreateOptionsMenu(Menu menu)
    {
        menu.add(0, 1, 0, "Help");
        menu.add(0, 2, 0, "Dont save");
        menu.add(0, 3, 0, "Save");
        
        return true;
    }
    
    public boolean onOptionsItemSelected(MenuItem mi)
    {
        if (mi.getItemId() == 1) {
            return false;
        }

        if (mi.getItemId() == 2) {
            setResult(Activity.RESULT_CANCELED);
            finish();
            return true;
        }

        if (mi.getItemId() == 3) {
            returnResult();
            return true;
        }

        return false;
    }
    
    private void returnResult()
    {
        CheckBox cb = (CheckBox) findViewById(R.id.LocaleEnabled);
        
        Intent intent = new Intent();
        intent.putExtra(Prefs.PREFS_SERVICE_ENABLED, cb.isChecked());
        String blurb = "updates ";
        if(cb.isChecked()) {
            blurb += "active";
        } else {
            blurb += "disabled";
        }
        blurb += " ";
        int index = 0;
        for(HashMap<String, Object> m : content) {
            String name = (String) m.get("name");
            intent.putExtra("favorite" + index, name);
            Boolean b = (Boolean) m.get("enabled");
            intent.putExtra("favoriteEnabled" + index++, b);
            if(b == null || b.booleanValue()) {
                blurb += "+";
            } else {
                blurb += "-";
            }
            blurb += name;
        }
        intent.putExtra("com.twofortyfouram.locale.intent.extra.BLURB", blurb);
        setResult(Activity.RESULT_OK, intent);
        finish();
    }
}
