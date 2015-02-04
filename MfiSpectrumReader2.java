//import gsfc.nssdc.cdf.*;
import java.util.*;
import java.io.*;

/**
*  We want to make files of power spectrum data
*
*   format: int tenths of seconds since 1996, float P_div, P_curl
*            with 65 coefficients for the powers P_* at frequencies
*
*            compute freqeuncies with Spectrum.getPositiveFrequencies
*  Lukas Saul, Feb 2003
*
*  We need to remove spectra from WIND MAGNETOSPHERE time periods..
*   we'll let CTOFHistogrammer handle that for us!  may2003
*
*/
public class MfiSpectrumReader2 {

	DataInputStream disD, disC;
	private int currentDate;
	private float currentFDate;
	private float[] currentSpecD, currentSpecC;
	boolean loaded = false;

	/**
	* Use this to open the streams for reading - uses two files
	*   if you must rename them you must recompile this class
	*/
	public MfiSpectrumReader2() {
		this("mfidata3SPEC_Z.bin","mfidata3SPEC_TRACE.bin");
	}

	public MfiSpectrumReader2(String n1, String n2) {
		currentSpecD = new float[65];
		currentSpecC = new float[65];
		try {
			disD = new DataInputStream(new FileInputStream(n1));
			disC = new DataInputStream(new FileInputStream(n2));
			System.out.println("opened streams: " + n1+" "+n2);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	*  Use this to get a spectrum from the binary file
	*   the "type" int tells which component - 0=div, 1=curl
	*/
	public float[] getSpectrum(int type, float startDate) {
		try {
			while (currentFDate < startDate) {
				advance(disD,currentSpecD);
				advance(disC,currentSpecC);
			}

			// OK we should be on the right place...
			if (type==0) return currentSpecD;
			else return currentSpecC;
		}
		catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	* Use this after getNExtSpectrum (if you go that route)
	*/
	public float getCurrentDate() {
		return currentFDate;
	}

	public int getCurrentIntDate() {
		return currentDate;
	}

	/**
	* For reading sequential spectrums.  Use getCurrentDate first to know where you are
	*  and/or use getSpectrum to set start date
	*/
	public float[][] getNextSpectrum() throws Exception {
		try {
			advance(disD,currentSpecD);
			advance(disC,currentSpecC);
		}
		catch (Exception e) {
			throw e;
		}
		float[][] tbr = new float[2][currentSpecD.length];
		for (int i=0; i<currentSpecD.length; i++) {
			tbr[0][i]=currentSpecD[i];
		 	tbr[1][i]=currentSpecC[i];
		}
		return tbr;
	}

	private void advance(DataInputStream dis, float[] currentSpec) throws Exception {
		//System.out.println("did an advanceDate() call in MfiSpectrumReader2.java");
		try {
			currentDate = dis.readInt(); // get the date out of there in the meantime
			currentFDate = (float)currentDate/60.0f/60.0f/24.0f/10.0f;
			for (int i=0; i<65; i++) currentSpec[i] = dis.readFloat();
		}
		catch (Exception e) {
			throw e;
		}
	}

	public static final void main(String[] args) {
		double[] freqs = Spectrum.getPositiveFrequencies(3.0f,65);
		for (int i=0; i<freqs.length; i++) {
			o(i+ " - " + freqs[i]);
		}
	}

	private static void o(String s) {
		System.out.println(s);
	}
}

