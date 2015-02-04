import java.io.*;

/** This class just reads the file for magnetic field data.
*  access with getField methods: getP(int date)
*                                getT(int date)
*                                getM(int date)
*
*  It is assumed that the data will be requested in sequential order!!
*  No smoothing or checking here.
*
*  Last modified August 2001
*/
public class BFloatReader {


	private DataInputStream dis;
	private int currentD, lastD; // date
	private float currentEta, lastEta; // theta
	private float currentM, lastM;  // magnitude
	private TimeInterval currentTI;
	private file logFile;
	private String CRLF = System.getProperty("line.separator");
	private boolean stuck = false;

	private static int extension = 1000;  // how far do we leave chunks at edges???
	public static int NO_DATA = -2;

	// this saves hundreds of thousands of events, a significant factor in statisitics.

	/** Initialize the searcher here in the constructor
	*
	*/
	public BFloatReader(String fileName) {
		logFile = new file("BreaderLog.txt");
		try {
			dis = new DataInputStream(new FileInputStream(fileName));
		}
		catch (Exception e) {
			System.out.println("Problems loading magfloatdata.bin");
			e.printStackTrace();
		}
		currentD = 0;
		loadNext();
		lastD = currentD;
	}


	/**
	* Use this to get the theta (polar angle) in degrees for a given date
	*
	*/
	public float getEta(int date) {
		// 3 options - we have it, it's too high, it's too low
		// we have it right here
		if (currentTI.check(date)) {
			if (stuck) logFile.saveShit("No longer stuck! " + date + CRLF, true);
			stuck = false;
			return currentEta;
		} // well, are we past it already?
		else if (date<currentTI.startDate) {
			// one last chance...
			if ((date+extension) >= currentTI.startDate) {
				return currentEta;
			}
			else if ((date - extension) <= lastD) {
				return lastEta;
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
					return currentEta; // it was in the next interval
				}
				else if (date<currentTI.startDate) {
					if ((date+extension) >= currentTI.startDate) {
						return currentEta;
					}
					else if ((date - extension) <= lastD) {
						return lastEta;
					}
					else {
						if (!stuck) logFile.saveShit("Backwards after checking next " + date + CRLF, true);
						stuck = true;
						return -1; // backwards. we audi.
					}
				} // and if it's still higher... load the next!!
			}
		}
		System.out.println("You are no longer in our universe");
        return -1;
	}

	public float getM(int date) {
		// 3 options - we have it, it's too high, it's too low
		// we have it right here
		if (currentTI.check(date)) {
			if (stuck) logFile.saveShit("No longer stuck! " + date + CRLF, true);
			stuck = false;
			return currentM;
		} // well, are we past it already?
		else if (date<currentTI.startDate) {
			// one last chance...
			if ((date+extension) >= currentTI.startDate) {
				return currentM;
			}
			else if ((date - extension) <= lastD) {
				return lastM;
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
					return currentM; // it was in the next interval
				}
				else if (date<currentTI.startDate) {
					if ((date+extension) >= currentTI.startDate) {
						return currentM;
					}
					else if ((date - extension) <= lastD) {
						return lastM;
					}
					else {
						if (!stuck) logFile.saveShit("Backwards after checking next " + date + CRLF, true);
						stuck = true;
						return -1; // backwards. we audi.
					}
				} // and if it's still higher... load the next!!
			}
		}
        System.out.println("You are no longer in our universe");
        return -1;
	}

	/** Changing of the guard
	*  Read magnetic field info here...
	*/
	private boolean loadNext() {
		try{
			lastD = currentD; // -1 pointer exception
			currentD = dis.readInt();
			lastM = currentM;
			currentM = ((float)dis.readShort())/10;
			lastEta = currentEta;
			currentEta = ((float)dis.readFloat());
			//System.out.println("test breader: " + currentD + " " + currentT + " "+currentP+
			//		" " + currentM);
			// made you look
			currentTI = new TimeInterval(currentD, currentD + 350);
			return true;
		}
		catch (Exception e) {
			System.out.println("Done with B data");
			return false;
		}
	}
}
