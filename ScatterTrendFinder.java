import java.util.Vector;
import java.util.StringTokenizer;

/**
*  Take a two column scatter datafile, and bin it in the x axis.
*  The average of each bin and uncertainty in the mean in each are the output
*
*/
public class ScatterTrendFinder {

	private static String inFileD = "hecvptr2c.dat";
	private static String outFileD = "hecvptr2c_t.dat";

	private int numDivisions = 40;
	public String inFile, outFile;

	// USE this to switch X and Y axes
	private boolean reverse = true;

	public String CRLF = System.getProperty("line.separator");



	/**

	* for using the files above rather than command line style

	*/

	public ScatterTrendFinder() {
		this(inFileD,outFileD);
	}


	/**
	*
	*/
	public ScatterTrendFinder(String _inFile, String _outFile) {

		inFile = _inFile;  outFile=_outFile;
		file of = new file(outFile);
		file inf = new file(inFile);
		String line = "";
		of.initWrite(false);
		inf.initRead();

		// load the file...
		Vector xVec = new Vector();
		Vector yVec = new Vector();

		boolean eof = false;
		while (!eof) {
			if ((line = inf.readLine()) == null) eof = true;
			else {
				try {
					StringTokenizer st = new StringTokenizer(line);
					float nx = Float.parseFloat(st.nextToken());
					float ny = Float.parseFloat(st.nextToken());
					xVec.addElement(new Float(nx));
					yVec.addElement(new Float(ny));
				}catch (Exception e) {e.printStackTrace();}
			}
		}

		System.out.println("Loaded " + xVec.size() + " - " + yVec.size());
		inf.closeRead();

		if (reverse) {
			Vector temp = (Vector)xVec.clone();
			xVec = yVec;
			yVec = temp;
		}

		// cast the vectors to an array and find min and max
		float xMax = -100000000;
		float xMin = 100000000;
		float yMax = -100000000;
		float yMin = 100000000;
		float[] x = new float[xVec.size()];
		float[] y = new float[yVec.size()];
		for (int i=0; i<xVec.size(); i++) {
			x[i]=((Float)xVec.elementAt(i)).floatValue();
			if (x[i]>xMax) xMax = x[i];
			if (x[i]<xMin) xMin = x[i];
		}
		for (int i=0; i<yVec.size(); i++) {
			y[i]=((Float)yVec.elementAt(i)).floatValue();
			if (y[i]>yMax) yMax = y[i];
			if (y[i]<yMin) yMin = y[i];
		}

		// our output array can now be defined..
		float[] xOut = new float[numDivisions+1];
		float[] yOut = new float[numDivisions];
		float[] eOut = new float[numDivisions];
		float delta = (xMax-xMin)/numDivisions;

		// call the xOut the left edge of each bin
		for (int i=0; i<numDivisions+1; i++) {
			xOut[i]=xMin+i*delta;
		}

		// lets compute the averages and uncertainties in mean for each bin
		for (int i=0; i<numDivisions; i++) {
			float tempAverage = 0.0f;
			int tempCounter = 0;
			float tempDeviation = 0.0f;
			for (int j=0; j<x.length; j++) {
				if (x[j]>xOut[i] && x[j]<xOut[i+1]) {
					tempAverage+=y[j];
					tempCounter++;
				}
			}
			if (tempCounter>0) tempAverage /= tempCounter;
			yOut[i] = tempAverage;

			// do it again to get standard deviation
			for (int j=0; j<x.length; j++) {
				if (x[j]>xOut[i] && x[j]<xOut[i+1]) {
					tempDeviation+=(y[j]-tempAverage)*(y[j]-tempAverage);
				}
			}
			if (tempCounter>0) tempDeviation /= tempCounter;
			tempDeviation = (float)Math.sqrt(tempDeviation/tempCounter);
			eOut[i] = tempDeviation;
			System.out.println("bin: " + i + " counts: " + tempCounter);
		}

		// ok, output to file and we done!

		for (int i=0; i<numDivisions; i++) {
			of.write(xOut[i]+"\t"+yOut[i]+"\t"+eOut[i]+CRLF);
		}
		of.closeWrite();
	}


	public static final void main(String[] args) {
		ScatterTrendFinder stf = new ScatterTrendFinder();
	}
}
