package divestoclimb.lib.scuba;

public interface GasSource {

	/**
	 * Returns the partial pressure of oxygen at depth while breathing this gas.
	 * @param depth The depth in the currently set units
	 * @return The partial pressure of oxygen in ATA
	 */
	abstract public float pO2AtDepth(int depth);

	/**
	 * Returns the partial pressure of nitrogen at depth while breathing this gas.
	 * @param depth The depth in the currently set units
	 * @return The partial pressure of nitrogen in ATA
	 */
	abstract public float pN2AtDepth(int depth);

	/**
	 * Returns the partial pressure of helium at depth while breathing this gas. 
	 * @param depth The depth in the currently set units
	 * @return The partial pressure of helium in ATA
	 */
	abstract public float pHeAtDepth(int depth);

}
