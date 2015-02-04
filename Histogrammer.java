import java.util.Date;
import java.util.Arrays;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import javax.swing.*;
import java.awt.*;
import java.awt.print.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;
import javax.swing.tree.*;
import javax.swing.text.DefaultEditorKit;
import java.io.*;
import java.util.Vector;

/**
*  Creates Histogram (returnArray) of PHA data from ClusterII CIS.
*
*  Lukas Saul   Oct., 2000
*    This object reads through the file and checks each event
*    against criteria passed in through the GUI and common block JPHA.
*
*/
public class Histogrammer extends Thread {

	private String CRLF = System.getProperty("line.separator");
	/** Our old friend, the GUI and common block
	*
	*/
	private JPHA theMain;

	private int dataProduct, currentEnergy, currentSweep, guiderNumber;
	private BufferedWriter bw = null;
	private FileInputStream fis = null;
	private DataInputStream dis = null;
	private boolean original;

	// Don't need to output the date for the Histogram - just check to see if it's in the intervals
	private static double ms_1958 = 3.786732*Math.pow(10,11);

	private String outputFileName, hist, fileName;
	private Date currentDate;
	private JLabel statusLabel, statusLabel2;
	private boolean orignal; // for product 3 list option
	private float filesLength, readLength;

	private File[] allFiles;
	private int[] energies, angles, tofs, sweeps, species; // the fields to accept
	private boolean anyEnergies, anyAngles, anyTofs, anySweeps, anySpecies; // to speed us up
	private long[] returnArray;  // the number of counts required
	private LookupTable lt;

	// variables for doing time histogram
	private boolean timeHist = false;
	/** Unforunately, this guy needs to be filled with objects.
	* Could speed us up if we could fill it with int...
	*/
	private Vector timeVector;
	private Date lastDate = null;
	private int currentIndex;
	private long millisecondsToAverage = 0;

	/**  Main constructor - requires passing in pointer to the GUI
	*          that's where all the histogram parameters are stored
 	*/
	public Histogrammer(JPHA parent) {

		// get variables from the pointer "parent"
		theMain=parent;
		allFiles = theMain.allFiles;
		outputFileName = theMain.outputFileName;
		statusLabel = theMain.statusLabel;
		statusLabel2 = theMain.statusLabel2;
		hist = (String)theMain.histogramComboBox.getSelectedItem();
		tofs = theMain.tofs;;
		energies = theMain.energies;
		angles = theMain.starts;
		sweeps = theMain.sweeps;
		species = theMain.species;

		// set up if which criteria to check
		anyEnergies = (energies.length==128);
		anyAngles = (angles.length==8);
		anySweeps = (sweeps.length==32);
		anySpecies = (species.length==5);

		lt = theMain.lt;
		filesLength = 0; readLength = 0;

		// set up return array...
		if (hist.equals("TOF")) {
			returnArray = new long[256];
		}
		else if (hist.equals("Energy")) {
			returnArray = new long[128];
		}
		else if (hist.equals("Angle")) {
			returnArray = new long[8];
		}
		else if (hist.equals("Sweep")) {
			returnArray = new long[32];
		}
		else if (hist.equals("Time")) {
			timeHist = true;
			returnArray = new long[0]; // just for now
			timeVector = new Vector();
			millisecondsToAverage = (long) (1000 * theMain.timeAverage);
		}
		else statusLabel.setText("Error reading histogram options");

		Arrays.fill(returnArray,0); // all channels start at 0
	}


	/**  This is the thread - does all file reading and creates return array...
	*   after it has been properly constructed,
	*   this routine writes output file and
	*   creates sample plot.
	*
	*/
	public void run() {

		dataProduct = 0;
		guiderNumber = -4; // initialize
		fileName = allFiles[0].getName();
		original = false;

		if (fileName.indexOf("PROM_3xxxx") != -1) {
			dataProduct = 3;
		}

		if (fileName.indexOf("EVE_28xxxx") != -1) {
			dataProduct = 28;
		}

		if (dataProduct != 0) {

			// set up status of how much is left to do
			filesLength = 0;
			for (int i=0; i<allFiles.length; i++) {
				filesLength += allFiles[i].length();
			}
			readLength = 0; // we haven't read anything yet!


			System.out.println("starting... product #" + dataProduct);
			printASCIIHeader();
			getHistogram(); // fills the variable "returnArray"
			if(timeHist) setupReturnArrayFromVector();
			printASCII(); // prints to the output file with "returnArray"
			plot(); // generates the plot
			try { bw.close(); }
			catch (Exception e) {e.printStackTrace();}
		}

		if (dataProduct == 0) {
			statusLabel.setText("Incorrect File Format");
		}
	}

	// Print the header of the histogram output file
	private void printASCIIHeader() {
		System.out.println("Writing histogram header to file...");
		try {
			bw = new BufferedWriter(new FileWriter(outputFileName,false));

			bw.write("JPHA Histogram Output" + CRLF);
			bw.write(hist + "\t" + "counts" + CRLF);
		}
		catch (Exception e) { e.printStackTrace(); }
	}

