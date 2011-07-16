package divestoclimb.gasmixer;

import java.text.DecimalFormat;
import java.text.NumberFormat;

import divestoclimb.lib.scuba.Units;

/**
 * A class of static methods that provides some common functionality to the entire
 * application.
 * @author Ben Roberts (divestoclimb@gmail.com)
 */
public class Params {
	// The state preference filename
	public static final String STATE_NAME = "State";
	
	// This is our number format object
	public static DecimalFormat mMixPercent = new DecimalFormat("#0.0");
	public static DecimalFormat mPartialPressure = new DecimalFormat("#0.0");
	
	// Use default formatting for pressures
	public static NumberFormat getPressureFormat(Units u) {
		if(u.pressureUnit() == Units.PRESSURE_PSI) {
			return NumberFormat.getIntegerInstance();
		} else if(u.pressureUnit() == Units.PRESSURE_BAR) {
			return new DecimalFormat("#0.0");
		} else {
			return new DecimalFormat("#0.0");
		}
	}
	
	public static NumberFormat getCapacityFormat(Units u) {
		// Right now all unit systems use the same precision for capacities.
		return NumberFormat.getIntegerInstance();
	}
	
}