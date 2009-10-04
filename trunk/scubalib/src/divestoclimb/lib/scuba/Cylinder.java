package divestoclimb.lib.scuba;

// A class that represents a gas cylinder (or a manifolded set of cylinders)
public class Cylinder {
	// The total physical volume of the cylinder(s)
	// Internal volume is stored in standard "capacity units" for the defined
	// unit system. Some unit systems (i.e. Imperial) have special units that
	// are typically used for measuring internal volumes because capacity units
	// are too large to be convenient.
	private float mInternalVolume;
	// The service pressure
	private int mServicePressure;

	/**
	 * Constructor is meant to take values as returned from a tank data model
	 * which stores internal volumes and service pressures (the metric way).
	 *
	 * @param internal_volume Internal volume of the cylinder in volume units
	 * @param service_pressure Service pressure of the cylinder
	 */
	public Cylinder(float internal_volume, int service_pressure) {
		mInternalVolume = Units.volumeToCapacity(internal_volume);
		mServicePressure = service_pressure;
	}

	/**
	 * Build a Cylinder object with a capacity instead of an internal volume
	 * @param capacity The volume of gas the cylinder's contents would occupy at
	 * sea level pressure when the cylinder is filled to the service pressure
	 * @param service_pressure Service pressure of the cylinder
	 * @return A Cylinder object initialized with the given parameters
	 */
	public static Cylinder fromCapacity(float capacity, int service_pressure) {
		float internal_volume = capacity * Units.pressureAtm() / service_pressure;
		return new Cylinder(internal_volume, service_pressure);
	}

	/** Returns the gas capacity of the cylinder(s)
	 * @return The volume of gas the cylinder's contents would occupy at sea level
	 * pressure when the cylinder is filled to the service pressure, in capacity
	 * units
	 */
	public float getCapacity() {
		return mInternalVolume * mServicePressure / Units.pressureAtm();
	}

	public void setCapacity(float capacity) {
		mInternalVolume = capacity * Units.pressureAtm() / mServicePressure;
	}

	/**
	 * Get the internal volume of this cylinder
	 * @return The internal volume in volume units
	 */
	public float getInternalVolume() {
		return Units.capacityToVolume(mInternalVolume);
	}

	public void setInternalVolume(float internal_volume) {
		mInternalVolume = Units.volumeToCapacity(internal_volume);
	}

	public int getServicePressure() {
		return mServicePressure;
	}

	public void setServicePressure(int service_pressure) {
		mServicePressure = service_pressure;
	}

	public int getIdealGasVolumeAtPressure(int pressure) {
		return mInternalVolume * pressure / Units.pressureAtm();
	}

	public int getIdealGasPressureAtVolume(int volume) {
		return volume * Units.pressureAtm() / mInternalVolume;
	}

	/**
	 * Solves Van der Waals gas equation to get equivalent atmospheric volume at
	 * a given pressure
	 * @param P The pressure of the gas in the cylinder
	 * @param mix The mix in the cylinder, needed to determine a and b constants.
	 * @return
	 */
	public int getVdwGasVolumeAtPressure(int P, Mix mix) {
		// This is solved by finding the root of a cubic polynomial
		// for the molar volume v = V/n:
		// choose a reasonable value for T
		//   P * v^3 - (P*b + R*T) * v^2 + a * v - a * b = 0
		//   n = V/v
		// Then we can use ideal gas laws to convert n to V @ 1 ata
		double a = VanDerWaals.computeA(m), b = VanDerWaals.computeB(m);
		double RT = Units.ambientAbsoluteTemperature() * Units.idealGasConstant();
		// A bit of optimization to reduce number of calculations per iteration
		double PbRT = P*b + RT, PbRT2 = 2 * PbRT, ab = a * b, P3 = 3 * P;
		// Approximate the solution with ideal gas laws to seed Newton-Raphson
		double v0, v1 = RT / P;

		// First-order uncertainty propagation. This lets us know within what
		// tolerance we need to compute v to get the right volume.
		// The variable we are solving for is v.
		// The result we care about the uncertainty for is V0, the volume at 1 ata.
		//   V0 = n * R * T / P0 [ideal gas law] = V * R * T / (P0 * v)
		// To compute the uncertainty in V0, we use the Taylor series method for
		// v alone.
		//   deltaV0 = dV0/dv*deltav
		// ...where dV0/dv = - V*R*T / (P0 * v^2)
		// We want to make sure deltaV0 is less than 0.5, so...
		//   deltav < P0 * v^2 / (2 * V * R * T)
		double uncertainty_multiplier = Units.pressureAtm() / (2 * mInternalVolume * RT);

		do {
			v0 = v1;
			double f = P * Math.pow(v0, 3) - PbRT * Math.pow(v0, 2) + a * v0 - ab;
			double fprime = P3 * Math.pow(v0, 2) - PbRT2 * v0 + a;
			v1 = v0 - f / fprime;
		} while(Math.abs(v0 - v1) / uncertainty_multiplier < v1 * v1);

		return (int)Math.floor(mInternalVolume * RT / (Units.pressureAtm() * v1));
	}
	
	public int getVdwGasPressureAtVolume(int volume, Mix mix) {
		// This is given by the following:
		// choose a reasonable value for T
		// n = Patm*V/(R*T) (since volume is at atmospheric pressure, it's close enough to ideal)
		// v = V/n
		// P = R * T / (v - b) - a / v^2
		float RT = Units.ambientAbsoluteTemperature() * Units.idealGasConstant();
		double v = mInternalVolume * RT / (Units.pressureAtm() * volume),
				a = VanDerWaals.computeA(m), b = VanDerWaals.computeB(m);
		return (int)Math.ceil(RT / (v - b) - a / Math.pow(v, 2));
	}
}
