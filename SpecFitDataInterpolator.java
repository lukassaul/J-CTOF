import java.util.StringTokenizer;

/**
*  Use this to interpolate discrete data to midpoints
*    very useful for comparing two datasets
*
*  Apr. 03 -  we are using longer HUNT routine - non-monotonic OK..
*/
public class SpecFitDataInterpolator {

	//public static int NUM_LINES = 40640;

	public int[] dates;
	public float[] intercepts;
	public float[] slopes;
	public int lastIndex;
	public int NUM_LINES;
	public String fileName;

	private boolean debug;
	private boolean firstError;

	private ShiftInterpolator si;
	private boolean doShift;

	/**
	*  Use this constructor to load the data into RAM
	*
	*/
	public SpecFitDataInterpolator(String _fileName, int num_lines) {
		debug = false;
		firstError = true;

		fileName = _fileName;
		NUM_LINES = num_lines;
		StringTokenizer st;
		dates = new int[NUM_LINES];
		slopes = new float[NUM_LINES ];
		intercepts = new float[NUM_LINES];

		System.out.println("Starting to load SpecFit data...");

		file f = new file(fileName);
		f.initRead();
		//DataInputStream dis = new DataInputStream(new FileInputStream(filename));

		String line = "";
		//int lastDate = 0;
		boolean eof = false;
		int i = 0;
		while ((line=f.readLine())!=null) {
			st = new StringTokenizer(line);
			//int dd = Integer.parseInt(st.nextToken());
			dates[i] = Integer.parseInt(st.nextToken());
			intercepts[i] = Float.parseFloat(st.nextToken());
			slopes[i] = Float.parseFloat(st.nextToken());
			//temps[i] = Float.parseFloat(st.nextToken());
			i++;
		}
		f.closeRead();
		System.out.println("Loaded SpecFit Data " + dates[i-1] + " " + i);
		//NUM_LINES=i;
		lastIndex = 0;
	}


	/**
	* Use this to add a convect shift to the specFitINterpolator..
	*
	*/
	public void setConvect(String con_file) {
		doShift = true;
		file f = new file(con_file);
		int qq = f.readShitNumLines();
		si = new ShiftInterpolator(con_file,qq);
	}




	/**
	* Use this to input DOY ...
	*  Use the ShiftInterpolator here also!!
	*/
	public float[] getData(float date) {
		if (!doShift) return getData((int)(date*24*60*60*10));
		else {
			float shift = si.getShift(date);
			return getData((int)(date*24*60*60*10-shift));
		}
	}

	/**
	* Use this to get the data at a value date
	*   making requests nearby each other will speed up search
	*   uses linear interpolation and hunt/bifurcate search
	*/
	public float[] getData(int date) {
		// no extrapolation allowed
		if (date<dates[0] | date>dates[NUM_LINES-1]) {
			if (firstError) {
				System.out.println("SpecFit interpolator outside data range with: "
							+ date + " " + fileName + " zero: " + dates[0] + " " +
								dates[NUM_LINES-1]);
				firstError = false;
			}
			return null;
		}
		int index = 0;
		//huntNM(dates, NUM_LINES-1, date, lastIndex);
		lastIndex = huntNM(dates,date,lastIndex);
		index = lastIndex;
		//System.out.println(lastIndex+"");
		if (index>NUM_LINES-2) System.out.println("a prob. " + date);

		float[] tbr = new float[2];
		int div = dates[index+1] - dates[index];
		int dif = date - dates[index];
		if (div==0) System.out.println("a problem, div=0");
		float pseudoIndex = index + ((float)dif/(float)div);
		tbr[0] = lineSolver(intercepts, pseudoIndex);
		tbr[1] = lineSolver(slopes, pseudoIndex);
		//tbr[2] = lineSolver(temps, pseudoIndex);
		return tbr;
	}

	/**
	* This interpolates the value at pseudo index jj (between j and j+1)
	* (linear interpolation)
	*/
	private float lineSolver(float[] xx, float jj) {
		if (debug) o("trying line solver: " + jj);

		try {
			int j = (int)Math.floor((double)jj);
			return ( (xx[j+1]-xx[j])*(jj-(float)(j+1)) + xx[j+1]);
		}
		catch (Exception e) {
			e.printStackTrace();
			System.out.println("index search: " + jj + " size: " + xx.length);
			return 1/0;
		}
	}

	/**
	*  This one finds the appropraite index for a non-monotonic array
 	*
	*/
	public int huntNM(int[]xx, int x, int jlow) {
		if (xx[jlow]==x) return jlow;
		if (xx[jlow]<x) {
			for (int i=jlow+1; i<xx.length; i++) {
				if (xx[i]>x) {
					return(i-1);
					//i=xx.length;
				}
			}
		}
		if (xx[jlow]>x) {
			for (int i=jlow-1; i>=0; i--) {
				if (xx[i]<x) {
					return i;
					//i=-1;
				}
			}
		}
		System.out.println("didn't find the jlow, out of range??????");
		return 1/0;
	}

	private void o(String s) {
		System.out.println(s);
	}

	public void setDebug(boolean b) {
		debug = b;
	}

	/**
	* For testing ...
	*
	*/
	public static final void main(String[] args) {

		int testDate = 69168095;
		file f = new file("mfidata3SPEC_MAGfit_10.dat");
		int q = f.readShitNumLines();

		SpecFitDataInterpolator swdi = new SpecFitDataInterpolator("mfidata3SPEC_MAGfit_10.dat",q);

		//swdi.setDebug(true);

		float[] data = swdi.getData(testDate);

		System.out.println(testDate+" "+data[0]+" "+data[1]);

		data = swdi.getData(92.44f);
		System.out.println("92.44"+" "+data[0]+" "+data[1]);
	}

}
