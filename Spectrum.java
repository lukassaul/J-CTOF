import java.io.*;
import JSci.maths.vectors.*;
import JSci.maths.polynomials.*;
import JSci.maths.*;
import java.util.StringTokenizer;
import java.util.Date;

/**
*
* At this point I'm using JSci library instead of ported NR routines
*
*  seems just as fast despite heavyweight "complex" object -  FFT ~ 20ms for 600mhz pentium 3
*
*  Lukas Saul 2002
*
*    Follows NR - step through data in interval 1/2 of data size for each FFT.  PSD standard.
*
*   Modified to include Linear Detrending Jan, 2003
*/
public class Spectrum {

	/**
	* We need an array of frequencies to see what we're working with
	*
	*  returns an array of size k/2 + 1 where k is next highest power of 2 over n
	*
	*  (input the deltaT between data points, and num. of + freqs)
	*
	*  this should end with Nyquist freq. and start with 0
	*   we are assuming here that we don't need the negative frequencies
	*  return doubles to point out this routine is not optimized
	*/
	public static double[] getPositiveFrequencies(double deltaT, int n) {
		o(deltaT + " " + n);

		// what if n is too big?
		// we need to check...
		double[] test = new double[n];
		for (int i=0; i<test.length; i++) test[i]=0.0;
		n = getPaddedArray(test).length/2;
		// the n now is n/2 from page 508 of NR for C

		// that could have been done quicker, no need to build fictitious array
		double[] tbr = new double[n+1];

		int k = n;
		for (int i=0; i<tbr.length; i++) {
			tbr[i] = (double)(n - k)/2.0/(double)n/deltaT;
			k--;
		}
		o("brought k to " + k);
		return tbr;
	}


	/**
	* Use this to get magnitudes of an FFT Xform
	*
	*/
	public static double[] getFFTMagnitudes(double[] input) {
		double[] start = getPaddedArray(input);
		Complex[] tbt = new Complex[start.length];
		for (int i=0; i<start.length; i++) {
			tbt[i]=new Complex(start[i],0.0);
		}
		Complex[] done = FourierMath.transform(start);
		double[] tbr = new double[start.length];
		for (int i=0; i<start.length; i++) {
			tbr[i]=Math.sqrt(SQR(done[i].real())+SQR(done[i].imag()));
		}
		return tbr;
	}


	/**
	*  what if dIn.length is not a power of 2?
	*  this routine requires a padding
	*
	*/
	public static double[] getPaddedArray(double[] input) {
		System.out.println("padding an array of length: " + input.length);

		int size = input.length;
		int test=1;
		int i = 0;
		while (test != 0) {
			i++;
			test = size >> i; // divide by 2^i (binary shift)
			System.out.println("test = " + test);
		}
		i=i-1;
		System.out.println("final i = " + i);
		int dif = size - (int)Math.pow(2,i);
		System.out.println("dif from power of 2 = " + dif);
		if (dif > 0) { // In this case we need to pad the array with zeroes
			double[] dIn2 = new double[(int)Math.pow(2.0,i+1)]; //this could be faster

			//copy by "hand"
			for (int k=0; k<input.length; k++) {
				dIn2[k]=input[k]; // the real data
			}
			for (int j=input.length; j<dIn2.length; j++) {
				dIn2[j]=0; // here's the padding
			}
			// the new data:
			System.out.println("new size " + dIn2.length);
			return dIn2;
		}
		else return input;
	}

	/**
	*  Use this to get averaged FFT magnitudes from a file!
	*  It uses all values in the file and starts at the beginning.
	*/
	public static double[] getAveragedFFTMagnitudes(
				file f, int numPerSegment)
				throws IOException {

		// just create the binary datafile and go from there..
		DataOutputStream dos = new DataOutputStream(new FileOutputStream("tempdat.bin"));
		StringTokenizer st;
		String line = "";  String garbage = "";  String goods = "";
 		f.initRead();
		boolean eof = false;
		int i = 0;
		while ((line=f.readLine())!=null) {
			st = new StringTokenizer(line);
			garbage = st.nextToken();
			goods = st.nextToken();
			//System.out.println("goods: " + goods);
			dos.writeInt((int)(1000*Float.parseFloat(goods)));
			i++;
		}
		f.closeRead();
		System.out.println("found " + i +" values in getAFM(file)");
		return getAveragedFFTMagnitudes(
			new DataInputStream(new FileInputStream("tempdat.bin")), i, numPerSegment,1,0);
	}


