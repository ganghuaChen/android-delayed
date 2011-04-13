package se.sandos.android.delayed.db;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import se.sandos.android.delayed.TrainEvent;
import se.sandos.android.delayed.scrape.DelayRunnable;
import se.sandos.android.delayed.scrape.ScrapePool;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Debug;
import android.util.Log;

public class DBAdapter {
	private final static String Tag = "dldAdapter";
	
	private final static boolean TRACE = false;
	
	private final static String DATABASE_NAME = "delayed";
	private final static int DATABASE_VERSION = 1;
	
	private static final String STATION_TABLE_NAME = "stations";
	private static final String STATION_KEY_NAME = "name";
	private static final String STATION_KEY_URLID = "urlid";
	
	//private static final String TRAIN_TABLE_NAME = "trains";

	private static final String TRAINEVENT_TABLE_NAME = "trainevents";

	private static final String TRAINEVENT_KEY_TIME = "time";
	private static final String TRAINEVENT_KEY_STATION = "station";
	private static final String TRAINEVENT_KEY_TRACK = "track";
	private static final String TRAINEVENT_KEY_NUMBER = "number";
	private static final String TRAINEVENT_KEY_DELAY = "delay";
	private static final String TRAINEVENT_KEY_EXTRA = "extra";
	private static final String TRAINEVENT_KEY_TIMESTAMP = "timestamp";
	private static final String TRAINEVENT_KEY_DESTINATION = "destination";
	
    public static final DateFormat SIMPLE_DATEFORMATTER = SimpleDateFormat.getDateTimeInstance();
	
    private static final String DATABASE_CREATE =
        "create table stations (_id integer primary key autoincrement, " +
        "name text not null, urlid text not null)";
    private static final String DATABASE_CREATE_2 =  
        "create table trains (_id integer primary key autoincrement, " + 
        "number, destination)";
    private static  final String DATABASE_CREATE_3 =
	    "create table trainevents(_id integer primary key autoincrement, " + 
	    "station, time, track, number, delay, extra, timestamp, destination)";
    private static  final String DATABASE_CREATE_4 =
        "create table eventlog(_id integer primary key autoincrement, " + 
        "timestamp, type, text)";
 
    //Cache for reading the DB from _disk_
    private static Map<String, StationCache> stationCache = new HashMap<String, StationCache>();

    public static class StationCache
    {
        private static final long CACHE_TTL = 30000;
        private List<TrainEvent> events;
        private long time;
        private String stationName;
        
        public StationCache(List<TrainEvent> events, String name)
        {
            this.events = events;
            time = System.currentTimeMillis();
            stationName = name;
        }

        public List<TrainEvent> getEvents()
        {
            return events;
        }

        public String getStationName()
        {
            return stationName;
        }

        public boolean isCurrent()
        {
            if((System.currentTimeMillis() - time) > CACHE_TTL)
            {
                return false;
            }
            
            return true;
        }
        
        @Override
        public String toString()
        {
            StringBuilder builder = new StringBuilder();
            builder.append("StationCache [time=").append(time).append(", stationName=").append(stationName).append("]");
            return builder.toString();
        }

        @Override
        public int hashCode()
        {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((stationName == null) ? 0 : stationName.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj)
        {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            StationCache other = (StationCache) obj;
            if (stationName == null)
            {
                if (other.stationName != null)
                    return false;
            }
            else if (!stationName.equals(other.stationName))
                return false;
            return true;
        }
    }
    
