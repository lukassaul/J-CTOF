import java.util.StringTokenizer;

/**
*  Use this to interpolate from a set of datat
*
*  The _maim_ class will create a file that is a monotonic increasing data file
*/
public class DataInterpolator {

	/**
	* For the _main_ class, to be changed as needed for various date conventions
	*/
	public static int GOODS = (int)(8.20368000 * Math.pow(10,8)); // 1996 - (12/30/69 19:00:00)

	//public static int NUM_LINES = 40640;
	private int lastIndex;
	private int[] dates;
	private float[] data;
	private int NUM_LINES;

	/**
	*  Use this constructor to load the data into RAM
	*
	*/
	public DataInterpolator (String fileName, int num_lines) {
		NUM_LINES = num_lines;
		StringTokenizer st;
		dates = new int[NUM_LINES];
		data = new float[NUM_LINES ];

		file f = new file(fileName);
		f.initRead();
		String line = "";

		boolean eof = false;
		int i = 0;
		while ((line=f.readLine())!=null) {
			st = new StringTokenizer(line);
			dates[i] = (int)(24.0*60.0*60.0*Float.parseFloat(st.nextToken()) + GOODS);
			data[i] = Float.parseFloat(st.nextToken());
			i++;
		}
		f.closeRead();
		System.out.println("Loaded Shift Data " + i);
		System.out.println("a sample:  "+dates[0]+" "+data[0]+" "+dates[1]+" "+data[1]);
		lastIndex = 0;
	}

	/**
	*
	*   pass in day in float doy 1996 form
	*/
	public float getData(float dt) {
		int date = (int)(24*60*60*dt)+ GOODS;
		return getData(date);
	}

	/**
	* Use this to get the data at a value date
	*   making requests nearby each other will speed up search
	*   uses linear interpolation and hunt/bifurcate search
	*
	*/
	public float getData(int date) {

		// no extrapolation allowed
		if (date<dates[0] | date>dates[NUM_LINES-1]) {
		//	System.out.println("outside data range in getShift with: " + date);
			return 100000f;
		}
		int index = 0;
		lastIndex = huntNM(dates,date,lastIndex);
		index = lastIndex;

		//System.out.println(lastIndex+"");
		if (index>NUM_LINES-2) System.out.println("a problem. " + date);

		int div = dates[index+1] - dates[index];
		int dif = date - dates[index];

		if (div==0) System.out.println("a problem, div=0");
		float pseudoIndex = index + ((float)dif/(float)div);

		return lineSolver(data, pseudoIndex);
	}

	/**
	* This interpolates the value at pseudo index jj (between j and j+1)
	* (linear interpolation)
	*/
	private float lineSolver(float[] xx, float jj) {
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
	*
	*/
	public int huntNM(int[]xx, int x, int jlow) {
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
		//System.out.println("search for: " + x + " return: " + jlow);
		System.out.println("didn't find the jlow, out of range?");
		return 1/0;
	}

	/**
	* For creating monotonic array, espcially for later fourier analysis
	*/
	public static final void main(String[] args) {

		int startDate = 80*24*60*60 + GOODS;
		int finishDate = 230*24*60*60 + GOODS;
		int deltaT = 15*60;

		DataInterpolator swdi = new DataInterpolator("raw_1_3-3_0.dat",13229);
		file outF = new file("raw_1_3-3_0_I.dat");
		outF.initWrite(false);
		for (int i=startDate; i<finishDate; i+=deltaT) {
			float data = swdi.getData(i);
			outF.write(i+"\t"+data+"\n");
		}
		outF.closeWrite();
	}
}
