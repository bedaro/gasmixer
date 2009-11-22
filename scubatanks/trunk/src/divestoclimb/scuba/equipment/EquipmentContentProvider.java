package divestoclimb.scuba.equipment;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.util.Log;

public class EquipmentContentProvider extends ContentProvider {

	private static final String TABLE_CYLINDERSIZES = "cylindersizes";
	
	private static final String DATABASE_NAME="equipment";
	private static final int DATABASE_VERSION = 1;
	
	private static final String TAG = "EquipmentContentProvider";
	
	private DatabaseHelper mOpenHelper;
	
	// URI matching
	private static final int CYLINDERS = 1;
	private static final int SPECIFIC_CYLINDER = 2;
	private static final UriMatcher URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
	private static final ContentValues al80 = new ContentValues(), lp95s = new ContentValues();
	static {
		URI_MATCHER.addURI(CylinderSizeClient.CONTENT_URI.getAuthority(),
				"cylinders/sizes", CYLINDERS);
		URI_MATCHER.addURI(CylinderSizeClient.CONTENT_URI.getAuthority(),
				"cylinders/sizes/#", SPECIFIC_CYLINDER);
	}
	
	private static class DatabaseHelper extends SQLiteOpenHelper {

		DatabaseHelper(Context context, int version) {
			super(context, DATABASE_NAME, null, version);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			final String TABLE_CREATE_CYLINDERSIZES =
				"create table %1$s ("+
					"%2$s integer primary key autoincrement, "+
					"%3$s text unique not null, "+
					"%4$s real, "+
					"%5$s real"+
				")";
			db.execSQL(String.format(TABLE_CREATE_CYLINDERSIZES,
				TABLE_CYLINDERSIZES,
				CylinderSizeClient._ID,
				CylinderSizeClient.NAME,
				CylinderSizeClient.INTERNAL_VOLUME,
				CylinderSizeClient.SERVICE_PRESSURE
			));
			db.insert(TABLE_CYLINDERSIZES, null, al80);
			db.insert(TABLE_CYLINDERSIZES, null, lp95s);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			if(oldVersion <= 5) {
				Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
						+ newVersion + ", which will destroy all old data");
				db.execSQL("DROP TABLE IF EXISTS "+TABLE_CYLINDERSIZES);
				onCreate(db);
			}
		}
	}

	@Override
	public boolean onCreate() {
		Context c = getContext();
		mOpenHelper = new DatabaseHelper(c, DATABASE_VERSION);

		al80.put(CylinderSizeClient.NAME, c.getString(R.string.luxfer_aluminum_80));
		al80.put(CylinderSizeClient.INTERNAL_VOLUME, 11.11);
		al80.put(CylinderSizeClient.SERVICE_PRESSURE, 206.896551);
		lp95s.put(CylinderSizeClient.NAME, c.getString(R.string.double_steel_95s));
		lp95s.put(CylinderSizeClient.INTERNAL_VOLUME, 30);
		lp95s.put(CylinderSizeClient.SERVICE_PRESSURE, 182.0689965);

		return true;
	}

	@Override
	public String getType(Uri uri) {
		switch(URI_MATCHER.match(uri)) {
		case CYLINDERS:
			return "vnd.android.cursor.dir/vnd.divestoclimb.scuba.equipment.cylinders.size";
		case SPECIFIC_CYLINDER:
			return "vnd.android.cursor.item/vnd.divestoclimb.scuba.equipment.cylinders.size";
		default:
			return null;
		}
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		long new_id = db.insert(TABLE_CYLINDERSIZES, null, values);
		Uri newUri = Uri.withAppendedPath(CylinderSizeClient.CONTENT_URI,
				String.valueOf(new_id));
		getContext().getContentResolver().notifyChange(newUri, null);
		return newUri;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		SQLiteDatabase db = mOpenHelper.getReadableDatabase();
		SQLiteQueryBuilder qBuilder = new SQLiteQueryBuilder();
		qBuilder.setTables(TABLE_CYLINDERSIZES);
		
		boolean insert_if_null = false;
		if(URI_MATCHER.match(uri) == SPECIFIC_CYLINDER) {
			qBuilder.appendWhere(CylinderSizeClient._ID + "=" +
					uri.getLastPathSegment());
		} else {
			// We're returning a query of all cylinders in the Database. If the
			// query comes back empty, insert a default cylinder size into the
			// database and return that. There's no good reason for the DB to be
			// empty.
			insert_if_null = true;
		}
		Cursor c = qBuilder.query(db, projection, selection, selectionArgs,
				null, null, (sortOrder != null)? sortOrder:
				CylinderSizeClient.NAME);
		if(c == null && insert_if_null) {
			// Put a record into the database so we can re-query and return it
			db.insert(TABLE_CYLINDERSIZES, null, al80);
			c = qBuilder.query(db, projection, selection, selectionArgs, null, null, null);
		}
		if(c != null) {
			c.setNotificationUri(getContext().getContentResolver(), uri);
		}
		return c;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		String mySelection, myArgs[] = new String[1];
		if(URI_MATCHER.match(uri) != SPECIFIC_CYLINDER) {
			throw new IllegalArgumentException("Unknown URI");
		} else {
			mySelection = CylinderSizeClient._ID +"=?";
			myArgs[0] = uri.getLastPathSegment();
		}
		SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		int count = db.update(TABLE_CYLINDERSIZES, values, mySelection, myArgs);
		getContext().getContentResolver().notifyChange(uri, null);
		return count;
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		String mySelection, myArgs[] = new String[1];
		if(URI_MATCHER.match(uri) != SPECIFIC_CYLINDER) {
			throw new IllegalArgumentException("Unknown URI");
		} else {
			mySelection = CylinderSizeClient._ID +"=?";
			myArgs[0] = uri.getLastPathSegment();
		}
		SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		int rows = db.delete(TABLE_CYLINDERSIZES, mySelection, myArgs);
		getContext().getContentResolver().notifyChange(uri, null);
		return rows;
	}

}
