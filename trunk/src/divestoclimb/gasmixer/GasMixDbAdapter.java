package divestoclimb.gasmixer;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

// This is an adapter for a database of simple key/value pairs
// The database stores values as integers, but it is able to
// store decimal values to within a given number of decimal
// places.
public class GasMixDbAdapter {
	// The name field uses constant keys as defined in GasMixer to
	// define unique items by OR'ing.
	public final String KEY_NAME = "name";
	
	// This is the value of a key represented as an integer
	public final String KEY_VALUE = "value";
	
	// This is the number of decimal places value has. The true value
	// is value/10^places
	public final String KEY_PLACES = "places";
	
	public static final String TAG = "GasMixDbAdapter";
    private DatabaseHelper mDbHelper;
    private SQLiteDatabase mDb;
	
	private static final String DATABASE_CREATE =
		"CREATE TABLE current_values (name VARCHAR(20) PRIMARY KEY NOT NULL, value INT, places INT)";
	private static final String DATABASE_NAME = "data";
	private static final String DATABASE_TABLE = "current_values";

	private static final int DATABASE_VERSION = 1;
	
	private final Context mCtx;
	
	private static class DatabaseHelper extends SQLiteOpenHelper {
		DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {

            db.execSQL(DATABASE_CREATE);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
                    + newVersion + ", which will destroy all old data");
            db.execSQL("DROP TABLE IF EXISTS current_values");
            onCreate(db);
        }
    }
	
	/**
     * Constructor - takes the context to allow the database to be
     * opened/created
     * 
     * @param ctx the Context within which to work
     */
    public GasMixDbAdapter(Context ctx) {
        this.mCtx = ctx;
    }
    
    /**
     * Open the database. If it cannot be opened, try to create a new
     * instance of the database. If it cannot be created, throw an exception to
     * signal the failure
     * 
     * @return this (self reference, allowing this to be chained in an
     *         initialization call)
     * @throws SQLException if the database could be neither opened or created
     */
    public GasMixDbAdapter open() throws SQLException {
        mDbHelper = new DatabaseHelper(mCtx);
        mDb = mDbHelper.getWritableDatabase();
        return this;
    }
    
    public void close() {
        mDbHelper.close();
    }
    
    /**
     * Save a setting to the database.
     * @param name The name of the setting to save.
     *             If a setting with this name already
     *             exists, the old setting will be replaced.
     * @param value The value of the setting
     * @param places The number of decimal places of value to keep.
     * @return int (1 if successful, 0 if failed)
     */
    public int saveSetting(String name, float value, int places) {
    	// First check if a setting with the same name already exists
    	Cursor check = mDb.query(DATABASE_TABLE, new String[] { KEY_NAME }, KEY_NAME + "=?", new String[] { name }, null, null, null);

		ContentValues args = new ContentValues();
		args.put(KEY_NAME, name);
		args.put(KEY_VALUE, (int)Math.round(value*Math.pow(10, places)));
		args.put(KEY_PLACES, places);
		
    	if((check != null) && check.moveToFirst()) {
    		// do an update

    		return mDb.update(DATABASE_TABLE, args, KEY_NAME + "=?", new String[] { name });
    	} else {
    		// do an insert
    		
    		return mDb.insert(DATABASE_TABLE, null, args) == -1? 0: 1;
    	}
    }
    
    /**
     * Retrieve a value from the database
     * @param name The name of the setting to retrieve
     * @return The setting in its original decimal form. If no setting exists, return -1
     */
    public float fetchSetting(String name) {
    	Cursor c = mDb.query(DATABASE_TABLE, new String[] { KEY_VALUE, KEY_PLACES }, KEY_NAME + "=?", new String[] { name }, null, null, null);
    	if((c != null) && c.moveToFirst()) {
    		return (float) (c.getInt(c.getColumnIndexOrThrow(KEY_VALUE)) / Math.pow(10, c.getInt(c.getColumnIndexOrThrow(KEY_PLACES))));
    	} else {
    		return -1;
    	}
    }
}
