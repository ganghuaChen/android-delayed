package se.sandos.android.delayed.prefs;

import se.sandos.android.delayed.R;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.CheckBox;
import android.widget.TextView;

public class FavoriteActivity extends Activity {
    
    private Favorite favorite;
    
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
    
        setContentView(R.layout.favorite);
    }

    public boolean onCreateOptionsMenu(Menu menu)
    {
        menu.add(0, 1, 0, "Spara ej");
        menu.add(0, 2, 0, "Spara");
        menu.add(0, 3, 0, "Avancerat");
        
        return true;
    }
    
    public boolean onOptionsItemSelected(MenuItem mi)
    {
        if(mi.getItemId() == 1) {
            finish();
            return true;
        }

        if(mi.getItemId() == 2) {
            CheckBox cb = (CheckBox) findViewById(R.id.FavoriteEnabled);
            favorite.setActive(cb.isChecked());
            if(isReal()) {
                favorite.persist(getApplicationContext());
            } else {
                Intent intent = new Intent();
                intent.putExtra("name", favorite.getName());
                intent.putExtra("enabled", favorite.isActive());
                setResult(Activity.RESULT_OK, intent);
            }
          
            finish();
            return true;
        }
        
        return false;
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
    }
}
