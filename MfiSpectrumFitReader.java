import gsfc.nssdc.cdf.*;
import java.util.*;
import java.io.*;

/**
*
* Use this to read spectral fits from a binary file
*
*/
public class MfiSpectrumFitReader {

	DataInputStream dis;
	private int currentDate;
	private float currentFDate;
	private float currentIntercept, currentSlope;
	boolean loaded = false;

	/**
	*   Use this to open the streams for reading - uses two files
	*   if you must rename them you must recompile this class
	*/
	public MfiSpectrumFitReader(String fileName) {
		currentIntercept = 0.0f;
		currentSlope = 0.0f;
		try {
			dis = new DataInputStream(new FileInputStream(fileName));
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	*  Use this to get a spectrum fit from the binary file
	*/
	public float[] getFit(float startDate) {
		try {
			if (!loaded) {
				loaded=true;
				advance();
			}
			while (currentFDate < startDate) {advance();}
			// OK we should be on the right place...

			float[] tbr = new float[2];
			tbr[0]=currentIntercept; tbr[1]=currentSlope;
			return tbr;
		}
		catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	*  Get the current date as floating point DOY
	*/
	public float getCurrentDate() {
		return currentFDate;
	}

	private void advance() throws Exception {
		//System.out.println("did an advanceDate() call in MfiSpectrumReader2.java");
		try {
			currentDate = dis.readInt(); // get the date out of there in the meantime
			currentFDate = (float)currentDate/60.0f/60.0f/24.0f/10.0f;
			currentIntercept = dis.readFloat();
			currentSlope = dis.readFloat();
		}
		catch (Exception e) {
			throw e;
		}
	}

	private static void o(String s) {
		System.out.println(s);
	}

	public static final void main(String[] args) {
		MfiSpectrumFitReader msfr = new MfiSpectrumFitReader("mfidata3SPEC_PARfit.bin");
		float dd = 220.3f;
		float[] fit = msfr.getFit(dd);
		o(dd+ " - " + fit[0] + " " + fit[1]);
	}
}

