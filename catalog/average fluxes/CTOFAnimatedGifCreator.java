
import java.io.*;
import net.jmge.gif.*;
import java.awt.Image;
import java.awt.image.BufferedImage;
import javax.swing.ImageIcon;

/**
*  Create an animated gif file for a certain time period
*
*  Each frame will represent a one hour average, shift them by 15 min per frame for now
*
*  Use pre-calculated fluxes in 15 min bins for this one, in binary form
*
*
*
*  July 2002 - now lets make the frames individually to save memory...
*
*/
public class CTOFAnimatedGifCreator {

	public static final String CRLF = System.getProperty("line.separator");
    public static int GOODS = (int)(8.20368000 * Math.pow(10,8)); // 1996 - (12/30/69 19:00:00)
    /** This must be the same as the DELTAT in PHADataFixer3
	*/
	public static int DELTAT = 906;
	public static int SLIDE_DELTAT = 3000;

	public static float HISTOGRAM_W_START = (float)1.2;
	public static float HISTOGRAM_W_FINISH = (float)4.0;


			// Here's the input dates
	float fdStart = (float)98.0;
	float fdFinish = (float)100.0;

	private FluxHistogram vvHist[];

	// the data readers are here:
	private SwReader sr; // read binary solar wind data
	private FluxReader fr; // read PHAdata
	private FluxData fd;

	// load these arrays to save time with loadEnergies()
	private double[] vMin = new double[62];
	private double[] vMax = new double[62];
	private double[] energies = new double[62];

	private File outputFile;
	private FileOutputStream fos;


	private int secondsToAverage, date, lastDate; // lastDate is for timeProfile
	private float vsw; // current
	private int totalBytes, counter, goodEvents, onePercent, label, eq;

	// for limiting the flux to a certain range
	private boolean doDiff;
	private float startBin, finishBin;

	private NOAAHistogramFrame nhf;

	private Image ii[];

	private boolean stop = false;

	/**
	* Set the type of histogram here
	*/
	private static String histType = "EFlux";

	/**
	* This is for doing the distribution function
	*
	*/
	public static double HE_MASS = 4.167*Math.pow(10,-5);

	/** Well we only need the constructor to get our data, mainly the CTOFGui
	*/
	public CTOFAnimatedGifCreator() {

		//doMakeSlidingAverage = false;
		secondsToAverage = 0;
		date = 0;
		startBin=0;
		finishBin=0;
		lastDate = 0; // with mary jane
		//haveEnergies = false;


		stop = false;


		counter = 0;
		goodEvents = 0;
		label = 0;

		File f = new File("phadataJ.bin");
		totalBytes = ((int)f.length());
		System.out.println(f.length() + " " + totalBytes);
		onePercent = totalBytes/100 + 1;
		System.out.println("one percent" + onePercent);

		loadEnergies();


		int dStart = (int)(fdStart*3600*24 + GOODS);
		int dFinish = (int)(fdFinish*3600*24 + GOODS);

		int numHistograms = (dFinish - dStart) / DELTAT;

		o("going to make: " + numHistograms + " histograms");

		vvHist = new FluxHistogram[1];
		//ii = new Image[numHistograms];

		// create an array of graphics objects
		//nhf = new NOAAHistogramFrame;

		for (int i=0; i<numHistograms; i++) {
			o(i+ "/" + numHistograms);
			setupEnergyHistogram(0);
			run(dStart + i*DELTAT, dStart + i*DELTAT + SLIDE_DELTAT, 0);
			String labelNN = "" + (float)(dStart + i*DELTAT - GOODS)/3600/24;


			nhf = new NOAAHistogramFrame(vvHist[0].label, vvHist[0].data, labelNN);


			//ii[i] = nhf[0].getImage();
			try{

				outputFile = new File("gTot_" +(1000+i)+".gif");
				fos = new FileOutputStream(outputFile);
				o("starting to write animated gif...");
				//writeAnimatedGIF(ii,"CTOF He+", true, 3.0, fos);
				writeNormalGIF(nhf.getImage(), "CTOF He+", -1, false, fos);
				fos.close();
			}
			catch (Exception e) {
				e.printStackTrace();
			}

		}

		//if (ii[0]==null) o("shit, no images in here!");




		o("we seem to have finished our work here");
		System.exit(0);
	}

