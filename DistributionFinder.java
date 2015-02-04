import java.util.Vector;
import java.util.StringTokenizer;

/**
* Take a datafile, histogram it,
*    output it.  Nice and simple
*/
public class DistributionFinder {

	private static String inFile = "ptr_rad_v_doy.dat";
	private static String outFile = "ptr_hist_3.dat";

	private boolean skipFirstColumn = true;

	private int numDivisions = 100;

	public String CRLF = System.getProperty("line.separator");

	/**
	*
	*/
	public DistributionFinder() {
		file of = new file(outFile);
		file inf = new file(inFile);
		String line = "";
		of.initWrite(false);
		inf.initRead();

		// load the file...
		Vector xVec = new Vector();

		boolean eof = false;
		while (!eof) {
			if ((line = inf.readLine()) == null) eof = true;
			else {
				try {
					StringTokenizer st = new StringTokenizer(line);

					// skip the first entry, we want the y column..
					if (skipFirstColumn) {
						String garbage = st.nextToken();
					}
					float nx = Float.parseFloat(st.nextToken());

					xVec.addElement(new Float(nx));
				}catch (Exception e) {e.printStackTrace();}
			}
		}

		System.out.println("Loaded " + xVec.size() );
		inf.closeRead();



		// cast the vectors to an array and find min and max
		float xMax = -100000000;
		float xMin = 100000000;
		float[] x = new float[xVec.size()];
		for (int i=0; i<xVec.size(); i++) {
			x[i]=((Float)xVec.elementAt(i)).floatValue();
			if (x[i]>xMax) xMax = x[i];
			if (x[i]<xMin) xMin = x[i];
		}

		// our output array can now be defined..
		float[] xOut = new float[numDivisions+1];
		float delta = (xMax-xMin)/numDivisions;

		// call the xOut the left edge of each bin
		for (int i=0; i<numDivisions+1; i++) {
			xOut[i]=xMin+i*delta;
		}

		// our Histogram
		Histogram hg = new Histogram(xOut[0],xOut[numDivisions],numDivisions);

		// add the numbers to the histogram now.
		for (int j=0; j<x.length; j++) {
			hg.addEvent(x[j]);
		}

		// lets do a curve fit to the histogram while we have the arrays..
		CurveFitter cf = new CurveFitter(hg.label,hg.data);
		cf.doFit(CurveFitter.GAUSSIAN);
		System.out.println(cf.getResultString());


		// ok, output to file and we done!

		for (int i=0; i<numDivisions; i++) {
			of.write(hg.label[i]+"\t"+hg.data[i]+CRLF);
		}
		of.closeWrite();
	}


	public static final void main(String[] args) {
		DistributionFinder stf = new DistributionFinder();
	}
}
