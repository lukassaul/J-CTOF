import java.util.Vector;
import java.util.Arrays;

/**
*
*  Moving on to a cooler statistical procedure.
*   Pearsons Correlation Coefficient
*
*/
public class PearsonsCorrelation {

	public Vector v_y, v_x; // here they are being added
	public boolean finalized;
	public float answer;
	public float error;
	public float error2; // in Fisher's Z here..

	private long lastDateAdded = 0;
	private int startDelta = 0;

	private static boolean d = false;

	/**
	* Lets correlate some variables, shall we?
	*/
	public PearsonsCorrelation() {
		answer = 0f;
		error = 0f;
		v_y = new Vector();
		v_x = new Vector();
		finalized = false;
	}

	/**
	* An interesting technique... will it work?
	*
	*
	*/
	public static float getCorrelation(float[] x, float[] y) {
		float xBar = getAverage(x);
		float yBar = getAverage(y);
		if (d) o(xBar+" "+yBar);
		float pxx=0; float pyy=0.0f; float pxy = 0.0f;
		for (int i=0; i<x.length; i++) {
			pxx+=getSquare(x[i]-xBar);
			pyy+=getSquare(y[i]-yBar);
			pxy+=(x[i]-xBar)*(y[i]-yBar);
		}
		if (d) o(pxx+" "+pyy+" "+pxy);
		float theGoods = pxy/(float)Math.sqrt(pxx*pyy);
		return theGoods;
	}


	public static float getAverage(float[] a) {
		float sum = 0f;
		for (int i=0; i<a.length; i++) {
			sum+=a[i];
		}
		return sum/a.length;
	}

	public static float getSquare(float f) {
		return f*f;
	}


	/**
	* Use this to add points to memory for performing correlation
	* points must be added in order by date!!!
	*
	* note- points are only added if they match requirements!!
	*/
	public void addPoint(float x, float y) {
		if(d)  o("adding: "+ x+" "+y);
		v_x.add(new Float(x));
		v_y.add(new Float(y));
	}

    /**
    * This one takes a long time for some reason...
    *
    */
	public void finalize() {
		if (!finalized) {
			System.out.println("finalizing correlation with " + v_y.size() + " points");
			float[] y = new float[v_y.size()];
			float[] x = new float[v_x.size()];
			for (int i=0; i<x.length; i++) {
				x[i] = ((Float)v_x.elementAt(i)).floatValue();
				y[i] = ((Float)v_y.elementAt(i)).floatValue();
				// Vectors are nice, but can be a pain!
			}
			answer = getCorrelation(x,y);
			error = (float)Math.sqrt((1-answer*answer)/(x.length-2));

			error2 = (float)Math.sqrt(1/((float)x.length-3.0f));

			finalized = true;
		}
	}

	private static void o(String s) {
		System.out.println(s);
	}

}


