package divestoclimb.lib.scuba;

// This class is a container for a bunch of static methods which are
// dependent on the current system of units in use in a SCUBA application.
// This allows the rest of the application to be unit-independent.
// It defines a heirarchy of types:
// - Unit System: an arbitrary collection of units, one for each
//   dimension of measurement available (length, mass, time, temperature, etc.).
//   Real life examples in the strictest sense are SI and CGS.
// - Unit: a defined value representing a dimension of measurement (foot, meter,
//   kilogram, Pascal, etc.)
// The application using this class should reference unit systems as
// a way to change all the customary units of measurement and for
// conversions. Any other decision that needs to be made regarding
// units should be done via the other supplied methods, or as a last
// resort the different *Unit methods to compare what the actual
// unit is you're dealing with.
public class Units {
	
	// The unit systems for this app
	public static final int IMPERIAL = 0;
	public static final int METRIC = 1;
	
	// The current units in effect. Defaults to imperial
	private static int unitSystem = IMPERIAL;
	
	// Set the current system of units. Pass either Units.IMPERIAL
	// or Units.METRIC as the argument.
	// Returns true if successful, false if not (i.e. an invalid
	// argument was passed)
	public static boolean change(int u) {
		if(u == IMPERIAL || u == METRIC) {
			unitSystem=u;
			return true;
		} else {
			return false;
		}
	}
	
	// Get the current units system
	// DO NOT RELY ON THIS TO TELL YOU WHAT SPECIFIC UNITS ARE IN
	// EFFECT! This function should only be used by unit system
	// switching routines.
	public static int getCurrentSystem() {
		return unitSystem;
	}
	
	// The different units for volumes and capacities
	// This is a little complicated because a capacity measurement and a
	// volume measurement may be in two different unit systems where cylinders
	// are concerned.
	public static final int VOLUME_CUIN = 0;
	public static final int VOLUME_LITER = 1;
	public static final int CAPACITY_CUFT = 0;
	public static final int CAPACITY_LITER = 1;
	
	private static final float volume_increment[] = { 1, 0.5f };
	public static float volumeIncrement() { return volume_increment[unitSystem]; }

	private static final float capacity_increment[] = { 1, 1 };
	public static float capacityIncrement() { return capacity_increment[unitSystem]; }

	private static final int volume_norm_tank[] = { 678, 12 };
	public static float volumeNormalTank() { return volume_norm_tank[unitSystem]; }

	private static int capacity_precision[] = { 1, 0 };
	public static int capacityPresision() { return capacity_precision[unitSystem]; }

	private static int volume_precision[] = { 0, 1 };
	public static int volumePrecision() { return volume_precision[unitSystem]; }

	public static int volumeUnit() {
		return unitSystem == IMPERIAL? VOLUME_CUIN: VOLUME_LITER;
	}

	public static int capacityUnit() {
		return unitSystem == IMPERIAL? CAPACITY_CUFT: CAPACITY_LITER;
	}

	public static float capacityToVolume(float capacity) {
		if(capacityUnit() == CAPACITY_CUFT && volumeUnit() == VOLUME_CUIN) {
			return capacity * 1728;
		} else {
			return capacity;
		}
	}

	public static float volumeToCapacity(float capacity) {
		if(capacityUnit() == CAPACITY_CUFT && volumeUnit() == VOLUME_CUIN) {
			return capacity / 1728;
		} else {
			return capacity;
		}
	}

	// The different units of pressure available
	public static final int PRESSURE_PSI = 0;
	public static final int PRESSURE_BAR = 1;
	
	// An array that holds the standard increment for pressure in each
	// pressure unit, indexed by the pressure unit constants above.
	// In plain language, this says that in imperial units we
	// tend to increment pressures in 100 imperial units (psi), and
	// in metric it's in 10 metric units (bar)
	private static final int pressure_increment[] = { 100, 10 };
		
	// This may be a little pedantic, but store some standard values
	// in each unit system for different customary qualitative 
	// measurements. These are used as default values throughout
	// the application.
	private static final int pressure_tank_low[] = { 700, 50 };
	private static final int pressure_tank_full[] = { 3000, 200 };
	private static final int pressure_tank_max[] = { 4500, 300 };
	
	// This method defines which unit of pressure is defined for each
	// unit system. Use this method when determining what the unit is
	// you're working with.
	public static int pressureUnit() {
		return unitSystem == IMPERIAL? PRESSURE_PSI: PRESSURE_BAR;
	}
	
	private static final float pressure_atm[] = { 14.7f, 1 };
	
	public static float pressureAtm() { return pressure_atm[pressureUnit()]; }

