import java.io.*;
import java.util.Vector;
import gov.noaa.pmel.sgt.*;
import gov.noaa.pmel.sgt.dm.*;
import java.awt.Color;
import JSci.maths.*;

/**
*  This thread gets the goods from theMain (an instance of CTOFGui), and goes through the
*  pha data making desired output
*
*  Designed for SOHO CTOF Helium data prepared with PHADataFixer2
*
*  2000 - Lukas Saul
*
*  May 2001- changed this so it keeps an array of FluxHistograms for each energy
*  Fall 2002 - color time spectra implemented
*  Jan 2003- adding catalog functionality
*  Mar 2003 - adding energy calibration functionality
*/
public class CtofHistogrammer extends Thread {
	//  These are constants
	public static final String CRLF = System.getProperty("line.separator");
    /*
    *  This must be updated with changes to method getQuantity(FluxData fd, int unit)
    */
    public static String UNITS_DESCRIPTION =  "POLAR = 1" + CRLF +
		   "AZIMUTH = 2;"  + CRLF +
		   "MAGNITUDE = 3;"  + CRLF +
    		"ALFVEN = 4;"  + CRLF +
		  "ALFVEN_RATIO = 5;"  + CRLF +
		  "ANGLE_TO_RADIAL = 6" + CRLF +
		  "B_RADIAL_COMPONENT = 7" + CRLF +
		  "DELTA B = 8" + CRLF +
		  "AV_Mag = 9" + CRLF +
		  "AV_Eta = 10" + CRLF +
		  "SINE(ANGLE_TO_RADIAL) = 11" + CRLF +
		   "VELOCITY = 101;"  + CRLF +
			"DENSITY = 102;" + CRLF +
		   "THERMAL_VELOCITY = 103;"  + CRLF +
		   "VELOCITY_WIND = 104;" + CRLF +
		   "DENSITY_WIND = 105;" + CRLF +
		   "THERMAL_VELOCITY_WIND = 106;" + CRLF +
		   "FLUX = 201;"  + CRLF +
		   "TAIL = 202;"  + CRLF +
			"EFLUX = 203;"+ CRLF +
			"INTEGRAL_FLUX = 204;"+ CRLF +
			"CUTOFF = 205;" + CRLF +
			"ADIABATIC COOLING INDEX = 206" + CRLF +
			"Schwadron_E_Tail_Param = 207" + CRLF +
			"Tail Power Index = 208" + CRLF +
			"Cutoff fit = 209" + CRLF +
			"INTEGRAL_FLUX SMOOTHED_1 = 210" + CRLF +
			"INTEGRAL_FLUX_SMOOTHED_2 = 211" + CRLF +
			"DOY = 301;" + CRLF +
			"SPEC_DIV .1-.15 = 401" + CRLF +
			"SPEC_CURL .1-.15 = 402" + CRLF +
			"PAR_INTERCEPT @ .1hZ = 403" + CRLF +
			"PAR_INDEX = 404" + CRLF +
			"TRACE_INTERCEPT @ .1hZ = 405" + CRLF +
			"TRACE_INDEX = 406" + CRLF +
			"TRACE/PAR INTERCEPT @ .1hZ = 407" + CRLF +
			"D_VV INDEX = 408" + CRLF +
			"ELECTRON_PARAMETER = 501";

	public static int GOODS = (int)(8.20368000 * Math.pow(10,8)); // 1996 - (12/30/69 19:00:00)
    //public static int GOODS = (int)(8.20472400 * Math.pow(10,8)); // 1996 - (12/31/70 00:00:00) in secs
	/** This must be the same as the DELTAT in PHADataFixer3
	*/
	public static int DELTAT = 906;
	public static HelioPoint HPREF = new HelioPoint(HelioPoint.CARTESIAN, -1.0, 0.0, 0.0);
	public static float HISTOGRAM_W_START = (float)1.3;
	public static float HISTOGRAM_W_FINISH = (float)3.0;
	public static float NAN = Float.NaN;


	/**
	* This constant determines where to start counting flux
	*  as part of the tail, rather than part of the bulk.
	*/
	private float tailStart;

	private BInterval bi;
	private SWInterval si;
	private TimeInterval ti;
	private TimeInterval currentTI; // for doing time profile
	private boolean doMag, doSW, doShocks, doVVHist, doTP, doTI, doCorrelation, doPCorrelation;

	// keep it simple!!
	private boolean doScatter;

	// lets do it with only one scatter!!
	private Scatter scatter;
	private Correlation correlation;
	private PearsonsCorrelation pcorrelation;
	private int xunit, yunit, numScatBins, xunitCorr, yunitCorr;
	private FluxHistogram vvHist;
	//private boolean haveEnergies; // no need to recompute these!

	// the data readers are here:
	private ShockReader shr;  // read shocks
	private BReader br; // read binary magnetic field datat
	private BFloatReader bfr;
	private SwReader srSoho, srWind; // read binary solar wind data
	private FluxReader fr; // read PHAdata
	private FluxData fd;

	// load these arrays to save time with loadEnergies()
	private double[] vMin = new double[62];
	private double[] vMax = new double[62];
	private double[] energies = new double[62];

	//private DataInputStream dis; // read the PHA data here
	private file outputFile;
	private CTOFGui theMain;
	private HelioPoint hp; // for unit converstions

	private int secondsToAverage, date, lastDate; // lastDate is for timeProfile
	private float vsw; // current
	private int totalBytes, counter, goodEvents, onePercent, label, eq, intervals;
	public boolean stop; // any time, guys!

	// for limiting the flux to a certain range
	private boolean doDiff;
	private float startBin, finishBin;

	/** Energy efficiency?
	*/
	private float gamma;
	private float lastM;  // for computing dB
	private NOAAHistogramFrame nhf;

	/** Use this from here to make a sliding average histogram
	*/
	public static boolean doMakeSlidingAverage = false;
	private file slideFile = new file("slidingPha.dat");
	private String histType;
	/**
	* for time profile
	*
	*  changed jan 2003 - use only max resolution given by .bin file of computed fluxes
	*/
	//private int numSegments;
	private FluxHistogram[] tpHist;
	private Vector tpHistVector;
	private int[] tpTimes;
	private Vector tpTimesVector;
	private int tpStart, tpFinish;
	/**
	*for catalog shita
	*/
	private float catalogSize; // IN seconds!!!
	private String catalogFilePrefix;
	private boolean doCatalog;
	private Vector c_bmag, c_np, c_vsw, c_bangle, c_bx, c_date, c_vth;
	private TimeInterval c_currentTI;

	private MfiSpectrumReader2 msr2;
	private SpecFitDataInterpolator msrPar, msrTrace;
	private SpecInterval speci;
	private boolean doSpec;

	private boolean doKallenbach;

	// Shock list stuff
	private boolean removeShocks = false;
	private boolean onlyShocks = false;
	private float shockDeltaT = 0.0f;

	// ok, we'll use log base 10
	public static double fudge = Math.log(10.0);

	//electron stuff
	private float electronMinParam, electronMaxParam;
	private boolean doElectrons;
	private ElectronParameterReader epr;

	// total intesity or eflux stuff
	private boolean doIntensity;
	private boolean doTotalEFlux;
	private float intensityF;
	//private int numIntensity;
	private float maxIntensity;
	private float totalEFluxF;

	// accel region stuff
	private boolean doAccel;
	private int accelEvents;

	// do non-normalized version for fun
	private boolean dontNorm;

	// shall we compensate for nonzer v_inj when making spectra?
	private boolean doCutoffShift;

