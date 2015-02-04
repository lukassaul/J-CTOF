
/**
* Send spec data from a .bin file to JColorGraph window ...
*
*
*/
public class MfiSpectrumGrapher {

	public static final void main(String[] args) {

		float startDate = 149.5f;
		int numSpecs = 100;
		int numFreqs = 65/2;
		float deltaT = 302.0f/60.0f/60.0f/24.0f;

		double[] x = new double[numSpecs];
		double[] y = new double[numFreqs];
		double[] z = new double[numSpecs*numFreqs];
		int index = 0;

		MfiSpectrumReader msr = new MfiSpectrumReader();
		float[] l1specs;
		double[] posFreqs = Spectrum.getPositiveFrequencies(3.0f,128);
		float[] times = new float[numSpecs];

		int dif = posFreqs.length - numFreqs;
		for (int i=0; i<numFreqs; i++) {
			y[i]=(double)posFreqs[i+dif];
		}

		try {
			// get first one by itself to set date
			times[0] = startDate;
			x[0] = startDate;
			l1specs = msr.getSpectrum(startDate);
			for (int j=0; j<numFreqs; j++) {
			    // let's cap at a zmax of 10
				if (l1specs[j+dif]>5) z[index]=5.0;
				else
					z[index]=(double)l1specs[j+dif];
				index++;
			}

			for (int i=1; i<numSpecs; i++) {
				l1specs = msr.nextSpectrum();
				times[i] = msr.getDate();
				x[i] = (double)times[i];
				o("x: " + x[i] + " i " + i + " " +l1specs[0]);
				for (int j=0; j<numFreqs; j++) {
				// let's cap at a zmax of 10
					if (l1specs[j+dif]>5) z[index]=5.0;
					else
						z[index]=(double)l1specs[j+dif];
					index++;
				}
			}
		}
		catch (Exception e) { e.printStackTrace(); }

		/*
		for (int i=0; i<numSpecs; i++) {
			times[i]=startDate+deltaT*i;
			x[i] = (double)times[i];
			l1specs = msr.getSpectrum(times[i]);
			for (int j=0; j<numFreqs; j++) {
				// let's cap at a zmax of 10
				if (l1specs[j+dif]>5) z[index]=5.0;
				else
				z[index]=(double)l1specs[j+dif];
				//o(x[index]+" "+y[index]+" "+z[index]+" "+index);
				index++;
			}
		}*/


		/*for (int i=0; i<numSpecs; i++) {
			times[i]=startDate+deltaT*i;
			o("getting spctrum for: " + times[i]);
			l1specs = msr.getSpectrum(times[i]);
			for (int j=0; j<l1specs.length; j++) {
				z[index]=(double)l1specs[j];
				y[index]=posFreqs[j];
				x[index]=(double)times[i];
				//o(x[index]+" "+y[index]+" "+z[index]+" "+index);
				index++;
			}
		}*/

		System.out.println("done getting data - index: " + index + " _ " + (numSpecs*numFreqs));


		JColorGraph jcg = new JColorGraph(x,y,z);
		jcg.run();
	}

	private static void o(String s) {
		System.out.println(s);
	}
}