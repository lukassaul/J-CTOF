import java.io.*;
import java.util.*;

public class CreateSlidingMagAngleAverageDataset {

	public file inFile = new file("MAG_ANGLE_DATA.dat");
	public file outFile = new file("SLIDING_MAG_ANGLE_DATA.dat");

	// average over 30 min.
	public static float TIME_BIN = (float)(60*30);  // in days

	/**
	* This will do the compiling for us. As we read through
	*   the file, we add to this
 	*/
	public class FloatAngleSet {
		private Vector angles;
		private Vector dates;
		/**
		* This indicates if a date has been kicked out since last adding
		*/
		public boolean kicked;
		public FloatAngleSet () {
			angles = new Vector();
			dates = new Vector();
		}
		public void addData(int date, float mag) {
			// first we get rid of any old shit no longer in the interval
			kicked = false;
			for (int i=0; i<dates.size(); i++) {
				float dd = ((Integer)(dates.elementAt(i))).intValue();
				if (dd< (date-TIME_BIN)) {
					dates.removeElementAt(i);
					angles.removeElementAt(i);
					kicked = true;
				}
			}

			angles.addElement(new Float(mag));
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

		public float getAverageAngle() {
			float ff = 0;
			for (int i=0; i<dates.size(); i++) {
				ff+= ((Float)(angles.elementAt(i))).floatValue();
			}
			ff = ff/dates.size();
			return ff;
		}

		public float getSize() {
			return dates.size();
		}
	}


	public CreateSlidingMagAngleAverageDataset () {
		FloatAngleSet fs = new FloatAngleSet();
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
			float ang = Float.parseFloat(st.nextToken());

			fs.addData(date,ang);
			if (fs.kicked & (fs.getSize() > 5)) {
				// we output some data
				float av = fs.getAverageAngle();
				//float etaSq = fs.getEtaSquared(av);
				int dateM = fs.getMidDate();

				outFile.write(dateM + "\t" + av + "\n");
			}
		}
		inFile.closeRead();
		outFile.closeWrite();
		System.out.println(counter +"");
	}

	public static void main(String[] args) {

		CreateSlidingMagAngleAverageDataset csmad = new CreateSlidingMagAngleAverageDataset();
	}
}