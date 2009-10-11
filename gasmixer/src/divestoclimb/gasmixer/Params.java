package divestoclimb.gasmixer;

import java.text.DecimalFormat;
import java.text.NumberFormat;

import android.content.Context;
import android.content.res.Resources;

import divestoclimb.lib.scuba.Mix;
import divestoclimb.lib.scuba.Units;

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

	/**
	 * Map values returned by Mix.getFriendlyName() to localized strings.
	 * There should be a better place for this to go but... there isn't, and
	 * I don't want to add Android library dependencies to divestoclimb.lib.scuba.
	 * @param m The Mix to get the friendly name of
	 * @param c The current context
	 */
	public static String mixFriendlyName(Mix m, Context c) {
		String fn = m.friendlyName();
		Resources r = c.getResources();
		if(fn == Mix.OXYGEN) {
			return r.getString(R.string.oxygen);
		} else if(fn == Mix.NITROGEN) {
			return r.getString(R.string.nitrogen);
		} else if(fn == Mix.HELIUM) {
			return r.getString(R.string.helium);
		} else if(fn == Mix.AIR) {
			return r.getString(R.string.air);
		} else {
			return fn;
		}
	}
	
	// Get the string value for the current unit of pressure
	public static String pressure(Context c, Units u) {
		return c.getResources().getString(u.pressureUnit() == Units.IMPERIAL? R.string.pres_imperial: R.string.pres_metric);
	}
	
	// Get the string value for the current unit of depth
	public static String depth(Context c, Units u) {
		return c.getResources().getString(u.depthUnit() == Units.DEPTH_FOOT? R.string.depth_imperial: R.string.depth_metric);
	}
	
}
