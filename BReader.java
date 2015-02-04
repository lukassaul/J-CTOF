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
*
* constructor changed feb 03
*/
public class BReader {


	private DataInputStream dis;
	private int currentD, lastD; // date
	private float currentP, lastP; // phi
	private float currentT, lastT; // theta
	private float currentM, lastM;  // magnitude
	private TimeInterval currentTI;
	private file logFile;
	private String CRLF = System.getProperty("line.separator");
	private boolean stuck = false;

	private static int extension = 1000;  // how far do we leave chunks at edges???
	// this saves hundreds of thousands of events, a significant factor in statisitics.

	public static int NO_DATA = -2;

	public BReader() {
		this ("magdata.bin");
	}

	/**
	* Initialize the searcher here in the constructor
	*/
	public BReader(String s) {
		logFile = new file("BreaderLog.txt");
		try {
			dis = new DataInputStream(new FileInputStream(s));
		}
		catch (Exception e) {
			System.out.println("Problems loading magdata.bin");
			e.printStackTrace();
		}
		currentD = 0;
		loadNext();
		lastD = currentD;
	}

	/** This is called to get a phi in deg.
	*/
	public float getP(int date) {
		//System.out.println("looking for date in breader: " + date);
		// 3 options - we have it, it's too high, it's too low
		// we have it right here
		if (currentTI.check(date)) {
			if (stuck) logFile.saveShit("No longer stuck! " + date + CRLF, true);
			stuck = false;
			return currentP;
		} // well, are we past it already?
		else if (date<currentTI.startDate) {
			// one last chance...
			if ((date+extension) >= currentTI.startDate) {
				System.out.println("BReader going back?? found it w/extension...");
				return currentP;
			}
			else if ((date - extension) <= lastD) {
				System.out.println("BReader goin back?? found it w/ext #2...");
				return lastP;
			}
			else {
				if (!stuck) logFile.saveShit("Data ends at date: " + date + CRLF, true);
				stuck = true;
				//System.out.println("BReader has been cruelly abused with backwards query");
				return -1; // if we are going backwards, throw -1
			}
		} // it's ahead!  good!  let's look ahead!
		else {
			//System.out.println("Breader going forward!!");
			while(loadNext()) {
				if (currentTI.check(date)) {
					if (stuck) logFile.saveShit("No longer stuck! " + date + CRLF, true);
					stuck = false;
					return currentP; // it was in the next interval
				}
				else if (date<currentTI.startDate) {
					if ((date+extension) >= currentTI.startDate) {
						return currentP;
					}
					else if ((date - extension) <= lastD) {
						return lastP;
					}
					else {
						if (!stuck) logFile.saveShit("Backwards after checking next " + date + CRLF, true);
						stuck = true;
						return -1; // backwards. we audi.
					}
				} // and if it's still higher... load the next!!
			}
			//System.out.println("You are no longer in our universe");
		}
        return -1;
	}

	/**
	* Use this to get the theta (polar angle) in degrees for a given date
	*
	*/
	public float getT(int date) {
		// 3 options - we have it, it's too high, it's too low
		// we have it right here
		if (currentTI.check(date)) {
			if (stuck) logFile.saveShit("No longer stuck! " + date + CRLF, true);
			stuck = false;
			return currentT;
		} // well, are we past it already?
		else if (date<currentTI.startDate) {
			// one last chance...
			if ((date+extension) >= currentTI.startDate) {
				return currentT;
			}
			else if ((date - extension) <= lastD) {
				return lastT;
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
					return currentT; // it was in the next interval
				}
				else if (date<currentTI.startDate) {
					if ((date+extension) >= currentTI.startDate) {
						return currentT;
					}
					else if ((date - extension) <= lastD) {
						return lastT;
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
			lastP = currentP;
			currentP = ((float)dis.readShort())/10;
			lastT = currentT;
			currentT = ((float)dis.readShort())/10;
			lastM = currentM;
			currentM = ((float)dis.readShort())/10;
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
