package divestoclimb.lib.scuba;

import java.text.NumberFormat;

/**
 * This is a class for a given gas mix of oxygen, nitrogen, and helium
 * @author Ben Roberts (divestoclimb@gmail.com)
 */
public class Mix implements GasSource {
	// The fraction of oxygen
	private double mO2;
	
	// The fraction of helium
	private double mHe;
	
	// The values of Van der Waals' a and b constants for each constituent gas
	public static final float A_OXYGEN = 1.382f;
	public static final float A_HELIUM = 0.0346f;
	public static final float A_NITROGEN = 1.370f;
	public static final float B_OXYGEN = 0.03186f;
	public static final float B_HELIUM = 0.02380f;
	public static final float B_NITROGEN = 0.03870f;
	
	// These static fields are returned by getFriendlyName. They can be used
	// to map gas names to names in other languages.
	public static final String OXYGEN="OXYGEN";
	public static final String HELIUM="HELIUM";
	public static final String NITROGEN="NITROGEN";
	public static final String AIR="AIR";

	// Constructor. Takes fractions of oxygen and helium (between 0 and 1)
	public Mix(double o2, double he) {
		mO2=o2;
		mHe=he;
	}
	
	/**
	 * Get the percentage of O2 in the mix, from 0 to 100.
	 * @return The O2 %
	 */
	public float getO2() {
		return (float)(mO2 * 100);
	}
	
	/**
	 * Get the fraction of O2 in the mix, from 0 to 1.
	 * @return The O2 fraction
	 */
	public double getfO2() {
		return mO2;
	}
	
	/**
	 * Get the percentage of helium in the mix, from 0 to 100.
	 * @return The helium %
	 */
	public float getHe() {
		return (float)(mHe * 100);
	}
	
	/**
	 * Get the fraction of helium in the mix, from 0 to 1.
	 * @return The helium fraction
	 */
	public double getfHe() {
		return mHe;
	}
	
	/**
	 * Get the fraction of nitrogen in the mix, from 0 to 1.
	 * @return The nitrogen fraction
	 */
	public double getfN2() {
		return 1 - mHe - mO2;
	}
	
	public String friendlyName() {
		NumberFormat nf = NumberFormat.getIntegerInstance();
		if(getO2()==100) {
			return OXYGEN;
		}
		if(getHe()==100) {
			return HELIUM;
		}
		if(getO2() + this.getHe() == 0) {
			return NITROGEN;
		}
		if((getO2()==21) && (getHe()==0)) {
			return AIR;
		}
		if(this.getHe()==0) {
			// A Nitrox mix
			return nf.format(getO2())+"%";
		}
		// After all of the above, we have a trimix
		return nf.format(getO2())+"/"+nf.format(getHe());
	}
	
	public boolean isEqualTo(Mix m2) {
		return getfO2() == m2.getfO2() && getfHe() == m2.getfHe();
	}

	public double pHeAtDepth(int depth, Units units) {
		return (depth / units.depthPerAtm() + 1) * getfHe();
	}

	public double pN2AtDepth(int depth, Units units) {
		return (depth / units.depthPerAtm() + 1) * getfN2();
	}

	public double pO2AtDepth(int depth, Units units) {
		return (depth / units.depthPerAtm() + 1) * getfO2();
	}
	
	/**
	 *  Return the Maximum Operating Depth of this mix.
	 * @param maxpO2 The maximum desired partial pressure of oxygen (usually 1.4 or 1.6)
	 * @return The maximum operating depth in the current system of units, rounded down to the nearest standard depth increment
	 */
	public float MOD(Units units, float maxpO2) {
		return ((int) Math.floor((maxpO2 / mO2 - 1) * units.depthPerAtm()) / units.depthIncrement()) * units.depthIncrement();
	}
	
	/**
	 * Return the Equivalent Air Depth of this mix at a given depth.
	 * @param depth The depth to determine the EAD for.
	 * @return The equivalent air depth in the current system of units, rounded up to the nearest standard depth increment
	 */
	public float EAD(int depth, Units units) {
		// This is the same computation as END without considering the narcotic effect of
		// oxygen
		return END(depth, units, false);
	}

