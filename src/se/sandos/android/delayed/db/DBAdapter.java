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

public class DBAdapter {
	private final Context context;
	
	private final static String DATABASE_NAME = "delayed";
	private final static int DATABASE_VERSION = 1;
	
	private static final String TABLE_NAME = "stations";
	
	private static final String KEY_NAME = "name";
	private static final String KEY_URLID = "urlid";
	
    private static final String DATABASE_CREATE =
        "create table stations (_id integer primary key autoincrement, "
        + "name text not null, urlid text not null) ";

	private DBHelper helper = null;
	private SQLiteDatabase db = null;
	
	public DBAdapter(Context ctx)
	{
		this.context = ctx;
		helper = new DBHelper(ctx);
	}
	
	public DBAdapter open() throws SQLException
	{
		db = helper.getWritableDatabase();
		return this;
	}
	
	public void close()
	{
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
		
		return new StationList(res);
	}
	
	public long addStation(String name, String urlid)
	{
		ContentValues cv = new ContentValues();
		cv.put(KEY_NAME, name);
		cv.put(KEY_URLID, urlid);
		return db.insert(TABLE_NAME, null, cv);
	}
	
	public int getNumberOfStations()
	{
		Cursor c = db.rawQuery("select count(*) from stations", null);
		c.move(1);
		return c.getInt(0);
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
