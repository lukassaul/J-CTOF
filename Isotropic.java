import java.util.*;

/**
* Use this to model PUI distribution & observed eflux a la
*
*  Vasilyunas & Siscoe..
*
*   here consider only due upwind direction.
*
*/
public class Isotropic {

	public static double AU  = 1.49598* Math.pow(10,11); //meters
	public static double NaN = Double.NaN;
	public static double MAX = Double.MAX_VALUE;
	public static double U   = 440*1000;
	public static double N0  = Math.pow(10,-6);
	public static double G = 6.673 * Math.pow(10,-11);  // m^3/s^2/kg
	public static double Ms = 1.98892 * Math.pow(10,30);  // kg
	public static double PI = Math.PI;

	public static double beta = 3*Math.pow(10,-8);


	/**
	* THe velocity of a cold interstellar particle at earth.  Calculate in constructor.
	*/
	public double v_earth;
	public static double v_infinity = 27500.0;  //m/s

	//Integration ig;

	public Isotropic() {
		v_earth = Math.sqrt(2*G*Ms/AU + v_infinity*v_infinity);
		System.out.println("v_earth: " + v_earth);
	}

	/**
	*calculate EFlux at r in w = v/ v_sw
	*/
	public double eflux(final double norm, final double w2) {
		//gamma = gg;
		return f(norm,w2)*Math.pow((w2),-0.5);
	}


	/**
	*  The main portion of this class..
	*    here we calculate the f+ distribution analytically
	*/
	public double f(final double norm, final double w) {
		if (w>=1.0) return 0.0;

		// we are assuming isotropic, ideal adiabatic cooling
		// e.g. v/vsw = (r0/r)^-2/3

		//rOfv = AU*Math.pow(w,1.5);
		return norm*N(AU*Math.pow(w,1.5))*Math.pow(w,3.0);

	}



	/**
	* The model of interstellar neutral density..
	*
	*   based on no inflow, e.g. no angulage dependence just
	*   ionization depletion.
	*/
	/*private static double N(double r) {
		double A = 4.0*AU; // one parameter model
		return N0*Math.exp(-A/r);
	}*/


	/**
	* The model of interstellar neutral density..
	*
	*   based on cold model, due upwind..
	*   see Moebius SW&CR homework set #4!
	*/
	private double N(double r) {

		return N0*Math.exp(-beta*AU*AU*Math.sqrt(v_infinity*v_infinity+2*G*Ms/r)/G/Ms);

		//return N0*Math.exp(-2*beta*AU*AU/r/v_infinity);
	}


	/**
	* For Testing
	*/
	public static final void main(String[] args) {
		file q;
		System.out.println(""+2*beta*AU*AU/v_infinity/AU);
		System.out.println(""+beta*AU*AU*Math.sqrt(v_infinity*v_infinity+2*G*Ms/AU)/G/Ms);


		Isotropic h = new Isotropic();
		q = new file("isoTest.dat");
		q.initWrite(false);
		for (double w=0.01; w<1.01; w+=.01) {
			try {
				//q.write((w+1.0)+"\t"+h.eflux(a,b,w)+"\n");
				q.write((w+1.0)+"\t"+h.eflux(1.0,w)+"\n");
				//q.write(w+"\t"+h.N(AU*w)+"\n");
			} catch (Exception e) {e.printStackTrace();}
		}
		q.closeWrite();
	}

}

