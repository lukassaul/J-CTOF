import java.io.*;

/**
*  Create an animated gif file for a certain time period
*
*
*
*/
public class CTOFAnimatedGifCreator {

	public static final String CRLF = System.getProperty("line.separator");
    public static int GOODS = (int)(8.20368000 * Math.pow(10,8)); // 1996 - (12/30/69 19:00:00)
    /** This must be the same as the DELTAT in PHADataFixer3
	*/
	public static int DELTAT = 906;

	public static HelioPoint HPREF = new HelioPoint(HelioPoint.CARTESIAN, -1, 0, 0);
	public static float HISTOGRAM_W_START = (float)1.2;
	public static float HISTOGRAM_W_FINISH = (float)4.0;

	private TimeInterval currentTI; // for doing time profile

	private FluxHistogram vvHist[];
	//private boolean haveEnergies; // no need to recompute these!

	// the data readers are here:
	private SwReader sr; // read binary solar wind data
	private FluxReader fr; // read PHAdata
	private FluxData fd;

	// load these arrays to save time with loadEnergies()
	private double[] vMin = new double[62];
	private double[] vMax = new double[62];
	private double[] energies = new double[62];

	private file outputFile;
	private HelioPoint hp; // for unit converstions

	private int secondsToAverage, date, lastDate; // lastDate is for timeProfile
	private float vsw; // current
	private int totalBytes, counter, goodEvents, onePercent, label, eq, intervals;

	// for limiting the flux to a certain range
	private boolean doDiff;
	private float startBin, finishBin;

	/** Energy efficiency?
	*/
	private float gamma;
	private NOAAHistogramFrame nhf;

	/** Use this from here to make a sliding average histogram
	*/
	public static boolean doMakeSlidingAverage = false;
	private file slideFile = new file("slidingPha.dat");
	private String histType;

	/** Well we only need the constructor to get our data, mainly the CTOFGui
	*/
	public CTOFAnimatedGifCreator() {


		//doMakeSlidingAverage = false;
		secondsToAverage = 0;
		date = 0;
		gamma = 0;
		tailStart = 2;
		startBin=0;
		finishBin=0;
		lastDate = 0; // with mary jane
		//haveEnergies = false;
		hp = new HelioPoint();

		outputFile = new file("gifOutput.gif");
		outputFile.clear();
		counter = 0;
		goodEvents = 0;
		label = 0;

		File f = new File("phadataJ.bin");
		totalBytes = ((int)f.length());
		System.out.println(f.length() + " " + totalBytes);
		onePercent = totalBytes/100 + 1;
		System.out.println("one percent" + onePercent);

		loadEnergies();

		gamma = 0;
	}

	/**
	* This is to display the next histogram in a sequence
	*
	*/
	public void doNextHistogram() {
		int numPerHist = 0;
		try {
			numPerHist = Integer.parseInt(theMain.intervalsField.getText());
		}
		catch (Exception e) {}
		setupEnergyHistogram();
		for (int i=0; i<numPerHist; i++)  {
			loadNextEvent();
			addFluxesToVVsw(fd);
		}
		theMain.timeStartField.setText(getQuantity(fd,301)+"");
		String outputString = "";
		outputString += theMain.getParamString();
		outputString += "Good Events: " + numPerHist + CRLF;

		outputString += "V/Vsw histogram data: " + CRLF;
		for (int i=0; i<vvHist.data.length; i++) {
			outputString += vvHist.label[i] + "\t" + vvHist.data[i] + "\t"
				+ vvHist.counts[i] + CRLF ;
		}
		outputFile.saveShit(outputString);
		nhf.changeData(vvHist.label,
						vvHist.data);
		nhf.show();
	}


