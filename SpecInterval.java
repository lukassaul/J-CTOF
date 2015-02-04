/**
*  Wrapper for selection interval in B Spectra
*
*  Add to this as we need to select spectral charachteristics
*    i.e. spectral index, single frequency component, etc.
*
*  Saul Feb. 2003
*
*  May 2 2003 -  added fit parameters intercept and index
*
*  June 10 2004 !   (has it really been over 1 year? )  added energy diffusion index
*
*  here are the frequencies by bin
*			0 - 0.0
*			1 - 0.0026041666666666665
*			2 - 0.005208333333333333
*			3 - 0.0078125
*			4 - 0.010416666666666666
*			5 - 0.013020833333333334
*			6 - 0.015625
*			7 - 0.018229166666666668
*			8 - 0.020833333333333332
*			9 - 0.0234375
*			10 - 0.026041666666666668
*			11 - 0.028645833333333332
*			12 - 0.03125
*			13 - 0.033854166666666664
*			14 - 0.036458333333333336
*			15 - 0.0390625
*			16 - 0.041666666666666664
*			17 - 0.044270833333333336
*			18 - 0.046875
*			19 - 0.049479166666666664
*			20 - 0.052083333333333336
*			21 - 0.0546875
*			22 - 0.057291666666666664
*			23 - 0.059895833333333336
*			24 - 0.0625
*			25 - 0.06510416666666667
*			26 - 0.06770833333333333
*			27 - 0.0703125
*			28 - 0.07291666666666667
*			29 - 0.07552083333333333
*			30 - 0.078125
*			31 - 0.08072916666666667
*			32 - 0.08333333333333333
*			33 - 0.0859375
*			34 - 0.08854166666666667
*			35 - 0.09114583333333333
*			36 - 0.09375
*			37 - 0.09635416666666667
*			38 - 0.09895833333333333
*			39 - 0.1015625
*			40 - 0.10416666666666667
*			41 - 0.10677083333333333
*			42 - 0.109375
*			43 - 0.11197916666666667
*			44 - 0.11458333333333333
*			45 - 0.1171875
*			46 - 0.11979166666666667
*			47 - 0.12239583333333333
*			48 - 0.125
*			49 - 0.12760416666666666
*			50 - 0.13020833333333334
*			51 - 0.1328125
*			52 - 0.13541666666666666
*			53 - 0.13802083333333334
*			54 - 0.140625
*			55 - 0.14322916666666666
*			56 - 0.14583333333333334
*			57 - 0.1484375
*			58 - 0.15104166666666666
*			59 - 0.15364583333333334
*			60 - 0.15625
*			61 - 0.15885416666666666
*			62 - 0.16145833333333334
*			63 - 0.1640625
*			64 - 0.16666666666666666
*/

public class SpecInterval {

	private int divStartBin, divFinishBin, curlStartBin, curlFinishBin;
	private float divMin, divMax, curlMin, curlMax;
	private float parIntMin, parIntMax, traceIntMin, traceIntMax;
	private float parIndMin, parIndMax, traceIndMin, traceIndMax;
	private float ratioMin, ratioMax;
	private boolean doDiv, doCurl;
	public int numberOfTries;

	private float d_vvMin, d_vvMax;
	private boolean doD_vv;

	private static float MIN = -Float.MAX_VALUE;
	private static float MAX = Float.MAX_VALUE;


	// ok, we'll use log base 10
	public static double fudge = Math.log(10.0);


	/**
	* Initialize an all-encompassing specInterval (i.e. let all through)
	*
	*/
	public SpecInterval() {
		numberOfTries = 0;
		divStartBin = 0;
		divFinishBin = 65;
		curlStartBin = 0;
		curlFinishBin=65;
		divMin=MIN;
		divMax=MAX;
		curlMin=MIN;
		curlMax=MAX;
		parIntMin = MIN;
		parIntMax = MAX;
		traceIntMin = MIN;
		traceIntMax = MAX;
		parIndMin = MIN;
		parIndMax = MAX;
		traceIndMin = MIN;
		traceIndMax = MAX;
		ratioMin = MIN;
		ratioMax = MAX;
		d_vvMin = MIN;
		d_vvMax = MAX;
		doDiv = false;
		doCurl = false;
		doD_vv = false;
	}


