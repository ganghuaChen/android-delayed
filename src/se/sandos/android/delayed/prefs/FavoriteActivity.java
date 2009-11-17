package se.sandos.android.delayed.prefs;

import se.sandos.android.delayed.R;
import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.CheckBox;

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
            favorite.persist(getApplicationContext());
            return true;
        }
        
        return false;
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

        setTitle(favorite.getName());
        
        CheckBox cb = (CheckBox) findViewById(R.id.FavoriteEnabled);
        cb.setChecked(favorite.isActive());
    }
}
