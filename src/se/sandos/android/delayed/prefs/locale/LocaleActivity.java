package se.sandos.android.delayed.prefs.locale;

import se.sandos.android.delayed.R;
import se.sandos.android.delayed.prefs.Prefs;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.CheckBox;

public class LocaleActivity extends Activity 
{
    private static final String Tag = "LocaleActivity";
    
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.localesettings);
        
        CheckBox cb = (CheckBox) findViewById(R.id.LocaleEnabled);
        cb.setChecked(getIntent().getBooleanExtra(Prefs.PREFS_SERVICE_ENABLED, false));
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
        intent.putExtra("com.twofortyfouram.locale.intent.extra.BLURB", blurb);
        
        setResult(Activity.RESULT_OK, intent);
        finish();
    }
}
