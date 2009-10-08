package se.sandos.android.delayed.db;

import java.util.LinkedList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.util.Log;

public class DBAdapter {
	private final static String Tag = "DBAdapter";
	private final Context context;
	
	private final static String DATABASE_NAME = "delayed";
	private final static int DATABASE_VERSION = 1;
	
	private static final String STATION_TABLE_NAME = "stations";
	private static final String STATION_KEY_NAME = "name";
	private static final String STATION_KEY_URLID = "urlid";
	
	private static final String TRAIN_TABLE_NAME = "trains";
	
	private static final String TRAINEVENT_TABLE_NAME = "trainevents";
	
    private static final String DATABASE_CREATE =
        "create table stations (_id integer primary key autoincrement, " +
        "name text not null, urlid text not null);" + 
        "create table trains (_id integer primary key autoincrement, " + 
        "track, number, destination, time);" + 
	    "create table trainevents(_id integer primary key autoincrement, " + 
	    "station, time, track, number);";
    
    
    //Trains are identified by number
    //Trainevents are identified by train and station? Time, track is mutable.

	private DBHelper helper = null;
	private SQLiteDatabase db = null;
	
	public DBAdapter(Context ctx)
	{
		this.context = ctx;
		helper = new DBHelper(ctx);
	}
	
	public DBAdapter open() throws SQLException
	{
		Log.i(Tag, "opening db");
		db = helper.getWritableDatabase();
		return this;
	}
	
	public void close()
	{
		Log.i(Tag, "closing db");
		helper.close();
	}
	
	public StationList getStations()
	{
		Cursor c = db.rawQuery("select name, urlid from stations", null);
		c.move(1);
		List<Station> res = new LinkedList<Station>();
		while(!c.isAfterLast() && !c.isBeforeFirst())
		{
			Station s = new Station(c.getString(0), c.getString(1)); 
			res.add(s);
			c.move(1);
		}
		
		c.close();
		
		return new StationList(res);
	}
	
	public long addStation(String name, String urlid)
	{
		ContentValues cv = new ContentValues();
		cv.put(STATION_KEY_NAME, name);
		cv.put(STATION_KEY_URLID, urlid);
		return db.insert(STATION_TABLE_NAME, null, cv);
	}
	
	public void clearStations()
	{
		Cursor c = db.rawQuery("delete from " + STATION_TABLE_NAME, null);
		c.move(1);
	}
	
	public String getUrl(String stationName) 
	{
		Cursor c = null;
		try {
			c = db.rawQuery("select urlid from stations where name = ?", new String[]{stationName});
			
			if(c != null && c.getCount() != 0) {
				c.move(1);
				String url = c.getString(c.getColumnIndex(STATION_KEY_URLID));
				return url;
			}
		} catch(Throwable e) {
			Log.w(Tag, "Error when fetching data from db: " + e.getMessage());
		} finally {
			if(c != null) {
				c.close();
			}
		}
		
		return null;
	}
	
	public int getNumberOfStations()
	{
		Cursor c = db.rawQuery("select count(*) from stations", null);
		c.move(1);
		int num = c.getInt(0);
		c.close();
		return num;
	}
	
	private static class DBHelper extends SQLiteOpenHelper
	{

		public DBHelper(Context context, String name, CursorFactory factory,
				int version) {
			super(context, name, factory, version);
		}

		public DBHelper(Context ctx) {
			super(ctx, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db)
		{
            db.execSQL(DATABASE_CREATE);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		}
		
	}
}