	/**
	* In this one we want to get a better idea of the PSD, power spectrum distribution
	*
	*   we try to minimize variance - following NR.com - windowing the data
	*   and using a sliding average
	*     we use Bartlett window "good for practical purposes"
	*     see WindowFunction.java for more windows
	*
	*   making our own scheme here as spctrm c code was too hard to follow...
	*   numPerSegment must be power of 2!!  we are expecting floats from the DIS
	*
	*   len is length - note that we are reading integers from a binary file!!
	*
	*  include a skip for if the datainput stream has extra data to skip
	*    (every skip*4th byte we take)
	*    we won't skip on the last one!!
	*
	*  WindowFunction.BARTLETT, .SQUARE, .HANN, .WELCH....  others?
	*
	*  Whoops!!  We need to divide by 1000 here after readInt because
	*   we have been storing B fields as ints (after dividing by 1000)
	*   to save space...  Apr 27, 2003
	*/
	public static double[] getAveragedFFTMagnitudes(
				DataInputStream dis, int len, int numPerSegment, int window, int skip)
				throws IOException {

		try {

			// here's what we're working to return
			double[] tbr = new double[numPerSegment/2+1];
			for (int i=0; i<tbr.length; i++) tbr[i]=0.0;
			int norm = 0;
			float garbage;
			int garbagei;

			// set up window coefficients first
			WindowFunction wf = new WindowFunction();
			wf.setWindowType(window);
			float[] winCoeff = wf.generate(numPerSegment);

			// first lets see how many segments we need to transform
			int num = len/numPerSegment;
			int halfSeg = numPerSegment/2;

			//o("trying to do the averaged fft - halfseg: " + halfSeg);

			//create the arrays
			double[] temp1 = new double[halfSeg];
			double[] temp2 = new double[halfSeg];
			double[] data = new double[numPerSegment];


			// load first bit into temp array
			for (int i=0; i<halfSeg; i++) {
				temp2[i] = (double)dis.readInt()/1000;
				for (int j=0; j<skip; j++) garbagei = dis.readInt();
				//garbage = dis.readFloat(); garbage = dis.readFloat(); garbagei = dis.readInt();
			}

			// ok we're ready to start looping - each time here will be a FFT
			for (int i=0; i<num; i++) {
				for (int k=0; k<halfSeg; k++) {
					temp1[k] = temp2[k];
				}
				for (int k=0; k<halfSeg; k++) {
					temp2[k] = (double)dis.readInt()/1000;

					// LAST TIME DON'T SKIP - (the date is needed by other readers)
					if (k!=halfSeg-1 || i!=num-1)
						   for (int j=0; j<skip; j++)  garbagei = dis.readInt();
				}
				// we have the data - put it in the data array from temps
				for (int k=0; k<halfSeg; k++) {
					data[k]=temp1[k];
				}
				for (int k=0; k<halfSeg; k++) {
					data[halfSeg+k]=temp2[k];
				}

				// the data array is filled.  lets try removing any linear trend:
				double[][] deTrendThis = new double[2][numPerSegment];
				for (int k=0; k<numPerSegment; k++) {
					deTrendThis[0][k]=k;
					deTrendThis[1][k]=data[k];
				}
				//DoubleVector lsf = LinearMath.leastSquaresFit(1,deTrendThis);
				RealPolynomial lsfa = LinearMath.leastSquaresFit(1,deTrendThis);
				double[] lsf = lsfa.getCoefficientsAsDoubles();

				//System.out.println("dimension: " + lsf.dimension());
				double intercept = lsf[0];
				double slope = lsf[1];
				for (int k=0; k<numPerSegment; k++) {
					data[k] = data[k]-intercept-k*slope;
				}

				// time to adjust the data with the window
				for (int k=0; k<numPerSegment; k++) {
					data[k]*=winCoeff[k];
				}

				// now the FFT - set imaginary parts to zero
				Complex[] dat = new Complex[numPerSegment];
				for (int k=0; k<numPerSegment; k++) {
					dat[k] = new Complex(data[k],0.0);
				}
				Complex[] fComps = FourierMath.transform(dat);  // Here's where it happens
				for (int k=0; k<tbr.length; k++) {
					tbr[k] += Math.sqrt(fComps[k].real()*fComps[k].real() +
										fComps[k].imag()*fComps[k].imag());
				}
				norm++;
			}
			// we need correct normalization!
			norm*=32;
			if (norm!=0) for (int i=0; i<tbr.length; i++) tbr[i]/=norm;
			return tbr;
		}

		catch (IOException e) {
			throw e;
		}

	}


