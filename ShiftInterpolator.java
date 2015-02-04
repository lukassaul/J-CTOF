import java.util.StringTokenizer;

public class ShiftInterpolator {


	public static int GOODS = (int)(8.20368000 * Math.pow(10,8)); // 1996 - (12/30/69 19:00:00)

	//public static int NUM_LINES = 40640;
	private int lastIndex;
	private int[] dates;
	private float[] shifts;
	private int NUM_LINES;

	/**
	*  Use this constructor to load the data into RAM
	*
	*/
	public ShiftInterpolator (String fileName, int num_lines) {
		NUM_LINES = num_lines;
		StringTokenizer st;
		dates = new int[NUM_LINES];
		shifts = new float[NUM_LINES ];

		file f = new file(fileName);
		f.initRead();
		String line = "";

		boolean eof = false;
		int i = 0;
		while ((line=f.readLine())!=null) {
			st = new StringTokenizer(line);
			//int dd = Integer.parseInt(st.nextToken());
			dates[i] = Integer.parseInt(st.nextToken());
			shifts[i] = Float.parseFloat(st.nextToken());
			i++;
		}
		f.closeRead();
		System.out.println("Loaded Shift Data " + i);
		System.out.println("a sample:  "+dates[0]+" "+shifts[0]+" "+dates[1]+" "+shifts[1]);
		lastIndex = 0;
	}

	/**
	*
	*   pass in day in float doy 1996 form
	*/
	public float getShift(float dt) {
		int date = (int)(24*60*60*dt)+ GOODS;
		return getShift(date);
	}

	/**
	* Use this to get the data at a value date
	*   making requests nearby each other will speed up search
	*   uses linear interpolation and hunt/bifurcate search
	*
	*/
	public float getShift(int date) {

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

		return lineSolver(shifts, pseudoIndex);
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
	* For testing...
	*/
	public static final void main(String[] args) {

		int testDate = 827561691;

		ShiftInterpolator swdi = new ShiftInterpolator("convect_pearson_result.dat",40294);

		//swdi.setDebug(true);

		float data = swdi.getShift(testDate);

		System.out.println(testDate+" "+data);
	}
}