	/**
	*  Well we only need the constructor to get our data,
	*   and parameters for analysis mainly the CTOFGui
	*
	*/
	public CtofHistogrammer(CTOFGui _theMain) {
		System.out.println("Creating the CTOF Histogrammer...!!!");
		theMain = _theMain;
		u("loading ctofHistogrammer....");
		// these booleans could be done away with via direct calls to theMain
		doMag = false;
		doSW = false;
		//doShocks = false;
		//  redone
		doVVHist = false;
		doTP = false;
		doTI = false;
        doScatter = false;
        doCorrelation = false;
        doPCorrelation = false;
        doDiff = false;
        doCatalog = false;
        doKallenbach = false;
        doElectrons = false;
        doIntensity = false;
        doTotalEFlux = false;
        dontNorm=false;
        doCutoffShift=false;
        //numIntensity = 0;
		maxIntensity = 0.0f;

		//doMakeSlidingAverage = false;
		secondsToAverage = 0;
		stop = false;
		date = 0;
		gamma = 0;
		tailStart = 2;
		startBin=0;
		finishBin=0;
		lastDate = 0; // with mary jane
		//haveEnergies = false;
		hp = new HelioPoint();
		lastM = 0;
		// not much to do around here!
		/*try {
			dis = new DataInputStream(new FileInputStream(f));
		}
		catch (Exception e) {
			u("No Data!!");
			e.printStackTrace();
		}*/

		outputFile = new file(theMain.user_dir + theMain.file_sep +
				theMain.outputFileField.getText());
		outputFile.clear();
		counter = 0;
		goodEvents = 0;
		label = 0;
		histType = "EFlux";

		File f = new File("phadataJ.bin");
		totalBytes = ((int)f.length());
		System.out.println(f.length() + " " + totalBytes);
		onePercent = totalBytes/100 + 1;
		System.out.println("one percent" + onePercent);

		loadEnergies();

		try {
			gamma = Float.parseFloat(theMain.gammaField.getText());
		}
		catch (Exception e) {
			gamma = 0;
		}
		if (theMain.kallenbachCheckBox.isSelected()) doKallenbach=true;
		try {
			tailStart = Float.parseFloat(theMain.tailStartField.getText());
		}
		catch (Exception e) {
			tailStart = 2;
		}
		System.out.println("Setting up spec data... ");
		try {
			String dd = theMain.spDivFileField.getText();
			String cc = theMain.spCurlFileField.getText();
			file ccf = new file(cc); file ddf = new file(dd);
			int c = ccf.readShitNumLines();
			int d = ddf.readShitNumLines();

			//if (ccf.exists() && ddf.exists()) msr2 = new MfiSpectrumReader2(dd,cc);
			//else msr2 = new MfiSpectrumReader2();

			//msrPar = new MfiSpectrumFitReader("mfidata3SPEC_PARfit.bin");
			msrPar = new SpecFitDataInterpolator(dd,d);
			//msrTrace = new MfiSpectrumFitReader("mfidata3SPEC_TRACEfit.bin");
			msrTrace = new SpecFitDataInterpolator(cc,c);

			msrPar.setConvect("convec_times.dat");
			msrTrace.setConvect("convec_times.dat");
		}
		catch (Exception e) {
			System.out.println("problems setting up spec data...");
		}
		o("setting up electron data..");
		epr = new ElectronParameterReader(theMain.electronFileField.getText());
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
		srSoho = new SwReader("swdata.bin");
		srWind = new SwReader("windswdata_15.bin");
		br = new BReader(theMain.magFileField.getText());
		fr = new FluxReader(theMain.heFileField.getText());
		bfr = new BFloatReader(theMain.floatingAverageFileField.getText());


		// READ IN PARAMETERS FROM GUI...
		//if (theMain.shockCheckBox.isSelected() | theMain.allShockCheckBox.isSelected()) {
		//	o("going to go for shocks");
		//	doShocks = true;
		//	shr = new ShockReader();
		//}
		//else o("not checking shocks");

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
			try {
				o("we need a time profile. HHHHHHHHH..");
				doTP = true;

				//float numMinutes = Float.parseFloat(theMain.numSegmentsField.getText());
				//float numSecs = numMinutes*60;
				//numSegments = (int)((float)(ti.finishDate - ti.startDate)/numSecs);
				//tpHist = new FluxHistogram[numSegments];
				//tpTimes = new int[tpHist.length];

				tpHistVector = new Vector();
				tpTimesVector = new Vector();
				histType = (String)theMain.histTypeComboBox.getSelectedItem();
			}
			catch (Exception e) {
				o("error parsing time profile fields");
				doTP = false;
			}
			o("running setupTimeProfile");
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
			o("setting up to do correlation.. ");
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

		if (theMain.pcorrelationCheckBox.isSelected()) {
			o("setting up to do pearson's correlation.. ");
			try {
				xunitCorr = Integer.parseInt(theMain.corrXField.getText());
				yunitCorr = Integer.parseInt(theMain.corrYField.getText());
				o("We are doing pearson's correlation......");
				pcorrelation = new PearsonsCorrelation();
				doPCorrelation = true;
			}
			catch (Exception e) {}
		}

		if (theMain.catalogCheckBox.isSelected()) {
			o("setting up to catalog the data..");
			try {
				// this is in days
				catalogSize = Float.parseFloat(theMain.c_numFilesField.getText());
				catalogSize=catalogSize*24*60*60;
				o("the catalog page size in seconds: " + catalogSize);
				catalogFilePrefix = theMain.c_outputFileField.getText();
				if (doTI) doCatalog = true;
				else o("no catalog can be created without date range");

				// we need to make a bunch of time profiles.
			//	float numMinutes = Float.parseFloat(theMain.numSegmentsField.getText());
			//	float numSeconds = numMinutes*60;
			//	o("numSeconds in pixel of time profile: " + numSeconds);
			//	numSegments = (int)(catalogSize/numSeconds);
				//tpHist = new FluxHistogram[numSegments];
				//tpTimes = new int[tpHist.length];

				tpHistVector = new Vector();
				tpTimesVector = new Vector();

				histType = (String)theMain.histTypeComboBox.getSelectedItem();

				setupCatalog(ti.startDate);
			}
			catch (Exception e) {
				o("couldn't set up catalog: ");
				e.printStackTrace();
			}
		}

		if (theMain.specCheckBox.isSelected()) {
			o("setting up spectral interval...");
			speci=new SpecInterval();
			doSpec = true;
			//msr2 = new MfiSpectrumReader2(); // already loaded in constructor

			try {
				int i1 = Integer.parseInt(theMain.spDivMinBinField.getText());
				int i2 = Integer.parseInt(theMain.spDivMaxBinField.getText());
				float i3 = Float.parseFloat(theMain.spDivMinField.getText());
				float i4 = Float.parseFloat(theMain.spDivMaxField.getText());
				speci.setDivInterval(i1,i2,i3,i4);
				o("set sepci.div");
			}
			catch (Exception e) {}
			try {
				int i1 = Integer.parseInt(theMain.spCurlMinBinField.getText());
				int i2 = Integer.parseInt(theMain.spCurlMaxBinField.getText());
				float i3 = Float.parseFloat(theMain.spCurlMinField.getText());
				float i4 = Float.parseFloat(theMain.spCurlMaxField.getText());
				speci.setCurlInterval(i1,i2,i3,i4);
				o("set speci.curl");
			}
			catch (Exception e) {}
			// new spec stuff here may 03
			try {
				float i1 = Float.parseFloat(theMain.parInterceptMinField.getText());
				float i2 = Float.parseFloat(theMain.parInterceptMaxField.getText());
				speci.setParInterceptInterval(i1,i2);
				o("set speci par intercept");
			}
			catch (Exception e) {}
			try {
				float i1 = Float.parseFloat(theMain.parIndexMinField.getText());
				float i2 = Float.parseFloat(theMain.parIndexMaxField.getText());
				speci.setParIndexInterval(i1,i2);
				o("set speci par intercept");
			}
			catch (Exception e) {}
			try {
				float i1 = Float.parseFloat(theMain.traceInterceptMinField.getText());
				float i2 = Float.parseFloat(theMain.traceInterceptMaxField.getText());
				speci.setTraceInterceptInterval(i1,i2);
				o("set speci par intercept");
			}
			catch (Exception e) {}
			try {
				float i1 = Float.parseFloat(theMain.traceIndexMinField.getText());
				float i2 = Float.parseFloat(theMain.traceIndexMaxField.getText());
				speci.setTraceIndexInterval(i1,i2);
				o("set speci par intercept");
			}
			catch (Exception e) {}
			try {
				float i1 = Float.parseFloat(theMain.ratioMinField.getText());
				float i2 = Float.parseFloat(theMain.ratioMaxField.getText());
				speci.setRatioInterval(i1,i2);
				o("set speci par intercept");
			}
			catch (Exception e) {}
			try {
				float i1 = Float.parseFloat(theMain.d_vvMinField.getText());
				float i2 = Float.parseFloat(theMain.d_vvMaxField.getText());
				speci.setD_vvInterval(i1,i2);
				o("set speci par intercept");
			}
			catch (Exception e) {}
			o("done setting up spec interval");
		}

		if (theMain.electronCheckBox.isSelected()) {
			doElectrons = true;
			try {
				electronMinParam = Float.parseFloat(theMain.electronMinField.getText());
				electronMaxParam = Float.parseFloat(theMain.electronMaxField.getText());
				o("loaded eMin - eMax: " + electronMinParam + " " + electronMaxParam);
			}
			catch (Exception e) {
				o("trouble parsing e- param fields");
			}
		}

		try {
			shockDeltaT = Float.parseFloat(theMain.shockTimeField.getText());
			shockDeltaT/=24;  // we want it in days...
			if (theMain.shockCheckBox.isSelected()) removeShocks = true;
			else removeShocks = false;
			if (theMain.allShockCheckBox.isSelected()) onlyShocks = true;
			else onlyShocks = false;
		}
		catch (Exception e) {
			removeShocks = false;
			onlyShocks = false;
			o("problem setting up shock checking...");
		}


		try {
			startBin = Float.parseFloat(theMain.binStartField.getText());
			finishBin = Float.parseFloat(theMain.binFinishField.getText());
			doDiff = true;
			o("We are selectin a flux interval in w: " + startBin + " " + finishBin);
		}
		catch (Exception e) {}

		if (theMain.intensityCheckBox.isSelected()) {
			doIntensity = true;
			intensityF=0.0f;
		}

		if (theMain.totalEFluxCheckBox.isSelected()) {
			doTotalEFlux = true;
			totalEFluxF=0.0f;
		}

		if (theMain.accelCheckBox.isSelected()) {
			doAccel = true;
			accelEvents = 0;
		}

		if (theMain.dontNormalizeCheckBox.isSelected()) {
			dontNorm = true;
		}

		if (theMain.removeShiftCheckBox.isSelected()) {
			doCutoffShift = true;
		}

		o("Starting to read PHA Helium data...");
		u("Starting to read PHA Helium data...");

		// This is the heart of the program
		//******************************************************************
		while (loadNextEvent() && !stop) {
			intervals++;
			if (checkEvent()) { // if it ain't good, we don't want it!
				// we have a good event, now send it along for processing...
				if (doTP && !doCatalog) {
					addFluxesToTime(fd);
					//timeHist.addEvent((float)(date-GOODS)/24/60/60);
				}
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
					if ((xC!= 0) & (xC!=-1) & (yC!=-1) & (yC != 0) &
						(xC!= Float.NaN) & (yC != Float.NaN)) {
						correlation.addPoint(xC,yC,fd.date/1000);
                    }
				}
				if (doPCorrelation) {
					float xC = getQuantity(fd,xunitCorr);
					float yC = getQuantity(fd,yunitCorr);
					if ((xC!=-1) & (yC!=-1)  &
						(xC < Float.MAX_VALUE) & (xC > -Float.MAX_VALUE) &
						(yC < Float.MAX_VALUE) & (yC > -Float.MAX_VALUE) &
						(xC!= NAN) & (yC != NAN)
						//(xC!= Double.NaN) & (yC != Double.NaN)
						) {
						pcorrelation.addPoint(xC,yC);
                    }
				}
				if (doIntensity) {
					intensityF = getQuantity(fd,204);
					if (intensityF > maxIntensity) maxIntensity = intensityF;
				}
				if (doTotalEFlux) {
					totalEFluxF += getQuantity(fd,204);
				}
				if (doCatalog) {
					addFluxesToCatalog(fd);
				}
			}
		}
		//******************************************************************

		// we need to output!!
		System.out.println("Done processing " + intervals + " intervals");
		u("Finalizing histograms...");
		//if (timeHist!=null) timeHist.finalize();

		if (vvHist!=null) vvHist.finalize();
		if (doTP) {
			o("reporting time profile..");
			o("tpHistVector.size(): " + tpHistVector.size());
			tpHist = new FluxHistogram[tpHistVector.size()];
			//if (tpHist!=null)
			for (int i=0; i<tpHist.length; i++) {
				tpHist[i] = (FluxHistogram)tpHistVector.elementAt(i);
				if (tpHist[i]!=null) tpHist[i].finalize();
			}
			tpTimes = new int[tpTimesVector.size()];
			for (int i=0; i<tpTimesVector.size(); i++) {
				tpTimes[i] = ((Integer)tpTimesVector.elementAt(i)).intValue();
			}
		}

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
		//if (doShocks) o("shockReader stats.  total tries: " + shr.numberOfTries + " total hits: " + shr.numberOfHits);
		// put other data here perhaps?
		//outputString += "Total Events : " + totalBytes + CRLF ;