	/**
	* This one is from NR.com
	*  DEPRECATED - use getAveragedFFTComponents above instead
	*
	* Reads data from input stream speci?ed by ?le pointer fp and returns as p[j] the data’s power
	* (mean square amplitude) at frequency (j-1)/(2*m) cycles per gridpoint, for j=1,2,...,m,
	* based on (2*k+1)*m data points (if ovrlap is set true (1)) or 4*k*m data points (if ovrlap
	* is set false (0)). The number of segments of the data is 2*k in both cases: The routine calls
	* four1 k times, each call with 2 partitions each of 2*m real data points.
	*/
    public static double[] spctrm(DataInputStream fp, int m, int k, boolean ovrlap)  {
		System.out.println("USING WRONG SPCTRM ROUTINE IN SPECTRUM.JAVA");
		try{

		double[] p = new double[m+1];
		//void four1(float data[], unsigned long nn, int isign);

		int mm,m44,m43,m4,kk,joffn,joff,j2,j;
		double w,facp,facm,sumw=0.0,den=0.0;
		double[] w1, w2;
		mm=m+m;           //Useful factors.
		m43=(m4=mm+mm)+3; // note slickness
		m44=m43+1;
		//w1 = new double[m4];
		//w2 = new double[m];
		w1 = new double[(m+1)*4];
		w2 = new double[m+1];
		facm=(double)m;
		facp=1.0/facm;
		for (j=1;j<=mm;j++) sumw += SQR(WINDOW(j,facm,facp)); //Accumulate the squared sum of the weights.

		for (j=1;j<=m;j++) p[j]=0.0; //Initialize the spectrum to zero.

		if (ovrlap)  //Initialize the “save” half-bu?er.
			for (j=1;j<=m;j++) w2[j]=fp.readFloat(); //fscanf(fp,"%f",&w2[j]);

		for (kk=1;kk<=k;kk++) {   	//Loop over data set segments in groups of two.
			for (joff = -1;joff<=0;joff++) { //Get two complete segments into workspace.
				if (ovrlap) {
					for (j=1;j<=m;j++) w1[joff+j+j]=w2[j];
					for (j=1;j<=m;j++) w2[j]=fp.readFloat();//fscanf(fp,"%f",&w2[j]);
					joffn=joff+mm;
					for (j=1;j<=m;j++) w1[joffn+j+j]=w2[j];
				} else {
					for (j=joff+2;j<=m4;j+=2) w1[j]=fp.readFloat(); //fscanf(fp,"%f",&w1[j]);
				}
			}
			for (j=1;j<=mm;j++) { //Apply the window to the data.
				j2=j+j;
				w=WINDOW(j,facm,facp);
				w1[j2] *= w;
				w1[j2-1] *= w;
			}
			//w1 = four1(w1,mm,1); //Fourier transform the windowed data.
			Complex[] wc1 = FourierMath.transform(w1);
			for (int i=0; i<wc1.length; i++) {
				w1[2*i] = wc1[i].real();
				w1[2*i+1] = wc1[i].imag();
			}

			p[1] += (SQR(w1[1])+SQR(w1[2])); //Sum results into previous segments.

			for (j=2;j<=m;j++) {
				j2=j+j;
				p[j] += (SQR(w1[j2])+SQR(w1[j2-1])
				+SQR(w1[m44-j2])+SQR(w1[m43-j2]));
			}
			den += sumw;
		}
		den *= m4; //Correct normalization
		for (j=1;j<=m;j++) p[j] /= den; //Normalize the output.
		//free_vector(w2,1,m);
		//free_vector(w1,1,m4);

		return p;
		}

		catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}


