
public class FluxSpectrum {

	public static void main(String[] args) {

		// lets do it from a scatter file
		ScatterInterpolator si = new ScatterInterpolator("BvDOY.dat");
		o("date range: " + si.startDate + " " + si.finishDate);

		float deltaT = .0105f;
		int num = (int)((si.finishDate - si.startDate)/deltaT);
		num = num - 10;
		o("num: " + num);

		double[] fluxes = new double[num];
		for (int i=0; i<num; i++) {
			fluxes[i] = (double)si.getData(si.startDate + i*deltaT);
		}

		o(fluxes [23] + " " + fluxes[1000]);

		double[] mags = Spectrum.getFFTMagnitudes(fluxes);
		double[] freqs = Spectrum.getPositiveFrequencies((double)deltaT*24.0*60.0*60.0,num);

		file f = new file("BvDOY_spectrum.dat");
		f.initWrite(false);
		for (int i=0; i<freqs.length; i++) {
			f.write(freqs[i]+"\t"+Math.log(mags[i])+"\n");
		}
		f.closeWrite();


		/*
		// this stuff was for testing the FluxReader
		FluxReader fr = new FluxReader();
		int dif = 0;
		FluxData fd;
		int lastDate = 0;
		int num = 0;
		while ( (fd=fr.next())!=null ) {
			num++;
			if (dif != (fd.date - lastDate)) {
				dif = fd.date - lastDate;
				if (dif != 906) o("new dif: " + dif);
			}
			lastDate = fd.date;
		}
		o("num: " + num);
		*/



	}

	public static void o(String s) {
		System.out.println(s);
	}
}