	/**
	*  This thread will run an energy histogram or time profile
	* (or one of the scatter plots as checked in CTOFGui)
	*/
	public void run() {
		System.out.println("Loading required data files...");
		u("creating readers..."); // when necessary...
		sr = new SwReader();
		br = new BReader();
		fr = new FluxReader();
		bfr = new BFloatReader(theMain.floatingAverageFileField.getText());


		// CHECK PARAMETERS FROM GUI...
		if (theMain.shockCheckBox.isSelected() | theMain.allShockCheckBox.isSelected()) {
			o("going to go for shocks");
			doShocks = true;
			shr = new ShockReader();
		}
		else o("not checking shocks");

		try {
			System.out.println("Checking for time interval...");
			int d1 = (int)( 24*60*60*Float.parseFloat(theMain.timeStartField.getText()) )+ GOODS;
			int d2 = (int)( 24*60*60*Float.parseFloat(theMain.timeEndField.getText()) )+ GOODS;
			System.out.println("setting up Time Interval " + d1 + " " + d2);
			ti = new TimeInterval(d1, d2);
			doTI = true;
			theMain.doTI = true;
		}catch (Exception e) {
			o("no time interval - using all dates");
		}


		if (theMain.bCheckBox.isSelected())	{
			doMag = true;
			//br = new BReader();
			getBInterval();
		}
		else o("no mag checking this 1time");

		if (theMain.swCheckBox.isSelected()) {
			o("going to go for wind data");
			doSW = true;
			// we already need the reader for vsw!!
			//sr = new SwReader();
			getSWInterval();
		}
		else o("No sw checking this time");

		if (theMain.energyHistogramCheckBox.isSelected()) {
			o("we need to make energy histogram...");
			doVVHist = true;
			setupEnergyHistogram();

			histType = (String)theMain.histTypeComboBox.getSelectedItem();

		}
		else o("no energy hist this time");

		if (theMain.timeProfileCheckBox.isSelected()) {
			o("we need a time profile...");
			doTP = true;
			setupTimeProfile();
		}
		else o("no time profile this time");

		if (theMain.doScatCheckBox.isSelected()) {
			o("We are making a scatter plot of some sort...");
			doScatter = true;
			scatter = new Scatter("A Scatter Plot");
			try {
				xunit = Integer.parseInt(theMain.scatXField.getText());
				yunit = Integer.parseInt(theMain.scatYField.getText());
				//if (theMain.histScatCheckBox.isSelected()) {
				//	numScatBins = Integer.parseInt(theMain.scatBinsField.getText());
				//    //scatter.doHistogram(numScatBins);
                //}
			}
			catch (Exception e) {}
		}

		if (theMain.correlationCheckBox.isSelected()) {
			try {
				int del = Integer.parseInt(theMain.corrDeltaField.getText());
				int num = Integer.parseInt(theMain.corrNumField.getText());
				xunitCorr = Integer.parseInt(theMain.corrXField.getText());
				yunitCorr = Integer.parseInt(theMain.corrYField.getText());
				o("We are doing correlation......");
				doCorrelation = true;
				correlation = new Correlation(del,num);
			}
			catch (Exception e) {}
		}

		try {
			startBin = Float.parseFloat(theMain.binStartField.getText());
			finishBin = Float.parseFloat(theMain.binFinishField.getText());
			doDiff = true;
			o("We are selectin a flux interval in w: " + startBin + " " + finishBin);
		}
		catch (Exception e) {}

		o("Starting to read PHA Helium data...");
		u("Starting to read PHA Helium data...");

		// This is the heart of the program
		//******************************************************************
		while (loadNextEvent() && !stop) {
			intervals++;
			if (checkEvent()) { // if it ain't good, we don't want it!
				// we have a good event, now send it along for processing...
				//if (doTP) {
					//addFluxesToTime(fd);
					//timeHist.addEvent((float)(date-GOODS)/24/60/60);
				//}
				if (doVVHist) {
					addFluxesToVVsw(fd);
					//vvHist.addEvent(vvMin, vvMax);
				}
				if (doScatter) {
					float xQ = getQuantity(fd,xunit);
                    float yQ = getQuantity(fd,yunit);
                    if ((xQ!= 0) & (xQ!=-1) & (yQ!=-1)) {
                        scatter.addPoint(xQ,yQ);
                    }
				}
				if (doCorrelation) {
					float xC = getQuantity(fd,xunitCorr);
					float yC = getQuantity(fd,yunitCorr);
					if ((xC!= 0) & (xC!=-1) & (yC!=-1) & (yC != 0)) {
						correlation.addPoint(xC,yC,fd.date/1000);
                    }
				}
			}
		}
		//******************************************************************

		// we need to output!!
		System.out.println("Done processing " + intervals + " intervals");
		u("Finalizing histograms...");
		if (timeHist!=null) timeHist.finalize();

		if (vvHist!=null) vvHist.finalize();

		u("Finished reading, now writing output... ");
		// that was easY!!

	    // to make this faster we don't want to have a long String!
	    outputFile.initWrite(false);
		//String outputString = "";
		//outputString += theMain.getParamString();
		//outputString += "Good Events: " + goodEvents + CRLF;
		outputFile.write(theMain.getParamString() + CRLF);
		outputFile.write("Good Events: " + goodEvents + CRLF);
		if (doMag) o("bInterval stats.  total tries: " + bi.numberOfTries + " total hits: " + bi.numberOfHits);
		if (doSW) o("swInterval stats.  total tries: " + si.numberOfTries + " total hits: " + si.numberOfHits);
		if (doShocks) o("shockReader stats.  total tries: " + shr.numberOfTries + " total hits: " + shr.numberOfHits);
		// put other data here perhaps?
		//outputString += "Total Events : " + totalBytes + CRLF ;

		if (doTP) {
			//outputString += "Time histogram data: " + CRLF;
			outputFile.write("Time histogram data: " + CRLF);
			for (int i=0; i<timeHist.data.length; i++) {
				//outputString += (timeHist.label[i]) + "\t" + timeHist.data[i] + CRLF;
				outputFile.write( (timeHist.label[i]) + "\t" + timeHist.data[i] + CRLF);
			}
			//plot(timeHist.getArray());
		}
		if (doVVHist) {
			//outputString += "V/Vsw histogram data: " + CRLF;
			outputFile.write("V/Vsw histogram data: " + CRLF);
			//outputString += theMain.getParamString();
			// done labels, now save the data:
			for (int i=0; i<vvHist.data.length; i++) {
				//outputString += vvHist.label[i] + "\t" + vvHist.data[i] + "\t"
				//	+ vvHist.counts[i] + CRLF ;
				if (vvHist.counts[i]!=0) outputFile.write(vvHist.label[i] + "\t" + vvHist.data[i] + "\t"
					+ Math.sqrt(vvHist.counts[i])/vvHist.counts[i]*vvHist.data[i] + CRLF);

				else outputFile.write(vvHist.label[i] + "\t" + vvHist.data[i] + "\t"
					+ 0+"" + CRLF);
			}

			///plot(vvHist.getArray());
		}
		if (doScatter) {
			u("Finalizing scatter plot...");
			scatter.finalize();
			//outputString += "Scatter points for " + xunit + " vs " + yunit + CRLF;
			//outputString += UNITS_DESCRIPTION+ CRLF;
			outputFile.write("Scatter points for " + xunit + " vs " + yunit + CRLF);
			outputFile.write(UNITS_DESCRIPTION+ CRLF);
			for (int i=0; i<scatter.x.length; i++) {
				//outputString += scatter.x[i] + "\t" + scatter.y[i] + CRLF;
				outputFile.write(scatter.x[i] + "\t" + scatter.y[i] + CRLF);
			}
		}
		if (doCorrelation) {
			u("Doing correlation integral... ");
			correlation.finalize();
			//outputString += "Corr. points for " + xunitCorr + " vs " + yunitCorr + CRLF;
			//outputString += UNITS_DESCRIPTION+ CRLF;
			outputFile.write("Corr. points for " + xunitCorr + " vs " + yunitCorr + CRLF);
			outputFile.write( UNITS_DESCRIPTION+ CRLF);
			for (int i=0; i<correlation.xOut.length; i++) {
				//outputString += correlation.xOut[i] + "\t" + correlation.yOut[i] + CRLF;
				outputFile.write(correlation.xOut[i] + "\t" + correlation.yOut[i] + CRLF);
			}
		}


		System.out.println("starting to save data file...");
		// OK - Save the output file!!
		//outputFile.saveShit(outputString);
		outputFile.closeWrite();
		u("done saving data file... displaying NOAA graph...");

		// Now let's make some graphs just for fun...
		// Use the SGT from NOAA to graph the results
		if (doTP) {NOAAHistogramFrame nhf = new NOAAHistogramFrame(timeHist.label,
						timeHist.data, theMain);}

		if (doVVHist) {nhf = new NOAAHistogramFrame(vvHist.label,
						vvHist.data, theMain);}

		if (doScatter) {NOAAScatterFrame nsf = new NOAAScatterFrame(scatter);}

		if (doCorrelation) {NOAACorrelationFrame ncf = new NOAACorrelationFrame(correlation);}

		u("Idle");
	}

