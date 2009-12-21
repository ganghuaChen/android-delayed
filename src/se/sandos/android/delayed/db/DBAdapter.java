package se.sandos.android.delayed.db;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import se.sandos.android.delayed.TrainEvent;
import se.sandos.android.delayed.scrape.ScrapePool;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.os.Debug;
import android.util.Log;

public class DBAdapter {
	private final static String Tag = "DBAdapter";
	
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
    

    public void addTrainEvents(final List<TrainEvent> l)
    {
        // (Shallow) Copy the list to avoid ConcurrentModificationException
        final List<TrainEvent> trainevents = new ArrayList<TrainEvent>(l);

        if (trainevents == null || trainevents.size() == 0) {
            return;
        }

        ScrapePool.addJob(new Runnable() {
            public void run()
            {
                if (TRACE) {
                    Debug.startMethodTracing("dbstore");
                }
                Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
                long s = System.currentTimeMillis();

                String station = trainevents.get(0).getStation().getName();
                int[] numbers = new int[trainevents.size()];
                int index = 0;
                for (TrainEvent te : trainevents) {
                    numbers[index++] = te.getNumber();
                    Log.v(Tag, "Exists 1: " + numbers[index-1]);
                }
                TrainEvent[] events = getTrainEvents(station, numbers);

                Set<Integer> existing_events = new HashSet<Integer>();
                Log.v(Tag, "Number of found pre-existing trains: " + events.length);
                for (int i = 0; i < events.length; i++) {
                    Log.v(Tag, "Exists 2: " + events[i]);
                    existing_events.add(Integer.valueOf(events[i].getNumber()));
                }
    
                int count = 0;
                for (TrainEvent te : trainevents) {
                    if (!existing_events.contains(Integer.valueOf(te.getNumber()))) {
                        addTrainEventImpl(te.getStation().getName(), te.getDepartureDate(), te.getTrack(), te.getNumber(), te
                                .getDelayedDate(), te.getExtra(), te.getDestination(), true);
                    } else {
                        // Might still need updating, extra or delay!
                        addTrainEventImpl(te.getStation().getName(), te.getDepartureDate(), te.getTrack(), te.getNumber(), te
                                .getDelayedDate(), te.getExtra(), te.getDestination(), false);
                    }
                    count++;
                }
                if (TRACE) {
                    Debug.stopMethodTracing();
                }
                Log.v(Tag, "Took " + (System.currentTimeMillis() - s) + " for " + count);
            }
        });
    }
    
