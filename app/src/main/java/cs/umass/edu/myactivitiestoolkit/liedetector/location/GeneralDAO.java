package cs.umass.edu.myactivitiestoolkit.liedetector.location;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

/**
 * This component is a general data access object handling the live cycle of the
 * connection. Other DAOs should subclass this component.
 * 
 * @author Matthias Boehmer, matthias.boehmer@dfki.de
 */
public abstract class GeneralDAO {

    private Context context;
	private MyDBHelper dbHelper;
	protected SQLiteDatabase db;
	
	private static final String TAG ="GeneralDAO";

	public GeneralDAO(Context context) {
		this.context = context;
		dbHelper = new MyDBHelper(this.context);
	}

	public GeneralDAO open() {
		db = dbHelper.getWritableDatabase();
		return this;
	}
	
	public GeneralDAO openIfNotAlready() {
		if(db==null|| !db.isOpen())
			db = dbHelper.getWritableDatabase();
		return this;
	}

	public GeneralDAO openWrite() {
		db = dbHelper.getWritableDatabase();
		return this;
	}

	public GeneralDAO openRead() {
		db = dbHelper.getReadableDatabase();
		//db = dbHelper.getWritableDatabase();
		Log.d(TAG, "db opened4read :-)");
		return this;
	}

	public void close() {
		db.close();
		Log.d(TAG, "db closed :-(");
	}
	
}
