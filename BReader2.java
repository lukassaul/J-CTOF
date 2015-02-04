import java.io.*;

/**
*  OK, lets use GSE coordinates to keep things simple here
*
*  If sequential calls have the same date, they had better not advance the reader..
*
*  However a higher date will ALWAYS advance the reader..
*
*  Backwards calls should make NOISY error messages
*
*  Bad time periods must be EXPLICITLY LABELED HERE!!
*
*
*  This is a better reader - l. saul - may 2003
*/
public class BReader2 {

	private DataInputStream dis;
	private int lastDate; // date
	private float currentX, currentY, currentZ; // the field strengths

	private static String CRLF = System.getProperty("line.separator");
	public static int GOODS = (int)(8.20368000 * Math.pow(10,8)); // 1996 - (12/30/69 19:00:00)
	private static float NAN = Float.NaN;

	public BReader2() {
		this ("mfi_15_.bin");
	}

	/**
	* Initialize the searcher here in the constructor
	*/
	public BReader2(String s) {
		try {
			dis = new DataInputStream(new FileInputStream(s));
		}
		catch (Exception e) {
			System.out.println("Problems loading magdata.bin");
			e.printStackTrace();
		}
		lastDate = 0;
	}

	/**
	*This is called to get a phi in deg.
	*/
	public float getX(int date) {
		advance(date);
		if (check(date)) return currentX;
		else return NAN;
	}

	/**
	*This is called to get a phi in deg.
	*/
	public float getY(int date) {
		advance(date);
		if (check(date)) return currentY;
		else return NAN;
	}

	/**
	*This is called to get a phi in deg.
	*/
	public float getZ(int date) {
		advance(date);
		if (check(date)) return currentZ;
		else return NAN;
	}

	/**
	*  Changing of the guard
	*  Read magnetic field info here...
	*/
	private void advance(int date) {
		if (date==lastDate) return;
		if (date<lastDate) System.out.println("BReader2 cruelly abused with BACKWARDS QUERY");
		try{
			while (lastDate<date) {
				lastDate = dis.readInt();
				currentX = (float)dis.readShort()/10.0f;
				currentY = (float)dis.readShort()/10.0f;
				currentZ = (float)dis.readShort()/10.0f;
			}
		}
		catch (Exception e) {
			System.out.println("Done with B data");
		}
	}

	private int t86 = GOODS+86*24*60*60;
	private int t89 = GOODS+89*24*60*60;
	private int t108 = GOODS+108*24*60*60;
	private int t111 = GOODS+111*24*60*60;
	private int t130 = GOODS+130*24*60*60;
	private int t133 = GOODS+133*24*60*60;
	private int t70 = GOODS+70*24*60*60;
	private int t240 = GOODS+240*24*60*60;

	/**
	* Here make sure the date is a valid one for the dataset..
	*  if WIND is inside magnetospher, return false
	*/
	private boolean check(int date) {
		if (date>t86 & date<t89) return false;
		if (date>t108 & date<t111) return false;
		if (date>t130 & date<t133) return false;
		if (date<t70) return false;
		if (date>t240) return false;
		return true;
	}
}
