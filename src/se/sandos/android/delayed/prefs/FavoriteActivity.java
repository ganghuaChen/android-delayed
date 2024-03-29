package se.sandos.android.delayed.prefs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import se.sandos.android.delayed.R;
import se.sandos.android.delayed.StationListActivity;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

public class FavoriteActivity extends Activity {
    private static final String Tag = "FavoriteActivity";
    
    private Favorite favorite;
    
    private List<HashMap<String, String>> content = new ArrayList<HashMap<String, String>>(10);
    private SimpleAdapter sa = null;
    
    private boolean hasShownDialog = false;
    
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
    
        setContentView(R.layout.favorite);
        
        Button b = (Button) findViewById(R.id.AddTarget);
        if(isReal()) {
            b.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    Intent ii = new Intent("android.intent.action.MAIN", null, getApplicationContext(), StationListActivity.class);
                    ii.putExtra("chooser", "true");
                    startActivityForResult(ii, 0);
                }
            });
        } else {
            b.setVisibility(View.GONE);
        }
    }

    public boolean onCreateOptionsMenu(Menu menu)
    {
        menu.add(0, 1, 0, "Spara ej");
        menu.add(0, 2, 0, "Spara");
        //menu.add(0, 3, 0, "Avancerat");
        
        return true;
    }
    
    
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if(requestCode == 0 && data != null) {
            addToTargets(data.getStringExtra("name"));
        }
    }
    
    public void onBackPressed()
    {
        if(hasShownDialog)
        {
//            finish();
        }
        
        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int which)
            {
                switch (which)
                {
                    case DialogInterface.BUTTON_POSITIVE:
                        saveMe();
                        
                        finish();
                        break;

                    case DialogInterface.BUTTON_NEGATIVE:
                        finish();
                        break;
                }
            }
        };

        hasShownDialog = true;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Vill du spara ändringar?").setPositiveButton("Ja", dialogClickListener)
                .setNegativeButton("Nej", dialogClickListener).show();
    }

    private void addToTargets(String target) {
        Log.v(Tag, "Target: " + target);
        if(targetExists(target)) {
            return;
        }

        HashMap<String, String> m = new HashMap<String, String>();
        m.put("name", target);
        
        content.add(m);
        
        ListView lv = (ListView) findViewById(R.id.TargetList);

        if(sa == null) {
            sa = new SimpleAdapter(getApplicationContext(), content, R.layout.schedulerrow, new String[]{"name"}, new int[]{R.id.SchedulerName});
            lv.setAdapter(sa);
        }
        
        lv.setOnItemLongClickListener(new OnItemLongClickListener() {

            @SuppressWarnings("unchecked")
            public boolean onItemLongClick(AdapterView<?> adapter, View view, int pos, long id) {
                HashMap<String, String> m = (HashMap<String, String>) adapter.getAdapter().getItem(pos);

                List<HashMap<String, String>> toRemove = new ArrayList<HashMap<String, String>>(10);
                for(HashMap<String, String> orig : content) {
                    if(orig.get("name").equals(m.get("name"))) {
                        toRemove.add(orig);
                    }
                }
                for(HashMap<String, String> map : toRemove) {
                    content.remove(map);
                }
                sa.notifyDataSetChanged();

                return true;
            }
        });
        
        sa.notifyDataSetChanged();
    }

    private boolean targetExists(String target) {
        for(HashMap<String, String> m : content) {
            if(m.get("name") != null && m.get("name").equals(target)) {
                return true;
            }
        }
        
        return false;
    }

    public boolean onOptionsItemSelected(MenuItem mi)
    {
        if(mi.getItemId() == 1) {
            finish();
            return true;
        }

        if(mi.getItemId() == 2) {
            saveMe();
          
            finish();
            return true;
        }
        
        return false;
    }

    private void saveMe()
    {
        CheckBox cb = (CheckBox) findViewById(R.id.FavoriteEnabled);
        favorite.setActive(cb.isChecked());
        cb = (CheckBox) findViewById(R.id.OtherFavoritesEnabled);
        favorite.setOtherFavorites(cb.isChecked());
        
        ArrayList<String> targets = new ArrayList<String>(10);
        for(HashMap<String, String> m : content) {
            Log.v(Tag, "M " + m);
            targets.add(m.get("name"));
        }
        
        favorite.setTargets(targets);
        
        if(isReal()) {
            favorite.persist(getApplicationContext());
        } else {
            Intent intent = new Intent();
            intent.putExtra("name", favorite.getName());
            intent.putExtra("enabled", favorite.isActive());
            setResult(Activity.RESULT_OK, intent);
        }
    }

    private boolean isReal()
    {
        return getIntent().getData().getSchemeSpecificPart() != null && getIntent().getData().getSchemeSpecificPart().equals("favorite");
    }
    
    public Favorite getFavorite()
    {
        return favorite;
    }
    
    @Override
    public void onResume()
    {
        super.onResume();
        
        hasShownDialog = true;
        
        favorite = Prefs.getFavorite(getApplicationContext(), getIntent().getData().getFragment());
        
        if(favorite == null) {
            finish();
            return;
        }

        if(isReal()) {
            setTitle("Favorit: " + favorite.getName());
        } else {
            setTitle("Locale > Edit situation > Delayed > Favorit > " + favorite.getName());
        }
        
        TextView tv = (TextView) findViewById(R.id.FavoriteName);
        tv.setText(favorite.getName());
        
        CheckBox cb = (CheckBox) findViewById(R.id.FavoriteEnabled);
        if(isReal()) {
            cb.setChecked(favorite.isActive());
        } else {
            cb.setChecked(getIntent().getBooleanExtra("enabled", false));
        }
        
        cb = (CheckBox) findViewById(R.id.OtherFavoritesEnabled);
        if(isReal()) {
            cb.setChecked(favorite.targetOtherFavorites());
        } else {
            cb.setVisibility(View.GONE);
        }
        
        if(isReal()) {
            for(String fav : favorite.getTargets()) {
                addToTargets(fav);
            }
        }
    }
}
