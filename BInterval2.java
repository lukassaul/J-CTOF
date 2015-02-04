/**
*  This class is a wrapper for some magnetic field configuration interval with:
*  2 bins in azimuth, 2 bins in polar, 1 bin in magnitude, 1 bin in "radial"
*  plus some bins in alfven speed
*    (note:  alven speed goes here instead of in SW reader, although that data is used
*      to compute alven speed as well)
*/

public class BInterval2 {

	/**
	* These static ints are for setting certain parameters
	* in a desired magnetic field "interval"
	*/
	public static int POLAR = 1;
	public static int AZIMUTH = 2;
	public static int MAGNITUDE = 3;
	public static int ALFVEN = 4;
	public static int ALFVEN_RATIO = 5;
	public static int RADIAL = 6;
	public static int B_RADIAL_COMPONENT = 7;
	public static int DELTA_B = 8;
	public static int AVERAGE_MAGNITUDE = 9;
	public static int ETA = 10;

	/**
	* These are from SWInterval.java, just here for reference
	*/
	public static int VELOCITY = 101;
	public static int DENSITY = 102;
	public static int THERMAL_VELOCITY = 103;

	/**
	* Some more units here...
	*
	*/
	public static int FLUX = 201;
	public static int TAIL = 202;
	public static int EFLUX = 203;



	/** this signifies degees away from radial, in either direction.
	* thus, we have range from 0 to 90
	*/
	public int numberOfTries, numberOfHits;

	float a1min, a1max, a2min, a2max, p1min, p1max, p2min, p2max, magmin, magmax;
	float radmin, radmax, almin, almax, alratmin, alratmax, etamin, etamax;
	float averagebmin, averagebmax;
	boolean doPolar, doAzimuth, doMagnitude, doRadial, doAlfven, doAlfvenRatio;
	boolean doAverage, doEta;
	HelioPoint hp, hpRef;
	float va;  // ALFVEN SPEED


	/**  The default is -1, this means don't check it
	*  Use this constructor to initialize a magnetic field interval
	*/
	public BInterval() {
		a1min=-1;
		a1max=-1;
		a2min=-1;
		a2max=-1;
		p1min=-1;
		p1max=-1;
		p2min=-1;
		p2max=-1;
		magmin=-1;
		magmax=-1;
		radmin=-1;
		radmax=-1;
		almin=-1;
		almax=-1;
		alratmin=-1;
		alratmax=-1;
		etamin=-1;
		etamax=-1;
		averagebmin=-1;
		averagebmax=-1;
		doPolar = false;
		doAzimuth = false;
		doMagnitude = false;
		doRadial = false;
		doAlfven = false;
		doAlfvenRatio = false;
		doAverage = false;
		doEta = false;
		numberOfTries = 0;
		numberOfHits = 0;
		hp = new HelioPoint();
		hpRef = new HelioPoint(HelioPoint.CARTESIAN, -1,0,0);
	}


	/** This only ads the interval if there's a spot for it - add intervals before checking B configs!
	*/
	public void addInterval(float i1, float i2, int description) {
		System.out.println("new interval " + description + " " + i1 + " " + i2);
		if (description == POLAR) {
			doPolar = true;
			if (p1min == -1) {
				p1min = i1;
				p1max = i2;
			}
			else if (p2min == -1) {
				p2min = i1;
				p2max = i2;
			}
		}
		if (description == AZIMUTH) {
			doAzimuth = true;
			if (a1min == -1) {
				a1min = i1;
				a1max = i2;
			}
			else if (a2min == -1) {
				a2min = i1;
				a2max = i2;
			}
		}if (description == MAGNITUDE) {
			doMagnitude = true;
			if (magmin == -1) {
				magmin = i1;
				magmax = i2;
			}
		}if (description == RADIAL) {
			doRadial = true;
			radmin = i1;
			radmax = i2;
		}if (description == ALFVEN) {
			doAlfven = true;
			almin=i1;
			almax=i2;
		}if (description == ALFVEN_RATIO) {
			doAlfvenRatio = true;
			alratmin=i1;
			alratmax=i2;
		}if (description == AVERAGE_MAGNITUDE) {
			doAverage = true;
			averagebmin=i1;
			averagebmax=i2;
		}if (description == ETA) {
			doEta = true;
			etamin=i1;
			etamax=i2;
		}
	}


	/** This method used to check a magnetic field configuration to see if it fits criteria
	*
	*  we are passing in some vsw data here also, usually this is ignored
	*   (only for alfven speed calculations)
	* phi,the,mag,vel,de
    */
	public boolean check(float a, float p, float m, float vsw, float den, float av, float eta) {
		numberOfTries++;
		boolean inp = false;
		boolean ina = false;
		boolean inm = false;
		boolean inr = false;
		boolean inalf = false;
		boolean inalfrat = false; // alfven speed to vsw ratio
		boolean inaverage = false;
		boolean ineta = false;

		if (doAlfven | doAlfvenRatio) {
			// compute alfven speed HERE:  va = b^2 / (4*pi*density)
			va = 100*m/2/(float)Math.sqrt(1.67*Math.PI*den);
			//System.out.println("va = " + va + " vsw= " + vsw);
		}

		//first check polar
		if (!doPolar) inp = true;
		else {
			if ( (p > p1min) && (p < p1max) ) inp = true;
			else if ((p2min!=-1) && (p > p2min) && (p < p2max) ) inp = true;
		}
		// check azimuth
		if (!doAzimuth) ina = true;
		else {
			if ( (a > a1min) && (a < a1max) ) ina = true;
			else if ((a2min!=-1) && (a > a2min) && (a < a2max) ) ina = true;
		}
		// check magnitude
		if (!doMagnitude) inm = true;
		else {
			if ( (m > magmin) && (m < magmax) ) inm = true;
		}
		// check degrees off radial
		if (!doRadial) inr = true;
		else {
			// this is a conversion to spherical from a GSE (r, phi, latitude)
			hp.setCoords(HelioPoint.SPHERICAL, m, a*Math.PI/180, p*Math.PI/180 + Math.PI/2);
			double angle = hp.getAngle(hpRef);
			if (angle>Math.PI/2) {
				angle=Math.PI/2 - (angle - Math.PI/2);
			}
			angle = angle*180/Math.PI; // convert to degrees
			//System.out.println("checking radial BInterval: " + a + " " + p + " angle: " + angle);

			if ( (angle > radmin) && (angle < radmax) )	inr = true;
		}

		// check alfven speed
		if (!doAlfven) inalf = true;
		else {
			if ( (va > almin) && (va < almax) ) inalf = true;
		}

		// check alfven ratio
		if (!doAlfvenRatio) inalfrat = true;
		else {
			if ( (va/vsw > alratmin) && (va/vsw < alratmax) ) inalfrat = true;
		}

		// check average B
		if (!doAverage) inaverage = true;
		else {
			if ( (av > averagebmin) && (av < averagebmax) ) inaverage = true;
		}

		// check eta
		if (!doEta) ineta = true;
		else {
			if ( (eta > etamin) && (eta < etamax) ) ineta = true;
		}


		// only return true if all are good
		if (inp && ina && inm && inr && inalf && inalfrat && inaverage && ineta) {
			numberOfHits++;
			return true;
		}
		else return false;
	}
}