	private static void testFFT() {

		Complex[] testDat = new Complex[1024];
		int index = 0;
		double x = 0;
		o("setting start array");
		while (index<1024) {
			x = 3.0 + 4*Math.sin(2*Math.PI*index/10) + Math.sin(2*Math.PI*index/100);

			testDat[index]=new Complex(x,0.0);
			index++;
		}
		Date d1 = new Date();
		o("about to do transform");
		testDat = FourierMath.transform(testDat);
		//testDat = FourierMath.sort(testDat);
		Date d2 = new Date();
		o(d2.getTime() - d1.getTime() + "");

		double[] freqs = getPositiveFrequencies(1.0, testDat.length);

		file f = new file("testoutSpec5.dat");
		f.initWrite(false);
		for (int i=0; i<testDat.length/2; i++) {
			f.write(freqs[i]+"\t"+(SQR(testDat[i].real())+SQR(testDat[i].imag())) + "\n");
		}
		f.closeWrite();

	}
	/**
	* Use this to manually take a fourier transform of data from a file
	*
	*/
	public static final void main(String[] args) {
		// output PDF to file from string file of date-float type data
		file f = new file("raw_1_3-3_0_I.dat");
		file fOut = new file("raw_1_3-3_0_S.dat");
		fOut.initWrite(false);
		double[] pdf=new double[0];
		try {
			pdf = getAveragedFFTMagnitudes(f, 4096);
		}
		catch(Exception e) {e.printStackTrace();}
	//	for (int i=0; i<pdf.length; i++) System.out.println(""+pdf[i]);

		double[] freqs = getPositiveFrequencies(15.0/24.0/60.0,4096);
		System.out.println(pdf.length + " " + freqs.length);
		//for (int i=0; i<freqs.length; i++) System.out.println(""+freqs[i]);

		System.out.println("survived to last step..");
		for (int i=0; i<pdf.length; i++) {
			fOut.write(freqs[i]+"\t"+pdf[i]+"\n");
			//System.out.println("wrote: " + i);
		}
		fOut.closeWrite();


		//testFFT();

		// lets test getFFTMagnitudes
		//
		// suppose we have solar cycle data, coming in as 2000 1hr segments
		//

		/*double[] testDat = new double[2000];
		for (int i=0; i<testDat.length; i++) {
			testDat[i] = Math.sin((double)i*2.0*Math.PI/27.0/24.0);
		}


		double[] testX = getFFTMagnitudes(testDat);
		double[] freqs = getPositiveFrequencies(60.0*60.0, testDat.length);

		o(freqs.length + " is freqs length" );
		o(testX.length + " is ffmMags length");

		file f = new file("spec1.dat");
		f.initWrite(false);
		for (int i=0; i<freqs.length; i++) {
			f.write(freqs[i]+"\t"+testX[i] + "\n");
		}
		f.closeWrite();
		*/

		//test averagedFFTMags...
		/*try {
			DataOutputStream dos = new DataOutputStream(new FileOutputStream("temp.tmp"));
			for (int i=0; i<1000; i++) {
				dos.writeFloat((float)(3*i+12+Math.sin((double)i*Math.PI*2/10)+
												4*Math.sin((double)i*Math.PI*1/2)));
			}
			dos.close();

			DataInputStream dis = new DataInputStream(new FileInputStream("temp.tmp"));
			double[] testX = Spectrum.getAveragedFFTMagnitudes(dis,1000,64,1, 0);
			double[] freqs = Spectrum.getPositiveFrequencies(1, 64);

			o(freqs.length + " is freqs length" );
			o(testX.length + " is ffmMags length");

			file f = new file("testSpec.dat");
			f.initWrite(false);
			for (int i=0; i<freqs.length; i++) {
				f.write(freqs[i]+"\t"+testX[i] + "\n");
			}
			f.closeWrite();
		}
		catch (Exception e) {
			e.printStackTrace();
		}*/


		// test padded array
		/*double[] joe = new double[1000];
		for (int i=0; i<joe.length; i++) {
			joe[i] = Math.sin(i);
		}
		double[] tim = getPaddedArray(joe);
		o("tim: " + tim.length);
		for (int i=990; i<tim.length; i++) {
			o(tim[i]+"");
		}
		*/
	}

	private static final double SQR(double d) {
		return d*d;
	}

	/**
	* These are the windows most commonly used
	*       Bartlett and Welch are "good enough for most practical use"
	*
	*  DEPRECATED - use WindowFunction.class instead
	*/
	private static float WINDOW(int j, double a, double b) {
		return 	((float)1.0-(float)Math.abs((((j)-1)-(a))*(b)));   /* Bartlett */
		/* return (float)1.0 */ /* Square */
		/* return (1.0-SQR((((j)-1)-(a))*(b))) */ /* Welch */
	}

	private static float SQR(float a) {
		return a*a;
	}

	private static void o(String s) {
		System.out.println(s);
	}
}