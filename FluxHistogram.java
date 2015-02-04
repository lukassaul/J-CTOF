//import JSci.maths.*;
import javax.swing.*;

/**
* This tabulates fluxes in a histogram
*
* The variable data[] from Histogram.java now is used to hold a flux value
*
* We add a variable here, float[] counts, to keep track of statistics
*
*
*
*  Added fitting functionality 2/03
*
*/

public class FluxHistogram extends Histogram {
	// these variables from Histogram:
	/*
	public int numberOfBins;
	public float[] data; // this has a number of "events" per bin
	private float top, bottom, binWidth;
	public float[] label; // this has the bottom value of each bin
	private int currentBin;
	*/

	/**
	* This represents the number of delta ts for each bin
	*  this is not an integer because it may need to be split
	* to avoid fencepost effect
	*
	* We only split a segment into at most 2 bins with this histogram
	*
	*/
	public float[] deltas;

	public float[] counts;
	private boolean finalized;
	private JFrame parent;

	/** The constructor is similar to that from Histogram...
	*
	*  Use this to create a V/Vsw flux histogram
	*
	*/
	public FluxHistogram (float _bottom, float _top, float _binWidth) {
		//System.out.println("creating new histogram with binWidth");
		finalized = false;
		top = _top;
		bottom = _bottom;
		binWidth = _binWidth;
		currentBin = 0;
		numberOfBins = (int)((top - bottom)/binWidth);
		//System.out.println("going to use " + numberOfBins + "bins");
		data = new float[numberOfBins];
		label = new float[numberOfBins];
		deltas = new float[numberOfBins];
		counts = new float[numberOfBins];

		for (int i=0; i<label.length; i++) {
			label[i] = bottom + (i*binWidth);
		}
		for (int k=0; k<data.length; k++) {
					deltas[k] = 0;
		}
		for (int j=0; j<data.length; j++) {
			data[j] = 0;
		}

		//System.out.println("numBins: " + numberOfBins + " binWidth: " + binWidth);
		//System.out.println("label1 " + label[1] + " label0 + bw " + label[0]+binWidth);
	}

	/** a given histogram is made with one of these constructors
	* This one is if we know numBins
	*/
	public FluxHistogram(float _bottom, float _top, int _numberOfBins) {
		//System.out.println("creating new histogram with numBins");
		finalized = false;
		top = _top;
		bottom = _bottom;
		numberOfBins = _numberOfBins;
		currentBin = 0;
		binWidth = (top - bottom)/numberOfBins;

		//System.out.println("going to use " + numberOfBins + "bins");
		data = new float[numberOfBins];
		label = new float[numberOfBins];
		deltas = new float[numberOfBins];
		counts = new float[numberOfBins];

		for (int i=0; i<label.length; i++) {
			label[i] = bottom + i*binWidth;
		}
		for (int j=0; j<data.length; j++) {
			data[j] = 0;
		}
		for (int k=0; k<data.length; k++) {
			deltas[k] = 0;
		}
		for (int k=0; k<data.length; k++) {
			counts[k] = 0;
		}
		//System.out.println("numBins: " + numberOfBins + " binWidth" + binWidth);
		//System.out.println("label1 " + label[1] + " label0 + bw " + label[0]+binWidth);
	}

	/** Add flux to a v/vsw bin
	* split events to avoid fencepost effect
	*  keep track of how many time intervals, we need to average fluxes here.
	*/
	public void addFlux(float lowEvt, float highEvt, float flux, int _counts) {
		//System.out.println("adding flux: " + lowEvt + " " + highEvt + " " + flux);
		// first make sure we're in range...
		if ((lowEvt<bottom) || (highEvt>top)) return;

		// if it's all inside, just increment current bin!!
		if ( (lowEvt >= label[currentBin]) && (highEvt <= label[currentBin]+binWidth) ) {
			data[currentBin]+=flux;
			deltas[currentBin]++;
			counts[currentBin]+=_counts;
		}
		else {
			// we first need to find the bin that the lowEvt is inside of
			boolean gotIt = false;
			for (int i=0; i<numberOfBins; i++) {
				if ( (lowEvt >= label[i]) && (lowEvt <= label[i]+binWidth) ) {
					gotIt = true;
					currentBin = i;
					i = numberOfBins; // end loop
				}
			}
			if (!gotIt) {
				//JOptionPane.showMessageDialog(parent,"LowEvt not in FluxHistogram range: " +
				//	lowEvt);
				return;
			}

			// find bin that contains highEvt
			gotIt = false;
			int highBin = 0;
			for (int i=0; i<numberOfBins; i++) {
				if ( (highEvt >= label[i]) && ( highEvt <= label[i]+binWidth) ) {
					gotIt = true;
					highBin = i;
					i=numberOfBins; // end loop
				}
			}
			if (!gotIt) {
				//JOptionPane.showMessageDialog(parent,"HighEvt not in FluxHistogram range: " +
				//	highEvt);
				return;
			}

			// if they are both in the same bin...
			if (currentBin == highBin) {
				data[currentBin]+=flux;
				deltas[currentBin]++;
				counts[currentBin]+=_counts;
 			}

			else  { // we know the bins, we need to do the splitting
				// first calculate fractions
				float bottomFraction = (label[currentBin+1]-lowEvt)/(highEvt - lowEvt);
				float fullBinFraction = (label[currentBin+1]-label[currentBin])/(highEvt - lowEvt);
				float topFraction = (highEvt - label[highBin])/(highEvt - lowEvt);

				// now add to appropriate bins
				data[currentBin] += flux*bottomFraction;
				deltas[currentBin] += bottomFraction;
				counts[currentBin] += _counts*bottomFraction;
				data[highBin] += flux*topFraction;
				deltas[highBin] += topFraction;
				counts[highBin] += _counts*topFraction;
				// now add to any middle bins
				for (int i=currentBin+1; i<highBin; i++) {
					data[i] += flux*fullBinFraction;
					deltas[i] += fullBinFraction;
					counts[i] += _counts*fullBinFraction;
				}
			}

			//	else {
			//		JOptionPane.showMessageDialog(parent,"Too many bins in Histogram - Data straddling >2 bins");
			//		System.out.println("tossing flux data in flux histogram...");
			//	}
		}
	}