	/**
	* set a limit to divergence wave power coefficients in this interval
	*/
	public void setDivInterval(int minbin, int maxbin, float i1, float i2) {
		divStartBin = minbin;
		divFinishBin = maxbin;
		divMin = i1;
		divMax = i2;
		doDiv = true;
	}

	/**
	* set a limit to curl wave power coefficients in this interval
	*/
	public void setCurlInterval(int minbin, int maxbin, float i1, float i2) {
		curlStartBin = minbin;
		curlFinishBin = maxbin;
		curlMin = i1;
		curlMax = i2;
		doCurl = true;
	}

	/**
	* set PAR component intercept interval
	*/
	public void setParInterceptInterval(float i1, float i2) {
		parIntMin = i1; parIntMax = i2;
	}

	/**
	* set PAR component index interval
	*/
	public void setParIndexInterval(float i1, float i2) {
		parIndMin = i1; parIndMax = i2;
	}

	/**
	* set TRACE component intercept interval
	*/
	public void setTraceInterceptInterval(float i1, float i2) {
		traceIntMin = i1; traceIntMax = i2;
	}

	/**
	* set TRACE component index interval
	*/
	public void setTraceIndexInterval(float i1, float i2) {
		traceIndMin = i1; traceIndMax = i2;
	}

	/**
	* set RATIO component index interval
	*/
	public void setRatioInterval(float i1, float i2) {
		ratioMin = i1; ratioMax = i2;
	}

	/**
	* set D_VV  component index interval
	*/
	public void setD_vvInterval(float i1, float i2) {
		d_vvMin = i1; d_vvMax = i2;
		doD_vv = true;
	}




	/**
	*
	*  Send in the spectral data and see if it's valid
	*  DEPRECATED
    */
	public boolean check(float[] divs, float[] curls, float np, float bmag) {
		System.out.println("DEPRECATED SPEC CHECKER - WARNING");
		numberOfTries++;
		if (doDiv) {
			float val = 0.0f;
			int num = 0;
			for (int i=divStartBin; i<=divFinishBin; i++) {
				val+=divs[i];
				num++;
			}
			val=val/num;
			if (val>=divMin & val<=divMax) return true;
			else return false;
		}

		if (doCurl) {
			float val = 0.0f;
			int num = 0;
			for (int i=curlStartBin; i<=curlFinishBin; i++) {
				val+=curls[i];
				num++;
			}
			val=val/num;
			if (val>=curlMin & val<=curlMax) return true;
			else return false;
		}

		return true;

	}

	/**
	* Send in a spectral fit and see if it's valid
	*
	*/
	public boolean check(float parInt, float parInd, float traceInt, float traceInd,
						 float np, float bmag) {
		float pInt1 = (float)(parInt+parInd*Math.log(0.1)/fudge);
		if (pInt1 > parIntMax | pInt1 < parIntMin) return false;
		if (parInd > parIndMax | parInd < parIndMin) return false;

		float tInt1 = (float)(traceInt+traceInd*Math.log(0.1)/fudge);
		if (tInt1 > traceIntMax | tInt1 < traceIntMin) return false;
		if (traceInd > traceIndMax | traceInd < traceIndMin) return false;
		float rat = (float)( (parInt+parInd*Math.log(0.1)/fudge) /
						(traceInt+traceInd*Math.log(0.1)/fudge) );
		if (rat > ratioMax | rat < ratioMin) return false;

		if (doD_vv) {
			float val = 0.0f;
			// **  HERE's THE DEFINITION OF THE DIFFUSION INDEX ** //
			val = bmag*(float)Math.exp(traceInt)/np;
			if (val > d_vvMax | val < d_vvMin) return false;
		}

		return true;
	}
}