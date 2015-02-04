import java.io.*;
import java.util.*;

/**
* Looks like we are making a time average here - with width TIME_BIN
*
*
*/
public class CreateSlidingMagAverageDataset {

	public file inFile = new file("MAGDATA.dat");
	public file outFile = new file("SLIDING_MAGDATA.dat");

	public static float TIME_BIN = (float)(.5*24*60*60);  // in days

	/**
	* Use this to create a sliding average for
	*  Schwadron's eta parameter...
	*/
	public CreateSlidingMagAverageDataset () {
		FloatSet fs = new FloatSet();
		inFile.initRead();
		outFile.initWrite(false);
		String garbage = "garbage";
		String line = "";
		int counter = 0;
		while ((line=inFile.readLine())!=null) {
			counter++;
			StringTokenizer st = new StringTokenizer(line);
			//garbage = st.nextToken();
			int date = Integer.parseInt(st.nextToken());
			garbage = st.nextToken();
			garbage = st.nextToken();
			garbage = st.nextToken();
			float mag = Float.parseFloat(st.nextToken());

			if (mag<30) fs.addData(date,mag);
			if (fs.kicked & (fs.getSize() > 10)) {
				// we output some data
				float av = fs.getAverageMag();
				float etaSq = fs.getEtaSquared(av);
				int dateM = fs.getMidDate();

				outFile.write(dateM + "\t" + av + "\t" + etaSq + "\n");
			}
		}
		inFile.closeRead();
		outFile.closeWrite();
		System.out.println(counter +"");
	}

	/**
	*  (INNER CLASS)
	* This will do the compiling for us. As we read through
	*   the file, we add to this
	*/
	public class FloatSet {
		private Vector mags;
		private Vector dates;
		/**
		* This indicates if a date has been kicked out since last adding
		*/
		public boolean kicked;
		public FloatSet () {
			mags = new Vector();
			dates = new Vector();
		}
		public void addData(int date, float mag) {
			// first we get rid of any old shit no longer in the interval
			kicked = false;
			for (int i=0; i<dates.size(); i++) {
				float dd = ((Integer)(dates.elementAt(i))).intValue();
				if (dd< (date-TIME_BIN)) {
					dates.removeElementAt(i);
					mags.removeElementAt(i);
					kicked = true;
				}
			}

			mags.addElement(new Float(mag));
			dates.addElement(new Integer(date));
		}

		public int getMidDate() {
			float ff = 0;
			for (int i=0; i<dates.size(); i++) {
				ff+= ((Integer)(dates.elementAt(i))).intValue();
			}
			ff = ff/dates.size();
			return (int)ff;
		}

		public float getAverageMag() {
			float ff = 0;
			for (int i=0; i<dates.size(); i++) {
				ff+= ((Float)(mags.elementAt(i))).floatValue();
			}
			ff = ff/dates.size();
			return ff;
		}

		/**
		* This is kind of like a standard deviation, normalized to the inputted average
		*/
		public float getEtaSquared(float bAv) {
			float ff = 0;
			for (int i=0; i<dates.size(); i++) {
				float aMag= ((Float)(mags.elementAt(i))).floatValue();
				ff += (aMag-bAv)*(aMag-bAv)/bAv/bAv;
			}
			ff = ff/dates.size();
			return ff;
		}

		public float getSize() {
			return dates.size();
		}
	}

	public static void main(String[] args) {

		CreateSlidingMagAverageDataset csmad = new CreateSlidingMagAverageDataset();
	}
}