	/** This method does the averaging after building flux histogram
	*
	*  Use only after adding all fluxes.
	*/
	public void finalize() {
		if (!finalized) {
			for (int i=0; i<numberOfBins; i++) {
				if (deltas[i]==0) data[i]=0;
				else data[i] = data[i]/deltas[i];
			}
			finalized = true;
		}
		// leave the counts alone here!!
	}

	/*public float getTailPowerIndex(float startVVsw) {

		if (!finalized) finalize();

		int firstBin = 0;

		for (int i=0; i<label.length; i++) {
			if (label[i] >= startVVsw) {
				firstBin = i;
				i = label.length; // exit loop
			}
		}

		// we don't want to include points with too much error.  Find last good bin here:
		int lastBin = label.length-1;
		for (int i=firstBin; i<label.length; i++) {
			if (counts[i] <= 4) {
				lastBin = i;
				i=label.length; // exit loop !IMPORTANT!
			}
		}

		int numTailBins = lastBin-firstBin;
		if (numTailBins==0) return -1;
		double[][] indexThis = new double[2][numTailBins];
		int j=0;
		for (int i=firstBin; i<lastBin; i++) {
			indexThis[0][j]=(double)(label[i]);
			indexThis[1][j]=Math.log((double)data[i]);
			j++;
			//o("i0: " + indexThis[0][j]);
			//o("i1: " + indexThis[1][j]);
		}
		// do the fit now
		DoubleVector lsf = LinearMath.leastSquaresFit(1,indexThis);
		//double intercept = lsf.getComponent(0);
		double slope = lsf.getComponent(1);
		//o("numTailBins: " + numTailBins + "slope: " + slope + " J: " + j);
		return (float)slope;
	}*/



	public void setParent(JFrame aFrame) {
		parent = aFrame;
	}

}

	/**
	*  Use this to remove a time interval from a flux calculation,
	*    thereby increasing the flux (for same number of counts)
	*
	*  DEPRECATED - do not use!!
 	*/
	/*public void subtractTime(TimeInterval ti) {
		boolean subtracted = false;
		if ( (ti.startFloat >= label[currentBin]) && (finshFloat <= label[currentBin]+binWidth) ) {
			subtractTimeFromBin(ti, currentBin);
			subtracted = true;
			return;
		}
		else {
			for (int i=0; i<numberOfBins; i++) {
				if ( (ti.startFloat >= label[i]) && (ti.finishFloat <= label[i]+binWidth) ) {
					currentBin = i;
					subtractTimeFromBin(ti, currentBin);
					subtracted = true;
					return;
					i = numberOfBins;
				}
			}
		}

		// we must have a "fencepost" interval
		if (!subtracted) {
			// find the interval of the start time
			int leftBin, rightBin;
			for (int i=0; i<numberOfBins; i++) {
				if ( (ti.startFloat >= label[i]) && (ti.startFloat <= label[i]+binWidth) ) {
					leftBin = i;
					rightBin = i+1;
					subtractTimeFromBin( (label[rightBin]-ti.startFloat), leftBin);
					subtractTimeFromBin( (ti.finishFloat-label[rightBin]), rightBin);
					subtracted = true;
					i = numberOfBins;
				}
			}
		}

		if (!subtracted) {
			System.out.println("Discarded time interval out of range");
		}
	}*/

	/** Use this after going through the dataset
	*
	*  This will turn the array data[] of counts, into
	*   the array data[] of fluxes.
	*
	*  Flux calcuation courtesty of Yuri Litvinenko
	*
	*  DEPRECATED - flux calcuation must come FIRST
	*/
	//public void calculateFluxes() {
	//	for (int i=0; i<data.length; i++) {
	//		data[i]









	// from the Histogram.java file again:
	/*public void addEvent(float evt) {
			boolean added = false;
			if ( (evt >= label[currentBin]) && (evt <= label[currentBin]+binWidth) ) {
				data[currentBin]++;
				added = true;
			}
			else {
				for (int i=0; i<numberOfBins; i++) {
					if ( (evt >= label[i]) && (evt <= label[i]+binWidth) ) {
						currentBin = i;
						data[currentBin]++;
						added = true;
						i = numberOfBins;
					}
				}
			}
			if (!added) {
				//System.out.println("couldn't add date to time profile : " + evt);
			}
		}
		*/
