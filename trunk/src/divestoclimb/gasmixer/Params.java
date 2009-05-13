package divestoclimb.gasmixer;

import java.text.DecimalFormat;
import java.text.NumberFormat;

public class Params {
	// This is the minimum percentage of O2 that's allowed in any mix.
	public static final int O2_LOWER_LIMIT=5;
	
	// This is our number format object
	public static DecimalFormat mMixPercent = new DecimalFormat("#0.0");
	
	// Use default formatting for pressures
	public static NumberFormat getPressureFormat() {
		if(Units.pressureUnit() == Units.PRESSURE_PSI) {
			return NumberFormat.getIntegerInstance();
		} else if(Units.pressureUnit() == Units.PRESSURE_BAR) {
			return new DecimalFormat("#0.0");
		} else {
			return new DecimalFormat("#0.0");
		}
	}
	//public static NumberFormat mPressure = NumberFormat.getIntegerInstance();
}
