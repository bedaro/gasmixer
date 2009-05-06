package divestoclimb.gasmixer;

import java.text.NumberFormat;

// This is a class for a given gas mix
public class Mix {
	// Ten times the percentage of oxygen
	private int o2Times10;
	
	// Ten times the percentage of helium
	private int heTimes10;
	
	// Pressure of this mix
	//private Pressure pressure;

	// Constructor. Takes fractions of oxygen and helium (between 0 and 1)
	public Mix(double o2, double he) {
		o2Times10=(int)Math.round(o2*1000);
		heTimes10=(int)Math.round(he*1000);
	}
	
	//public setVolume(int v) {
		
	//}
	
	public double getO2() {
		return o2Times10 / 10;
	}
	
	public double getHe() {
		return heTimes10 / 10;
	}
	
	public String friendlyName(NumberFormat nf) {
		if(this.getO2()==100) {
			return "Oxygen";
		}
		if(this.getHe()==100) {
			return "Helium";
		}
		if(this.getO2() + this.getHe() == 0) {
			return "Nitrogen";
		}
		if((this.getO2()==21) && (this.getHe()==0)) {
			return "Air";
		}
		if(this.getHe()==0) {
			// A Nitrox mix
			return nf.format(this.getO2())+"%";
		}
		// After all of the above, we have a trimix
		return nf.format(this.getO2())+"/"+nf.format(this.getHe());
	}

	// Returns the pressure in the correct units
	//public double getPressure() {

	//}
	
	// Top up this mix with supply, up to the given pressure.
	// Return the result. 
	//public Mix topUp(Mix supply, Pressure p) {
		
	//}
	
	// Return the Maximum Operating Depth of this mix.
	//public Depth MOD() {
		
	//}
	
	// Return the Equivalent Narcotic Depth of this mix.
	//public Depth END() {
		
	//}
}