	private boolean loadNextEvent() {

		//if (counter == onePercent) {
		//	label++;
		//	u(label + "%" + "  matches: " + goodEvents);
		//}
		try {
			//date = dis.readInt();
			//eq = dis.readShort();
			boolean gotIt=false;
			while (!gotIt) {
				fd = fr.next(); // get the fluxData from the fluxReader
				if (fd.date==0) {
					o("problems with fluxData.date = 0");
					return false;
				}
				else date=fd.date;

				vsw = sr.getV(fd.date /*, fd.date + DELTAT*/);
				if (vsw==-1) {
					o("big problems - pha data with no vsw at date: " + fd.date);
					//return false; // this had better never happen!
				}
				else {
					gotIt = true;
				}

				if (counter >= onePercent) {
					label++;
					u(label + "%" + "  matches: " + goodEvents);
					if (label == 100) gotIt = true;
					counter=0;
				}
				else counter += fd.size();
			}
			return true;
		}
		catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	private void setupEnergyHistogram() {
		float s = HISTOGRAM_W_START;
		float f = HISTOGRAM_W_FINISH;
		System.out.println("Histogram start " + s + " finish " + f);
		vvHist = new FluxHistogram(s, f, Integer.parseInt(theMain.vvBinField.getText()));
		vvHist.setParent(theMain);
	}


	/**
	* Here we check which type of hist to do
	*
	*/
    private void addFluxesToVVsw(FluxData d) {
		if (histType.equals("EFlux")) {
			for (int i=0; i<d.fluxes.length; i++) {
				double vmx = vMax[i+d.startBin];
				double vmn = vMin[i+d.startBin];
				if (gamma!=0) {
					vvHist.addFlux((float)(vmn/vsw), (float)(vmx/vsw),
						(float)(Math.pow(energies[i],1+gamma)*d.fluxes[i]), d.counts[i]);
				}
				else {
					vvHist.addFlux((float)(vmn/vsw), (float)(vmx/vsw),
						(float)(energies[i]*d.fluxes[i]), d.counts[i]);
				}
			}
		}
		else if (histType.equals("Flux"))  {
			for (int i=0; i<d.fluxes.length; i++) {
				double vmx = vMax[i+d.startBin];
				double vmn = vMin[i+d.startBin];
				if (gamma!=0) {
					vvHist.addFlux((float)(vmn/vsw), (float)(vmx/vsw),
						(float)(Math.pow(energies[i],gamma)*d.fluxes[i]), d.counts[i]);
				}
				else {
					vvHist.addFlux((float)(vmn/vsw), (float)(vmx/vsw),
						(float)(d.fluxes[i]), d.counts[i]);
				}
			}
		}
		// in s^3 / km^6
		else if (histType.equals("Distribution Function")) {
			for (int i=0; i<d.fluxes.length; i++) {
				double vmx = vMax[i+d.startBin];
				double vmn = vMin[i+d.startBin];
				double vmid = (vmx+vmn)/2;
				double c = 4.167*Math.pow(10,5);
				if (gamma!=0) {
					vvHist.addFlux((float)(vmn/vsw), (float)(vmx/vsw),
						(float)(Math.pow(energies[i],gamma)*d.fluxes[i]), d.counts[i]);
				}
				else {
					vvHist.addFlux((float)(vmn/vsw), (float)(vmx/vsw),
						(float)(d.fluxes[i]*c/vmid/vmid), d.counts[i]);
				}
			}
		}
	}


    /**
	* Load the array of (logarithmic) energies from the matrix rates calibration
	*
    *
    *  - Yuri Litvinenko,
	*/
	public void loadEnergies() {
		System.out.println("making energy array");
		for (int eq=0; eq<62; eq++) {
			double eqMin = (double)34.673 * (double)Math.pow(105, (double)(0 - eq)/(double)116);
			double eqMax = (double)34.673 * (double)Math.pow(105, (double)(1 - eq)/(double)116);
			vMax[eq] = Math.sqrt(eqMax*4.8)*100;
			vMin[eq] = Math.sqrt(eqMin*4.8)*100;
			//o(vMaxd + " " + vMind);
			//vMax[eq] = vMaxd/vsw;
			//vvswMin[eq] = vMind/vsw;
			System.out.println("Bin: " + eq + " E from "+eqMin+" to "+eqMax +" vmax/min: " + vMax[eq]+" "+vMin[eq]);
			energies[eq] = (eqMax+eqMin)/2;
		}
	}

	/**
	* This is for doing the distribution function
	*
	*/
	public static double HE_MASS = 4.167*Math.pow(10,-5);


	// debug string output
	private void o(String s) {
		System.out.println(s);
	}

	// GUI update
	private void u(String s) {
		theMain.statusLabel.setText(s);
	}
}

