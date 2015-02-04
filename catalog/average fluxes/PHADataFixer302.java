import java.util.StringTokenizer;
import java.io.*;
import java.util.Arrays;

/**
*  Make counts into fluxes at 302 s intervals.  Averaging can be done later.
*
*  CURRENT FORMAT:
*  int startDate for bin, 62*float - flux values per energy bin
*    (we are only using 62 bins because that is the highest bin in the dataset)
*
*  Change in Format: Nov 28, 2001
*    we need to keep # of counts per bin for statistical error analysis
*  int startDate for bin, 62* (1 float (flux) then 1 int (counts)) (each bin)
*
*
*/
public class PHADataFixer302 {
	public static int GOODS = (int)(8.20474200 * Math.pow(10,8)); // 1996 - (12/31/69 19:00:00) in secs


	/**
	*This parameter compensates for any unusual energy dependence.
	*Multiplies flux by Energy^gamma, during this preprocessing stage
	*
	* For binary table of difEFlux, use GAMMA=1
	* For binary table of difFlux, use GAMMA=0
	*
	* recommended use: GAMMA=0.  Adjustments can be made during reading of file at low cost
	*/
	public static int GAMMA = 0;
	//public static int SDELTAT = 300; // step through time
	public static int DELTAT = 302; // sliding average time
	public static double e = 1;
	public static String OUTPUT_FILE_NAME = "phadAvWstats.bin";
	//public static double Mhe = 4*1.6726231 * Math.pow(10,-27); // 4 * 1.6726231 x 10-24 gm

	private int numIntervals;
	private int numTotalBins;
	private int numIntervalsWithBins;
	private int numZeroBins;

	private int debug, maxE, minE, intervals,bins;
	private SWDataInterpolator sr;

	private double[] energies = new double[62];
	private double[] vMin = new double[62];
	private double[] vMax = new double[62];

	private PHADataInterpolator phadi;

	public PHAAverageDataFixer(String[] args) {
		try {
			phadi = new PHADataInterpolator();

			int day = 0;
			intervals = 0;
			bins = 0;
			loadEnergies();

			// a flux for each energy
			double[] fluxes = new double[62];
			int[] counts = new int[62];

			// leave the date in the same format for now (integer, see
			//int lastDate = MyDate2.parse("01/01/1969 00:00:00");
			int startDate = 827439184;  //829700322
			int finishDate = 840239804;
			int numberWithoutSpeed = 0;
			int numberOutsideRange = 0;
			int numberOfEvents = 0;
			int numberAbove4 = 0;
			String CRLF = System.getProperty("line.separator");
			//SwReader swr = new SwReader();

			DataOutputStream dos = new DataOutputStream(new FileOutputStream(OUTPUT_FILE_NAME));

			// loop through the dataset with
			for (int d=startDate; d<=finishDat; d+= DELTAT) {
				// reset arrays
				Arrays.fill(fluxes,0);
				Arrays.fill(counts,0);

				PHAData[] pd = phadi.getData(d,d+DELTAT);

				// set the counts
				for (int i=0; i<pd.length; i++) {
					counts[pd[i].energy]++;
				}

				// DIFFERENTIAL FLUX CALCULATION
				// for each energy bin
				// System.out.println("calculating fluxes for " + lastDate + " " + date);
				for (int i=0; i<62; i++) {
					fluxes[i]=(double)counts[i]*62/SDELTAT;
					//System.out.println("fluxes1 : "+fluxes[i]);

					// GEOMETRIC FACTOR:
					fluxes[i]=fluxes[i]/(double)(0.16*.4462*.056);


					// ENERGY EFFICIENCY
					double u_acc;
					double req;

					// this is a dummy variable - doesn't get used!!
					// (SEE FEPS_F AND FEPS_DOM)
					int ionMass = 0;


					//int ionCharge = e;
					if (day<180) u_acc = 19;
					else u_acc = (double)22.7;

					req = u_acc + energies[i]/e;

					double epssr = (double)Math.min(1.0, feps_f(ionMass, e, req));
					double epsstr = (double)Math.min(1.0, feps_dom(ionMass, e, req));
					fluxes[i] = fluxes[i]/(epssr*epsstr)*(double)Math.pow(energies[i],GAMMA);

					//if (debug%20 == 1) {
						//System.out.println("bin+ " + i + " counts+ " + counts[i] + "fluxes: "
						//+ fluxes[i]);
					//}
					//debug ++;

				}

						// OUTPUT FLUXES:
						dos.writeInt(lastDate);
						for (int i=0; i<62; i++) {
						//	testFile.write((float)fluxes[i]+"\t");
							dos.writeFloat((float)fluxes[i]);
							dos.writeInt(counts[i]);
							if (fluxes[i]==0) numZeroBins++;
							numTotalBins++;
						}
						//testFile.write(System.getProperty("line.separator"));



						// reset count bins
						Arrays.fill(fluxes,0);
						Arrays.fill(counts,0);

						// important - reset for next histogram bin
						lastDate = date;
						//intervals++;
					}
				}
			}
			dos.close();
			inf.closeRead();
			testFile.closeWrite();
			//o("events without vsw: " + numberWithoutSpeed);
			//o("events outside range: " + numberOutsideRange);
			//o("events below 1.7: " + numberAbove4);
			//o("total good events: " + numberOfEvents);
			o("intervals: " +numIntervals);
			o("bins: " + numTotalBins);
			//o("ints with bins: " + numIntervalsWithBins);
			o("zero bins: " + numZeroBins);
			System.out.println("Max and min: " + maxE +" "+minE);

		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**uebernommen aus Cal_srvc (HEG, TUrbo Pascal)
	*vgl. Eichaquswertung F1,f2 SOWICOMS, Mai 1988
	* MPAe/Gruenwaldt, 19.2.90
	*version2 15.8.96
	*modified 7/15/97 - He only?
	*
	*/
	public static double feps_dom (int m, double q, double req) {
		double leni = (double)alog10(req*q);
		double epsido = (double)Math.pow(10,-4.1830 + leni*3.2667 - 0.7633*leni*leni);
		//System.out.println("feps_dom: req= "+req + "returns: " + epsido);
		return epsido;
	}

