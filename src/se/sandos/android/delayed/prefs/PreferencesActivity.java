package se.sandos.android.delayed.prefs;

import se.sandos.android.delayed.R;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.TextView;

/**
 * Set preferences, filters etc
 * @author John Bäckstrand
 *
 */
public class PreferencesActivity extends Activity {
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.prefs);
		
		SharedPreferences sp = getApplicationContext().getSharedPreferences("delayedfilter", Context.MODE_WORLD_READABLE);
		TextView tv = (TextView) findViewById(R.id.FavoriteStation);
		tv.setText(sp.getString("favoriteName", "NA"));


	}
}
