import java.util.StringTokenizer;
import java.io.*;
import java.util.Arrays;

/**
* This takes the ASCII output of PHADataFixer1 and creates
* an ASCII file of fluxes, for further manipulation.
*
*
*  Here we generate an array of fluxes at small time intervals. (DELTAT)
*
*  Here we only consider TOTAL EFLUX, i.e. add eflux per channel together
*
*/
public class PHADataFixer4 {
	public static int GOODS = (int)(8.20474200 * Math.pow(10,8)); // 1996 - (12/31/69 19:00:00) in secs
	/**
	*This parameter compensates for any unusual energy dependence.
	*Multiplies flux by Energy^gamma, during this preprocessing stage
	*/
	public static int GAMMA = 0;
	public static int DELTAT = 906;
	public static double e = 1;
	//public static double Mhe = 4*1.6726231 * Math.pow(10,-27); // 4 * 1.6726231 x 10-24 gm

	private int numIntervals;
	private int numTotalBins;
	private int numIntervalsWithBins;
	private int numZeroBins;

	private int debug, maxE, minE, intervals,bins;
	//private SwReader sr;

	private int[] counts = new int[62];
	private double[] energies = new double[62];
	private double[] vMin = new double[62];
	private double[] vMax = new double[62];
	public PHADataFixer4(String[] args) {
		try {
			numIntervals = 0;
			numTotalBins = 0;
			numIntervalsWithBins = 0;
			numZeroBins = 0;

			int day = 0;
			minE = 129;
			maxE = 0;
			intervals = 0;
			bins = 0;
			loadEnergies();
			//SwReader sr = new SwReader();

			// a flux for each energy
			double[] fluxes = new double[62];
			int[] counts = new int[62];
			Arrays.fill(fluxes,0);
			Arrays.fill(counts,0);

			// leave the date in the same format for now (integer, see
			//int lastDate = MyDate2.parse("01/01/1969 00:00:00");
			int lastDate = 827439184;  //829700322
			int numberWithoutSpeed = 0;
			int numberOutsideRange = 0;
			int numberOfEvents = 0;
			int numberAbove4 = 0;
			String CRLF = System.getProperty("line.separator");
			//SwReader swr = new SwReader();

			//DataOutputStream dos = new DataOutputStream(new FileOutputStream("phadataJ.bin"));
			file out = new file("phadataFlux.ascii");
			out.initWrite(false);

			file inf = new file("phadata.dat");
			inf.initRead();

			String line = "";
			boolean eof = false;
			while (!eof) {
				if ((line = inf.readLine()) == null) eof = true;
				else {
					StringTokenizer st = new StringTokenizer(line);
					int date = Integer.parseInt(st.nextToken());
					int day2 = (date-GOODS)/24/3600;
					if (day2 != day) {
						day = day2;
						System.out.println("date: " + date+ " DAY: "+ day);
					}

					int eq = Integer.parseInt(st.nextToken());
					if (eq>maxE) maxE=eq;
					if (eq<minE) minE=eq;
					if (date-DELTAT < lastDate) {
						// keep adding to current flux histograms
						counts[eq]++;
					}
					else {
						numIntervals++;
						// here we need to calculate the flux, then clear the count
						// histograms for the next run

						// DIFFERENTIAL ENERGY FLUX
						// for each energy bin
					//	System.out.println("calculating fluxes for " + lastDate + " " + date);
						for (int i=0; i<62; i++) {
							fluxes[i]=(double)counts[i]*62/DELTAT;
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
						out.write(lastDate + "\t");
						double toat = 0;
						for (int i=0; i<62; i++) {
							//testFile.write((float)fluxes[i]+"\t");
							//dos.writeFloat((float)fluxes[i]);
							toat += fluxes[i];
							if (fluxes[i]==0) numZeroBins++;
							numTotalBins++;
						}
						out.write(toat + System.getProperty("line.separator"));

						// reset count bins
						Arrays.fill(fluxes,0);
						Arrays.fill(counts,0);

						// important - reset for next histogram bin
						lastDate = date;
						//intervals++;
					}
				}
			}
			//dos.close();
			out.closeWrite();
			inf.closeRead();
			//testFile.closeWrite();
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
		PHADataFixer4 pdf4 = new PHADataFixer4(args);
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