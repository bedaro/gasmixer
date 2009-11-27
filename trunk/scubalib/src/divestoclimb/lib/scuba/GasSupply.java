package divestoclimb.lib.scuba;

/**
 * A class that represents a complete gas system: a tank containing a mix filled
 * to a given pressure. Supports operations to add and remove gas from the tank.
 * @author Ben Roberts (divestoclimb@gmail.com)
 */
public class GasSupply {
	private Mix mMix;
	private Cylinder mCylinder;
	private double mPressure;
	private float mTemperature;
	private boolean mUseIdealGasLaws;
	
	public GasSupply(Cylinder c) {
		this(c, new Mix(0.21f, 0), 0);
	}
	
	/**
	 * Create a new gas source from a cylinder size, an initial mix, and a starting
	 * pressure.
	 * @param c The cylinder object to use for this supply
	 * @param m The initial mix in the cylinder
	 * @param pressure The initial pressure of the cylinder's content, in the same
	 * units that were used for the cylinder object.
	 */
	public GasSupply(Cylinder c, Mix m, int pressure) {
		this(c, m, pressure, false);
	}
	
	public GasSupply(Cylinder c, Mix m, int pressure, boolean ideal_gas_laws) {
		this(c, m, pressure, ideal_gas_laws, c.getUnits().absTempAmbient());
	}
	
	public GasSupply(Cylinder c, Mix m, int pressure, boolean ideal_gas_laws, float temperature) {
		mMix = m;
		mCylinder = c;
		mPressure = pressure;
		mUseIdealGasLaws = ideal_gas_laws;
		mTemperature = temperature;
	}
	
	public void useIdealGasLaws() {
		useIdealGasLaws(true);
	}
	
	public void useIdealGasLaws(boolean set) {
		mUseIdealGasLaws = set;
	}
	
	public Mix getMix() {
		return mMix;
	}
	
	public void setMix(Mix m) {
		mMix = m;
	}
	
	public Cylinder getCylinder() {
		return mCylinder;
	}
	
	public void setCylinder(Cylinder c) {
		mCylinder = c;
	}
	
	public double getPressure() {
		return mPressure;
	}
	
	public void setPressure(int p) {
		mPressure = p;
	}
	
	public double getTemperature() {
		return mTemperature;
	}
	
	public void setTemperature(int t) {
		mTemperature = t;
	}
	
	/**
	 * Get the total amount of gas in capacity units at sea level pressure
	 * @return The amount of gas in the supply
	 */
	public double getGasAmount() {
		if(mUseIdealGasLaws) {
			return mCylinder.getIdealCapacityAtPressure(mPressure);
		} else {
			return mCylinder.getVdwCapacityAtPressure(mPressure, mMix, mTemperature);
		}
	}
	
	public double getO2Amount() {
		return getGasAmount() * mMix.getfO2();
	}
	
	public double getN2Amount() {
		return getGasAmount() * mMix.getfN2();
	}
	
	public double getHeAmount() {
		return getGasAmount() * mMix.getfHe();
	}
	
	/**
	 * Adjust the pressure in the supply so there's the given amount of gas. 
	 * @param amt The amount to leave in the cylinder in capacity units at
	 * sea level pressure
	 * @return The GasSupply object
	 */
	public GasSupply drainToGasAmount(double amt) {
		if(mUseIdealGasLaws) {
			mPressure = mCylinder.getIdealPressureAtCapacity(amt);
		} else {
			mPressure = mCylinder.getVdwPressureAtCapacity(amt, mMix, mTemperature);
		}
		return this;
	}
	
	/**
	 * Adjust the pressure in the supply so there's the given amount of oxygen.
	 * @param amt The amount of oxygen to leave in the cylinder in capacity
	 * units at sea level pressure
	 * @return The GasSupply object
	 */
	public GasSupply drainToO2Amount(double amt) {
		return drainToGasAmount(amt / mMix.getfO2());
	}
	
	public GasSupply drainToN2Amount(double amt) {
		return drainToGasAmount(amt / mMix.getfHe());
	}
	
	public GasSupply drainToHeAmount(double amt) {
		return drainToGasAmount(amt / mMix.getfN2());
	}
	