    public synchronized void addTrainEvents(final List<TrainEvent> l, String stationName)
    {
        // (Shallow) Copy the list to avoid ConcurrentModificationException
        final List<TrainEvent> trainevents = new ArrayList<TrainEvent>(l);

        Log.v(Tag, "Number of events to store: " + trainevents.size() + " " + Thread.currentThread());
        
        if (trainevents == null || trainevents.size() == 0) {
            //Clean everything
            int res = db.delete(TRAINEVENT_TABLE_NAME, TRAINEVENT_KEY_STATION + " = ?", new String[]{stationName});
            Log.v(Tag, "Deleted " + res + " rows");
            return;
        }

        if (TRACE) {
            Debug.startMethodTracing("dbstore");
        }
        Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
        long s = System.currentTimeMillis();

        Station st = trainevents.get(0).getStation();
        if(st == null)
        {
            Log.e(Tag, "Abnormal stuff, no station for first event: " + trainevents.get(0));
            return;
        }
        String station = st.getName();
        
        //Remove ALL other events for this station. Simplifies things
        //We trust whatever our source is to keep the right stuff there
        //(Not always true, but we can't really do it better ourselves!)
        int res = db.delete(TRAINEVENT_TABLE_NAME, TRAINEVENT_KEY_STATION + " = ?", new String[]{station});
        Log.v(Tag, "Deleted " + res + " rows");
        
        int count = 0;
        for (TrainEvent te : trainevents)
        {
            addTrainEventImpl(te.getStation().getName(), te.getDepartureDate(), te.getTrack(), te.getNumber(),
                    te.getDelayedDate(), te.getExtra(), te.getDestination(), true);
            count++;
        }
        if (TRACE) {
            Debug.stopMethodTracing();
        }
        Log.v(Tag, "Took " + (System.currentTimeMillis() - s) + " for " + count + " " + Thread.currentThread());
    }
    
    /**
     * Return all trainevents for this station. 
     * 
     * @param station
     * @return
     */
    public synchronized List<TrainEvent> getStationEvents(String station)
    {
        if(stationCache.containsKey(station))
        {
            StationCache sc = stationCache.get(station);
            if(sc.isCurrent())
            {
                Log.v(Tag, "Got eventlist from RAM cache");
                return sc.getEvents();
            }
            Log.v(Tag, "Purged eventlist from RAM cache");
            stationCache.remove(station);
        }
        else
        {
            Log.v(Tag, "" + station + " was not in cache");
        }
        
        ArrayList<TrainEvent> res = new ArrayList<TrainEvent>(100);

        Cursor c = db.query(TRAINEVENT_TABLE_NAME, 
            new String[] {
                TRAINEVENT_KEY_TIME, TRAINEVENT_KEY_EXTRA,
                TRAINEVENT_KEY_DELAY, TRAINEVENT_KEY_NUMBER, "_id",
                TRAINEVENT_KEY_DESTINATION, TRAINEVENT_KEY_TRACK },
                TRAINEVENT_KEY_STATION + "= ?", new String[] { station }, null,
                null, null);
        c.move(1);
        
        Log.v(Tag, "Number of events in db: " + c.getCount() + " " + Thread.currentThread());
        if (!c.isAfterLast() && !c.isBeforeFirst()) {
            while (!c.isAfterLast()) {
                TrainEvent te = new TrainEvent(null);
                te.setDeparture(c.getLong(0));
                te.setNumber(c.getString(3));
                if(!c.isNull(2)) {
                    te.setDelayed(c.getLong(2));
                }
                if(!c.isNull(1)) {
                    StringBuffer sb = te.getStringBuffer();
                    sb.setLength(0);
                    sb.append(c.getString(1));
                }
                if(!c.isNull(5)) {
                	te.setDestinationFromString(c.getString(5));
                }
                if(!c.isNull(6)) {
                	te.setTrack(c.getString(6));
                }

                res.add(te);
                
                c.move(1);
            }

            c.close();
            StationCache sc = new StationCache(res, station);
            stationCache.put(station, sc);
            Log.v(Tag, "Put eventlist to RAM cache: " + station + " " + res.size());
            return res;
        }

        c.close();
        
        ArrayList<TrainEvent> emptyRes = new ArrayList<TrainEvent>();
        stationCache.put(station, new StationCache(emptyRes, station));
        Log.v(Tag, "Put empty eventlist to RAM cache: " + station);
        return emptyRes;
    }

