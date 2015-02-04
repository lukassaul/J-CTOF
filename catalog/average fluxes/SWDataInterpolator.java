import java.util.StringTokenizer;

public class SWDataInterpolator {

	//public static int NUM_LINES = 40640;

	public int[] dates;
	public float[] speeds;
	public float[] densities;
	public float[] temps;
	public int lastIndex;
	public int NUM_LINES;

	/**
	*  Use this constructor to load the data into RAM
	*
	*/
	public SWDataInterpolator(String fileName, int num_lines) {
		NUM_LINES = num_lines;
		StringTokenizer st;
		dates = new int[NUM_LINES];
		speeds = new float[NUM_LINES ];
		densities = new float[NUM_LINES];
		temps = new float[NUM_LINES];
		System.out.println("Starting to load Wind data...");
		file f = new file(fileName);
		f.initRead();
		String line = "";
		boolean eof = false;
		int i = 0;
		while ((line=f.readLine())!=null) {
			st = new StringTokenizer(line);
			dates[i] = (int)Float.parseFloat(st.nextToken());
			speeds[i] = Float.parseFloat(st.nextToken());
			densities[i] = Float.parseFloat(st.nextToken());
			temps[i] = Float.parseFloat(st.nextToken());
			i++;
		}
		f.closeRead();
		System.out.println("Loaded Wind Data");
		lastIndex = 0;
	}

	/**
	* Use this to get the data at a value date
	*   making requests nearby each other will speed up search
	*   uses linear interpolation and hunt/bifurcate search
	*/
	public float[] getData(int date) {
		// no extrapolation allowed
		if (date<dates[0] | date>dates[NUM_LINES-1]) return null;
		int index = 0;
		index = hunt(dates, NUM_LINES-1, date, lastIndex);
		//index = lastIndex;

		float[] tbr = new float[3];
		int div = dates[index+1] - dates[index];
		int dif = date - dates[index];
		float pseudoIndex = index + (dif/div);
		tbr[0] = lineSolver(speeds, pseudoIndex);
		tbr[1] = lineSolver(densities, pseudoIndex);
		tbr[2] = lineSolver(temps, pseudoIndex);
		return tbr;
	}

	/**
	* This interpolates the value at pseudo index jj (between j and j+1)
	* (linear interpolation)
	*/
	private float lineSolver(float[] xx, float jj) {
		int j = (int)Math.floor((double)jj);
		return ( (xx[j+1]-xx[j])*(jj-(float)(j+1)) + xx[j+1]);
	}



	/**
	*  From NUMERICAL RECIPES nr.com
	*Given an array xx[1..n], and given a value x, returns a value jlo such that x is between
	*	xx[jlo] and xx[jlo+1]. xx[1..n] must be monotonic, either increasing or decreasing.
	*	jlo=0 or jlo=n is returned to indicate that x is out of range. jlo on input is taken as the
	*   initial guess for jlo on output.
	*/
	private int hunt(int[] xx, int n, int x, int jlow) {

		int jm,jhi,inc;
		boolean ascnd;
		ascnd=(xx[n] >= xx[1]); //True if ascending order of table, false otherwise.
		if (jlow <= 0 || jlow > n) { //Input guess not useful. Go immediately to bisection.
			jlow=0;
			jhi=n+1;
		} else {
			inc=1; //Set the hunting increment.
			if (x >= xx[jlow] == ascnd) { //Hunt up:
				if (jlow == n) return jlow;
				jhi=(jlow)+1;
				while (x >= xx[jhi] == ascnd) { //Not done hunting,
					jlow=jhi;
					inc += inc; //so double the increment
					jhi=(jlow)+inc;
					if (jhi > n) { //Done hunting, since o end of table.
						jhi=n+1;
						break;
					} //Try again.
				} //Done hunting, value bracketed.
			} else { //Hunt down:
				if (jlow == 1) {
					jlow=0;
					return jlow;
				}
				jhi=(jlow)--;
				while (x < xx[jlow] == ascnd) { //Not done hunting,
					jhi=(jlow);
					inc <<= 1; //double the increment
					if (inc >= jhi) { //Done hunting, since o end of table.
						jlow=0;
						break;
					}
					else jlow=jhi-inc;
				} //and try again.
			} //Done hunting, value bracketed.
		} //Hunt is done, so begin the nal bisection phase:
		while (jhi-(jlow) != 1) {
			jm=(jhi+(jlow)) >> 1;
			if (x >= xx[jm] == ascnd)
			jlow=jm;
			else
			jhi=jm;
		}
		if (x == xx[n]) jlow=n-1;
		if (x == xx[1]) jlow=1;
		return jlow;
	}
}
