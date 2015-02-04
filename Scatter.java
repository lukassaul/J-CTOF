/**
*  This class keeps track of points for generating a scatter plot.
*    2D only in this class
*  Lukas Saul, July 11, 2001
*
*
*  Going to add a histogram functionality here, for making sense of a scatter
*
*/
import java.util.Vector;
import java.util.LinkedList;
public class Scatter {

	public float[] y,x; // these are the scatter points, at the end
	public LinkedList v_y, v_x; // here they are being added
	public String description;
	public boolean finalized;

	public Scatter(String d) {
		v_y = new LinkedList();
		v_x = new LinkedList();
		description = d;
		finalized = false;
	}

	public void addPoint(float x, float y) {
		if (x<Float.MAX_VALUE & x>-Float.MAX_VALUE & y<Float.MAX_VALUE & y>-Float.MAX_VALUE) {
			v_x.add(new Float(x));
			v_y.add(new Float(y));
		}
	}
    /**
    * This one takes a long time for some reason...
    *
    */
	public void finalize() {
		if (!finalized) {
			y = new float[v_y.size()];
			x = new float[v_x.size()];
			int size = v_x.size();
			int dd = 0;
			for (int i=0; i<size; i++) {
				x[i] = ((Float)v_x.removeFirst()).floatValue();
				y[i] = ((Float)v_y.removeFirst()).floatValue();
				if (dd==500) {
					System.out.println("at i = " + i);
					dd = 0;
				}
				else dd++;
				// Vectors are nice, but can be a pain!
			}
			System.out.println("Done finalizing scatter plot...");
			finalized = true;
		}
	}
}




