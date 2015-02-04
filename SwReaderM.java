import java.io.*;

/**
* This SwReader uses large arrays of type short.
*
* Creates them with Vectors, then creates the arrays
* for reading (faster access).
*
* Use SwReader.java (the original) for machines w/out much Memory
*
*/
public class SwReaderM {
	/**
	* These arrays will have one value for each DELTAT.
	*
	*/
	public short[] vsw; // km/s
	public short[] density; // #/cm^3 * 10
	public short[] vth; // also *10

	private DataInputStream dis;
	private int currentD, lastD;
	private int currentV, lastV;
	private float currentN, lastN;
	private float currentVth, lastVth;
	private TimeInterval currentTI;
	private file logFile;
	private String CRLF = System.getProperty("line.separator");
	private boolean stuck = false;
	public static int buffer = 500; // seconds to extend a reading outside a data interval
	/** this is from our friend PHADataFixer3
	*/
	public static int DELTAT = 906;

	/** Initialize the searcher here in the constructor
	*
	*/
	public SwReaderM() {
		logFile = new file("swreaderLog.txt");
		try {
			dis = new DataInputStream(new FileInputStream("swdata.bin"));
		}
		catch (Exception e) {
			System.out.println("Problems loading swdata.bin");
			e.printStackTrace();
		}
		loadNext();
		lastD = currentD;
	}

	/** This is called to get an average Vsw for an interval
	* Again, we are expecting sequential calls here.
	*
	*/
	public int getV(int date1, int date2) {
		int dif = date2-date1;
		int steps = dif/300;
		int tbr = getV(date1);
		int num = steps;
		for (int i=0; i<steps; i++) {
			tbr += getV(date1+300*i);
		}

		// take the average
		tbr = tbr/num;
		return tbr;
	}

	/** This is called to get a Vsw in km/sec.
	*/
	public int getV(int date) {
		// 3 options - we have it, it's too high, it's too low
		// we have it right here
		if (currentTI.check(date)) {
			if (stuck) logFile.saveShit("No longer stuck! " + date + CRLF, true);
			stuck = false;
			return currentV;
		} // well, are we past it already?
		else if (date<currentTI.startDate) {
			// one last chance...
			if ((date+buffer) >= currentTI.startDate) {
				return currentV;
			}
			else if ((date - buffer) <= lastD) {
				return lastV;
			}
			else {
				if (!stuck) logFile.saveShit("Going backwards! " + date + CRLF, true);
				stuck = true;
				return -1; // if we are going backwards, throw -1
			}
		} // it's ahead!  good!  let's look ahead!
		else {
			while(loadNext()) {
				if (currentTI.check(date)) {
					if (stuck) logFile.saveShit("No longer stuck! " + date + CRLF, true);
					stuck = false;
					return currentV; // it was in the next interval
				}
				else if (date<currentTI.startDate) {
					if ((date + buffer) >= currentTI.startDate) {
						return currentV;
					}
					else if ((date - buffer) <= lastD) {
						return lastV;
					}
					else {
						if (!stuck) logFile.saveShit("Backwards after checking next " + date + CRLF, true);
						stuck = true;
						return -1; // backwards. we audi.
					}
				} // and if it's still higher... load the next!!
			}
			return -1;
		}
	}

	/** Again, we are expecting sequential calls here.
	* Average density this time...
	*/
	public float getN(int date1, int date2) {
		int dif = date2-date1;
		int steps = dif/300;
		float tbr = getN(date1);
		int num = steps;
		for (int i=0; i<steps; i++) {
			tbr += getN(date1+300*i);
		}

		// take the average
		tbr = tbr/num;
		return tbr;
	}

	public float getN(int date) {
		// 3 options - we have it, it's too high, it's too low
		// we have it right here
		if (currentTI.check(date)) {
			if (stuck) logFile.saveShit("No longer stuck! " + date + CRLF, true);
			stuck = false;
			return currentN;
		} // well, are we past it already?
		else if (date<currentTI.startDate) {
			// one last chance...
			if ((date+buffer) >= currentTI.startDate) {
				return currentN;
			}
			else if ((date - buffer) <= lastD) {
				return lastN;
			}
			else {
				if (!stuck) logFile.saveShit("Going backwards! " + date + CRLF, true);
				stuck = true;
				return -1; // if we are going backwards, throw -1
			}
		} // it's ahead!  good!  let's look ahead!
		else {
			while(loadNext()) {
				if (currentTI.check(date)) {
					if (stuck) logFile.saveShit("No longer stuck! " + date + CRLF, true);
					stuck = false;
					return currentN; // it was in the next interval
				}
				else if (date<currentTI.startDate) {
					if ((date + buffer) >= currentTI.startDate) {
						return currentN;
					}
					else if ((date - buffer) <= lastD) {
						return lastN;
					}
					else {
						if (!stuck) logFile.saveShit("Backwards after checking next " + date + CRLF, true);
						stuck = true;
						return -1; // backwards. we audi.
					}
				} // and if it's still higher... load the next!!
			}
			return -1;
		}
	}

	/** Again, we are expecting sequential calls here.
	* Average Vthermal this time...
	*/
	public float getVth(int date1, int date2) {
		int dif = date2-date1;
		int steps = dif/300;
		float tbr = getVth(date1);
		int num = steps;
		for (int i=0; i<steps; i++) {
			tbr += getVth(date1+300*i);
		}

		// take the average
		tbr = tbr/num;
		return tbr;
	}


	public float getVth(int date) {
		if (currentTI.check(date)) {
			if (stuck) logFile.saveShit("No longer stuck! " + date + CRLF, true);
			stuck = false;
			return currentVth;
		} // well, are we past it already?
		else if (date<currentTI.startDate) {
			// one last chance...
			if ((date+buffer) >= currentTI.startDate) {
				return currentVth;
			}
			else if ((date - buffer) <= lastD) {
				return lastVth;
			}
			else {
				if (!stuck) logFile.saveShit("Going backwards! " + date + CRLF, true);
				stuck = true;
				return -1; // if we are going backwards, throw -1
			}
		} // it's ahead!  good!  let's look ahead!
		else {
			while(loadNext()) {
				if (currentTI.check(date)) {
					if (stuck) logFile.saveShit("No longer stuck! " + date + CRLF, true);
					stuck = false;
					return currentVth; // it was in the next interval
				}
				else if (date<currentTI.startDate) {
					if ((date + buffer) >= currentTI.startDate) {
						return currentVth;
					}
					else if ((date - buffer) <= lastD) {
						return lastVth;
					}
					else {
						if (!stuck) logFile.saveShit("Backwards after checking next " + date + CRLF, true);
						stuck = true;
						return -1; // backwards. we audi.
					}
				} // and if it's still higher... load the next!!
			}
			return -1;
		}
	}

	/** Changing of the guard
	*/
	private boolean loadNext() {
		try{
			lastD = currentD;
			currentD = dis.readInt();
			lastV = currentV;
			currentV = dis.readShort();
			lastN = currentN;
			currentN = ((float)dis.readShort())/10;
			lastVth = currentVth;
			currentVth = ((float)dis.readShort())/10;
			// made you look
			currentTI = new TimeInterval(currentD, currentD + 350);
			return true;
		}
		catch (Exception e) {
			System.out.println("Done with sw data");
			e.printStackTrace();
			return false;
		}
	}
}