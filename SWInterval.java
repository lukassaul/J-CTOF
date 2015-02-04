/**
*  This class is a wrapper for some magnetic field configuration interval with:
*  2 bins in azimuth, 2 bins in polar, 1 bin in magnitude
*/

public class SWInterval {
	/**
	* From BInterval, here as a reference
	*/
	public static int POLAR = 1;
	public static int AZIMUTH = 2;
	public static int MAGNITUDE = 3;
	public static int ALFVEN = 4;
	public static int ALFVEN_RATIO = 5;
    
	public static int RADIAL = 6;

	/**
	* These are the useful ones for this class:
	*/
	public static int VELOCITY = 101;
	public static int DENSITY = 102;
	public static int THERMAL_VELOCITY = 103;

	float vmin, vmax, dmin, dmax, tvmin, tvmax;
	boolean doVelocity, doDensity, doThermalVelocity;
	public int numberOfTries, numberOfHits;

	/**  The default is -1, this means don't check it
	*/
	public SWInterval() {
		vmin = -1;
		vmax = -1;
		dmin = -1;
		dmax = -1;
		tvmin = -1;
		tvmax = -1;
		doVelocity = false;
		doDensity = false;
		doThermalVelocity = false;
		numberOfHits = 0;
		numberOfTries = 0;
	}

	/** This only ads the interval if there's a spot for it - add intervals before checking B configs!
	*/
	public void addInterval(float i1, float i2, int description) {
		System.out.println("Adding interval to swInterval " + description + " " + i1 + " " + i2);
		if (description == VELOCITY) {
			doVelocity = true;
			if (vmin == -1) {
				vmin = i1;
				vmax = i2;
			}
		}
		else if (description == DENSITY) {
			doDensity = true;
			if (dmin == -1) {
				dmin = i1;
				dmax = i2;
			}
		}
		else if (description == THERMAL_VELOCITY) {
			doThermalVelocity = true;
			if (tvmin == -1) {
				tvmin = i1;
				tvmax = i2;
			}
		}
	}

	/** This method used to check a magnetic field configuration to see if it fits criteria
	*/
	public boolean check(float v, float d, float vth) {
		numberOfTries ++;
		boolean inv = false;
		boolean ind = false;
		boolean invth = false;
		//first check vel
		if (!doVelocity) inv = true;
		else {
			if ( (v > vmin) && (v < vmax) ) inv = true;
		}
		// check dense
		if (!doDensity) ind = true;
		else {
			if ( (d > dmin) && (d < dmax) ) ind = true;
		}
		// check magnitude
		if (!doThermalVelocity) invth = true;
		else {
			if ( (vth > tvmin) && (vth < tvmax) ) invth = true;
		}
		// only return true if all 3 are good
		if (inv && ind && invth) {
			numberOfHits++;
			return true;
		}
		else return false;
	}
}
