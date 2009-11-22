package divestoclimb.scuba.equipment;

import java.text.DecimalFormat;
import java.text.NumberFormat;

import android.content.Context;

import divestoclimb.lib.scuba.Units;

public class Params {
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
	
	// Get the string value for the current unit of pressure
	public static CharSequence pressure(Context c, Units u) {
		return c.getText(u.pressureUnit() == Units.IMPERIAL? R.string.pres_imperial: R.string.pres_metric);
	}
	
	public static CharSequence volume(Context c, Units u) {
		return c.getText(u.volumeUnit() == Units.IMPERIAL? R.string.volume_imperial: R.string.volume_metric);
	}
	
	public static CharSequence capacity(Context c, Units u) {
		return c.getText(u.capacityUnit() == Units.IMPERIAL? R.string.capacity_imperial: R.string.capacity_metric);
	}
}
