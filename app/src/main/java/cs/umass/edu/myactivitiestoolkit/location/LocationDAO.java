package cs.umass.edu.myactivitiestoolkit.location;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.LinkedList;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.util.Log;

/**
 * Data access object for reports
 * 
 * @author Abhinav Parate
 */
public class LocationDAO extends GeneralDAO {

	// --------------------------------------------
	// SCHEMA
	// --------------------------------------------

	public static String TABLE_NAME = "reports";
	
	public static final String TAG = "LocationDAO";

	public static final String CNAME_ID = "_id";
	public static final String CNAME_TIMESTAMP = "timestamp";
	public static final String CNAME_LATITUDE = "latitude";
	public static final String CNAME_LONGITUDE = "longitude";
	public static final String CNAME_ACCURACY = "accuracy";
	

	public static final String[] PROJECTION = {
		CNAME_ID,
		CNAME_TIMESTAMP,
		CNAME_LATITUDE,
		CNAME_LONGITUDE,
		CNAME_ACCURACY
	};

	public final static int CNUM_ID = 0;
	public final static int CNUM_TIMESTAMP = 1;
	public final static int CNUM_LATITUDE = 2;
	public final static int CNUM_LONGITUDE = 3;
	public final static int CNUM_ACCURACY = 4;


	public static final String TABLE_CREATE = "CREATE TABLE " + TABLE_NAME + " (" +
			CNAME_ID + " INTEGER PRIMARY KEY, " +
			CNAME_TIMESTAMP + " LONG, " +
			CNAME_LATITUDE + " REAL, " +
			CNAME_LONGITUDE + " REAL, " +
			CNAME_ACCURACY + " REAL " +
			");";

	// --------------------------------------------
	// QUERIES
	// --------------------------------------------

	private final static String WHERE_ID = CNAME_ID + "=?";
	private final static String WHERE_TIME_RANGE = CNAME_TIMESTAMP + ">=?"+" AND "+CNAME_TIMESTAMP + "<=?";

	// --------------------------------------------
	// LIVECYCLE
	// --------------------------------------------

	public LocationDAO(Context context) {
		super(context);
	}

	// --------------------------------------------
	// QUERY IMPLEMENTATIONS
	// --------------------------------------------
	
	public GPSLocation getLocationById(int id) {
		Cursor c = db.query(
				TABLE_NAME, 
				PROJECTION, 
				WHERE_ID, 
				new String[]{id+""}, 
				null, 
				null, 
				null);
		return cursor2location(c);
	}
	
	public GPSLocation[] getLocationByTimeRange(long startTime, long endTime) {
		Cursor c = db.query(
				TABLE_NAME, 
				PROJECTION, 
				WHERE_TIME_RANGE, 
				new String[]{startTime+"",endTime+""}, 
				null, 
				null, 
				null);
		return cursor2locations(c);
	}
	
	public GPSLocation[] getAllLocations() {
		Cursor c = db.query(
				TABLE_NAME, 
				PROJECTION, 
				null, 
				null, 
				null, 
				null, 
				CNAME_TIMESTAMP+" DESC");
		return cursor2locations(c);
	}

	// --------------------------------------------
	// UPDATES
	// --------------------------------------------
		
	
	public void insert(GPSLocation r) {
		ContentValues cv = location2ContentValues(r);
		db.insert(TABLE_NAME, null, cv);
	}

	public void update(GPSLocation r) {
		ContentValues values = location2ContentValues(r);
		db.update(TABLE_NAME, values , WHERE_ID, new String[]{r.id+""});
	}

	public void delete(GPSLocation r) {
		Log.d(TAG,"delete report " + r.id);
		db.delete(TABLE_NAME, WHERE_ID, new String[]{r.id+""});
	}

	public void deleteAll() {
		Log.d(TAG,"delete all from " + TABLE_NAME);
		db.delete(TABLE_NAME, null, null);
	}

	// --------------------------------------------
	// LOCATION-CURSOR TRANSFORMATION UTILITIES
	// --------------------------------------------

	private static GPSLocation cursor2location(Cursor c) {
		c.moveToFirst();
		GPSLocation r = new GPSLocation();
		r.id = c.getInt(CNUM_ID); 
		r.timestamp =c.getLong(CNUM_TIMESTAMP);
		r.latitude = c.getDouble(CNUM_LATITUDE);
		r.longitude = c.getDouble(CNUM_LONGITUDE);
		r.accuracy = c.getFloat(CNUM_ACCURACY);
		return r;
	}

	public static GPSLocation[] cursor2locations(Cursor c) {
		c.moveToFirst();
		LinkedList<GPSLocation> locations = new LinkedList<GPSLocation>();
		while(!c.isAfterLast()){
			GPSLocation r = new GPSLocation();
			r.id = c.getInt(CNUM_ID); 
			r.timestamp =c.getLong(CNUM_TIMESTAMP);
			r.latitude = c.getDouble(CNUM_LATITUDE);
			r.longitude = c.getDouble(CNUM_LONGITUDE);
			r.accuracy = c.getFloat(CNUM_ACCURACY);
			locations.add(r);
			c.moveToNext();
		}
		return locations.toArray(new GPSLocation[locations.size()]);
	}

	private static ContentValues location2ContentValues(GPSLocation r) {
		ContentValues cv = new ContentValues();
		cv.put(CNAME_TIMESTAMP, r.timestamp);
		cv.put(CNAME_LATITUDE, r.latitude);
		cv.put(CNAME_LONGITUDE, r.longitude);
		cv.put(CNAME_ACCURACY, r.accuracy);
		return cv;
	}

	public static String getISOTimeString(long time) {
		Calendar gc = GregorianCalendar.getInstance();
		gc.setTimeInMillis(time);
		String AM = "AM";
		int day = gc.get(Calendar.DAY_OF_MONTH);
		String ds = (day<10?"0":"")+day;
		int month = (gc.get(Calendar.MONTH)+1);
		String ms = (month<10?"0":"")+month;
		int hour = gc.get(Calendar.HOUR_OF_DAY);
		String hs = "";
		if(hour>=12){ AM = "PM"; if(hour>12) hour = hour-12;}
		hs = (hour<10?"0":"")+hour;
		int min = gc.get(Calendar.MINUTE);
		String mins = (min<10?"0":"")+min;
		String s = gc.get(Calendar.YEAR)+"-"+ms+"-"+ds+" "+hs+":"+mins+" "+AM;
		return s;
	}


}
