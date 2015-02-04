import java.util.Vector;
import java.util.Arrays;

/**
*  This class keeps track of points for generating a correlation integral.
*
*  Lukas Saul, Sept 10, 2001
*
* First you add the points, then use finalize() to perform the integrations.
*
*/
public class Correlation {

	public float[] y,x; // these are the  points, at the end
	public long[] dates;
	public Vector v_y, v_x, d_ates; // here they are being added
	//public String description;
	public static int deltaStep;
	public static int numSteps, numPosSteps;
	public float[] yOut;
	public float[] xOut;
	public boolean finalized;

	private long lastDateAdded = 0;
	private int startDelta = 0;

	/**
	*  We pass in number of positive steps
	*  and the step interval in seconds
	*
	*/
	public Correlation(int _deltaStep, int _numPosSteps) {
		v_y = new Vector();
		v_x = new Vector();
		d_ates = new Vector();
		//description = d;
		finalized = false;
		deltaStep = _deltaStep;
		numPosSteps = _numPosSteps;
		numSteps = (2*_numPosSteps)+1;

		yOut = new float[numSteps];
		xOut = new float[numSteps];
		Arrays.fill(yOut,0);
		Arrays.fill(xOut,0);

		lastDateAdded = 0;
		startDelta = 0 - deltaStep*numPosSteps;
		System.out.println("Correlation created with numsteps"  + numSteps);
	}

	/**
	* Use this to add points to memory for performing correlation
	* points must be added in order by date!!!
	*
	* note- points are only added if they match requirements!!
	*/
	public void addPoint(float x, float y, long d) {
		if (finalized) System.out.println("Use a new Correlation instance!");
		if (d<=lastDateAdded) {
			System.out.println("***Problems! Trying to correlate backwards!!***");
		}
		else lastDateAdded = d;
		v_x.add(new Float(x));
		v_y.add(new Float(y));
		d_ates.add(new Long(d));
	}

    /**
    * This one takes a long time for some reason...
    *
    */
	public void finalize() {
		if (!finalized) {
			System.out.println("finalizing correlation with " + v_y.size() + " points");
			y = new float[v_y.size()];
			x = new float[v_x.size()];
			dates = new long[d_ates.size()];
			for (int i=0; i<x.length; i++) {
				x[i] = ((Float)v_x.elementAt(i)).floatValue();
				y[i] = ((Float)v_y.elementAt(i)).floatValue();
				dates[i] = ((Long)d_ates.elementAt(i)).longValue();
				// Vectors are nice, but can be a pain!
			}

			// uh oh, now we have to do the integraion.
			// only add points that are definitely good

			int outIndex = 0;
			int norm = 0; // we normalize to prevent errors due to missing data
			float tbr = 0;
			float tempY = 0;

			// first we have to loop through a range of deltaTs
			for (int t=0-numPosSteps; t<=numPosSteps; t++) {
				tbr = 0;
				tempY = 0;

				// we want to reset getY before doing each integral
				lastY = 0;
				norm = 0;

				// now here's the integral
				System.out.println("Doing corr. integral with delta = " + t*deltaStep);

				for (int i=0; i<x.length; i++) {
					tempY = getY(dates[i]+ t*deltaStep);
					if (tempY != 0) {
						norm++;
						tbr+=x[i]*tempY;
					}
				}
				yOut[outIndex]=tbr/norm;
				xOut[outIndex]=t*deltaStep;
				outIndex++;
			}
			finalized = true;
		}
	}

	/**
	* use this to remember value of y at date d
	*/
	private int lastY = 0;
	private float getY(long date) {
		// we don't want to get ahead of ourselves...
		if (dates[lastY]>date)  return 0;

		int max = dates.length-1;
		if (lastY>=max) return 0;
		System.out.println("trying lastY = " + lastY);;
		while ( !((dates[lastY] <= date) && (dates[lastY+1] >= date)) ) {
			if (dates[lastY]>date)  return 0;
			lastY++;
			if (lastY>=max) return 0;
		}
		return y[lastY];
	}
}