	/**
	 * Add a given amount of oxygen to the cylinder, updating the mix and pressure
	 * accordingly.
	 * @param amt The amount of oxygen to add in 1-atm volumes
	 * @return The modified GasSupply object
	 */
	public GasSupply addO2(double amt) {
		return addGas(new Mix(1, 0), amt);
	}
	
	/**
	 * Add a given amount of helium to the cylinder, updating the mix and pressure
	 * accordingly.
	 * @param amt The amount of helium to add in 1-atm volumes
	 * @return The modified GasSupply object
	 */
	public GasSupply addHe(double amt) {
		return addGas(new Mix(0, 1), amt);
	}
	
	/**
	 * Add a given amount of arbitrary gas to the cylinder, updating the mix and
	 * pressure accordingly.
	 * @param mix The gas mix being added
	 * @param amt The amount of gas to add in 1-atm volumes
	 * @return The modified GasSupply object
	 */
	public GasSupply addGas(Mix mix, double amt) {
		double current_amt = getGasAmount(),
				o2 = mMix.getfO2() * current_amt + mix.getfO2() * amt,
				he = mMix.getfHe() * current_amt + mix.getfHe() * amt,
				new_total_amt = current_amt + amt;
		mMix = new Mix(o2 / new_total_amt, he / new_total_amt);
		if(mUseIdealGasLaws) {
			mPressure = mCylinder.getIdealPressureAtCapacity(new_total_amt);
		} else {
			mPressure = mCylinder.getVdwPressureAtCapacity(new_total_amt, mMix, mTemperature);
		}
		return this;
	}
	
	/**
	 * Add a mix to the current contents of the supply.
	 * @param m The mix to add
	 * @param final_pressure The final pressure for the supply
	 * @return The modified GasSupply object.
	 */
	public GasSupply topup(Mix m, int final_pressure) {
		// Trivial solution: we're adding the same mix that's already in the cylinder
		if(mMix.isEqualTo(m)) {
			mPressure = final_pressure;
			return this;
		}
		// This method uses the Secant Method to numerically determine
		// the result to within 1/2% of each final mix. We do this because
		// writing out the single equation for the system would be terrible,
		// not to mention calculating its derivative.

		// Compute uncertainty
		// Max uncertainty in fo2 and fhe is 0.5% = 0.005.
		// fo2 == fo2i + fo2t == fo2i + vt * fo2t
		// e_fo2 == fo2t * e_vt <= 0.005
		// e_fhe == fhet * e_vt <= 0.005
		double error = 0.005 / Math.max(m.getfO2(), m.getfHe());
		
		// cache member variables as local
		Cylinder c = mCylinder;
		Mix mix = mMix;
		int pressure = (int)mPressure;

		// Start with two guesses for Secant Method
		// The first guess assumes ideal behavior as the gas is added, and assumes
		// the topup mix is close enough to determine capacity.
		double vt_n = (1 - pressure / (float)final_pressure) * c.getVdwCapacityAtPressure(final_pressure, m, mTemperature);
		// The second guess assumes ideal behavior as the gas is added, and assumes
		// the starting mix is close enough to determine capacity.
		double vt_n_1 = (1 - pressure / (float)final_pressure) * c.getVdwCapacityAtPressure(final_pressure, mix, mTemperature);

		double d;
		do {
			// Initialize a temporary GasSupply. Because addGas acts on the object,
			// we have to re-instantiate it each time.
			GasSupply test = new GasSupply(c, mix, pressure);
			// Each computation evaluates the difference between the actual pressure
			// after adding a certain amount of gas, and the desired pressure.
			double f_n = test.addGas(m, vt_n).getPressure() - final_pressure;
			test = new GasSupply(c, mix, pressure);
			double f_n_1 = test.addGas(m, vt_n_1).getPressure() - final_pressure;
			d = (vt_n - vt_n_1) / (f_n - f_n_1) * f_n;
			vt_n_1 = vt_n;
			vt_n -= d;
		} while(Math.abs(d) < error);
		
		// Now that we have our solution, run addGas on self.
		addGas(m, vt_n);
		// Cheat! Set mPressure to what would be expected since addGas may not have
		// gotten it exactly.
		mPressure = final_pressure;
		return this;
	}
}