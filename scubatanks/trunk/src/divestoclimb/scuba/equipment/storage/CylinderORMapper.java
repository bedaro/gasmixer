package divestoclimb.scuba.equipment.storage;

import java.lang.reflect.Method;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import divestoclimb.android.database.ORMapper;
import divestoclimb.lib.scuba.Cylinder;
import divestoclimb.lib.scuba.Units;

public class CylinderORMapper extends ORMapper<Cylinder> {

	public static final Uri CONTENT_URI = Uri.parse("content://divestoclimb.scuba.equipment/cylinders/sizes");

	// Integer. The unique key for the cylinder size
	public static final String _ID = "_id";
	// Text. A user-recognizable name for the cylinder
	public static final String NAME = "name";
	// Real. The internal volume of the cylinder in liters
	public static final String INTERNAL_VOLUME = "internalVolume";
	// Real. The service pressure of the cylinder in bar
	public static final String SERVICE_PRESSURE = "servicePressure";
	// Integer. The type of cylinder record (TYPE_GENERIC or TYPE_SPECIFIC)
	public static final String TYPE = "cylinderType";
	// Text. The serial number
	public static final String SERIAL_NUMBER = "serialNumber";
	// Text. The date of the last hydro test
	public static final String LAST_HYDRO = "lastHydro";
	// Text. The date of the last VIP
	public static final String LAST_VISUAL = "lastVisual";
	// Integer. Overridden hydro test interval in years
	public static final String HYDRO_INTERVAL_YEARS = "hydroIntervalYears";
	// Integer. Overridden VIP interval in months
	public static final String VISUAL_INTERVAL_MONTHS = "visualIntervalMonths";
	
	private static final DateFormat TEST_DATE_FORMAT = new SimpleDateFormat("yyyy-MM");
	
	protected Context mCtx;
	protected Units mUnits;
	
	public CylinderORMapper(Context ctx) {
		this(ctx, new Units(Units.METRIC));
	}

	public CylinderORMapper(Context ctx, Units units) {
		super(Cylinder.class);
		setPrimaryKey("Id");
		setDateFormat(TEST_DATE_FORMAT);
		ignoreField(new String[] { "Units", "IdealCapacity", "VdwCapacity" });
		mapField("Id", _ID);
		mapField("Type", TYPE);
		mapField("HydroInterval", HYDRO_INTERVAL_YEARS);
		mapField("VisualInterval", VISUAL_INTERVAL_MONTHS);

		mCtx = ctx;
		mUnits = units;
	}
	
	@Override
	protected Cylinder createObjectInstance() {
		return new Cylinder(mUnits, 0, 0);
	}
	
	public Units getUnits() { return mUnits; }
	
	@Override
	protected Cursor doQuery(String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		return mCtx.getContentResolver().query(CONTENT_URI, projection, selection, selectionArgs, sortOrder);
	}

	public Cursor fetchCylinders() {
		return doQuery(null, null, null, null);
	}
	
	public Cursor fetchCylinders(int type) {
		return doQuery(null, TYPE + "=?",
				new String[] { String.valueOf(type) }, null);
	}
	
	public Cursor fetchCylindersByName(String name) {
		return doQuery(new String[] { _ID },
				NAME + "=?", new String[] { name }, null);
	}
	
	public Cursor fetchCylindersBySerialNumber(String serialNumber) {
		return doQuery(new String[] { _ID },
				SERIAL_NUMBER + "=?", new String[] { serialNumber }, null);
	}
	
	public Cylinder fetchCylinder(long id) {
		final Cursor c = mCtx.getContentResolver().query(
				Uri.withAppendedPath(CONTENT_URI, String.valueOf(id)),
				null, null, null, null);
		if(c != null) {
			c.moveToFirst();
			final Cylinder cylinder = fetch(c);
			c.close();
			return cylinder;
		}
		return null;
	}
	
	@Override
	protected void columnToField(Cursor c, Cylinder instance, Method setter) {
		final String fieldName = setter.getName().substring(3);
		if(fieldName.equals("ServicePressure")) {
			final int servicePressure = Math.round(mUnits.convertPressure(c.getFloat(c.getColumnIndexOrThrow(SERVICE_PRESSURE)), Units.METRIC));
			instance.setServicePressure(servicePressure);
		} else if(fieldName.equals("InternalVolume")) {
			final float internalVolume = mUnits.convertCapacity(c.getFloat(c.getColumnIndexOrThrow(INTERNAL_VOLUME)), Units.METRIC);
			instance.setInternalVolume(internalVolume);
		} else {
			super.columnToField(c, instance, setter);
		}
	}
	
	@Override
	protected void fieldToColumn(Method getter, Object value, Cylinder o, ContentValues values) {
		final String fieldName = getter.getName().substring(3);
		if(fieldName.equals("InternalVolume")) {
			values.put(INTERNAL_VOLUME, Units.convertCapacity((Float)value,
					o.getUnits().getCurrentSystem(), Units.METRIC));
		} else if(fieldName.equals("ServicePressure")) {
			values.put(SERVICE_PRESSURE, Units.convertPressure((Integer)value,
					o.getUnits().getCurrentSystem(), Units.METRIC));
		} else {
			super.fieldToColumn(getter, value, o, values);
		}
	}
	
	@Override
	protected boolean doCreate(Cylinder c, ContentValues values) {
		final Uri newItem = mCtx.getContentResolver().insert(CONTENT_URI, values);
		if(newItem == null) {
			return false;
		}
		try {
			c.setId(Long.parseLong(newItem.getLastPathSegment()));
			return true;
		} catch(NumberFormatException e) {
			return false;
		}
	}
	
	@Override
	protected boolean doUpdate(Cylinder c, ContentValues values) {
		final Uri uri = Uri.withAppendedPath(CONTENT_URI, String.valueOf(c.getId()));
		return mCtx.getContentResolver().update(uri, values, null, null) > 0;
	}
	
	@Override
	public boolean doDelete(Cylinder c) {
		return deleteCylinder(c.getId());
	}
	
	public boolean deleteCylinder(long cylinder_id) {
		final Uri uri = Uri.withAppendedPath(CONTENT_URI, String.valueOf(cylinder_id));
		return mCtx.getContentResolver().delete(uri, null, null) > 0;
	}
}