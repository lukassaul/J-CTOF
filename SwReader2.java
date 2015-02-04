import java.io.*;

/**
* This one reads the solar wind data file.
*
*  Same reading format here as BReader2.java
*
*  Better than SwReader.java!!  Use this - lsaul may2003
*/
public class SwReader2 {

	private DataInputStream dis;
	private int lastDate;
	private float currentV, currentN, currentVth;
	private String filename;

	private String CRLF = System.getProperty("line.separator");

	/**
	*   Initialize the searcher here in the constructor
	*/
	public SwReader2() {
		this ("swdata.bin");
	}

	public SwReader2(String _filename) {
		filename = _filename;
		try {
			dis = new DataInputStream(new FileInputStream(filename));
		}
		catch (Exception e) {
			System.out.println("Problems loading swdata bin file...");
			e.printStackTrace();
		}
		lastDate = 0;
	}

	/**
	* This is called to get a Vsw in km/sec.
	*/
	public float getV(int date) {
		if (advance(date)) return currentV;
		else return Float.NaN;
	}

	/**
	* This is called to get Np in 1/cm^3.
	*/
	public float getN(int date) {
		if (advance(date)) return currentN;
		else return Float.NaN;
	}

	/**
	* This is called to get a Vth in km/sec.
	*/
	public float getVth(int date) {
		if (advance(date)) return currentVth;
		else return Float.NaN;
	}

	/**
	*  Changing of the guard
	*  Read magnetic field info here...
	*/
	private boolean advance(int date) {
		if (date<lastDate) {
			System.out.println("SwReader2 "+ filename + "BACKWARDS QUERY " + date);
			return false;
		}
		if (date==lastDate) {
			return true;
		}
		try {
			while (lastDate<date) {
				lastDate = dis.readInt();
				currentV = ((float)dis.readShort())/10.0f;
				currentN = ((float)dis.readShort())/10.0f;
				currentVth = ((float)dis.readShort())/10.0f;
			}
			return true;
		}
		catch (Exception e) {
			System.out.println("Done with SW data");
			return false;
		}
	}
}


