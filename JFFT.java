import java.lang.Math;
import java.util.Date;
import java.util.Vector;
import java.util.StringTokenizer;

/**
*  Lukas Saul - March 27, 2001
*  Taken from C routine Four1, from Numerical Recipes
*
*
*  updated a bit july 2002
*  for use with CDF data
*/
public class JFFT {
	public double[] dOutImaginary, dOutMagnitude, dOutReal, dOutFreq, dOutPeriod;
	private double[] dInReal;
	private String outputFile = "JFFTOutput3.dat";
	//private long nn;
	//private int iSign;
	//private double wr, wi, wpr, wpi, wtemp, theta;

	public JFFT(double[] _dIn, double delta) {
		this(_dIn,delta,"");
	}

	public JFFT(double[] _dIn, double delta, String outF) {
		outputFile = outF;


		Date d1 = new Date();
		dInReal = _dIn;
		int size = dInReal.length;
		o("size of array: " + size);


		// what if dIn.length is not a power of 2?
		// this routine requires a padding
		int test=1;
		int i = 0;
		while (test != 0) {
			i++;
			test = size >> i; // divide by 2^i (binary shift)
			o("test = " + test);
		}
		i=i-1;
		o("final i = " + i);
		int dif = size - (int)Math.pow(2,i);
		o("dif from power of 2 = " + dif);
		if (dif > 0) { // In this case we need to pad the array with zeroes
			double[] dIn2 = new double[(int)Math.pow(2.0,i+1)]; //this could be faster
			//copy by "hand"
			for (int k=0; k<dInReal.length; k++) {
				dIn2[k]=dInReal[k]; // the real data
			}
			for (int j=dInReal.length; j<dIn2.length; j++) {
				dIn2[j]=0; // here's the padding
			}
			// the new data:
			dInReal = dIn2;
			o("new size " + dInReal.length);
		}
		Date d2 = new Date();
		o("setup took: " + (d2.getTime() - d1.getTime()) );
		// done with padding

		// Set up our discrete complex array
		double[] ourData = new double[dInReal.length*2];
		for (int j=0; j<dInReal.length; j++) {
			ourData[2*j] = dInReal[j];
			ourData[2*j+1] = 0; // set imaginary part to zero
		}

		// final check on our points:
		double rMin = 10000.0;
		double rMax = -10000.0;
		o("here's what we are about to transform (1st 100 points of " + ourData.length + ")");
		for (int j=0; j<ourData.length; j++) {
			if (ourData[j]<rMin) rMin=ourData[j];
			if (ourData[j]>rMax) rMax=ourData[j];
			if (j<101) o("array entry " + j + " :"+ ourData[j]);
		}
		o("rMin: " + rMin);
		o("rMax: "+ rMax);

		//**
		//**
		// Ok, let's do the FFT on the ourData array:

		ourData = four1(ourData, dInReal.length, 1);
		o("Done FFT - now arranging results...");

		//**
		//**
				Date d3 = new Date();
		o("fft took: " + (d3.getTime() - d1.getTime()) );
		o("here's what we've got after the fft: (1st 100 of " + ourData.length + ")");
		rMin = 10000.0;
		rMax = -10000.0;
		for (int j=0; j<ourData.length; j++) {
			if (ourData[j]<rMin) rMin=ourData[j];
			if (ourData[j]>rMax) rMax=ourData[j];
			if (j<100) o("array entry " + j + " :"+ ourData[j]);

		}
		o("rMin: " + rMin);
		o("rMax: "+ rMax);


		// separate the output into real and imaginary frequencies, and total amplitude
		dOutReal = new double[dInReal.length];
		dOutImaginary = new double[dInReal.length];
		dOutMagnitude = new double[dInReal.length];


		for (int j=0; j<dInReal.length; j++) {
			dOutReal[j] = ourData[2*j];
			dOutImaginary[j] = ourData[2*j+1];
			dOutMagnitude[j] = Math.sqrt(dOutReal[j]*dOutReal[j] + dOutImaginary[j]*dOutImaginary[j]);
		}
		o(dOutMagnitude[1]+" "+dOutMagnitude[2]+" "+dOutMagnitude[3]);

		// now let's graph the output!

		// first a REAL SIMPLE graph:
		/*
		double[] xat = new double[dInReal.length];
		for (int l = 0; l<xat.length; l++) {
			xat[l] = l;
		}
		NOAAHistogramFrame nhgf = new NOAAHistogramFrame(xat, dOutReal);
		NOAAHistogramFrame nhgf2 = new NOAAHistogramFrame(xat, dOutImaginary);
		*/


		// NOW A BETTER VERSION:
		// first create the frequency array, as dictated by page 508 of Numerical Recipes for C
		int N = dOutMagnitude.length;
		o("it looks like we have an array of : " + N);
		dOutFreq = new double[N];
		dOutFreq[0]=0;
		for (int l=1; l<N/2; l+=1) {
			dOutFreq[l]=(double)1/(double)N/delta*(double)(2*l);
		}
		dOutFreq[N/2]=1.0/delta;
		int index = 0; // for building array of negative frequencies
		for (int m=N-1; m>N/2; m--) {
			index++;
			dOutFreq[m]=-(double)2*(double)index/(double)N/delta;
		}

		// lets take a look at our frequency array
		rMin = 10000.0;
		rMax = -10000.0;
		for (int j=0; j<dOutFreq.length; j++) {
			if (dOutFreq[j]<rMin) rMin=dOutFreq[j];
			if (dOutFreq[j]>rMax) rMax=dOutFreq[j];
			if (j<100) o("frequency entry " + j + " :"+ dOutFreq[j]);
			if (j<100) o("period entry " + j + " :"+ 2*Math.PI/dOutFreq[j]);

		}
		o("rMin: " + rMin);
		o("rMax: "+ rMax);

		// the zeroth element is the zero frequency - infinite period - we'll chop this at the end
		// the next trhough n/2-2 are:
		//for (int l=1; l<xArray.length; l++) {
		//	int q = n/2 - l;
		//	xArray[l] = 1.0/( (double)(n/2 - q)/(double)n/(double)delta );
		//}
		// the middle frequency is plus or minus the largest one available
		//xArray[n/2]=1/2/delta; // we take positive for now
		// now do the negative frequencies
		//for (int l=1; l<=(n/2 - 1); l++) {
		//	int q = n/2 - l;
		//	xArray[n/2+l] = -(n/2 - q/n/delta); // for now
		//}
		// and that's the frequency array!
		// list the frequencies for debug:
		//o("Frequencies:");
		//for (int j=0; j<N; j++) {
		//		o(dOutFreq[j]);
		//}

		// create an array of periods
		dOutPeriod = new double[N];
		for (int l=0; l<N; l+=1) {
			dOutPeriod[l] = 2*Math.PI/dOutFreq[l];
		}
	}

