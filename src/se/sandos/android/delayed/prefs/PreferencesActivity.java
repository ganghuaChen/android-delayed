package se.sandos.android.delayed.prefs;

import se.sandos.android.delayed.R;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

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

    @Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.prefs);
		
		SharedPreferences sp = getApplicationContext().getSharedPreferences(PREFS_KEY, Context.MODE_APPEND | Context.MODE_PRIVATE);
		TextView tv = (TextView) findViewById(R.id.FavoriteStation);
		tv.setText(sp.getString(PREFS_FAV_NAME, "NA"));
	}
}
