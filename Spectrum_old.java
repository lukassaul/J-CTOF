import java.io.DataInputStream;
import java.io.*;

/**
* Ported from spctrm from numerical recipes for C (the bible)
*  by Lukas Saul, summer 2002
*
*/
public class Spectrum {

	/**
	* Let's test this routine
	*
	* we'll make a file first with recognizeable power spectrum
	*/
	public static final void main(String[] args) {
		try {
		DataOutputStream dos = new DataOutputStream(new FileOutputStream("sample.bin"));
		double deltaT = 15.0*60.0;
		double period = 27.0*24.0*60.0*60.0;
		o("deltaT: " + deltaT);
		o("period: "+period);
		o("period in days: " + period/24.0/60.0/60.0);
		o("total time: " + 12800*deltaT);
		o("total time in days: " + 12800*deltaT/24.0/60.0/60.0);
		for (int i=0; i<12800; i++) {
			dos.writeFloat((float)2.0+(float)Math.sin(i*deltaT/period));
		}
		dos.close();

		DataInputStream dis = new DataInputStream(new FileInputStream("sample.bin"));
		float[] out = spctrm(dis,128,49,true);

		file f = new file("testoutputSPCTRM1.dat");
		f.initWrite(false);
		for (int i=0; i<out.length; i++) {
			f.write((i-1)/2*128*deltaT+"\t"+out[i]+"\n");
		}
		f.closeWrite();

		}
		catch (Exception e) {
			System.out.println("whoops!  something wrong in main()");
			e.printStackTrace();
		}
	}

	/**
	* Reads data from input stream speci?ed by ?le pointer fp and returns as p[j] the data’s power
	* (mean square amplitude) at frequency (j-1)/(2*m) cycles per gridpoint, for j=1,2,...,m,
	* based on (2*k+1)*m data points (if ovrlap is set true (1)) or 4*k*m data points (if ovrlap
	* is set false (0)). The number of segments of the data is 2*k in both cases: The routine calls
	* four1 k times, each call with 2 partitions each of 2*m real data points.
	*/
    public static float[] spctrm(DataInputStream fp, int m, int k, boolean ovrlap)  {
		try{

		float[] p = new float[m+1];
		//void four1(float data[], unsigned long nn, int isign);

		int mm,m44,m43,m4,kk,joffn,joff,j2,j;
		float w,facp,facm,sumw=(float)0.0,den=(float)0.0;
		float[] w1, w2;
		mm=m+m;           //Useful factors.
		m43=(m4=mm+mm)+3; // note slickness
		m44=m43+1;
		w1 = new float[m4];
		w2 = new float[m];
		facm=(float)m;
		facp=(float)1.0/facm;
		for (j=1;j<=mm;j++) sumw += SQR(WINDOW(j,facm,facp)); //Accumulate the squared sum of the weights.

		for (j=1;j<=m;j++) p[j]=(float)0.0; //Initialize the spectrum to zero.

		if (ovrlap)  //Initialize the “save” half-bu?er.
			for (j=1;j<=m;j++) w2[j]=fp.readFloat(); //fscanf(fp,"%f",&w2[j]);

		for (kk=1;kk<=k;kk++) {
		//Loop over data set segments in groups of two.
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
			w1 = four1(w1,mm,1); //Fourier transform the windowed data.
			p[1] += (SQR(w1[1])+SQR(w1[2])); //Sum results into previous segments.

			for (j=2;j<=m;j++) {
				j2=j+j;
				p[j] += (SQR(w1[j2])+SQR(w1[j2-1])
				+SQR(w1[m44-j2])+SQR(w1[m43-j2]));
			}
			den += sumw;
		}
		den *= m4; //Correct normalization.
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

	// try again

	//#define SWAP(a,b) tempr=(a);(a)=(b);(b)=tempr

	/**Replaces data[1..2*nn] by its discrete Fourier transform, if isign is input as 1; or replaces
	*data[1..2*nn] by nn times its inverse discrete Fourier transform, if isign is input as -1.
	*data is a complex array of length nn or, equivalently, a real array of length 2*nn. nn MUST
	*  be an integer power of 2 (this is not checked for!).
	*/
	public static double[] four1(float data[], long nn, int isign) {

		long n,mmax,m,j,istep,i;
		double wtemp,wr,wpr,wpi,wi,theta; // Double precision for the trigonometric recurrences
		float tempr,tempi;
		n=nn/2;
		j=1;
		for (i=1;i<n;i+=2) { //This is the bit-reversal section of the
			if (j > i) {
				SWAP(data[j],data[i]); Exchange the two complex numbers.
				SWAP(data[j+1],data[i+1]);
			}
			m=nn;
			while (m >= 2 && j > m) {
				j -= m;
				m *= 2;
			}
			j += m;
		}
		//Here begins the Danielson-Lanczos section of the routine.
		mmax=2;
		while (n > mmax) { Outer loop executed log2 nn times.
			istep=mmax << 1;
			theta=isign*(6.28318530717959/mmax); Initialize the trigonometric recurrence.
			wtemp=sin(0.5*theta);
			wpr = -2.0*wtemp*wtemp;


			wpi=sin(theta);
			wr=1.0;
			wi=0.0;
			for (m=1;m<mmax;m+=2) { //Here are the two nested inner loops.
				for (i=m;i<=n;i+=istep) {
					j=i+mmax; //This is the Danielson-Lanczos formula:
					tempr=wr*data[j]-wi*data[j+1];
					tempi=wr*data[j+1]+wi*data[j];
					data[j]=data[i]-tempr;
					data[j+1]=data[i+1]-tempi;
					data[i] += tempr;
					data[i+1] += tempi;
				}
				wr=(wtemp=wr)*wpr-wi*wpi+wr; //Trigonometric recurrence.
				wi=wi*wpr+wtemp*wpi+wi;
			}
			mmax=istep;
		}
	}
}





	/**
	*  this is the routine from Numerical Recipes for C
	*   (ported again)
	* this stuff is an old port
	*/
	/*
	private static final float[] four1(float[] data, int nn, int isign) {
		o("computing fft with args: " + data.length + " " + nn);

		long n, mmax, m, j, istep, i;
		float wtemp, wr, wpr, wpi, wi, theta;
		float tempr, tempi;

		n = nn/2;
		o("compare " + n + " " + nn + " " + nn/2);
		j=1;

		for (i=1; i<n; i+=2) {
			if (j>i) {
				//swap(data[(int)j], data[(int)i]);
				float temp = data[(int)j];
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
			theta = isign*((float)6.28318530717959/mmax);
			wtemp = (float)Math.sin(0.5*theta);
			wpr = (float)-2.0*wtemp*wtemp;
			wpi = (float)Math.sin(theta);
			wr=(float)1.0;
			wi=(float)0.0;
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
	*/

	/**
	* These are the windows most commonly used
	*       Bartlett and Welch are "good enough for most practical use"
	*/
	private static float WINDOW(float j, float a, float b) {
		return 	((float)1.0-(float)Math.abs((((j)-1)-(a))*(b)));   /* Bartlett */
		/* return (float)1.0 */ /* Square */
		/* return (1.0-SQR((((j)-1)-(a))*(b))) */ /* Welch */
	}

	private static float SQR(float a) {
		return a*a;
	}


	public static void o(String s) {
		//System.out.println(s);
	}
}