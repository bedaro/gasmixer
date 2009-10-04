package divestoclimb.lib.scuba;

public class VanDerWaals {

	public static final float A_OXYGEN = 1.382f;
	public static final float A_HELIUM = 0.0346f;
	public static final float A_NITROGEN = 1.370f;
	public static final float B_OXYGEN = 0.03186f;
	public static final float B_HELIUM = 0.02380f;
	public static final float B_NITROGEN = 0.03870f;
	// Avogradro's number
	public static final float N_A = 6.022E23f;
	
	/**
	 * Computes the particle attraction factor a for a theoretical homogeneous
	 * gas equivalent in behavior to the given gas mixture.
	 * @param m The gas mix to generate a for.
	 * @return The value of a.
	 */
	public static double computeA(Mix m) {
		float x[] = { m.getfO2(), m.getfN2(), m.getfHe() };
		float a[] = { A_OXYGEN, A_NITROGEN, A_HELIUM };
		double total = 0;
		for(int i = 0; i < 3; i++) {
			for(int j = 0; j < 3; j++) {
				total += (float)(Math.sqrt(a[i]*a[j])*x[i]*x[j]);
			}
		}
		return total;
	}
	
	/**
	 * Computes the particle volume factor b for a theoretical homogeneous
	 * gas equivalent in behavior to the given gas mixture.
	 * @param m The gas mix to generate b for.
	 * @return The value of b.
	 */
	public static double computeB(Mix m) {
		float x[] = {m.getfO2(), m.getfN2(), m.getfHe() };
		float b[] = { B_OXYGEN, B_NITROGEN, B_HELIUM };
		double total = 0;
		for(int i = 0; i < 3; i++) {
			for(int j = 0; j < 3; j++) {
				total += (float)(Math.sqrt(b[i]*b[j])*x[i]*x[j]);
			}
		}
		return total;
	}
}