	// Dump the contents of our return array
	private void printASCII() {
		for (int i=0; i<returnArray.length; i++) {
			try {
				bw.write(i+"\t"+returnArray[i]+CRLF); // tab separated for spreadsheets
			}
			catch (Exception e) {e.printStackTrace();}
		}
	}


	public void getHistogram() {
		String CRLF = System.getProperty("line.separator");
		System.out.println("Preparing histogram...");

		for (int ii=0; ii<allFiles.length; ii++) {
			statusLabel.setText("Reading file " + (1+ii) );
			try {
				fis = new FileInputStream(allFiles[ii]);
				dis = new DataInputStream(fis);
			}
			catch (Exception e) {
				System.out.println("couldn't create fileInputStream");
				e.printStackTrace();
			}

			try {
				if (dataProduct == 3) {
					// at this point we have more difficulties- we need to discen events from "tags"
					// - the tags give us the sweep# and the energy (tags have type no 3)

					currentEnergy = 128;
					currentSweep = 1;
					guiderNumber = -4;
					int trash = 0;
					while(getHeader()) { // this set's the Date...
						if (theMain.checkIntervals(currentDate)) {
							for (int i=0; i<1024; i++) {
								if (dis.readShort()==3) {  // we have a tag - don't write to file
									trash = dis.readShort(); trash = dis.readShort();
									trash = dis.readShort(); trash = dis.readShort();
									trash = dis.readShort();
									increment(dis.readShort());  // set the new sweep and energy
								}
								else { //this is an event - test if it fits criteria and increment return array
									trash = dis.readShort();  // proton mode info
									trash = dis.readShort(); trash = dis.readShort(); // 3 bytes gone
									int st = dis.readShort();
									int to = dis.readShort();
									int en = currentEnergy;
									int sw = currentSweep;
									checkEvent(st,sw,en,to);
									trash = dis.readShort(); // throw out this one- the tag for type 3
								}
							}
						}else { // we aren't interested in this date
							for (int j=0; j<1024; j++) {
								trash=dis.readShort(); trash=dis.readShort();
								trash=dis.readShort(); trash=dis.readShort();
								trash=dis.readShort(); trash=dis.readShort();
								trash=dis.readShort();
							}
						}
						readLength += 14392;
						//System.out.println("Trying to update gui thermometer");
						theMain.updateStatus((int)(readLength/filesLength*100)+ "% done");
						// about to get header again
					}

				}

				else if (dataProduct == 28) {
					int trash = 0;  // Not so much trash in this file...
					while(getHeader()) {
						if (theMain.checkIntervals(currentDate)) {
							for (int i=0; i<48; i++) {
								//THIS IS AN EVENT - test if it fits criteria and increment return array
								int sw = dis.readShort();
								int st = dis.readShort();
								int to = dis.readShort();
								trash = dis.readShort(); // Proton mode...
								int en = dis.readShort();
								checkEvent(st,sw,en,to);
							}
						}
						else { // not interested in this Date
							for (int j=0; j<48; j++) {
								trash=dis.readShort(); trash=dis.readShort();
								trash=dis.readShort(); trash=dis.readShort();
								trash=dis.readShort();
							}
						}
						readLength += 536;
						//System.out.println("update GUI thermometer "+ readLength + " " + filesLength);
						theMain.updateStatus((int)(readLength/filesLength*100)+ "% done");
					}
				}// done if data_product = 28
			} // done try loop reading file
			catch (Exception e) { System.out.println("Ran out of data!"); e.printStackTrace();}
			try { fis.close(); dis.close(); }
			catch (Exception e) { e.printStackTrace(); }
		}// done for loop for files
	}


	/**
	*    here's the header reader.  Most stuff gets thrown out - only date is saved.
	*	 returns true if it finds a header and parses it correctly
	*	 otherwise returns false
	*
	*    This routine now handles building the timeVector for time histograms also.
	*/
	public boolean getHeader() {
		int trash = 0;
		try {
			trash = dis.readShort();
			currentDate = new Date((long)(dis.readDouble()-ms_1958));
			// that's ms since 01/01/1958 00:00:00

			// now there are 46 bytes left in the header
			for (int j =0; j<23; j++) {
				trash = dis.readShort();
			}

			if (timeHist) { // we may need to keep track of averaging time here
				// for first time...
				if (lastDate==null) {
					lastDate = currentDate;
					timeVector.addElement(new Long(0));
				}

				long dif = currentDate.getTime() - lastDate.getTime();
				if (dif > millisecondsToAverage) {
					timeVector.addElement(new Long(0));
					currentIndex++;
					lastDate = currentDate;
				}
			}


			// we should be out of the header and ready for data.
			return true;
		}
		catch (Exception e) {
			// this means no more headers left - we are done
			System.out.println("No more headers!");
			return false;
		}
	}

