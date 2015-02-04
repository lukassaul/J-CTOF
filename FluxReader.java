import java.io.*;

/**
* This class reads the main database file, of fluxes at dates per energy bin
*   , the file created by PHADataFixer3.java
*
*
*
* Saul 2001
*/
public class FluxReader {
	private int currentD, currentStart, currentNumBins;
	private float[] currentFluxes;
	private int[] currentCounts;
	private DataInputStream dis;
	private FluxData tbr;
	private float gamma;

	public String INPUT_FILE = "phadataWstats.bin";

	/** Initialize the searcher here in the constructor
	*  Send in the effieciency factor, E^gamma
	*  Actually, let's do the efficiency in CtofHistogrammer
	*/
	public FluxReader(String fileName/*float gamma*/) {
		INPUT_FILE = fileName;
		//logFile = new file("fluxreaderLog.txt");
		try {
			dis = new DataInputStream(new FileInputStream(INPUT_FILE));
		}
		catch (Exception e) {
			System.out.println("Problems loading binary flux data file");
			e.printStackTrace();
		}
		//loadNext();
		//lastD = currentD;
	}


	/** Use this to access flux data from Histogrammer or CTOFHistogrammer class
	*
	*/
	public FluxData next() {
		if (loadNext()) {
			tbr = new FluxData(currentD, currentStart, currentNumBins, currentFluxes, currentCounts);
			return tbr;
		}
		else return null;
	}




	/** Changing of the guard
	*/
	private boolean loadNext() {
		try{
			currentD = dis.readInt();
			//currentStart = dis.readShort();
			//currentNumBins = dis.readShort();
			currentStart = 0;
			currentNumBins = 62;
			currentFluxes = new float[62];
			currentCounts = new int[62];
			for (int i=0; i<62; i++) {
				currentFluxes[i] = dis.readFloat();
				currentCounts[i] = dis.readInt();
				//System.out.println("testing fluxes: " + currentFluxes[i]);
			}

			return true;
		}
		catch (Exception e) {
			System.out.println("Done with (or problems witH) flux data");
			e.printStackTrace();
			currentD = 0;
			return false;
		}
	}

}