	// Get the amount to increment pressure values for the current units
	public static float pressureIncrement() { return pressure_increment[pressureUnit()]; }
	// Get a low tank pressure 
	public static float pressureTankLow() { return pressure_tank_low[pressureUnit()]; }
	// Get the pressure of a typical full tank
	public static float pressureTankFull() { return pressure_tank_full[pressureUnit()]; }
	public static float pressureTankMax() { return pressure_tank_max[pressureUnit()]; }
	
	public static final int DEPTH_FOOT = 0;
	public static final int DEPTH_METER = 1;
	
	// A similar array to pressure_increment, except for depth
	private static final int depth_increment[] = { 10, 3 };
	
	// An array to store the depth of seawater per atmosphere of
	// pressure
	private static final int atm_depth[] = { 33, 10 };
	
	private static final int depth_narcotic[] = { 100, 30 };
	private static final int depth_max_narcotic[] = { 200, 60 };
	private static final int depth_toxic[] = { 180, 60 };
	private static final int depth_max[] = { 1000, 330 };

	// Get the unit of depth for this unit system
	public static int depthUnit() {
		return unitSystem == IMPERIAL? DEPTH_FOOT: DEPTH_METER;
	}

	// Get the amount to increment depth values for the current units
	public static float depthIncrement() { return depth_increment[depthUnit()]; }
	
	// Get the depth of one atmosphere of seawater in the current unit
	// system
	public static float depthPerAtm() { return atm_depth[depthUnit()]; }
	
	public static int depthNarcotic() { return depth_narcotic[depthUnit()]; }
	public static int depthMaxNarcotic() { return depth_max_narcotic[depthUnit()]; }
	public static int depthToxic() { return depth_toxic[depthUnit()]; }
	public static int depthMax() { return depth_max[depthUnit()]; }
	
	// Temperatures
	public static final int ABSTEMP_RANKINE = 0;
	public static final int ABSTEMP_KELVIN = 1;
	
	public static int absTempUnit() {
		return unitSystem == IMPERIAL? ABSTEMP_RANKINE: ABSTEMP_KELVIN;
	}
	
	private static final int abs_temp_ambient[] = { 535, 297 };
	
	public static int absTempAmbient() { return abs_temp_ambient[absTempUnit()]; }
	
	// Gas constants in imperial (ft^3 psi R^-1 lb-mol^-1), metric (L bar K^-1 mol^-1)
	// Source: http://en.wikipedia.org/wiki/Ideal_gas_constant
	private static final float gas_constant[] = { 10.731f, 8.3145E-2f };
	
	public static float gasConstant() { return gas_constant[unitSystem]; }
	
	// Unit conversion functions. These aren't influenced by the current
	// value of unit. They are only used when the user switches unit
	// systems and existing input values need to be converted.
	
	// Generic method to convert a value from one unit system to
	// another. This is used by the other functions
	// As written, it can only handle two known unit systems. If I
	// ever added more, this would quickly get more complex.
	// multiplier_imperial_to_metric is the number of imperial
	// units that equal one metric unit.
	// (and yes, there are more than two "unit systems" :) )
	// This needs to be redone to interface with the Unit definitions,
	// instead of relying on Unit Systems
	private static float convert(float value, float multiplier_imperial_to_metric, int from_unit, int to_unit) {
		// Convert to metric as a standard
		if(from_unit == IMPERIAL) {
			value /= multiplier_imperial_to_metric;
		}
		
		// Now that we know value is in metric units, convert to
		// imperial if requested
		if(to_unit == IMPERIAL) {
			value *= multiplier_imperial_to_metric;
		}

		// Whatever we have is now in the correct units. Return it.
		return value;
	}
	
	public static float convertPressure(float pressure, int from_unit) {
		return convertPressure(pressure, from_unit, unitSystem);
	}
	
	public static float convertPressure(float pressure, int from_unit, int to_unit) {
		return convert(pressure, 14.5f, from_unit, to_unit);
	}
	
	// Convert depth from one unit system to another
	public static float convertDepth(float depth, int from_unit) {
		return convert(depth, 3.28f, from_unit, unitSystem);
	}
	
	public static float convertVolume(float volume, int from_unit) {
		return convertVolume(volume, from_unit, unitSystem);
	}
	
	public static float convertVolume(float volume, int from_unit, int to_unit) {
		return convert(volume, 61.02f, from_unit, to_unit);
	}
	
	public static float convertCapacity(float capacity, int from_unit) {
		return convertCapacity(capacity, from_unit, unitSystem);
	}
	
	public static float convertCapacity(float capacity, int from_unit, int to_unit) {
		return convert(capacity, 3.531E-2f, from_unit, to_unit);
	}
}