	/** This is supposed to be as efficient as possible.
	*   Checks first if any are allowed, then goes through the array to see if
	*   the event meets criteria.
	*      Criteria for magnetic field and time are handled elsewhere (in the header)
	*/
	public void checkEvent(int st, int sw, int en, int to) {
		boolean in = true; // innocent until proven guilty
		if (returnArray.length==256) { // TOF histogram...
			if (!anySweeps && !contains(sweeps,sw)) in = false;
			else if (!anyEnergies && !contains(energies,en)) in = false;
			else if (!anyAngles && !contains(angles,st)) in = false;
			else if (!anySpecies &&	!contains(species,lt.getSpecies(en,to))) in = false;
			if (in) returnArray[to]++;
		}
		else if (returnArray.length==128) { // Energy histogram
			if (!anySweeps && !contains(sweeps,sw)) in = false;
			else if (!anyAngles && !contains(angles,st)) in = false;
			else if (!anyTofs && !contains(tofs,to)) in = false;
			else if (!anySpecies && !contains(species,lt.getSpecies(en,to))) in = false;
			if (in) returnArray[en]++;
		}
		else if (returnArray.length==8) { // Angle histogram
			if (!anySweeps && !contains(sweeps,sw)) in = false;
			else if (!anyEnergies && !contains(energies,en)) in=false;
			else if (!anyTofs && !contains(tofs,to)) in = false;
			else if (!anySpecies && !contains(species,lt.getSpecies(en,to))) in = false;
			if (in) returnArray[st]++;
		}
		else if (returnArray.length==32) { // Sweep histogram
			if (!anyAngles && !contains(angles,st)) in = false;
			else if (!anyEnergies && !contains(energies,en)) in = false;
			else if (!anyTofs && !contains(tofs,to)) in = false;
			else if (!anySpecies && !contains(species,lt.getSpecies(en,to))) in = false;
			if (in) returnArray[sw]++;
		}
		else if (returnArray.length==0) {// Time Average
			if (!anySweeps && !contains(sweeps,sw)) in = false;
			else if (!anyAngles && !contains(angles,st)) in = false;
			else if (!anyEnergies && !contains(energies,en)) in=false;
			else if (!anyTofs && !contains(tofs,to)) in = false;
			else if (!anySpecies && !contains(species,lt.getSpecies(en,to))) in = false;
			if (in) incrementCurrentTimeAverage();
		}
	}
	/**
	*  We have an event that matches criteria.  Increment the latet in timeVector
	*/
	public void incrementCurrentTimeAverage() {
		//Long currentCount = (Long)timeVector.elementAt(currentIndex);
		timeVector.setElementAt(
			new Long(((Long)timeVector.elementAt(currentIndex)).longValue()+1)
			,currentIndex);
	}

	/**
	*  Creates the returnArray of int from Vector of Integers
	*/
	public void setupReturnArrayFromVector() {
		returnArray = new long[timeVector.size()];
		for (int i=0; i<returnArray.length; i++) {
			returnArray[i] = ((Long)timeVector.elementAt(i)).longValue();
		}
	}

	/**
	*  To speed up product 3 - instead of doing the math we can increment most of the time
	*
	*
	*/
	public void increment(int newGuiderNumber) {
		if (newGuiderNumber != guiderNumber+1) {
			// here we need to reset entirely to new values
			guiderNumber = newGuiderNumber;
			processGuiderNumber();
		}

		else { // just increment.  This is usually what happens
			if (currentEnergy >= 127) {
				if (currentSweep >= 31) {
					currentSweep = 0;
					currentEnergy = 0;
				}else {
					currentEnergy = 0;
					currentSweep++;
				}
			}
			else currentEnergy ++;
			guiderNumber = newGuiderNumber;
		}
	}

	/**  This method is for product 3 - info for energy & sweep contained in last entry
	*     some math here-  avoid if possible with increment method
	*/
	public void processGuiderNumber() {
		currentEnergy = guiderNumber % 128;
		currentSweep = (guiderNumber / 128);
	}

	/** This method uses the NOAA Scientific Graphics Toolkit to generate a histogram plot
	*
	*
	*/
	private void plot() {
		// make a new dialog
		NOAAHistogramFrame nhd =
		            new NOAAHistogramFrame(returnArray, theMain.outputFileName, theMain);

		// THis is old stuff - ncsa routines that didn't work so hot
		/*data = new double[1][returnArray.length];
		for (int i=0; i<returnArray.length; i++) {
			data[1][i]=returnArray[i];
			data[0][i]=i;
		}
		XYPlot x = new XYPlot(theMain,"CIS Histogram "+hist, data);
		x.show();*/
	}


	/**
	* This method should be fast - binaray search - array must be sorted!.
	*  For seeing if an integer is in an array - t
	*/
	public boolean contains(int[] arr, int geh) {
		if (Arrays.binarySearch(arr,geh)>=0) return true;
		else return false;
	}
}
