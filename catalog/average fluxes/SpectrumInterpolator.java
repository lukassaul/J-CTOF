import java.util.StringTokenizer;
import java.util.Date;

/**
* used to interpolat x-y plots from files
*
*/
public class ScatterInterpolator {

	public int NUM_LINES = 13331;
	public float[] data;
	public int[] dates;

	/**
	*  Use this constructor to load the data into RAM
	*
	*/
	public ScatterInterpolator(String fileName) {
		file f=new file(fileName);
		NUM_LINES = f.readShitNumLines();
		StringTokenizer st;
		data = new data[NUM_LINES];
		System.out.println("Starting to load scatter data..." + fileName);

		f.initRead();
		for (int i=0; i<NUM_LINES; i++) {
			st = new StringTokenizer(f.readLine());
			dates[i]=Integer.parseInt(st.nextToken());
			data[i]=Float.parseFloat(st.nextToken());
		}
		f.closeRead();
		System.out.println("Loaded scatter Data");
		lastIndex = 0;
	}

	/**
	* Use this to get a count array for a given date range
	*   making requests nearby each other will speed up search
	*   uses hunt/bifurcate search
	*/
	public float getData(int date) {
		// no extrapolation allowed
		if (date<dates[0] | date>dates[NUM_LINES-1]) return null;
		int index = 0;
		index = hunt(dates, NUM_LINES-1, date, lastIndex);
		//index = lastIndex;

		int div = dates[index+1] - dates[index];
		int dif = date - dates[index];
		float pseudoIndex = index + (dif/div);
		return lineSolver(data, pseudoIndex);
		//return tbr;
	}

	/**
	* This interpolates the value at pseudo index jj (between j and j+1)
	* (linear interpolation)
	*
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

	public static final void main (String[] args) {
		Date d1  = new Date();
		PHADataInterpolator pi = new PHADataInterpolator();
		Date d2  = new Date();
		System.out.println("loaded in: " + (d2.getTime()-d1.getTime()));
		PHAData[] test = pi.getData(840239200,840239517);
		Date d3 = new Date();
		System.out.println("got data in: " + (d3.getTime()-d2.getTime()));
		for (int i=0; i<test.length; i++) {
			System.out.println(test[i].date + " " + test[i].energy);
		}

	}

}