	/**
	*  This will create a single energy histogram for the required dates
	*
	*/

	public void run(int d1, int d2, int numH) {
		System.out.println("Doing a histogram for "+ d1 + " - " + d2);
		//u("creating readers..."); // when necessary...
		sr = new SwReader();
		fr = new FluxReader();

		o("Starting to read PHA Helium data...");
		date = 0;
		stop = false;
		// This is the heart of the program
		//******************************************************************
		while (loadNextEvent() && !stop) {
			if ( fd.date>d1 ) { // if it ain't good, we don't want it!
				addFluxesToVVsw(fd, numH);
			}
			if (fd.date > d2) stop = true;
		}
		//******************************************************************
		o("finalizing for: " + numH);
		if (vvHist[numH]!=null) vvHist[numH].finalize();

	}


	private boolean loadNextEvent() {

		try {
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
			}
			return true;
		}
		catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	private void setupEnergyHistogram(int num) {
		float s = HISTOGRAM_W_START;
		float f = HISTOGRAM_W_FINISH;
		//System.out.println("Histogram start " + s + " finish " + f);
		vvHist[num] = new FluxHistogram(s, f, 60);
		//vvHist[num].setParent(theMain);
	}


	/**
	* Here we check which type of hist to do
	*
	*/
    private void addFluxesToVVsw(FluxData d, int numHist) {
		//System.out.println("adding fluxes..." + numHist);

		if (histType.equals("EFlux")) {
			for (int i=0; i<d.fluxes.length; i++) {
				double vmx = vMax[i+d.startBin];
				double vmn = vMin[i+d.startBin];
				vvHist[numHist].addFlux((float)(vmn/vsw), (float)(vmx/vsw),
						(float)(energies[i]*d.fluxes[i]), d.counts[i]);
			}
		}
		else if (histType.equals("Flux"))  {
			for (int i=0; i<d.fluxes.length; i++) {
				double vmx = vMax[i+d.startBin];
				double vmn = vMin[i+d.startBin];
				vvHist[numHist].addFlux((float)(vmn/vsw), (float)(vmx/vsw),
						(float)(d.fluxes[i]), d.counts[i]);
			}
		}
		// in s^3 / km^6
		else if (histType.equals("Distribution Function")) {
			for (int i=0; i<d.fluxes.length; i++) {
				double vmx = vMax[i+d.startBin];
				double vmn = vMin[i+d.startBin];
				double vmid = (vmx+vmn)/2;
				double c = 4.167*Math.pow(10,5);
				vvHist[numHist].addFlux((float)(vmn/vsw), (float)(vmx/vsw),
					(float)(d.fluxes[i]*c/vmid/vmid), d.counts[i]);
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

	private void writeAnimatedGIF(Image[] still_images,
	                        String annotation,
	                        boolean looped,
	                        double frames_per_second,
	                        OutputStream out) throws IOException  {

	    Gif89Encoder gifenc = new Gif89Encoder();
	    for (int i = 0; i < still_images.length; i++) {
	      	gifenc.addFrame(still_images[i]);
	      	o("added a frame");
		}
	    gifenc.setComments(annotation);
	    gifenc.setLoopCount(looped ? 0 : 1);
	    gifenc.setUniformDelay((int) Math.round(100 / frames_per_second));
	    o("starting to encode");
	    gifenc.encode(out);
	}

	private void writeNormalGIF(Image img,
					  String annotation,
					  int transparent_index,  // pass -1 for none
					  boolean interlaced,
					  OutputStream out) throws IOException   {
		Gif89Encoder gifenc = new Gif89Encoder(img);
		gifenc.setComments(annotation);
		gifenc.setTransparentIndex(transparent_index);
		gifenc.getFrameAt(0).setInterlaced(interlaced);
		gifenc.encode(out);
  	}


	// debug string output
	private void o(String s) {
		System.out.println(s);
	}

	// GUI update
	private void u(String s) {
		//theMain.statusLabel.setText(s);
	}

	public static void main(String[] args) {
		CTOFAnimatedGifCreator cagc = new CTOFAnimatedGifCreator();
	}
}