	/** uebernommen aus Cal_srvc (HEG, TUrbo Pascal)
	* vgl. Eichaquswertung F1,f2 SOWICOMS, Mai 1988
	* MPAe/Gruenwaldt, 19.2.90
	*version2 15.8.96
	*modified 7/15/97 - He only?
	*/
	public static double feps_f (int m, double q, double req) {
		double leni = alog10(req*q);
		double epsf = (double)Math.pow(10,-0.4856 + 0.2979*leni - 0.033*leni*leni);
		//System.out.println("feps_f: req= "+req + "returns: " + epsf);
		return epsf;
	}


	//private boolean didEnergies = false;;
	/**
	*  these formulas are from "matrix rates" from ctof
	*  this method depricated in favor of array access
	*/
	public double getEnergy (int i) {
		return energies[i];
	}

	/**
	* Load the array of (logarithmic) energies from the matrix rates calibration
	*
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
			System.out.println("E from "+eqMin+" to "+eqMax +" vmax/min: " + vMax[eq]+" "+vMin[eq]);
			energies[eq] = (eqMax+eqMin)/2;
		}
	}


	public static double alog10(double x) {
		double answer = (double)(Math.log(x)/Math.log(10));
		//System.out.println("x= " + x + "  alog = " + answer);
		return answer;
	}

	/*	// these formulas are from "matrix rates" from ctof
							//o(eq+"");

							double eqMax = 34.673 * Math.pow(105, (double)(0 - eq)/(double)116);
							double eqMin = 34.673 * Math.pow(105, (double)(1 - eq)/(double)116);
							//o(eqMax + " " + eqMin);
							// this one is just E/q = .5mv^2 (m = 4 amu)
							double vMaxd = Math.sqrt(eqMax*4.8)*100;
							double vMind = Math.sqrt(eqMin*4.8)*100;
							//o(vMaxd + " " + vMind);
							double vvMax = vMaxd/vsw;
							double vvMin = vMind/vsw;
							//o(vvMax + " " + vvMin);
							if ( (vvMin >= (double)1.7) && (vvMax <= 4) ) {
								dos.writeInt(date);
								dos.writeShort(eq);
								numberOfEvents++;
							}else {
								if (vvMin < (double)1.7) numberAbove4++;
							numberOutsideRange++;
*/

	public static void o(String s) {
		System.out.println(s);
	}

	public static final void main(String[] args) {
		PHADataFixer3 pdf3 = new PHADataFixer3(args);
	}
}

/*
OLD FORMATS:
* The output form: int, short, short, float*x
* the int is date in seconds, for start of a time bin
* the short is startBin for this DELTAT
* the next short is number of bins for this DELTAT
* the floats are Diff. EFlux for each bin
*/