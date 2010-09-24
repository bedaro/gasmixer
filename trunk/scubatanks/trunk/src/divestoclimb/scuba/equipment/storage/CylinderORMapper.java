package divestoclimb.scuba.equipment.storage;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import divestoclimb.lib.data.Record;
import divestoclimb.lib.scuba.Cylinder;
import divestoclimb.lib.scuba.Units;

public class CylinderORMapper {

	public static final Uri CONTENT_URI = Uri.parse("content://divestoclimb.scuba.equipment/cylinders/sizes");

	// Integer. The unique key for the cylinder size
	public static final String _ID = "_id";
	// String. A user-recognizable name for the cylinder
	public static final String NAME = "name";
	// Real. The internal volume of the cylinder in liters
	public static final String INTERNAL_VOLUME = "internalVolume";
	// Real. The service pressure of the cylinder in bar
	public static final String SERVICE_PRESSURE = "servicePressure";
	
	protected Context mCtx;
	protected Units mUnits;

	protected Cylinder mFlyCylinder;
	
	public CylinderORMapper(Context ctx, Units units) {
		mCtx = ctx;
		mUnits = units;
		
		mFlyCylinder = new Cylinder(mUnits, 0, 0);
	}
	
	public Units getUnits() { return mUnits; }
	
	public Cursor fetchCylinders() {
		return mCtx.getContentResolver().query(CONTENT_URI, null, null, null, null);
	}
	
	public Cylinder fetchCylinder(long id) {
		final Cursor c = mCtx.getContentResolver().query(
				Uri.withAppendedPath(CONTENT_URI, String.valueOf(id)
				), null, null, null, null);
		if(c != null) {
			c.moveToFirst();
			final Cylinder cylinder = fetchCylinder(c);
			c.close();
			return cylinder;
		}
		return null;
	}
	
	public Cylinder fetchCylinder(Cursor c) { return fetchCylinder(c, false); }
	public Cylinder fetchCylinder(Cursor c, boolean useFlyweight) { return fetchCylinder(c, useFlyweight? mFlyCylinder: null); }
	public Cylinder fetchCylinder(Cursor c, Cylinder instance) {
		final long id = c.getLong(c.getColumnIndexOrThrow(_ID));
		final float internalVolume = mUnits.convertCapacity(c.getFloat(c.getColumnIndexOrThrow(INTERNAL_VOLUME)), Units.METRIC);
		final int servicePressure = Math.round(mUnits.convertPressure(c.getFloat(c.getColumnIndexOrThrow(SERVICE_PRESSURE)), Units.METRIC));
		final String name = c.getString(c.getColumnIndexOrThrow(NAME));
		if(instance == null) {
			instance = new Cylinder(mUnits, id, name, internalVolume, servicePressure);
		} else {
			instance.reset(id, name, internalVolume, servicePressure);
		}
		instance.setUpdater(mCylinderUpdater);
		return instance;
	}
	
	public static ContentValues getCylinderValues(Cylinder c) {
		ContentValues values = new ContentValues();
		values.put(NAME, c.getName());
		values.put(INTERNAL_VOLUME, Units.convertCapacity(c.getInternalVolume(),
				c.getUnits().getCurrentSystem(), Units.METRIC));
		values.put(SERVICE_PRESSURE, Units.convertPressure(c.getServicePressure(),
				c.getUnits().getCurrentSystem(), Units.METRIC));
		return values;
	}
	
	public Record.Updater mCylinderUpdater = new Record.Updater() {
		@Override
		public long doCreate(Record cylinder) {
			Uri newItem = mCtx.getContentResolver().insert(CONTENT_URI, getCylinderValues((Cylinder)cylinder));
			if(newItem == null) {
				return -1;
			}
			return Long.parseLong(newItem.getLastPathSegment());
		}

		@Override
		public boolean doDelete(Record cylinder) {
			return deleteCylinder(cylinder.getId());
		}

		@Override
		public boolean doUpdate(Record cylinder) {
			final Uri uri = Uri.withAppendedPath(CONTENT_URI, String.valueOf(cylinder.getId()));
			return mCtx.getContentResolver().update(uri, getCylinderValues((Cylinder)cylinder), null, null) > 0;
		}
	};
	
	public boolean deleteCylinder(long cylinder_id) {
		final Uri uri = Uri.withAppendedPath(CONTENT_URI, String.valueOf(cylinder_id));
		return mCtx.getContentResolver().delete(uri, null, null) > 0;
	}
}