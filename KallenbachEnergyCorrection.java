import java.io.*;
import JSci.maths.*;
import java.util.Date;

/**
* This is used to change the fluxes due to energy callibration issues
*
*  See "Status of CTOF Instrument Functions", 1999 7th postlaunch workshop, CH
*    Ion-Optical transmission for pickup ions
*
*  this class -  lukas saul Mar 2003
*/
public class KallenbachEnergyCorrection {

	private static float a = 3.4632f;
	private static float b = -3.1948f;
	private static float c = 1.4095f;   // ax^2+bx+c is the fit

	//y = 3.4632x2 - 3.1948x + 1.4095
	//3.8961x2 - 3.4156x + 1.4351

	//3.4632x2 - 3.1948x + 1.4095

	private static float cutoff = 0.175f; // flux is unaffected below here (relative energy to 16kV)

	/**
	* Use this to get an efficiency for a given energy
	*
	*  Note that curve is calibrated to a maximal transmission coeff. of .55..
	*   thus we need to multiply by 1/.55
	*
	*/
	public static float efficiency(float e) {
		float relE = e/32;
		if (relE<cutoff) return 1.0f;
		else if (relE>.5) return 0.673f;
		else return (a*relE*relE + b*relE + c);
	}
}