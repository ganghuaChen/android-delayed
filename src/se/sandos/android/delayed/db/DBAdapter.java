package se.sandos.android.delayed.db;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import se.sandos.android.delayed.TrainEvent;
import se.sandos.android.delayed.scrape.StationScraper;
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
	
	private final static String DATABASE_NAME = "delayed";
	private final static int DATABASE_VERSION = 1;
	
	private static final String STATION_TABLE_NAME = "stations";
	private static final String STATION_KEY_NAME = "name";
	private static final String STATION_KEY_URLID = "urlid";
	
	private static final String TRAIN_TABLE_NAME = "trains";

	private static final String TRAINEVENT_TABLE_NAME = "trainevents";

	private static final String TRAINEVENT_KEY_TIME = "time";
	private static final String TRAINEVENT_KEY_STATION = "station";
	private static final String TRAINEVENT_KEY_TRACK = "track";
	private static final String TRAINEVENT_KEY_NUMBER = "number";
	private static final String TRAINEVENT_KEY_DELAY = "delay";
	private static final String TRAINEVENT_KEY_EXTRA = "extra";
	private static final String TRAINEVENT_KEY_TIMESTAMP = "timestamp";
	
	private DateFormat df = SimpleDateFormat.getTimeInstance(SimpleDateFormat.SHORT, java.util.Locale.GERMANY);
	
    private static final String DATABASE_CREATE =
        "create table stations (_id integer primary key autoincrement, " +
        "name text not null, urlid text not null)";
    private static final String DATABASE_CREATE_2 =  
        "create table trains (_id integer primary key autoincrement, " + 
        "track, number, destination, time)";
    private static  final String DATABASE_CREATE_3 =
	    "create table trainevents(_id integer primary key autoincrement, " + 
	    "station, time, track, number, delay, extra, timestamp)";
    
    //Trainevents are identified by train and station? Time, track, delay, extra is mutable.
    public long addTrainEvent(final String station, final Date time, final String track, final int number, final Date delay, final String extra)
    {
    	new Thread() {
    		public void run() {
    			addTrainEventImpl(station, time, track, number, delay, extra);
    		}
    	}.start();
    	
    	return -1;
    }
    
    //Trainevents are identified by train and station? Time, track, delay, extra is mutable.
    public long addTrainEventImpl(String station, Date time, String track, int number, Date delay, String extra)
    {
    	Log.v(Tag, "Add " + station + " " + time);
    	
    	long t = System.currentTimeMillis();
    	if(getTrainEvent(station, number) != null) {
        	Log.v(Tag, "Took3 " + (System.currentTimeMillis() - t));
    		Log.v(Tag, "Found already!");
    		return -1;
    	}

    	Log.v(Tag, "Took " + (System.currentTimeMillis() - t));
    	t = System.currentTimeMillis();
    	
		ContentValues cv = new ContentValues();
		cv.put(TRAINEVENT_KEY_STATION, station);
		cv.put(TRAINEVENT_KEY_TIME, df.format(time));
		cv.put(TRAINEVENT_KEY_TRACK, track);
		cv.put(TRAINEVENT_KEY_NUMBER, number);
		if(delay != null) {
			cv.put(TRAINEVENT_KEY_DELAY, df.format(delay));
		} else {
			cv.putNull(TRAINEVENT_KEY_DELAY);
		}
		cv.put(TRAINEVENT_KEY_EXTRA, extra);
		cv.put(TRAINEVENT_KEY_TIMESTAMP, new Date().toString());
		
		long status = db.insert(TRAINEVENT_TABLE_NAME, null, cv);

		Log.v(Tag, "Took2 " + (System.currentTimeMillis() - t));
		
		return status;
    }
    
    public TrainEvent getTrainEvent(String station, int number)
    {
    	if(station == null) {
    		return  null;
    	}
    	
    	Log.v(Tag, "Trying to find trainevent " + station + " " + number);
    	//Cursor c = db.rawQuery("select time, extra, delay from trainevents where station = ? and number = " + Integer.toString(number), new String[]{station});
		Cursor c = db.query(TRAINEVENT_TABLE_NAME,
			new String[] { 	TRAINEVENT_KEY_TIME, 
							TRAINEVENT_KEY_EXTRA,
							TRAINEVENT_KEY_DELAY }, 
			TRAINEVENT_KEY_NUMBER + "=" + number + " AND " + TRAINEVENT_KEY_STATION + "= ?", 
			new String[]{station} , null, null, null);
    	Log.v(Tag, "Numresults: " + c.getCount());
    	c.move(1);
		if(!c.isAfterLast() && !c.isBeforeFirst())
		{
			TrainEvent te = new TrainEvent(null);
			te.setArrival(StationScraper.parseTime(c.getString(1)));
			
			return te;
		}

		Log.v(Tag, "Found none");

		return null;
    }
    
	private DBHelper helper = null;
	private SQLiteDatabase db = null;
	
	public DBAdapter(Context ctx)
	{
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
		
		long status = db.insert(STATION_TABLE_NAME, null, cv);
		
		return status;
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
            db.execSQL(DATABASE_CREATE_2);
            db.execSQL(DATABASE_CREATE_3);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		}
		
	}
}
