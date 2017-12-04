package cs.umass.edu.myactivitiestoolkit.location;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * This is the basic component to create and manage the database. 
 * This class should only be used in the {@link GeneralDAO}.
 * 
 */
public class MyDBHelper extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 1;
	private static final String DATABASE_NAME = "eventreports";
	private static final String TAG = "MyDBHelper";

	public MyDBHelper(Context context) {
		this(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	public MyDBHelper(Context context, String name, CursorFactory factory, int version) {
		super(context, name, factory, version);
		Log.d(TAG,"database initialized");
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(LocationDAO.TABLE_CREATE);
		Log.d(TAG,"table " + LocationDAO.TABLE_NAME + " was created");

				
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		
		//Utils.d(this, "===========DROPPING DBs====== old: "+oldVersion+"====new: "+newVersion+"=========");

		// clear old schema and data
		//db.execSQL("DROP TABLE IF EXISTS " + ReportDAO.TABLE_NAME);
		
		// create new schema
		//onCreate(db);

	}

}
