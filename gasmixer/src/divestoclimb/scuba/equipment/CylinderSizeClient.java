package divestoclimb.scuba.equipment;

import divestoclimb.lib.scuba.Cylinder;
import divestoclimb.lib.scuba.Units;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

public class CylinderSizeClient {

	public static final Uri CONTENT_URI = Uri.parse("content://divestoclimb.scuba.equipment/cylinders/sizes");

	// Integer. The unique key for the cylinder size
	public static final String _ID = "_id";
	// String. A user-recognizable name for the cylinder
	public static final String NAME = "name";
	// Real. The internal volume of the cylinder in liters
	public static final String INTERNAL_VOLUME = "internalVolume";
	// Real. The service pressure of the cylinder in bar
	public static final String SERVICE_PRESSURE = "servicePressure";

	/**
	 * Does the object mapping for a Cylinder object from a Cursor record
	 * @param c The Cursor containing cylinder information, advanced to
	 * the position of the cylinder to build an object for.
	 * @return A Cylinder object containing the same information, converted into
	 * the unit system in use.
	 */
	public static Cylinder cursorToCylinder(Cursor c, Units units) {
		float internal_volume = c.getFloat(c.getColumnIndexOrThrow(INTERNAL_VOLUME)),
				service_pressure = c.getFloat(c.getColumnIndexOrThrow(SERVICE_PRESSURE));
		return new Cylinder(units, 
				units.convertCapacity(internal_volume, Units.METRIC),
				(int)Math.round(units.convertPressure(service_pressure, Units.METRIC))
		);
	}
	
	public static ContentValues cylinderToContentValues(Cylinder c, String name) {
		ContentValues values = new ContentValues();
		values.put(NAME, name);
		values.put(INTERNAL_VOLUME, Units.convertCapacity(c.getInternalVolume(),
				c.getUnits().getCurrentSystem(), Units.METRIC));
		values.put(SERVICE_PRESSURE, Units.convertPressure(c.getServicePressure(),
				c.getUnits().getCurrentSystem(), Units.METRIC));
		return values;
	}
}