	public double[] getPeriods() {
		return dOutPeriod;
	}

	public double[] getFrequencies() {
		return dOutFreq;
	}

	public double[] getCoefficients() {
		return dOutMagnitude;
	}

	public void output() {
		int N = dOutMagnitude.length;

		file f = new file(outputFile);
		f.initWrite(false);
		for (int j=0; j<N; j++) {
			if (dOutMagnitude[j]==0) {
				//f.write(dOutPeriod[j]+"\t"+dOutMagnitude[j]+"\n");
			}
			else {
				f.write(dOutPeriod[j]+"\t"+Math.log(dOutMagnitude[j])+"\n");
			}
		}
		f.closeWrite();

		//doctor up our output to take logarithm:
		//double[] yArray = new double[n/2 - 1];
		//for (int l=0; l<yArray.length; l++) {
		//	yArray[l] = Math.log(dOutAmplitude[l+1]);
		//}
		//o("Test yARray: " + yArray[1] + " " +yArray[2] + " " + yArray[3]);
		//NOAAHistogramFrame nhf = new NOAAHistogramFrame(dOutPeriod, dOutMagnitude);
		//NOAAHistogramFrame nhf2 = new NOAAHistogramFrame(xArray, dOutImaginary);
	}

	/*
	* this is the routine from Numerical Recipes for C
	*/
	public static final double[] four1(double[] data, int nn, int isign) {
		o("computing fft with args: " + data.length + " " + nn);
		long n, mmax, m, j, istep, i;
		double wtemp, wr, wpr, wpi, wi, theta;
		float tempr, tempi;

		n = nn/2;
		o("compare " + n + " " + nn + " " + nn/2);
		j=1;

		for (i=1; i<n; i+=2) {
			if (j>i) {
				//swap(data[(int)j], data[(int)i]);
				double temp = data[(int)j];
				data[(int)j]=data[(int)i];
				data[(int)i]=temp;

				//swap(data[(int)j+1], data[(int)i+1]);
				temp = data[(int)j+1];
				data[(int)j+1]=data[(int)i+1];
				data[(int)i+1]=temp;
			}
			m=n/2;
			while (m >= 2 && j>m)  {
				j -= m;
				m=m/2;
			}
			j += m;
		}

		// Here begins Danielson-Lanczos section of routine
		mmax=2;
		while (n > mmax) {
			istep = mmax << 1;
			theta = isign*(6.28318530717959/mmax);
			wtemp = Math.sin(0.5*theta);
			wpr = -2.0*wtemp*wtemp;
			wpi = Math.sin(theta);
			wr=1.0;
			wi=0.0;
			for (m=1; m<mmax; m+=2) {
				for (i=m; i<=n; i+=istep) {
					j=i+mmax;
					tempr = (float)(wr*data[(int)j] -
						wi*data[(int)j+1]);
					tempi = (float)(wr*data[(int)j+1] - wi*data[(int)j]);
					data[(int)j] = data[(int)i] - tempr;
					data[(int)j+1] = data[(int)i+1] - tempi;
					data[(int)i] += tempr;
					data[(int)i+1] += tempi;
				}
				wr = (wtemp=wr)*wpr - wi*wpi + wr;
				wi = wi*wpr + wtemp*wpi + wi;
			}
			mmax = istep;
		}

		return data;
	}