    /**
     * 
     * @param station
     * @param train
     * @return true if this train passes this station
     */
    public boolean checkIfPasses(String station, String train, Date departureDate)
    {
		Cursor c = db.query(TRAINEVENT_TABLE_NAME,
			new String[] { 	TRAINEVENT_KEY_TIME, 
							TRAINEVENT_KEY_EXTRA,
							TRAINEVENT_KEY_DELAY, 
							TRAINEVENT_KEY_NUMBER, 
							TRAINEVENT_KEY_DESTINATION}, 
			TRAINEVENT_KEY_NUMBER + " = ? AND " + TRAINEVENT_KEY_STATION + " = ?", 
			new String[]{train, station} , null, null, null);

		try {
//    		Log.v(Tag, "Count for passing: " + c.getCount() + " " + station + " " + train);
    		
    		if(c.getCount() > 0)
    		{
    		   c.moveToNext();
    		   long passes = c.getLong(0);
    		   
    		   //Don't look back in time, we want stations to come
    		   if(passes < departureDate.getTime())
    		   {
    		       return false;
    		   }
    		   return true; 
    		}
		}
		finally 
		{
		    c.close();
		}
		
		return false;
    }
    
    /**
     * 
     * @param station
     * @param time
     * @param track
     * @param number
     * @param delay
     * @param extra
     * @param destination
     * @param add Wether to add this event, or possibly update it
     * @return
     */
    protected long addTrainEventImpl(String station, Date time, String track, String number, Date delay, String extra, String destination, boolean add)
    {
        if(time == null)
        {
            Log.e(Tag, "Null time, not so sure about this");
            return -1;
        }
        
        stationCache.remove(station);

		ContentValues cv = new ContentValues();
		cv.put(TRAINEVENT_KEY_STATION, station);
	    cv.put(TRAINEVENT_KEY_TIME, time.getTime());
		cv.put(TRAINEVENT_KEY_TRACK, track);
		cv.put(TRAINEVENT_KEY_NUMBER, number);
		if(delay != null) {
			cv.put(TRAINEVENT_KEY_DELAY, delay.getTime());
		} else {
			cv.putNull(TRAINEVENT_KEY_DELAY);
		}
		cv.put(TRAINEVENT_KEY_EXTRA, extra);
		cv.put(TRAINEVENT_KEY_TIMESTAMP, System.currentTimeMillis());
		cv.put(TRAINEVENT_KEY_DESTINATION, destination);
		
		long status = db.insert(TRAINEVENT_TABLE_NAME, null, cv);

		return status;
    }
    
    public synchronized TrainEvent[] getTrainEvents(String station, String[] numbers)
    {
    	if(station == null) {
    		return  null;
    	}
    	
    	//Log.v(Tag, "Trying to find trainevent " + station + " " + number);
    	//Cursor c = db.rawQuery("select time, extra, delay from trainevents where station = ? and number = " + Integer.toString(number), new String[]{station});
		Cursor c = db.query(TRAINEVENT_TABLE_NAME,
			new String[] { 	TRAINEVENT_KEY_TIME, 
							TRAINEVENT_KEY_EXTRA,
							TRAINEVENT_KEY_DELAY, 
							TRAINEVENT_KEY_NUMBER, 
							TRAINEVENT_KEY_DESTINATION}, 
			TRAINEVENT_KEY_NUMBER + " IN(" + expand(numbers) + ") AND " + TRAINEVENT_KEY_STATION + " = ?", 
			new String[]{station} , null, null, null);
		c.move(1);
    	TrainEvent[] events = new TrainEvent[c.getCount()];
		if(!c.isAfterLast() && !c.isBeforeFirst())
		{
			int index = 0;
			while(!c.isAfterLast()) {
				TrainEvent te = new TrainEvent(null);
				te.setDeparture(c.getLong(0));
				te.setNumber(c.getString(3));
				te.setDestinationFromString(c.getString(4));
				events[index++] = te;
				c.move(1);
			}
			
			c.close();
			return events;
		}

		Log.v(Tag, "Found none");
		c.close();
		return new TrainEvent[0];
    }
    
	private String expand(String[] numbers) {
		StringBuffer sb = new StringBuffer();
		
		for(int i=0; i<numbers.length; i++) {
			sb.append("\"").append(numbers[i]).append("\"");
			if(i < (numbers.length - 1)) {
				sb.append(", ");
			}
		}
//		Log.v(Tag, "Returning " + sb.toString());
		return sb.toString();
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
	
	public synchronized StationList getStations()
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
	
	public synchronized void clearStations()
	{
		Cursor c = db.rawQuery("delete from " + STATION_TABLE_NAME, null);
		c.move(1);
		c.close();
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
            db.execSQL(DATABASE_CREATE_4);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		}
		
	}

}
