package divestoclimb.scuba.equipment.storage;

import divestoclimb.lib.scuba.Cylinder;
import divestoclimb.lib.scuba.Units;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/**
 * Version of CylinderORMapper that bypasses (mostly) the ContentProvider.
 * Not 100% implemented.
 * @author Ben Roberts (divestoclimb@gmail.com)
 */
class DirectCylinderORMapper extends CylinderORMapper {

	private SQLiteDatabase mDb;

	public DirectCylinderORMapper(Context ctx, SQLiteDatabase db) {
		super(ctx);
		mDb = db;
	}
	
	public DirectCylinderORMapper(Context ctx, Units units, SQLiteDatabase db) {
		super(ctx, units);
		mDb = db;
	}
	
	@Override
	protected Cursor doQuery(String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		return mDb.query(EquipmentContentProvider.TABLE_CYLINDERSIZES, projection, selection, selectionArgs, null, null, sortOrder);
	}
	
	// WARNING: the fetchCylinder method bypasses the doQuery protected method. Calling
	// that method in this class will go through the ContentProvider.
	
	@Override
	protected boolean doCreate(Cylinder c, ContentValues values) {
		final long new_id = mDb.insert(EquipmentContentProvider.TABLE_CYLINDERSIZES, null, values);
		if(new_id != -1) {
			c.setId(new_id);
		}
		return new_id != -1;
	}
	
	@Override
	protected boolean doUpdate(Cylinder c, ContentValues values) {
		final int count = mDb.update(EquipmentContentProvider.TABLE_CYLINDERSIZES, values,
				CylinderORMapper._ID + "=?", new String[] { String.valueOf(c.getId()) });
		return count > 0;
	}
}