	/**
	 * Return the Equivalent Narcotic Depth of this mix at a given depth.
	 * @param depth The depth to determine the END for.
	 * @param oxygenIsNarcotic Whether or not to consider the effects of oxygen in the calculation
	 * @return The equivalent narcotic depth in the current system of units, rounded up to the nearest standard depth increment
	 */
	public float END(int depth, Units units, boolean oxygenIsNarcotic) {
		double pNarc = oxygenIsNarcotic? pO2AtDepth(depth, units) + pN2AtDepth(depth, units): pN2AtDepth(depth, units);
		float pNarc0 = oxygenIsNarcotic? 1: 0.79f;
		return Math.max(((int) Math.ceil((pNarc / pNarc0 - 1) * units.depthPerAtm()) / units.depthIncrement()) * units.depthIncrement(), 0);
	}
	
	/**
	 * Return the minimum depth at which this mix may be breathed.
	 * @param minpO2 The minimum desired pO2 to have when breathing this mix (usually .16 or .17)
	 * @return The minimum depth in the current system of units, rounded up to the nearest standard increment
	 */
	public float ceiling(float minpO2, Units units) {
		// This function is nearly identical to MOD except we round up instead of
		// down
		return ((int) Math.ceil((minpO2 / mO2 - 1) * units.depthPerAtm()) / units.depthIncrement()) * units.depthIncrement();
	}
	
	/**
	 * Determine the best mix to use for a given depth based on the desired MOD and
	 * END at that MOD.
	 * @param depth The maximum desired depth for this mix in the current system of units
	 * @param maxEND The desired END. If you don't want a helium mix, pass the same value for mod and end.
	 * @param maxpO2 The maximum desired partial pressure of oxygen (usually 1.4 or 1.6)
	 * @param oxygenIsNarcotic Whether or not to consider the effects of oxygen in the calculation
	 * @return The Mix containing the highest possible percentage of oxygen and the lowest possible percentage of helium for the given parameters, rounded down/up to whole percentages.
	 */
	public static Mix best(int depth, int maxEND, Units units, float maxpO2, boolean oxygenIsNarcotic) {
		float dpa = units.depthPerAtm();
		float pAbs = depth / dpa + 1;
		float fO2Best = maxpO2 / pAbs;
		if(fO2Best > 1) {
			fO2Best = 1;
		} else {
			fO2Best = (float) (Math.floor(fO2Best * 100) / 100);
		}
		maxEND = Math.min(depth, maxEND);
		float fNarcBest = (float) (Math.floor((maxEND / dpa + 1) / pAbs * 100) / 100);
		float fHeBest = 1 - (oxygenIsNarcotic? fNarcBest: fNarcBest + fO2Best);
		if(fO2Best + fHeBest > 1) {
			return null;
		} else { 
			return new Mix(fO2Best, fHeBest);
		}
	}
	
	// Internal variables used for caching the computed a and b values for this mix
	private double mCacheA = 0, mCacheB = 0, mCacheHe = -1, mCacheO2 = -1;
	
	/**
	 * This private method does our a and b calculations and caches the results in
	 * private variables. This way the computation only has to be done once on a given
	 * mix.
	 */
	private void computeAB() {
		mCacheO2 = mO2;
		mCacheHe = mHe;
		double x[] = { mO2, getfN2(), mHe };
		float a[] = { A_OXYGEN, A_NITROGEN, A_HELIUM };
		float b[] = { B_OXYGEN, B_NITROGEN, B_HELIUM };
		mCacheA = 0;
		mCacheB = 0;
		for(int i = 0; i < 3; i++) {
			for(int j = 0; j < 3; j++) {
				mCacheA += (float)(Math.sqrt(a[i]*a[j])*x[i]*x[j]);
				mCacheB += (float)(Math.sqrt(b[i]*b[j])*x[i]*x[j]);
			}
		}
	}
	
	/**
	 * Returns the particle attraction factor a for a theoretical homogeneous
	 * gas equivalent in behavior to the given gas mixture.
	 * @param m The gas mix to generate a for.
	 * @return The value of a.
	 */
	public double getA() {
		if(mO2 != mCacheO2 || mHe != mCacheHe) {
			computeAB();
		}
		return mCacheA;
	}
	
	/**
	 * Returns the particle volume factor b for a theoretical homogeneous
	 * gas equivalent in behavior to the given gas mixture.
	 * @param m The gas mix to generate b for.
	 * @return The value of b.
	 */
	public double getB() {
		if(mO2 != mCacheO2 || mHe != mCacheHe) {
			computeAB();
		}
		return mCacheB;
	}
}