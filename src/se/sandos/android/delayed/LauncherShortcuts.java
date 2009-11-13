package se.sandos.android.delayed;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class LauncherShortcuts extends Activity
{
    @Override
    public void onCreate(Bundle icicle)
    {
        super.onCreate(icicle);
        
        final Intent intent = getIntent();
        final String action = intent.getAction();

        // If the intent is a request to create a shortcut, we'll do that and exit

        if (Intent.ACTION_CREATE_SHORTCUT.equals(action)) {
            setupShortcut();
            //finish();
            return;
        }

    }
    
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if(requestCode == 0) {

            Intent i = new Intent("se.sandos.android.delayed.Station", null, getApplicationContext(), StationActivity.class);
            i.putExtra("name", data.getExtras().getString("name"));
            i.putExtra("url", data.getExtras().getString("url"));

            // Then, set up the container intent (the response to the caller)

            Intent intent = new Intent();
            intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, i);
            intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, data.getExtras().getString("name"));
            // Now, return the result to the launcher

            setResult(RESULT_OK, intent);
            finish();
        }
    }
    
    private void setupShortcut() {
        Intent ii = new Intent("android.intent.action.MAIN", null, getApplicationContext(), StationListActivity.class);
        ii.putExtra("chooser", "true");
        startActivityForResult(ii, 0);
    }

}
