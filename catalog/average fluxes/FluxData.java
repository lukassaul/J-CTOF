/**
* A nice wrapper for some flux data over a time period
*
*  startBin is the first bin, i.e. we don't start at 0
*   because often flux(0) = 0
*
*  Lukas Saul 2001
*/
public class FluxData {
	public int date;
	public int startBin;
	public int numBins;
	public int[] counts;
	public float[] fluxes;
	public FluxData (int d, int s, int n, float[] f, int[] c) {
		date =d;
		startBin =s;
		numBins =n;
		fluxes = f;
		counts = c;
	}
	public int size() {
		return numBins*4+8;
	}
}