	private static void o(String s) {
		System.out.println(s);
	}


	/**
	*
	*for testing here...
	* lets test with a sine wave
 	* we want to check that we are getting sensible results first
	*/
	public static void main(String[] args) {

		double[] td = new double[10000];
		double deltaT = 15.0*60.0;
		double period = 27.0*24.0*60.0*60.0;
		o("deltaT: " + deltaT);
		o("period: "+period);
		o("period in days: " + period/24.0/60.0/60.0);
		o("total time: " + td.length*deltaT);
		o("total time in days: " + td.length*deltaT/24.0/60.0/60.0);
		for (int i=0; i<10000; i++) {
			td[i]=2.0+Math.sin(i*deltaT/period);
		}

		JFFT jfft = new JFFT(td,1.0,"testout2.dat");

		jfft.output();

		/*
		if (args.length != 2) System.exit(0);
		file f = new file(args[0]);
		Vector v = new Vector(); // vector of strings
		StringTokenizer st;
		boolean eof = false;
		double delta=0;
		double delta1=0;
		double delta2=0;
		boolean gotDelta = false;
		boolean gotDelta1 = false;
		String line = "";
		String trash = "";
		f.initRead();
		while (!eof) {
			if ((line = f.readLine())==null) eof = true;
			else {
				st = new StringTokenizer(line);
				if (!gotDelta) {
					if (!gotDelta1) {
						delta1=Double.parseDouble(st.nextToken().trim());
						gotDelta1 = true;
					}
					else {
						delta2=Double.parseDouble(st.nextToken().trim());
						delta = delta2-delta1;
						o("got delta: " + delta);
						gotDelta = true;
					}
				}
				else {
					trash = st.nextToken();
				}
				v.addElement(st.nextToken().trim());
			}
		}

		double[] testData = new double[v.size()];
		for (int m=0; m<testData.length; m++) {
			testData[m] = Double.parseDouble((String)v.elementAt(m)); // random function
		}

		//double delta = 1;       // we want delta in days

		JFFT j = new JFFT(testData, delta, args[1]);
		*/


	}
}