package divestoclimb.lib.scuba;

import java.text.NumberFormat;

// This is a class for a given gas mix of oxygen, nitrogen, and helium
public class Mix implements GasSource {
	// Ten times the percentage of oxygen
	private int o2Times10;
	
	// Ten times the percentage of helium
	private int heTimes10;
	
	// These static fields are returned by getFriendlyName. They can be used
	// to map gas names to names in other languages.
	public static final String OXYGEN="OXYGEN";
	public static final String HELIUM="HELIUM";
	public static final String NITROGEN="NITROGEN";
	public static final String AIR="AIR";

	// Constructor. Takes fractions of oxygen and helium (between 0 and 1)
	public Mix(float o2, float he) {
		o2Times10=(int)Math.round(o2*1000);
		heTimes10=(int)Math.round(he*1000);
	}
	
	public float getO2() {
		return o2Times10 / 10;
	}
	
	public float getfO2() {
		return o2Times10 / 1000;
	}
	
	public float getHe() {
		return heTimes10 / 10;
	}
	
	public float getfHe() {
		return heTimes10 / 1000;
	}
	
	public String friendlyName() {
		NumberFormat nf = NumberFormat.getIntegerInstance();
		if(this.getO2()==100) {
			return OXYGEN;
		}
		if(this.getHe()==100) {
			return HELIUM;
		}
		if(this.getO2() + this.getHe() == 0) {
			return NITROGEN;
		}
		if((this.getO2()==21) && (this.getHe()==0)) {
			return AIR;
		}
		if(this.getHe()==0) {
			// A Nitrox mix
			return nf.format(this.getO2())+"%";
		}
		// After all of the above, we have a trimix
		return nf.format(this.getO2())+"/"+nf.format(this.getHe());
	}

	public float pHeAtDepth(int depth) {
		return (depth / Units.depthPerAtm() + 1) * this.getfHe();
	}

	public float pN2AtDepth(int depth) {
		return (depth / Units.depthPerAtm() + 1) * (1 - this.getfHe() - this.getfO2());
	}

	public float pO2AtDepth(int depth) {
		return (depth / Units.depthPerAtm() + 1) * this.getfO2();
	}
	
	/**
	 *  Return the Maximum Operating Depth of this mix.
	 * @param maxpO2 The maximum desired partial pressure of oxygen (usually 1.4 or 1.6)
	 * @return The maximum operating depth in the current system of units, rounded down to the nearest standard depth increment
	 */
	public float MOD(float maxpO2) {
		return ((int) Math.ceil((maxpO2 / this.getfO2() - 1) * Units.depthPerAtm()) / Units.depthIncrement()) * Units.depthIncrement();
	}

	/**
	 * Return the Equivalent Narcotic Depth of this mix at a given depth.
	 * @param depth The depth to determine the END for.
	 * @param oxygenIsNarcotic Whether or not to consider the effects of oxygen in the calculation
	 * @return The equivalent narcotic depth in the current system of units, rounded up to the nearest standard depth increment
	 */
	public float END(int depth, boolean oxygenIsNarcotic) {
		float pNarc = oxygenIsNarcotic? pO2AtDepth(depth) + pN2AtDepth(depth): pN2AtDepth(depth);
		float pNarc0 = oxygenIsNarcotic? 1: (float) 0.79;
		return ((int) Math.floor((pNarc / pNarc0 - 1) * Units.depthPerAtm()) / Units.depthIncrement()) * Units.depthIncrement();
	}
	
	/**
	 * Return the minimum depth at which this mix may be breathed.
	 * @param minpO2 The minimum desired pO2 to have when breathing this mix (usually .16 or .17)
	 * @return The minimum depth in the current system of units, rounded up to the nearest standard increment
	 */
	public float ceiling(float minpO2) {
		// This function is nearly identical to MOD except we round up instead of
		// down
		return ((int) Math.floor((minpO2 / this.getfO2() - 1) * Units.depthPerAtm()) / Units.depthIncrement()) * Units.depthIncrement();
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
	public static Mix best(int depth, int maxEND, float maxpO2, boolean oxygenIsNarcotic) {
		float fO2Best = maxpO2 / (depth / Units.depthPerAtm() + 1);
		if(fO2Best > 1) {
			fO2Best = 1;
		} else {
			fO2Best = (float) (Math.ceil(fO2Best * 100) / 100);
		}
		float pNarc0 = oxygenIsNarcotic? 1: (float) 0.79;
		float pAbs = depth / Units.depthPerAtm() + 1;
		float fNarcBest = (float) (Math.ceil((maxEND / Units.depthPerAtm() + 1) * pNarc0 / pAbs * 100) / 100);
		float fHeBest = 1 - (oxygenIsNarcotic? fNarcBest: fNarcBest - fO2Best);
		return new Mix(fO2Best, fHeBest);
	}
}