    /**
     * Return all trainevents for this station. Culling?
     * 
     * @param station
     * @return
     */
    public List<TrainEvent> getStationEvents(String station)
    {
        ArrayList<TrainEvent> res = new ArrayList<TrainEvent>(100);

        Cursor c = db.query(TRAINEVENT_TABLE_NAME, 
            new String[] {
                TRAINEVENT_KEY_TIME, TRAINEVENT_KEY_EXTRA,
                TRAINEVENT_KEY_DELAY, TRAINEVENT_KEY_NUMBER, "_id",
                TRAINEVENT_KEY_DESTINATION, TRAINEVENT_KEY_TRACK },
                TRAINEVENT_KEY_STATION + "= ?", new String[] { station }, null,
                null, null);
        c.move(1);
        Calendar cal = Calendar.getInstance();
        //String cald = SIMPLE_DATEFORMATTER.format(cal);
        //-6 minute fuzz, arbitrary value for now
        //cal.add(Calendar.MINUTE, -4);
        
        Log.v(Tag, "Number of events in db: " + c.getCount());
        if (!c.isAfterLast() && !c.isBeforeFirst()) {
            while (!c.isAfterLast()) {
                TrainEvent te = new TrainEvent(null);
                te.setDeparture(c.getLong(0));
                te.setNumber(c.getInt(3));
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

                //Log.v(Tag, "Comparing " + te.getDepartureDate() + " " + cald);
                long now = cal.getTimeInMillis();
                long item = c.getLong(0);
                //Log.v(Tag, "Long: " + item + " " + SIMPLE_DATEFORMATTER.format(te.getDepartureDate()) + " " + cal.getTimeInMillis());

//                if(now < item) {
//                    Log.v(Tag, "Before!");
//                }
                //Calendar.before is broken??? Does not work for me anyway...
                if(te.getExtra().equals("")) {
                    if(now < (item + (1000*60*6)) || (te.getDelayedDate() == null || now < te.getDelayedDate().getTime())){
                        res.add(te);
                    } else {
                        //Remove it
                        Log.v(Tag, "Removing (empty extra) " + (now-item) + " " + SIMPLE_DATEFORMATTER.format(te.getDepartureDate()) + " " + SIMPLE_DATEFORMATTER.format(new Date(now)) + " #: " + te.getNumber() + " d: " + te.getDestination() + " tes departure: " + te.toString());
                        if(te.getDelayedDate() != null) {
                            Log.v(Tag, " Delayed until: " + SIMPLE_DATEFORMATTER.format(te.getDelayedDate()) + " " + now + " < " + te.getDelayedDate().getTime() + " " + SIMPLE_DATEFORMATTER.format(new Date(now)));
                        }
                        db.execSQL("delete from trainevents where _id = " + c.getInt(4), new Object[0]);
                    }
                } else {
                    Log.v(Tag, "Extra is set: " + te.getExtra());
                    if(now < (item + (1000*60*75))){
                        Log.v(Tag, "Adding: " + te.getNumber() + ":" + te.getStation());
                        Log.v(Tag, "Removing (non-empty extra) " + (now-item) + " " + SIMPLE_DATEFORMATTER.format(te.getDepartureDate()) + " " + SIMPLE_DATEFORMATTER.format(new Date(now)) + " #: " + te.getNumber() + " d: " + te.getDestination() + " tes departure: " + te.toString());
                        if(te.getDelayedDate() != null) {
                            Log.v(Tag, " Delayed until: " + SIMPLE_DATEFORMATTER.format(te.getDelayedDate()) + " " + now + " < " + te.getDelayedDate().getTime() + " " + SIMPLE_DATEFORMATTER.format(new Date(now)));
                        }
                        res.add(te);
                    } else {
                        Log.v(Tag, "Removing (non-empty extra) " + (now-item) + " " + SIMPLE_DATEFORMATTER.format(te.getDepartureDate()) + " " + SIMPLE_DATEFORMATTER.format(new Date(now)) + " #: " + te.getNumber() + " d: " + te.getDestination() + " tes departure: " + te.toString());
                        if(te.getDelayedDate() != null) {
                            Log.v(Tag, " Delayed until: " + SIMPLE_DATEFORMATTER.format(te.getDelayedDate()) + " " + now + " < " + te.getDelayedDate().getTime() + " " + SIMPLE_DATEFORMATTER.format(new Date(now)));
                        }
                        Log.v(Tag, "Removing due to non-empty extra, but too old: " + te.toString());
                        db.execSQL("delete from trainevents where _id = " + c.getInt(4), new Object[0]);
                    }
                }
                
                c.move(1);
            }

            c.close();
            return res;
        }

        c.close();
        return new ArrayList<TrainEvent>();
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
    public long addTrainEventImpl(String station, Date time, String track, int number, Date delay, String extra, String destination, boolean add)
    {
        if(!add) {
            Log.v(Tag, "Updating " + number);
            ContentValues cv = new ContentValues();
            if(delay != null) {
                Log.v(Tag, "Setting delayed: " + delay.getTime());
                cv.put(TRAINEVENT_KEY_DELAY, delay.getTime());
            } else {
                Log.v(Tag, "Setting null delayed");
                cv.putNull(TRAINEVENT_KEY_DELAY);
            }
            cv.put(TRAINEVENT_KEY_EXTRA, extra);
            Log.v(Tag, "Setting extra: " + extra);
            long res = db.update(TRAINEVENT_TABLE_NAME, cv, "" + TRAINEVENT_KEY_STATION + " = ? and " + TRAINEVENT_KEY_NUMBER + " = " + number, new String[]{station});
            Log.v(Tag, "Affected " + res);
            return res;
        }
        Log.v(Tag, "Adding " + number);
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
    
    public TrainEvent[] getTrainEvents(String station, int[] numbers)
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
				te.setNumber(c.getInt(3));
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
    
	private String expand(int[] numbers) {
		StringBuffer sb = new StringBuffer();
		
		for(int i=0; i<numbers.length; i++) {
			sb.append(numbers[i]);
			if(i < (numbers.length - 1)) {
				sb.append(", ");
			}
		}
		
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