		if (doTP) {

			o("trying to output the time profile data...");

			double[] x = new double[tpTimes.length];
			double[] y = new double[tpHist[0].data.length];
			double[] z = new double[x.length*y.length];

			int index = 0;
			for (int i=0; i<x.length; i++) {
				x[i]=(tpTimes[i]-GOODS)/24.0/60.0/60.0;
				for (int j=0; j<y.length; j++) {
					y[j]=tpHist[i].label[j];
					if (tpHist[i].data[j]==0) z[index]=0.0;
					else {
						z[index]=tpHist[i].data[j];
						//o("nonzero z: " + z[index]);
					}
					index++;
				}
			}
			o("x,y,z : " + x.length + " " + y.length + " " + z.length);

			o("building jcolorgraph in doTP..");
			JColorGraph jcg;
			if (theMain.monochromeCheckBox.isSelected()) jcg = new JColorGraph(x,y,z,false);
			else jcg = new JColorGraph(x,y,z);

			String unitString = "Diff. EFlux (1/cm^2/s/sr)";
			if (histType == "Flux") unitString = "Diff. Flux (1/cm^2/s/sr/keV)";
			if (histType == "Distribution Function") unitString = "Dist. Function (s^3/km^6)";
			if (histType == "Log Distribution Function") unitString = "log Dist. Function (s^3/km^6)";
			jcg.setLabels("SOHO CTOF He+","1996",unitString);

			jcg.run();
			jcg.showIt();

			// do this stuff with a scatter plot from now on
			//outputString += "Time histogram data: " + CRLF;
			//outputFile.write("Time histogram data: " + CRLF);
			//for (int i=0; i<timeHist.data.length; i++) {
			//	//outputString += (timeHist.label[i]) + "\t" + timeHist.data[i] + CRLF;
			//	outputFile.write( (timeHist.label[i]) + "\t" + timeHist.data[i] + CRLF);
			//}
			//plot(timeHist.getArray());
		}
		if (doVVHist) {
			//outputString += "V/Vsw histogram data: " + CRLF;
			//outputFile.write("Tail power index 2.2: " + vvHist.getTailPowerIndex(2.2f) +CRLF);
			//outputFile.write("Tail power index 2.0: " + vvHist.getTailPowerIndex(2.0f)+CRLF);
			//outputFile.write("Tail power index 2.3: " + vvHist.getTailPowerIndex(2.3f)+CRLF);
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

		if (doPCorrelation) {
			u("Doing correlation integral... ");
			pcorrelation.finalize();
			//outputString += "Corr. points for " + xunitCorr + " vs " + yunitCorr + CRLF;
			//outputString += UNITS_DESCRIPTION+ CRLF;
			outputFile.write("Pearson's Corr.for " + xunitCorr + " vs " + yunitCorr + CRLF);
			outputFile.write( UNITS_DESCRIPTION+ CRLF);
			outputFile.write("PEARSONS COEFFICIENT: " + CRLF + pcorrelation.answer + CRLF);
			outputFile.write(pcorrelation.error+ CRLF);
		}

		// output intensity and totalEflux stuff
		if (doIntensity) {
			//intensityF = intensityF/numIntensity;
			System.out.println("MAX INTENSITY RESULT: " + maxIntensity);
			outputFile.write("MAX INTENSITY RESULT: " + maxIntensity + CRLF);
		}
		if (doTotalEFlux) {
			System.out.println("TOTAL EFLUX RESULT: " + totalEFluxF);
			outputFile.write("TOTAL EFLUX RESULT: " + totalEFluxF + CRLF);
		}

		if (doAccel) {
			System.out.println("threw away " + accelEvents + " accel. events");
			outputFile.write("threw away " + accelEvents + " accel. events"+CRLF);
		}


		//System.out.println("starting to save data file...");
		// OK - Save the output file!!
		//outputFile.saveShit(outputString);
		outputFile.closeWrite();
		u("done saving data file... displaying NOAA graph...");

		// Now let's make some graphs just for fun...
		// Use the SGT from NOAA to graph the results
		//if (doTP) {NOAAHistogramFrame nhf = new NOAAHistogramFrame(timeHist.label,
		//				timeHist.data, theMain);}

		if (doVVHist) {nhf = new NOAAHistogramFrame(vvHist.label,
						vvHist.data, theMain);}

		if (doScatter) {NOAAScatterFrame nsf = new NOAAScatterFrame(scatter);}

		if (doCorrelation) {NOAACorrelationFrame ncf = new NOAACorrelationFrame(correlation);}

		if (doPCorrelation) {o(pcorrelation.answer+" "+pcorrelation.error+ " " +
						pcorrelation.error2);}


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

				vsw = srSoho.getV(fd.date /*, fd.date + DELTAT*/);
				if (vsw==-1) {
					// BIG PROBLEMS??
					//o("big problems - pha data with no vsw at date: " + fd.date);
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




	/**
	* This gets the magnetic field constraints from the GUI
	*/
	private void getBInterval() {
		System.out.println("setting up B criteria");
		bi = new BInterval();
		try{
			float mpmi1 = Float.parseFloat(theMain.azimuthMinField.getText());
			float mpma1 = Float.parseFloat(theMain.azimuthMaxField.getText());
			bi.addInterval(mpmi1,mpma1,BInterval.AZIMUTH);
		}
		catch (Exception e) {
			o("couldn't parse azimuth 1 fields");
		}
		try {
			float mpmi2 = Float.parseFloat(theMain.azimuthMinField2.getText());
			float mpma2 = Float.parseFloat(theMain.azimuthMaxField2.getText());
			bi.addInterval(mpmi2,mpma2,BInterval.AZIMUTH);
		}
		catch (Exception e) {
			o("couldn't parse azimuth  2 fields");
		}
		try {
			float mtmi1 = Float.parseFloat(theMain.polarMinField.getText());
			float mtma1 = Float.parseFloat(theMain.polarMaxField.getText());
			bi.addInterval(mtmi1,mtma1,BInterval.POLAR);
		}
		catch (Exception e) {
			o("couldn't parse polar 1 fields");
		}
		try {
			float mtmi2 = Float.parseFloat(theMain.polarMinField2.getText());
			float mtma2 = Float.parseFloat(theMain.polarMaxField2.getText());
			bi.addInterval(mtmi2,mtma2,BInterval.POLAR);
		}
		catch (Exception e) {
			o("couldn't parse polar 2 fields");
		}
		try {
			float mmi = Float.parseFloat(theMain.magnitudeMinField.getText());
			float mma = Float.parseFloat(theMain.magnitudeMaxField.getText());
			bi.addInterval(mmi,mma,BInterval.MAGNITUDE);
		}
		catch (Exception e) {
			o("couldn't parse B magnitude fields");
		}
		try {
			float mri = Float.parseFloat(theMain.radialMinField.getText());
			float mra = Float.parseFloat(theMain.radialMaxField.getText());
			bi.addInterval(mri,mra,BInterval.RADIAL);
		}
		catch (Exception e) {
			o("couldn't parse B radial fields");
		}
		try {
			float ami = Float.parseFloat(theMain.alfvenMinField.getText());
			float ama = Float.parseFloat(theMain.alfvenMaxField.getText());
			bi.addInterval(ami,ama,BInterval.ALFVEN);
		}
		catch (Exception e) {
			o("couldn't parse B alfven fields");
		}
		try {
			float amir = Float.parseFloat(theMain.alfvenRatioMinField.getText());
			float amar = Float.parseFloat(theMain.alfvenRatioMaxField.getText());
			bi.addInterval(amir,amar,BInterval.ALFVEN_RATIO);
		}
		catch (Exception e) {
			o("couldn't parse B alfven ratio fields");
		}
		try {
			float amir = Float.parseFloat(theMain.averageBMinField.getText());
			float amar = Float.parseFloat(theMain.averageBMaxField.getText());
			bi.addInterval(amir,amar,BInterval.AVERAGE_MAGNITUDE);
		}
		catch (Exception e) {
			o("couldn't parse B average Mag fields");
		}
		try {
			float amir = Float.parseFloat(theMain.etaMinField.getText());
			float amar = Float.parseFloat(theMain.etaMaxField.getText());
			bi.addInterval(amir,amar,BInterval.ETA);
		}
		catch (Exception e) {
			o("couldn't parse eta fields");
		}
	}

	private void getSWInterval() {
		si = new SWInterval();
		try {
			float vmi = Float.parseFloat(theMain.vswMinField.getText());
			float vma = Float.parseFloat(theMain.vswMaxField.getText());
			si.addInterval(vmi,vma,SWInterval.VELOCITY);
		}
		catch (Exception e) {
			o("couldn't parse vsw fields");
		}
		try {
			float nmi = Float.parseFloat(theMain.densityMinField.getText());
			float nma = Float.parseFloat(theMain.densityMaxField.getText());
			si.addInterval(nmi,nma,SWInterval.DENSITY);
		}
		catch (Exception e) {
			o("couldn't parse density fields");
		}
		try {
			float vtmi = Float.parseFloat(theMain.vthMinField.getText());
			float vtma = Float.parseFloat(theMain.vthMaxField.getText());
			si.addInterval(vtmi,vtma,SWInterval.THERMAL_VELOCITY);
		}
		catch (Exception e) {
			o("couldn't parse vthermal fields");
		}
		System.out.println(si.vmin+" "+si.vmax);
	}

	private void setupEnergyHistogram() {
		float s = HISTOGRAM_W_START;
		float f = HISTOGRAM_W_FINISH;
		System.out.println("Histogram start " + s + " finish " + f);
		vvHist = new FluxHistogram(s, f, Integer.parseInt(theMain.vvBinField.getText()));
		vvHist.setParent(theMain);
	}

	private void setupTimeProfile() {
		// we need to work in DOY here
		//int secsInDay = 60*60*24;
		//System.out.println(secsInDay);
		o("starting setupTimePRofile");
		try {
			tpStart = (int)( 24*60*60*Float.parseFloat(theMain.timeStartField.getText()) )+ GOODS;
			tpFinish = (int)( 24*60*60*Float.parseFloat(theMain.timeEndField.getText()) )+ GOODS;
		}
		catch (Exception e) {
			o("using all data for time profile");
			tpStart = (int)( 24*60*60*81 )+ GOODS;
			tpFinish = (int)( 24*60*60*240 )+ GOODS;
		}
		//System.out.println("Profile time start " + s + " finish " + f);
		/*float deltaT = (tpFinish-tpStart)/numSegments;
		try{
			for (int i=0; i<numSegments; i++) {
				tpHist[i] = new FluxHistogram(HISTOGRAM_W_START,HISTOGRAM_W_FINISH,
					Integer.parseInt(theMain.vvBinField.getText()));
			}
			// clearly not the fastest way to do this...
		}
		catch(Exception e) {
			o("problems in setupTimeProfile()");
		}
		for (int i=0; i<tpTimes.length; i++) {
			tpTimes[i] = (int)( tpStart + (i*deltaT));
		}*/
		o("done setting up time profile");
		//o("using events from day 80 to day 240 in time profile")
	}

	private void setupCatalog(int c_startDate) {
		//o("starting setupCatalog, w/ numsegments: " + numSegments);
		// we need to work in DOY here
		try {
			// clear all the histograms here ..
			/*for (int i=0; i<numSegments; i++) {
				tpHist[i] = new FluxHistogram(HISTOGRAM_W_START,HISTOGRAM_W_FINISH,
					Integer.parseInt(theMain.vvBinField.getText()));
			}
			// clearly not the fastest way to do this! (especially here)...

			float deltaT = catalogSize/numSegments;
			for (int i=0; i<tpTimes.length; i++) {
				tpTimes[i] = (int)(c_startDate + (i*deltaT));
			}*/
			c_currentTI = new TimeInterval(c_startDate, c_startDate+(int)(catalogSize));

			// empty the catlog vectors
			c_bmag = new Vector();
			c_np = new Vector();
			c_vsw = new Vector();
			c_bangle = new Vector();
			c_bx = new Vector();
			c_date = new Vector();
			c_vth = new Vector();

			o("done setting up catalog");
		}
		catch (Exception e) {
			o("problems in setupCatlog: ");
			e.printStackTrace();
		}
	}

	/** This is where all the checking goes down.  Run each PHA flux event through the gamut.
	*  Return false if we are not to consider this 15 second periond
	*  Check the date only here, against the other data.
	*/
	private boolean checkEvent() {
		if (doTI) {
			if (!ti.check(date)) {
				if (ti.finishDate < date) {
					System.out.println("date " + date +
							" outside TI: " + ti.finishDate + " " + ti.startDate);
					stop=true;
					u("Idle");
					o("Looks like we passed interval at " + date);
				}
				return false;
			}
		}
		if (doMag) {
			float phi = br.getP(date); if (phi == -1) return false;
			float the = br.getT(date); if (the == -1) return false;
			float mag = br.getM(date); if (mag == -1) return false;
			float den = srSoho.getN(date);
			float vel = (float)srSoho.getV(date);
			float average = bfr.getM(date);
			float eta = bfr.getEta(date);
			if (!bi.check(phi,the,mag,vel,den,average,eta)) return false;
		}
		if (doSW) {
			int vel = (int)srSoho.getV(date); if (vel == -1) return false;
			//System.out.println(sr.getV(date)+"");
			float den = srSoho.getN(date); if (den == -1) return false;
			float vth = srSoho.getVth(date); if (vth == -1) return false;
			if (!si.check(vel,den,vth)) return false;
		}
		if (doShocks) {
			if (theMain.shockCheckBox.isSelected()) {
				if (shr.getShock(date)!=-1) return false;
			}
			else if (theMain.allShockCheckBox.isSelected()) {
				if (shr.getShock(date)==-1) return false;
			}
		}
		if (doSpec) {
			float[] parfit = msrPar.getData((float)(date-GOODS)/24/60/60);
			float[] tracefit = msrTrace.getData((float)(date-GOODS)/24/60/60);
			float den = srSoho.getN(date);
			float mag = br.getM(date); if (mag == -1) return false;
			//float[] df = msr2.getSpectrum(0,(float)(date-GOODS)/24/60/60);
			//float[] cf = msr2.getSpectrum(1,(float)(date-GOODS)/24/60/60);
			//if (!speci.check(df,cf)) return false;
			try {
				if (!speci.check(parfit[0],parfit[1],tracefit[0],tracefit[1],den,mag)) return false;
			}
			catch (Exception e) {
				// null pointer?
				return false;
			}
		}
		if (removeShocks) {
			if (checkShock((float)(date-GOODS)/24/60/60, shockDeltaT)) return false;
		}

		if (onlyShocks) {
			if (!checkShock((float)(date-GOODS)/24/60/60, shockDeltaT)) return false;
		}

		if (doElectrons) {
			if (!checkSpec(date) | epr.getParameter((float)(date-GOODS)/24/60/60)>electronMaxParam |
				epr.getParameter((float)(date-GOODS)/24/60/60)<electronMinParam) return false;
		}

		if (doAccel) {
			if (checkAccel((float)(date-GOODS)/24/60/60)) return false;
		}

		goodEvents++;
		return true;
	}

	/**
	* Get'n functional
	*
	*/
    private void addFluxesToVVsw(FluxData d) {
		addFluxesToHistogram(vvHist,d);
	}

	/**
	*  Some radial velocites of interstellars at 1AU
	*   which come in "cold" at V_lism = 28km/s
 	*
	*/
	private double[] kepVelocities = {
		19276.78984,
		22454.68544,
		25689.84917,
		28890.3534,
		31992.10044,
		34948.53464,
		37922.42666,
		40421.62787,
		42701.75047,
		44733.32438,
		46491.99374,
		47957.72048,
		49114.35172,
		49949.40865,
		50453.99864,
		50622.78802,
		50453.99864,
		49949.40865,
		49114.35172,
		47957.72048,
		46491.99374,
		44733.32438,
		42701.75047,
		40421.62787,
		37922.42666,
		34948.53465,
		31992.10044,
		28890.3534,
		25689.84917,
		22454.68545 };

	/**

	* the dates for the above velocites
	*/
	private double[] kepDates = {

		65.54630935,
		71.64322208,
		77.74017361,
		83.83704748,
		89.93396021,
		96.03087288,
		102.1277856,
		108.2246983,
		114.321611,
		120.4185237,
		126.5154365,
		132.6123492,
		138.7092619,
		144.8061746,
		150.9030873,
		157.000,
		163.0969127,
		169.1938254,
		175.2907381,
		181.3876508,
		187.4845635,
		193.5814763,
		199.678389,
		205.7753017,
		211.8722144,
		217.9691271,
		224.0660398,
		230.1629525,
		236.2598652,
		242.3567779   };


	/**
	* Use this to get injection speed at DOY
	*/
	public double getInjectionSpeed(double doy) {
		//System.out.println("getting inj. speed: " + doy);
		for (int i=0; i<kepDates.length-1; i++) {
			if (kepDates[i]<doy && kepDates[i+1]>doy) {
				return (kepVelocities[i+1]+kepVelocities[i])/2;
			}
		}
		return 0.0;
	}





	/**
	* Here we check which type of hist to do
	*
	*/
	private void addFluxesToHistogram(FluxHistogram fHist, FluxData d) {
		if (dontNorm) {
			for (int i=0; i<d.fluxes.length; i++) {
				double vmx = vMax[i+d.startBin];
				double vmn = vMin[i+d.startBin];
				fHist.addFlux((float)(vmn), (float)(vmx),
						eCalib(energies[i])*(float)energies[i]*d.fluxes[i], d.counts[i]);
			}
		}
		else if (histType.equals("EFlux")) {
			for (int i=0; i<d.fluxes.length; i++) {
				double vmx = vMax[i+d.startBin];
				double vmn = vMin[i+d.startBin];
				if (doCutoffShift) {
					double factor = getInjectionSpeed((d.date-GOODS)/24/60/60)/1000;
					vmx = vmx-factor;
					vmn = vmn-factor;
				}
				fHist.addFlux((float)(vmn/vsw), (float)(vmx/vsw),
						eCalib(energies[i])*(float)energies[i]*d.fluxes[i], d.counts[i]);
			}
		}
		else if (histType.equals("Flux"))  {
			for (int i=0; i<d.fluxes.length; i++) {
				double vmx = vMax[i+d.startBin];
				double vmn = vMin[i+d.startBin];
				if (gamma!=0) {
					fHist.addFlux((float)(vmn/vsw), (float)(vmx/vsw),
						(float)(Math.pow(energies[i],gamma)*d.fluxes[i]), d.counts[i]);
				}
				else {
					fHist.addFlux((float)(vmn/vsw), (float)(vmx/vsw),
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
					fHist.addFlux((float)(vmn/vsw), (float)(vmx/vsw),
						(float)(Math.pow(energies[i],gamma)*d.fluxes[i]), d.counts[i]);
				}
				else {
					fHist.addFlux((float)(vmn/vsw), (float)(vmx/vsw),
						(float)(d.fluxes[i]*c/vmid/vmid), d.counts[i]);
				}
			}
		}else if (histType.equals("Log Distribution Function")) {
			for (int i=0; i<d.fluxes.length; i++) {
				double vmx = vMax[i+d.startBin];
				double vmn = vMin[i+d.startBin];
				double vmid = (vmx+vmn)/2;
				double c = 4.167*Math.pow(10,5);
				if (gamma!=0) {
					if (d.fluxes[i]!=0)
						fHist.addFlux((float)(vmn/vsw), (float)(vmx/vsw),
							(float)(Math.log(Math.pow(energies[i],gamma)*d.fluxes[i])), d.counts[i]);
				}
				else {
					if (d.fluxes[i]!=0)
						fHist.addFlux((float)(vmn/vsw), (float)(vmx/vsw),
							(float)(Math.log(d.fluxes[i]*c/vmid/vmid)), d.counts[i]);
				}
			}
		}
	}

	/**
	* See what bin to put it in from the date of the FluxData,
	*   then just send it to the same routine addFluxesToHistogram
	*
	*/
    private void addFluxesToTime(FluxData d) {
		//o("called addFluxesToTime()");
		// figure out which one to put it in
		/*int bin = -1;
		for (int j=0; j<numSegments-1; j++) {
			if (d.date >tpTimes[j] && d.date<tpTimes[j+1]) {
				bin = j;
				j = numSegments;
			}
		}*/
		//if (bin != -1) {
		//	addFluxesToHistogram(tpHist[bin],d);
		//}
		//else o("date of fluxData outsied time segment of time profile - " + d.date);
		if (!dontNorm) {
			FluxHistogram fh = new FluxHistogram(HISTOGRAM_W_START,HISTOGRAM_W_FINISH,
					Integer.parseInt(theMain.vvBinField.getText()));
			addFluxesToHistogram(fh,d);
			tpHistVector.add(fh);
			tpTimesVector.add(new Integer(d.date));
		}

		else {
			FluxHistogram fh = new FluxHistogram((float)vMin[vMin.length-2],(float)vMin[1],
					vMin.length-2);
			addFluxesToHistogram(fh,d);
			tpHistVector.add(fh);
			tpTimesVector.add(new Integer(d.date));
		}
	}

	/**
	* We are adding fluxes to histograms,
	* but now also keeping track of which page we are on
	*/
	private void addFluxesToCatalog(FluxData d) {

		if (!c_currentTI.check(d.date)) {
			outputCatalog();
			setupCatalog(d.date); // this clears catalog arrays and sets up for next one
		}

		// figure out which bin of our current time profile histograms one to put it in
		/*int bin = -1;
		for (int j=0; j<numSegments-1; j++) {
			if (d.date >tpTimes[j] && d.date<tpTimes[j+1]) {
				bin = j;
				j = numSegments;
			}
		}
		if (bin != -1) {
			addFluxesToHistogram(tpHist[bin],d);
		}
		else o("date of fluxData outsied time segment of time profile - " + d.date);
		*/
		FluxHistogram fh = new FluxHistogram(HISTOGRAM_W_START,HISTOGRAM_W_FINISH,
					Integer.parseInt(theMain.vvBinField.getText()));
		addFluxesToHistogram(fh,d);
		tpHistVector.add(fh);
		tpTimesVector.add(new Integer(d.date));

		// we also need to take care of some other business
		c_bmag.add(new Float(getQuantity(d,3)));
		c_np.add(new Float(getQuantity(d,102)));
		c_vsw.add(new Float(getQuantity(d,101)));
		c_bangle.add(new Float(getQuantity(d,6)));
		c_bx.add(new Float(getQuantity(d,7)));
		c_date.add(new Float((float)(d.date-GOODS)/24.0/60.0/60.0));
		c_vth.add(new Float(getQuantity(d,103)));
	}

	// some objects and primatives we need for outputCatalog only
	private double[] ca_bmag, ca_np, ca_vsw, ca_bangle, ca_bx, ca_date, ca_vth;
	private int catalogPageNumber = 1;
	private NOAAPanelPlotFrame tbr;
	private JColorGraph jcg;
	private NOAALineFrame nlf;
	private CartesianGraph cg;
	private LineAttribute la;
	private SGTData sgtdata;

	/**
	* Here create the pages of the catalog from the arrays we've made
	*
	*  Some problem here, we may need to do some manual garbage collection
	*/
	private void outputCatalog() {
		System.gc();
		o("starting outputCatlog()");
		String pageFileName = catalogFilePrefix + "_" + catalogPageNumber;
		catalogPageNumber++; // might as well do it now

		// each page will have 5 panels, 1 a color time
		tbr = new NOAAPanelPlotFrame();

		// build the arrays, to avoid Vectors
		ca_bmag = new double[c_bmag.size()];
		ca_np = new double[c_np.size()];
		ca_vsw = new double[c_vsw.size()];
		ca_bx = new double[c_bx.size()];
		ca_bangle = new double[c_bangle.size()];
		ca_date = new double[c_date.size()];
		ca_vth = new double[c_vth.size()];
		for (int i=0; i<ca_np.length; i++) {
			ca_np[i]=((Float)(c_np.elementAt(i))).doubleValue();
		}
		for (int i=0; i<ca_bmag.length; i++) {
			ca_bmag[i]=((Float)(c_bmag.elementAt(i))).doubleValue();
		}
		for (int i=0; i<ca_vsw.length; i++) {
			ca_vsw[i]=((Float)(c_vsw.elementAt(i))).doubleValue();
		}
		for (int i=0; i<ca_bangle.length; i++) {
			ca_bangle[i]=((Float)(c_bangle.elementAt(i))).doubleValue();
		}
		for (int i=0; i<ca_bx.length; i++) {
			ca_bx[i]=((Float)(c_bx.elementAt(i))).doubleValue();
		}
		for (int i=0; i<ca_date.length; i++) {
			ca_date[i]=((Float)(c_date.elementAt(i))).doubleValue();
		}
		for (int i=0; i<ca_vth.length; i++) {
			ca_vth[i]=((Float)(c_vth.elementAt(i))).doubleValue();
		}

		// finalize the histograms for the color plot while we're looping through..
		tpHist = new FluxHistogram[tpHistVector.size()];
		o("finalizing tpHist.. ");
		for (int i=0; i<tpHist.length; i++) {
			tpHist[i] = (FluxHistogram)tpHistVector.elementAt(i);
			if (tpHist[i]!=null) tpHist[i].finalize();
		}
		tpHistVector.removeAllElements();

		tpTimes = new int[tpTimesVector.size()];
		for (int i=0; i<tpTimesVector.size(); i++) {
			tpTimes[i] = ((Integer)tpTimesVector.elementAt(i)).intValue();
		}
		tpTimesVector.removeAllElements();

		o("the first date: " + ca_date[0]);
		o("casted to arrays in outputCatlog, length: "+ ca_bmag.length);

		// first add the color fluxes we've computed
		double[] x = new double[tpTimes.length];
		double[] y = new double[tpHist[0].data.length];
		double[] z = new double[x.length*y.length];
		int index = 0;
		for (int i=0; i<x.length; i++) {
			x[i]=(tpTimes[i]-GOODS)/24.0/60.0/60.0;
			for (int j=0; j<y.length; j++) {
				y[j]=tpHist[i].label[j];
				if (tpHist[i].data[j]==0) z[index]=0.0;
				else z[index]=tpHist[i].data[j];
				index++;
			}
		}
		o("making jcolorgraph in outputCatalog()");

		if (theMain.monochromeCheckBox.isSelected()) jcg = new JColorGraph(x,y,z,false);
		else jcg = new JColorGraph(x,y,z);
		String unitString = "Diff. EFlux (1/cm^2/s/sr)";
		if (histType == "Flux") unitString = "Diff. Flux (1/cm^2/s/sr/keV)";
		if (histType == "Distribution Function") unitString = "Dist. Function (s^3/km^6)";
		if (histType == "Log Distribution Function") unitString = "log Dist. Function (s^3/km^6)";
		jcg.setLabels("SOHO CTOF He+","1996",unitString);
		jcg.run();
		tbr.addPanel(jcg.rpl_,3.0f);
		jcg.setVisible(false);
		jcg.dispose();
		//try {jcg.finalize();}
		//catch (Throwable e) {e.printStackTrace();}
		System.gc();


		// let's try to get the ColorKey - it's hidden somewhere (where the handle at)
		try {
			ColorKey cky = (ColorKey)(jcg.gridKeyPane.getFirstLayer().getChild("Color Key"));
			cky.setBorderStyle(2);//ColorKey.NO_BORDER);
			tbr.addKey(cky,0.5f);
		}
		catch (Exception e) {
			o("couldn't get color key..");
			e.printStackTrace();
		}

		// OK, let's make some line plots here for y'all
		//CartesianGraph cg;
		//NOAALineFrame nlf;
		nlf = new NOAALineFrame(ca_date,ca_bmag,ca_bx,"","","Bx,|B|","nT");
		cg = (CartesianGraph)nlf.lpl.getFirstLayer().getGraph();
		cg.removeAllXAxes();
		tbr.addPanel(cg, 1.0f);

		// third thing added goes to the same graph as 2nd w/ no new axis
		try {cg = (CartesianGraph)nlf.lpl.getLayerFromDataId("Line 2").getGraph();}
		catch(Exception e){e.printStackTrace();}
		cg.removeAllXAxes();
		cg.removeAllYAxes();
		tbr.addPanel(cg, 0.0f);

		// this one is just the zero line for reference in the double line plot
		try {cg = (CartesianGraph)nlf.lpl.getLayerFromDataId("Line 3").getGraph();}
		catch(Exception e){e.printStackTrace();}
		sgtdata = nlf.lpl.getData("Line 3");
		la = new LineAttribute(LineAttribute.DASHED, Color.blue);
		cg.setData(sgtdata,la);
		cg.removeAllXAxes();
		cg.removeAllYAxes();
		tbr.addPanel(cg, 0.0f);
		nlf.dispose();
	//	nlf.finalize();

		nlf = new NOAALineFrame(ca_date,ca_bangle,"","","B ^","deg");
		cg = (CartesianGraph)nlf.lpl.getFirstLayer().getGraph();
		cg.removeAllXAxes();
		tbr.addPanel(cg, 1.0f);
		nlf.dispose();
	//	nlf.finalize();

		nlf = new NOAALineFrame(ca_date,ca_vth,"","","V_T","km/s");
		cg = (CartesianGraph)nlf.lpl.getFirstLayer().getGraph();
		sgtdata = nlf.lpl.getData("Line 1");
		la = new LineAttribute(LineAttribute.SOLID, Color.red);
		cg.setData(sgtdata,la);
		cg.removeAllXAxes();
		tbr.addPanel(cg, 1.0f);
		nlf.dispose();

		// let's make the mtof data red for the catalog
		nlf = new NOAALineFrame(ca_date,ca_np,"","","Np","cm^-3");
		cg = (CartesianGraph)nlf.lpl.getFirstLayer().getGraph();
		// we need the SGTData
		sgtdata = nlf.lpl.getData("Line 1");
		la = new LineAttribute(LineAttribute.SOLID, Color.red);
		cg.setData(sgtdata,la);
		cg.removeAllXAxes();
		tbr.addPanel(cg, 1.0f);
		nlf.dispose();

		nlf = new NOAALineFrame(ca_date,ca_vsw,"","","Vsw","km/s");
		cg = (CartesianGraph)nlf.lpl.getFirstLayer().getGraph();
		sgtdata = nlf.lpl.getData("Line 1");
		la = new LineAttribute(LineAttribute.SOLID, Color.red);
		cg.setData(sgtdata,la);
		cg.removeAllXAxes();
		tbr.addPanel(cg, 1.0f);
		nlf.dispose();

		tbr.build();

		tbr.save(pageFileName);
		//try {tbr.finalize();}
		//catch (Throwable e) {e.printStackTrace();}
		// that should also dispose tbr after saving - now recoup memory if nec.
		System.gc();
		o("done outputCatalog");
	}


    /**
	* These private methods take fluxData and analyze them
	*  for later adding or subtracting from histograms, scatter plots, etc.
	*
	*  Change info_string if you change this method
	* (see top of this file)
    *
    *  Here we do all scientific calculations with the data, i.e.
    *   alfven speeds, ratios, reading mag & sw files, etc.
    */
    private float getQuantity(FluxData d, int unit) {
        switch (unit) {
            case 1 : { // polar6)
                return br.getT(d.date);
            }
            case 2 : { // azimuth
                return br.getP(d.date);
            }
            case 3 : {// magnitude
              	//float x = br.getX(d.date);
			    //float y = br.getY(d.date);
                //float z = br.getZ(d.date);
                //return (float)Math.sqrt(x*x+y*y+z*z);
                return br.getM(d.date);
            }
            case 4 : { // alfven speed
               	float m = br.getM(d.date); if (m<=0) return 0;
        		float den = srSoho.getN(d.date); if (den<=0) return 0;
		        float va = 100*m/2/(float)Math.sqrt(1.67*Math.PI*den);
                return va;
            }
            case 5 : { // alfven speed over solar wind speed
                float m = br.getM(d.date); if (m<=0) return 0;
        		float den = srSoho.getN(d.date); if (den<=0) return 0;
		        float va = 100*m/2/(float)Math.sqrt(1.67*Math.PI*den);
                return (va/srSoho.getV(d.date));
            }
            case 6 : {  // angle to radial
            	float p = br.getP(d.date);
                float t = br.getT(d.date);
             //   if ((p==-1) | (p==0) | (t==-1) | (t==0)) return 0;
             // theta isn't really theta - it goes from -90 to 90 instead of 0 - 180
              //  if(t>0) t=Math.PI/2 - t*Math.PI/180.0;
              //  else t=Math.PI/2
                hp.setCoords(HelioPoint.SPHERICAL, 1,p*Math.PI/180.0, t*Math.PI/180.0 - Math.PI/2.0);
                double angle = (float)hp.getAngle(HPREF);

           		if (angle>Math.PI/2) {
                	angle=Math.PI/2 - (angle-Math.PI/2);
                }
               	angle = angle*180/Math.PI; // convert to degrees
                return (float)angle;
            }

            case 7 : { // Bx (radial magnitude)
            	//return br.getX(d.date);
            	float p = br.getP(d.date);
            	float t = br.getT(d.date);
            	float m = br.getM(d.date);
               // hp.setCoords(HelioPoint.SPHERICAL, m,p*Math.PI/180.0, t*Math.PI/180.0 - Math.PI/2.0);
            	//return (float)hp.getX();
            	return m*(float)Math.cos(t*Math.PI/180.0 - Math.PI/2.0)*(float)Math.cos(p*Math.PI/180.0);
			}
			case 8 : { // d|B|
				// this is a global variable we will initialize
				//float m = br.getM(d.date);
				//float dB = (float)Math.abs(m - lastM);
				//float dB = m-lastM;
				//lastM = m;
				//return dB;
			}
			case 9 : { // floating average magnitude
				return bfr.getM(d.date);
			}
			case 10 : { // floating average eta
				return bfr.getEta(d.date);
			}
			case 11 : { // sine of angle to radial

				// angle to radial
				float p = br.getP(d.date);
				float t = br.getT(d.date);

				hp.setCoords(HelioPoint.SPHERICAL, 1,p*Math.PI/180.0, t*Math.PI/180.0 - Math.PI/2.0);
				double angle = hp.getAngle(HPREF);

				if (angle>Math.PI/2) {
					angle=Math.PI/2 - (angle-Math.PI/2);
				}
				//angle = angle*180/Math.PI; // convert to degrees
                return (float)Math.sin(angle);
			}

            case 101 : { // vsw
                return srSoho.getV(d.date);
            }
            case 102 : { // n (proton density)
                return srSoho.getN(d.date);
            }
            case 103 : { // proton thermal speed
                return srSoho.getVth(d.date);
            }

            case 104 : { // vsw
            	if (checkSpec(d.date)) return srWind.getV(d.date);
            	else return NAN;
            }
            case 105 : { // n (proton density)
                if (checkSpec(d.date)) return srWind.getN(d.date);
            	else return NAN;
            }
            case 106 : { // proton thermal speed
                if (checkSpec(d.date)) return srWind.getVth(d.date);
            	else return NAN;
            }
            case 201 : { // total flux
            	// adjust fluxes with efficiency ratio, if non-zero
				float totT = 0;
				if (gamma!=0) {
	           		for (int i=0; i<d.fluxes.length; i++) {
		      		   totT += d.fluxes[i]*(float)Math.pow(energies[i],gamma);
	           		}
	                return totT;
				}
				else {
	           		for (int i=0; i<d.fluxes.length; i++) {
		      		   totT += d.fluxes[i];
	           		}
	                return totT;
				}
            }
            case 202 : { // tail
                // first figure out the first bin of tail flux
                int firstBin = 0;
                // we start from the top - that's lowest energy here...
                for (int i=d.fluxes.length-1; i>=0; i--) {
                    double vmx = vMax[i+d.startBin];
                    double vmn = vMin[i+d.startBin];
                    double vAvg = (vmx+vmn)/2;
                   	double m_vsw = srSoho.getV(d.date);
                    if (vAvg/m_vsw >= tailStart) {
                        firstBin = i;
                        i = -1; // exit loop
                    }
                }
                // now calculate the tail flux
                float totT = 0;
                if (gamma!=0) {
                	for (int i=d.startBin; i<firstBin; i++) {
                	    totT += d.fluxes[i];
                	}
				}
				else {
                	for (int i=d.startBin; i<firstBin; i++) {
                	    totT += d.fluxes[i];
                	}
				}
                return totT;
            }
            case 203 : { // eflux here
            // adjust fluxes with efficiency ratio
				float totT = 0;
				if (gamma!=0) {
			   		for (int i=0; i<d.fluxes.length; i++) {
			    		totT += d.fluxes[i]*(float)Math.pow(energies[i],gamma+1);
					}
                	return totT;
				}
				else {
					for (int i=0; i<d.fluxes.length; i++) {
						totT += d.fluxes[i]*(float)(energies[i]);
					}
                	return totT;
				}
			}
			case 204 : { // integral. eflux here
			    // we seek a range of w
                // first figure out the first bin of w
                int firstBin = 0;
                // we start from the top - that's lowest energy here...
                for (int i=d.fluxes.length-1; i>=0; i--) {
                    double vmx = vMax[i+d.startBin];
                    double vmn = vMin[i+d.startBin];
                    double vAvg = (vmx+vmn)/2;
                   	double m_vsw = srSoho.getV(d.date);
                    if (vAvg/m_vsw >= startBin) {
                        firstBin = i;
                        i = -1; // exit loop
                    }
                }

                int lastBin = 0;
				// we start from the bottom - highest energy here...
				for (int i=0; i<d.fluxes.length; i++) {
					double vmx = vMax[i+d.startBin];
					double vmn = vMin[i+d.startBin];
					double vAvg = (vmx+vmn)/2;
					double m_vsw = srSoho.getV(d.date);
					if (vAvg/m_vsw <= finishBin) {
						lastBin = i;
						i = d.fluxes.length; // exit loop
					}
                }

                // now calculate the total integral Eflux
                float totI = 0;
                if (gamma!=0) {
                	for (int i=lastBin; i<firstBin; i++) {
                	    totI += d.fluxes[i]*(float)Math.pow(energies[i],gamma+1);;
                	}
				}
				else {
                	for (int i=lastBin; i<firstBin; i++) {
                	    totI += d.fluxes[i]*energies[i];
                	}
				}

                //if (totB != 0) return totT/totB;
                //else return 0;
                return totI;
       		}
       		case 205 : { // here we want the cutoff velocity (in units of v/vsw)
				double biggestDif = 0;
				double cutoffV = 0;
				for (int i=1; i<d.fluxes.length; i++) {
					double vmx = vMax[i+d.startBin];
					double vmn = vMin[i+d.startBin];
					double vmx1 = vMax[i+d.startBin-1];
					double vmn1 = vMin[i+d.startBin-1];
					double dif = d.fluxes[i]*energies[i]-d.fluxes[i-1]*energies[i-1];
					if (dif>biggestDif) {
						biggestDif = dif;
						cutoffV = ( ((vmx+vmn)/2)+((vmx1+vmn1)/2) )/2;
					}
				}
				return (float)cutoffV/srSoho.getV(d.date);
			}
			case 206 : { // adiabatic cooling index, or ((1.8 - 2) - (1.6 - 1.8)) / (1.6 - 2)
			    // we seek a range of w
                // first figure out the first bin of w
                int firstBin = 0;
                // we start from the top - that's lowest energy here...
                for (int i=d.fluxes.length-1; i>=0; i--) {
                    double vmx = vMax[i+d.startBin];
                    double vmn = vMin[i+d.startBin];
                    double vAvg = (vmx+vmn)/2;
                   	double m_vsw = srSoho.getV(d.date);
                    if (vAvg/m_vsw >= 1.8) {
                        firstBin = i;
                        i = -1; // exit loop
                    }
                }

                int lastBin = 0;
				// we start from the bottom - highest energy here...
				for (int i=0; i<d.fluxes.length; i++) {
					double vmx = vMax[i+d.startBin];
					double vmn = vMin[i+d.startBin];
					double vAvg = (vmx+vmn)/2;
					double m_vsw = srSoho.getV(d.date);
					if (vAvg/m_vsw <= 2) {
						lastBin = i;
						i = d.fluxes.length; // exit loop
					}
                }

                // now calculate the integral Eflux from 1.8 - 2
                float totI = 0;
                if (gamma!=0) {
                	for (int i=lastBin; i<firstBin; i++) {
                	    totI += d.fluxes[i]*(float)Math.pow(energies[i],gamma+1);;
                	}
				}
				else {
                	for (int i=lastBin; i<firstBin; i++) {
                	    totI += d.fluxes[i]*energies[i];
                	}
				}

				// now repeat the procedure for 1.6 - 2
                firstBin = 0;
                // we start from the top - that's lowest energy here...
                for (int i=d.fluxes.length-1; i>=0; i--) {
                    double vmx = vMax[i+d.startBin];
                    double vmn = vMin[i+d.startBin];
                    double vAvg = (vmx+vmn)/2;
                   	double m_vsw = srSoho.getV(d.date);
                    if (vAvg/m_vsw >= 1.3) {
                        firstBin = i;
                        i = -1; // exit loop
                    }
                }

                lastBin = 0;
				// we start from the bottom - highest energy here...
				for (int i=0; i<d.fluxes.length; i++) {
					double vmx = vMax[i+d.startBin];
					double vmn = vMin[i+d.startBin];
					double vAvg = (vmx+vmn)/2;
					double m_vsw = srSoho.getV(d.date);
					if (vAvg/m_vsw <= 1.8) {
						lastBin = i;
						i = d.fluxes.length; // exit loop
					}
                }
                float totJ = 0;
                if (gamma!=0) {
                	for (int i=lastBin; i<firstBin; i++) {
                	    totJ += d.fluxes[i]*(float)Math.pow(energies[i],gamma+1);;
                	}
				}
				else {
                	for (int i=lastBin; i<firstBin; i++) {
                	    totJ += d.fluxes[i]*energies[i];
                	}
				}
				if (totJ==0) return 0;
                return (totI-totJ)/(totI+totJ);
			}
			case 207 : { // Schwadron _ E _ Tail _ parameter

				// First calculate Tail Flux
				 // first figure out the first bin of tail flux
				int firstBin = 0;
				// we start from the top - that's lowest energy here...
				for (int i=d.fluxes.length-1; i>=0; i--) {
					double vmx = vMax[i+d.startBin];
					double vmn = vMin[i+d.startBin];
					double vAvg = (vmx+vmn)/2;
					double m_vsw = srSoho.getV(d.date);
					if (vAvg/m_vsw >= tailStart) {
						firstBin = i;
						i = -1; // exit loop
					}
				}
				// now calculate the tail flux
				float totT = 0;
				if (gamma!=0) {
					for (int i=d.startBin; i<firstBin; i++) {
						totT += d.fluxes[i];
					}
				}
				else {
					for (int i=d.startBin; i<firstBin; i++) {
						totT += d.fluxes[i];
					}
				}


				// --------------------
				// We have TotT,  now calculate flux from 1.8 - 2
				// we seek a range of w
				// first figure out the first bin of w
				firstBin = 0;
				// we start from the top - that's lowest energy here...
				for (int i=d.fluxes.length-1; i>=0; i--) {
					double vmx = vMax[i+d.startBin];
					double vmn = vMin[i+d.startBin];
					double vAvg = (vmx+vmn)/2;
					double m_vsw = srSoho.getV(d.date);
					if (vAvg/m_vsw >= 1.8) {
						firstBin = i;
						i = -1; // exit loop
					}
				}

				int lastBin = 0;
				// we start from the bottom - highest energy here...
				for (int i=0; i<d.fluxes.length; i++) {
					double vmx = vMax[i+d.startBin];
					double vmn = vMin[i+d.startBin];
					double vAvg = (vmx+vmn)/2;
					double m_vsw = srSoho.getV(d.date);
					if (vAvg/m_vsw <= 2) {
						lastBin = i;
						i = d.fluxes.length; // exit loop
					}
				}

				// now calculate the INtegral Eflux in the specified range
				float totI = 0;
				if (gamma!=0) {
					for (int i=lastBin; i<firstBin; i++) {
						totI += d.fluxes[i]*(float)Math.pow(energies[i],gamma+0);
					}
				}
				else {
					for (int i=lastBin; i<firstBin; i++) {
						totI += d.fluxes[i];
					}
				}

				//return the tail parameter!
				if (totI != 0) return totT/totI;
				else return NAN;
			}

			case 208 : { // tail power index
				 // First calculate Tail Flux
				 // first figure out the first bin of tail flux
				int firstBin = d.fluxes.length-1;
				if (d.startBin!=0) o("d.startBin != 0 in case you were interested: " +d.startBin);
				double m_vsw = srSoho.getV(d.date);
				// we start from the top - that's lowest energy here...
				for (int i=d.fluxes.length-1; i>=0; i--) {
					//o("i=" + i + " d.counts[i]=" + d.counts[i]);
					double vmx = vMax[i+d.startBin];
					double vmn = vMin[i+d.startBin];
					double vAvg = (vmx+vmn)/2;
					if (vAvg/m_vsw >= tailStart) {
						firstBin = i;
						//o("first bin: " + firstBin + " v/vsw: " + vAvg/m_vsw);
						i = -1; // exit loop
					}
				}


				// we don't want to include points with too much error.  Find last good bin here:
				int lastBin = 0;
				for (int i=firstBin; i>=0; i--) {
					if (d.counts[i] <= 4 || d.fluxes[i]==0) {
						lastBin = i;
						//o("last bin: " + lastBin + "counts: " + d.counts[i]);
						i=-1; // exit loop !IMPORTANT!
					}
				}

				int numTailBins = firstBin-lastBin;
				if (numTailBins==0 || numTailBins==1) return -1;
				double[][] indexThis = new double[2][numTailBins];
				int j=0; // index for filling that array
				for (int i=firstBin; i>lastBin; i--) {
					indexThis[0][j]=(double)(energies[i+d.startBin]);
					indexThis[1][j]=Math.log((double)d.fluxes[i]);
					//o("i0: " + indexThis[0][j]);
					//o("i1: " + indexThis[1][j]);
					j++;
				}
				// do the fit now
				//DoubleVector lsf = LinearMath.leastSquaresFit(1,indexThis);
				//double intercept = lsf.getComponent(0);
				//double slope = lsf.getComponent(1);
				//o("numTailBins: " + numTailBins + "slope: " + slope + " J: " + j);
				//return (float)slope;
				return -1.0f;
			}
			case 209 : {
				double x[] = new double[d.fluxes.length];
				double y[] = new double[d.fluxes.length];
				double m_vsw = srSoho.getV(d.date);
				double yMax = 0.0;
				for (int i=0; i<x.length; i++) {
					double vmx = vMax[i+d.startBin];
					double vmn = vMin[i+d.startBin];
					x[i]=(vmx+vmn/2)/m_vsw;
					y[i]=d.fluxes[i]*energies[i];
					if (y[i]>yMax) yMax=y[i];
				}
				CurveFitter cf = new CurveFitter(x,y);
				//cf.setRestarts(200);

				double[] lims = {yMax/4 , yMax, 1.5, 3.0};
				int[] steps = {16,512};

				cf.doExhaustiveFit(CurveFitter.STEP,lims,steps);
				return (float)(cf.bestParams[1]);
			}

			case 210 : {
				float int_eflux = getQuantity(d,204); // here's the eflux..
				float theeta = getQuantity(d,6); // here's the angle in degrees..
				float ptr = getQuantity(d,405);
				float sw_np = srSoho.getN(d.date);
				float average_np = 8.836072f;
				float average_ptr = -1.58f;
				//float average_hec = 76721.0f;
				float slope = 5340f;
				float b = 26505.0f;

				float slope2 = 19551f;

				return int_eflux - (sw_np - average_np)*slope
									+ (float)(Math.cos(Math.PI/180.0f*theeta))*(ptr - average_ptr) ;
			}

			case 211 : {
				float int_eflux = getQuantity(d,204); // here's the eflux..
				float sw_np = srSoho.getN(d.date);
				float average_np = 8.836072f;
				//float average_hec = 76721.0f;
				float slope = 5340f;
				float b = 26505.0f;
				/*if (sw_np < 25 && sw_np > average_np) {
					float factor = (slope*average_np+b)/(slope*sw_np+b);
					return int_eflux*factor;
				}
				else if (sw_np > average_np) {
					float factor = (slope*average_np+b)/(slope*25.0f+b);
					return int_eflux*factor;
				}
				else return int_eflux;
				*/
				return int_eflux - (sw_np - average_np)*slope;
			}

       		case 301 : { // DOY wanted here
       			return (float)(d.date-GOODS)/24/60/60;
			}
			case 401 : { // want DIV spec from .1 to .15 hZ
				float[] shit = msr2.getSpectrum(0,(float)(d.date-GOODS)/24/60/60);

				// if spec is bad, return -1
				//for (int i=0; i<shit.length; i++) {
				//	if (shit[i] > 30000) return -1;
				//}

				// average the specs
				float tbr = 0.0f;
				int ind = 0;
				for (int i=39; i<58; i++) {
					ind++;
					tbr+=shit[i];
				}
				tbr=tbr/ind;
				if (tbr < 4000) return tbr;
				else return NAN;
			}
			case 402 : { // want CURL spec from .1 to .15 hZ
				float[] shit = msr2.getSpectrum(1,(float)(d.date-GOODS)/24/60/60);

				// if spec is bad, return -1
				//for (int i=0; i<shit.length; i++) {
				//	if (shit[i] > 30000) return -1;
				//}

				// average the specs
				float tbr = 0.0f;
				int ind = 0;
				for (int i=39; i<58; i++) {
					ind++;
					tbr+=shit[i];
				}
				tbr=tbr/ind;
				if (tbr<7000)return tbr;
				else return NAN;
			}
			case 403 : { // PAR intercept..
				float[] par = msrPar.getData((float)(d.date-GOODS)/24/60/60);
				//return par[0];
				// instead we use the fit to get spec(.1hZ):
				if (checkSpec(d.date)) return (float)(par[0]+par[1]*Math.log(0.1)/fudge);
				else return NAN;
			}
			case 404 : { // PAR index..
				float[] par = msrPar.getData((float)(d.date-GOODS)/24/60/60);
				if (checkSpec(d.date)) return par[1];
				else return NAN;
			}
			case 405 : { // TRACE intercept..
				float[] trace = msrTrace.getData((float)(d.date-GOODS)/24/60/60);
				//return trace[0];
				// instead we use the fit to get spec(.1hZ):
				if (checkSpec(d.date)) return (float)(trace[0]+trace[1]*Math.log(0.1)/fudge);
				else return NAN;
			}
			case 406 : { // TRACE index..
				float[] trace = msrTrace.getData((float)(d.date-GOODS)/24/60/60);
				if (checkSpec(d.date)) return trace[1];
				else return NAN;
			}
			case 407: { //log of TRACE / PAR ratio
				float[] par = msrPar.getData((float)(d.date-GOODS)/24/60/60);
				float[] trace = msrTrace.getData((float)(d.date-GOODS)/24/60/60);
				//return (float)(Math.exp((float)trace[0])/Math.exp((float)trace[1]));
				// instead we use the fit to get ratio atspec(.1hZ):
				if (checkSpec(d.date)) return (float)( (par[0]+par[1]*Math.log(0.1)/fudge) /
								(trace[0]+trace[1]*Math.log(0.1)/fudge) );
				else return NAN;
			}
			case 408: { // energy diffusion index
				float[] par = msrPar.getData((float)(d.date-GOODS)/24/60/60);
				float[] trace = msrTrace.getData((float)(d.date-GOODS)/24/60/60);
				double bmag = bfr.getM(d.date);
				double np = srSoho.getN(d.date);
				if (checkSpec(d.date)) {
					// ** HERE WE DEFINE ENERGY DIFFUSION INDEX ** //
					return (float)(bmag*Math.exp(trace[0])/np);
				}
				else return NAN;
			}
			case 501: { // electron impact parameter
				if (checkSpec(d.date)) return epr.getParameter((float)(d.date-GOODS)/24/60/60);
				else return NAN;
			}
		}
        return NAN;
    }

    /**
    *  Use this to modify a flux due to an energy calibration issue
    */
    public float eCalib(double energy){
		if (doKallenbach) return KallenbachEnergyCorrection.efficiency((float)energy);
		else if (gamma!=0) return (float)Math.pow(energy,gamma);
		else return 1.0f;
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

	// This stuff is to get rid of SPEC data from WIND in MAGNETOSPHERE
	private int t85 = GOODS+85*24*60*60;
	private int t86 = GOODS+86*24*60*60;
	private int t89 = GOODS+89*24*60*60;
	private int t108 = GOODS+108*24*60*60;
	private int t107 = GOODS+107*24*60*60;
	private int t111 = GOODS+111*24*60*60;
	private int t129 = GOODS+129*24*60*60;
	private int t130 = GOODS+130*24*60*60;
	private int t133 = GOODS+133*24*60*60;
	private int t70 = GOODS+70*24*60*60;
	private int t240 = GOODS+240*24*60*60;


	/**
	*  WIND public shock list, in DOY 1996
	*
	*/
	public float[] shockList = {80.64596065f,
		93.4221875f,
		94.4078125f,
		99.11185185f,
		105.4204514f,
		141.7222222f,
		168.1805556f,
		170.9444444f,
		175.5972222f,
		183.5416667f,
		210.5102546f,
		213.1944444f,
		213.4166667f};


	/**
	*  This returns true if a date in DOY is inside a shock
	*  region, defined around shocklist with float (days) deltaT
	*/
	public boolean checkShock(float date, float deltaT) {
		for (int i=0; i<shockList.length; i++) {
			if ( (date > (shockList[i]-deltaT)) & (date < (shockList[i]+deltaT)) ) return true;
		}
		return false;
	}


	/**
	*  Acceleration regions including shocks
	*     an array of type TimeInterval here
	*/
	public TimeInterval[] accelList = {
		new TimeInterval(91.7,92.0),
		new TimeInterval(93.2,93.5),
		new TimeInterval(94.1,94.45),
		new TimeInterval(95.1,95.3),
		new TimeInterval(98.9,99.7),
		new TimeInterval(100.1,100.7),
		new TimeInterval(105.3,105.7),
		new TimeInterval(105.8,105.9), // vsw discontinuity
		new TimeInterval(115.6,115.9),
		new TimeInterval(116.5,116.7),
		new TimeInterval(143.6,144.8), // missing page in catalog for this period..
		new TimeInterval(150.35,151.7),
		new TimeInterval(160.4,160.7),
		new TimeInterval(163.4,163.6),
		new TimeInterval(171.05,171.6),
		new TimeInterval(174.25,174.6),
		new TimeInterval(183.15,183.95),
		new TimeInterval(185.7,185.8), // vsw discontinuity
		new TimeInterval(197.5,198.2),
		new TimeInterval(210.25,210.8),
		new TimeInterval(212.2,212.8),
		new TimeInterval(213.1,213.5),
		new TimeInterval(221.15,221.2), // vsw discontinuity
		new TimeInterval(225.0,226.0)
	};

	/**
	* check to see if we are in an acceleration region, use array accelList above
	*/
	public boolean checkAccel(float date) {
		for (int i=0; i<accelList.length; i++) {
			if (accelList[i].check(date)) {
				accelEvents++;
				return true;
			}
		}
		return false;
	}


    /**
     * Gets index of highest value in an array.
     *
     * @param              Double array.
     * @return             Index of highest value.
     */
    public static int getMax(double[] array) {
        double max = array[0];
        int index = 0;
        for(int i = 1; i < array.length; i++) {
            if(max < array[i]) {
                max = array[i];
                index = i;
            }
        }
        return index;
    }


	/**
	* Here make sure the date is a valid one for the dataset..
	*  if WIND is inside magnetospher, return false
	*/
	private boolean checkSpec(int date) {
		if (date>t85 & date<t89) return false;
		if (date>t107 & date<t111) return false;
		if (date>t129 & date<t133) return false;
		if (date<t70) return false;
		if (date>t240) return false;
		return true;
	}

	// debug string output
	private void o(String s) {
		System.out.println(s);
	}

	// GUI update
	private void u(String s) {
		theMain.statusLabel.setText(s);
	}
}

















// Old code here:

/*



	private void addFluxesToFluxVsB(FluxData d) {
		float tot = 0;
		for (int i=0; i<d.fluxes.length; i++) {
			tot += d.fluxes[i];
		}
		// add the point to the scatter plot
		fluxBScatter.addPoint(br.getM(d.date),tot);
	}

	private void addFluxesToFluxVsVsw(FluxData d) {
		float tot = 0;
		for (int i=0; i<d.fluxes.length; i++) {
			tot += d.fluxes[i];
		}
		// add the point to the scatter plot
		fluxVswScatter.addPoint(sr.getV(d.date),tot);
	}

	private void addFluxesToFluxVsAlfven(FluxData d) {
		// first calculate Valfven
		float m = br.getM(d.date);
		float den = sr.getN(d.date);
		float va = 100*m/2/(float)Math.sqrt(1.67*Math.PI*den);
		// now calculate total flux
		float tot = 0;
		for (int i=0; i<d.fluxes.length; i++) {
			tot += d.fluxes[i];
		}
		fluxAlfvenScatter.addPoint(va,tot);
	}

	private void addFluxesToFluxVsBAngle(FluxData d) {
		float tot = 0;
		for (int i=0; i<d.fluxes.length; i++) {
			tot += d.fluxes[i];
		}

		float p = br.getP(d.date);
		float t = br.getT(d.date);
		hp.setCoords(HelioPoint.SPHERICAL, 1,p*Math.PI/180,t*Math.PI/180);
		double angle = (float)hp.getAngle(HPREF);
		if (angle>Math.PI/2) {
			angle=angle-Math.PI/2;
		}
		angle = angle*180/Math.PI; // convert to degrees
		// add the point to the scatter plot
		fluxBAngleScatter.addPoint((float)angle,tot);
	}

	private void addTailToTailVsB(FluxData d) {
		double m_vsw = sr.getV(d.date);
		if (m_vsw <= 0) return;
		// first figure out the first bin of tail flux
		int firstBin = 0;
		for (int i=d.fluxes.length-1; i>=0; i--) {
			double vmx = vMax[i+d.startBin];
			double vmn = vMin[i+d.startBin];
			double vAvg = (vmx+vmn)/2;
			if (vAvg/m_vsw >= TAIL_START) {
				firstBin = i;
				i = -1;
			}
		}
		// now calculate the total bulk flux
		float totT = 0;
		for (int i=0; i<firstBin; i++) {
			totT += d.fluxes[i];
		}
		// now calculate the tail flux
		float totB = 0;
		for (int i=firstBin; i<d.fluxes.length; i++) {
			totB += d.fluxes[i];
		}
		if (totB!=0) tailBScatter.addPoint(br.getM(d.date),totT/totB);
	}

	private void addTailToTailVsVsw(FluxData d) {
		double m_vsw = sr.getV(d.date);
		// first figure out the first bin of tail flux
		// Remember, low bin = high energy
		int firstBin = 0;
		for (int i=d.fluxes.length-1; i>=0; i--) {
			double vmx = vMax[i+d.startBin];
			double vmn = vMin[i+d.startBin];
			double vAvg = (vmx+vmn)/2;
			//System.out.println("vAvg = " + vAvg);
			//System.out.println("m_vsw ' " + m_vsw);
			if (vAvg/m_vsw >= TAIL_START) {
				firstBin = i;
				i = -1;
			}
		}
		// now calculate the total bulk flux
		float totT = 0;
		for (int i=0; i<firstBin; i++) {
			totT += d.fluxes[i];
		}
		// now calculate the tail flux
		float totB = 0;
		for (int i=firstBin; i<d.fluxes.length; i++) {
			totB += d.fluxes[i];
		}
		if (totB!=0) tailVswScatter.addPoint(sr.getV(d.date),totT/totB);
	}

	private void addTailToTailVsBAngle(FluxData d) {
		double m_vsw = sr.getV(d.date);
		if (m_vsw <= 0) return;
		// first figure out the first bin of tail flux
		int firstBin = 0;
		for (int i=d.fluxes.length-1; i>=0; i--) {
			double vmx = vMax[i+d.startBin];
			double vmn = vMin[i+d.startBin];
			double vAvg = (vmx+vmn)/2;
			if (vAvg/m_vsw >= TAIL_START) {
				firstBin = i;
				i = -1;
			}
		}
		// now calculate the total bulk flux
		float totT = 0;
		for (int i=0; i<firstBin; i++) {
			totT += d.fluxes[i];
		}
		// now calculate the tail flux
		float totB = 0;
		for (int i=firstBin; i<d.fluxes.length; i++) {
			totB += d.fluxes[i];
		}

		// now calculate B angle to radial
		float p = br.getP(d.date);
		float t = br.getT(d.date);
		hp.setCoords(HelioPoint.SPHERICAL, 1,p*Math.PI/180,t*Math.PI/180);
		double angle = (float)hp.getAngle(HPREF);
		if (angle>Math.PI/2) {
			angle=angle-Math.PI/2;
		}
		angle = angle*180/Math.PI; // convert to degrees
		if (totB!=0) tailBScatter.addPoint((float)angle,totT/totB);
	}

	private void addTailToTailVsAlfven(FluxData d) {
		double m_vsw = sr.getV(d.date);
		if (m_vsw <= 0) return;
		// first figure out the first bin of tail flux
		int firstBin = 0;
		for (int i=0; i<d.fluxes.length; i++) {
			double vmx = vMax[i+d.startBin];
			double vmn = vMin[i+d.startBin];
			double vAvg = (vmx+vmn)/2;
			if (vAvg/m_vsw >= TAIL_START) {
				firstBin = i;
				i = -1;
			}
		}
		// now calculate the total bulk flux
		float totT = 0;
		for (int i=0; i<firstBin; i++) {
			totT += d.fluxes[i];
		}
		// now calculate the tail flux
		float totB = 0;
		for (int i=firstBin; i<d.fluxes.length; i++) {
			totB += d.fluxes[i];
		}
		// now calcualte vAlfven
		float m = br.getM(d.date); if (m<=0) return;
		float den = sr.getN(d.date); if (den<=0) return;
		float va = 100*m/2/(float)Math.sqrt(1.67*Math.PI*den);
		if (totB!=0) tailVswScatter.addPoint(va,totT/totB);
	}

	case 202 : { // tail
	                	// first figure out the first bin of tail flux
	                int firstBin = 0;
	                for (int i=d.fluxes.length-1; i>=0; i--) {
	                    double vmx = vMax[i+d.startBin];
	                    double vmn = vMin[i+d.startBin];
	                    double vAvg = (vmx+vmn)/2;
	                   	double m_vsw = sr.getV(d.date);
	                    if (vAvg/m_vsw >= TAIL_START) {
	                        firstBin = i;
	                        i = -1; // exit loop
	                    }
	                }
	                // now calculate the total bulk flux
	                float totT = 0;
	                for (int i=0; i<firstBin; i++) {
	                    totT += d.fluxes[i];
	                }
	                // now calculate the tail flux
	                float totB = 0;
	                for (int i=firstBin; i<d.fluxes.length; i++) {
	                    totB += d.fluxes[i];
	                }
	                if (totB != 0) return totT/totB;
	                else return 0;
            }

	*/


