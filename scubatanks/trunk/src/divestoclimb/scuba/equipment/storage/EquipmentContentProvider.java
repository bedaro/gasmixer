package divestoclimb.scuba.equipment.storage;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;

public class EquipmentContentProvider extends ContentProvider {

	static final String TABLE_CYLINDERSIZES = "cylinders";
	
	private DatabaseHelper mOpenHelper;
	
	// URI matching
	private static final int CYLINDERS = 1;
	private static final int SPECIFIC_CYLINDER = 2;
	private static final String MIME_LEAF = "vnd.divestoclimb.scuba.equipment.cylinders";
	private static final UriMatcher URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
	static {
		URI_MATCHER.addURI(CylinderORMapper.CONTENT_URI.getAuthority(),
				"cylinders/sizes", CYLINDERS);
		URI_MATCHER.addURI(CylinderORMapper.CONTENT_URI.getAuthority(),
				"cylinders", CYLINDERS);
		URI_MATCHER.addURI(CylinderORMapper.CONTENT_URI.getAuthority(),
				"cylinders/sizes/#", SPECIFIC_CYLINDER);
		URI_MATCHER.addURI(CylinderORMapper.CONTENT_URI.getAuthority(),
				"cylinders/#", SPECIFIC_CYLINDER);
	}

	@Override
	public boolean onCreate() {
		Context c = getContext();
		mOpenHelper = new DatabaseHelper(c);

		return true;
	}

	@Override
	public String getType(Uri uri) {
		switch(URI_MATCHER.match(uri)) {
		case CYLINDERS:
			return "vnd.android.cursor.dir/" + MIME_LEAF;
		case SPECIFIC_CYLINDER:
			return "vnd.android.cursor.item/" + MIME_LEAF;
		default:
			return null;
		}
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		final long new_id = db.insert(TABLE_CYLINDERSIZES, null, values);
		if(new_id == -1) {
			return null;
		}
		final Uri newUri = Uri.withAppendedPath(CylinderORMapper.CONTENT_URI,
				String.valueOf(new_id));
		getContext().getContentResolver().notifyChange(newUri, null);
		return newUri;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		final SQLiteDatabase db = mOpenHelper.getReadableDatabase();
		final SQLiteQueryBuilder qBuilder = new SQLiteQueryBuilder();
		qBuilder.setTables(TABLE_CYLINDERSIZES);

		if(URI_MATCHER.match(uri) == SPECIFIC_CYLINDER) {
			qBuilder.appendWhere(CylinderORMapper._ID + "=" +
					uri.getLastPathSegment());
		}
		final Cursor c = qBuilder.query(db, projection, selection, selectionArgs,
				null, null, (sortOrder != null)? sortOrder:
				CylinderORMapper.NAME);
		if(c != null) {
			c.setNotificationUri(getContext().getContentResolver(), uri);
		}
		return c;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		final String mySelection, myArgs[] = new String[1];
		if(URI_MATCHER.match(uri) != SPECIFIC_CYLINDER) {
			throw new IllegalArgumentException("Unknown URI");
		} else {
			mySelection = CylinderORMapper._ID +"=?";
			myArgs[0] = uri.getLastPathSegment();
		}
		final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		final int count = db.update(TABLE_CYLINDERSIZES, values, mySelection, myArgs);
		getContext().getContentResolver().notifyChange(uri, null);
		return count;
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		final String mySelection, myArgs[] = new String[1];
		if(URI_MATCHER.match(uri) != SPECIFIC_CYLINDER) {
			throw new IllegalArgumentException("Unknown URI");
		} else {
			mySelection = CylinderORMapper._ID +"=?";
			myArgs[0] = uri.getLastPathSegment();
		}
		final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		final int rows = db.delete(TABLE_CYLINDERSIZES, mySelection, myArgs);
		getContext().getContentResolver().notifyChange(uri, null);
		return rows;
	}

}