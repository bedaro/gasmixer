package divestoclimb.lib.scuba;

// A class that represents a gas cylinder (or a manifolded set of cylinders)
public class Cylinder {
	// The total physical volume of the cylinder(s)
	// Internal volume is stored in standard "capacity units" for the defined
	// unit system. Some unit systems (i.e. Imperial) have special units that
	// are typically used for measuring internal volumes because capacity units
	// are too large to be convenient. It is up to the frontend to convert
	// capacity units returned by this class if it is desired to do so.
	private float mInternalVolume;
	// The service pressure
	private int mServicePressure;

	/**
	 * Constructor is meant to take values as returned from a tank data model
	 * which stores internal volumes and service pressures (the metric way).
	 *
	 * @param internal_volume Internal volume of the cylinder in capacity units
	 * @param service_pressure Service pressure of the cylinder
	 */
	public Cylinder(float internal_volume, int service_pressure) {
		mInternalVolume = internal_volume;
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
		Cylinder c = new Cylinder(0, service_pressure);
		c.setCapacity(capacity);
		return c;
	}

	/** Returns the air capacity of the cylinder(s)
	 * @return The volume of gas the cylinder's contents would occupy at sea level
	 * pressure when the cylinder is filled with air to the service pressure, in
	 * capacity units
	 */
	public float getCapacity() {
		return getVdwCapacityAtPressure(mServicePressure, new Mix(0.21f, 0));
	}

	public void setCapacity(float capacity) {
		//mInternalVolume = capacity * Units.pressureAtm() / mServicePressure;
		// This is quite similar to getVdwCapacityAtPressure, except
		// we are solving for V instead of n. The cubic
		// polynomial is the same, it's just that the
		// uncertainty is calculated differently.
		Mix m = new Mix(0.21f, 0);
		double a = VanDerWaals.computeA(m), b = VanDerWaals.computeB(m);
		double RT = Units.absTempAmbient() * Units.gasConstant();
		// A bit of optimization to reduce number of calculations per iteration
		double PbRT = mServicePressure*b + RT, PbRT2 = 2 * PbRT, ab = a * b, P3 = 3 * mServicePressure;
		// Come up with a guess to seed Newton-Raphson. The equation is easily
		// solved if a and b were 0
		double v0, v1 = RT / mServicePressure;
		
		// We know what n is because we were given capacity:
		double n = Units.pressureAtm() * capacity / RT;
		
		// Uncertainty math (see below)
		// V = nv
		// dV/dv = n
		float uncertainty = (float)(n / Math.pow(10, Units.volumePrecision()) / 2f);
		
		do {
			v0 = v1;
			double f = mServicePressure * Math.pow(v0, 3) - PbRT * Math.pow(v0, 2) + a * v0 - ab;
			double fprime = P3 * Math.pow(v0, 2) - PbRT2 * v0 + a;
			v1 = v0 - f / fprime;
		} while(Math.abs(v0 - v1) >= uncertainty);
		
		mInternalVolume = (float)(v1 * n);
	}

	/**
	 * Get the internal volume of this cylinder
	 * @return The internal volume in capacity units
	 */
	public float getInternalVolume() {
		return mInternalVolume;
	}

	public void setInternalVolume(float internal_volume) {
		mInternalVolume = internal_volume;
	}

	public int getServicePressure() {
		return mServicePressure;
	}

	public void setServicePressure(int service_pressure) {
		mServicePressure = service_pressure;
	}

	public float getIdealCapacityAtPressure(int pressure) {
		return (float)(Math.floor(mInternalVolume * pressure / Units.pressureAtm() * 10) / 10);
	}

	public int getIdealPressureAtCapacity(float capacity) {
		return (int)Math.ceil(capacity * Units.pressureAtm() / mInternalVolume);
	}

	/**
	 * Solves Van der Waals gas equation to get equivalent atmospheric volume at
	 * a given pressure
	 * @param P The pressure of the gas in the cylinder
	 * @param mix The mix in the cylinder, needed to determine a and b constants.
	 * @return The amount of gas in the cylinder to one decimal place
	 */
	public float getVdwCapacityAtPressure(int P, Mix m) {
		// This is solved by finding the root of a cubic polynomial
		// for the molar volume v = V/n:
		// choose a reasonable value for T
		//   P * v^3 - (P*b + R*T) * v^2 + a * v - a * b = 0
		//   n = V/v
		// Then we can use ideal gas laws to convert n to V @ 1 ata
		double a = VanDerWaals.computeA(m), b = VanDerWaals.computeB(m);
		double RT = Units.absTempAmbient() * Units.gasConstant();
		// A bit of optimization to reduce number of calculations per iteration
		double PbRT = P*b + RT, PbRT2 = 2 * PbRT, ab = a * b, P3 = 3 * P;
		// Come up with a guess to seed Newton-Raphson. The equation is easily
		// solved if a and b were 0
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
		// We want to make sure deltaV0 is less than 0.05, so...
		//   deltav < P0 * v^2 / (2 * V * R * T)
		double uncertainty_multiplier = Units.pressureAtm() / (20 * mInternalVolume * RT);

		do {
			v0 = v1;
			double f = P * Math.pow(v0, 3) - PbRT * Math.pow(v0, 2) + a * v0 - ab;
			double fprime = P3 * Math.pow(v0, 2) - PbRT2 * v0 + a;
			v1 = v0 - f / fprime;
		} while(Math.abs(v0 - v1) / uncertainty_multiplier >= v1 * v1);

		return (float)(Math.floor(mInternalVolume * RT / (Units.pressureAtm() * v1) * 10) / 10);
	}
	
	public int getVdwPressureAtCapacity(int capacity, Mix m) {
		// This is given by the following:
		// choose a reasonable value for T
		// n = Patm*V/(R*T) (since volume is at atmospheric pressure, it's close enough to ideal)
		// v = V/n
		// P = R * T / (v - b) - a / v^2
		float RT = Units.absTempAmbient() * Units.gasConstant();
		double v = mInternalVolume * RT / (Units.pressureAtm() * capacity),
				a = VanDerWaals.computeA(m), b = VanDerWaals.computeB(m);
		return (int)Math.ceil(RT / (v - b) - a / Math.pow(v, 2));
	}
}
