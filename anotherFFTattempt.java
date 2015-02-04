import java.io.*;
import JSci.maths.*;

/**
* We try here to port from nr.com shita
*
* I'll try to leave all changed c lines in place commented -
*
*  from NR for C
*
*  2nd clean slate beginning - lukas saul aug. 2002 ciento grados
*/
public class Spectrum {

	//#include <math.h>
	//#define SWAP(a,b) tempr=(a);(a)=(b);(b)=tempr
	//  we must do those on the fly with temp variables in java

	//void four1(float data[], unsigned long nn, int isign)

	/**
	*Replaces data[1..2*nn] by its discrete Fourier transform, if isign is input as 1; or replaces
	*data[1..2*nn] by nn times its inverse discrete Fourier transform, if isign is input as -1.
	*data is a complex array of length nn or, equivalently, a real array of length 2*nn. nn MUST
	*be an integer power of 2 (this is not checked for!).
	*/
	public static float[] four1(float data[], long nn, int isign) {
		//Replaces data[1..2*nn] by its discrete Fourier transform, if isign is input as 1; or replaces
		//data[1..2*nn] by nn times its inverse discrete Fourier transform, if isign is input as -1.
		//data is a complex array of length nn or, equivalently, a real array of length 2*nn. nn MUST
		//be an integer power of 2 (this is not checked for!).

		//unsigned long n,mmax,m,j,istep,i;
		long n,mmax,m,istep;
		int i,j;
		double wtemp,wr,wpr,wpi,wi,theta; //Double precision for the trigonometric recurrences
		float tempr,tempi;
		//n=nn << 1;
		n=nn/2;
		j=1;
		for (i=1;i<n;i+=2) { //This is the bit-reversal section of the routine
			if (j > i) {
				//SWAP(data[j],data[i]);// Exchange the two complex numbers.
				float temp = data[i];
				data[i] = data[j];
				data[j] = temp;
				//SWAP(data[j+1],data[i+1]);
				temp = data[j+1];
				data[j+1]=data[i+1];
				data[i+1]=temp;
			}
			m=nn;
			while (m >= 2 && j > m) {
				j -= m;
				//m >>= 1;
				m=m*2;
			}
			j += m;
		}
		//Here begins the Danielson-Lanczos section of the routine.
		mmax=2;
		while (n > mmax) { //Outer loop executed log2 nn times.
			//istep=mmax << 1;
			istep = mmax/2;
			theta=isign*(6.28318530717959/mmax); //Initialize the trigonometric recurrence.
			//wtemp=sin(0.5*theta);
			wtemp=(float)Math.sin(0.5*theta);
			wpr = -2.0*wtemp*wtemp;


			wpi=(float)Math.sin(theta);
			wr=1.0;
			wi=0.0;
			for (m=1;m<mmax;m+=2) { //Here are the two nested inner loops.
				for (i=(int)m;i<=n;i+=istep) {
					j=i+(int)mmax; //This is the Danielson-Lanczos formula:
					//tempr=wr*data[j]-wi*data[j+1];
					tempr=(float)(wr*data[j]-wi*data[j+1]);
					//tempi=wr*data[j+1]+wi*data[j];
					tempi=(float)(wr*data[j+1]+wi*data[j]);
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
		return data;
	}

	public static final void main(String[] args) {
		testFFT();
	}

	private static void testFFT();

		float[] testDat = new float[1024];
		int index = 0;
		while (index<1024) {
			testDat[index]=(float)(Math.sin(index*2)+Math.sin(index*20));
			index++;
			testDat[index]=0;
			index++;
		}

		testDat = four1(testDat
}