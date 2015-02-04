
import java.util.*;
import flanagan.math.*;

/**
*  Lukas Saul, UNH Space Science Dept.
*  Curvefitting utilities here.
*
*    Adapted to use simplex method from flanagan java math libraries - March 2006
*
*
*  >
*   --  added step function and pulled out of package IJ -- lukas saul, 11/04
*
*   -- adding EXHAUSTIVE SEARCH ability..  for nonlinear fits
*
*
*   -- adding HEMISPHERIC -  analytic fit to hemispheric model w/ radial field
*      see Hemispheric.java for info
*
*   -- adding quadratic that passes through origin for PUI / Np correlation
*
*   -- adding SEMI - analytic fit assuming constant adiabatic acceleration
*      that uses legendre polynomials for pitch angle diffusion.  see SemiModel.java
*      March, 2006
*
*/
public class CurveFitter implements MinimisationFunction {


    public static final int STRAIGHT_LINE=0,POLY2=1,POLY3=2,POLY4=3,
    EXPONENTIAL=4,POWER=5,LOG=6,RODBARD=7,GAMMA_VARIATE=8,
    LOG2=9, STEP=10, HEMISPHERIC=11, HTEST=12, QUAD_ORIGIN=13, GAUSSIAN=14, SEMI=15;

    public static final String[] fitList = {"Straight Line","2nd Degree Polynomial",
    	"3rd Degree Polynomial", "4th Degree Polynomial","Exponential","Power",
    	"log","Rodbard", "Gamma Variate", "y = a+b*ln(x-c)", "Step Function",
    	"Hemispheric", "H-test", "Quadratic Through Origin","Gaussian","Semi"};

    public static final String[] fList = {"y = a+bx","y = a+bx+cx^2",
    	"y = a+bx+cx^2+dx^3", "y = a+bx+cx^2+dx^3+ex^4","y = a*exp(bx)","y = ax^b",
    	"y = a*ln(bx)", "y = d+(a-d)/(1+(x/c)^b)", "y = a*(x-b)^c*exp(-(x-b)/d)",
    	"y = a+b*ln(x-c)", "y = a*step(x-b)", "y=hem.f(a,x)",
    	"y = norm*step()..","y=ax+bx^2", "y=a*EXP(-(x-b)^2/c)","y=semi.f(a,b,x)"};

    private static final double root2 = 1.414214; // square root of 2

    private int fit;                // Number of curve type to fit
    private double[] xData, yData;  // x,y data to fit

	public double[] bestParams; // take this after doing exhaustive for the answer
	public Hemispheric hh;
	public SemiModel sm;

	public double[] start, step;
	public double ftol;

	public Minimisation min;

	/**
	* build it with floats and cast
	*/
    public CurveFitter (float[] xDat, float[] yDat) {
		xData = new double[xDat.length];
		yData = new double[yDat.length];
		for (int i=0; i<xData.length; i++) {
			xData[i]=(double)xDat[i];
			yData[i]=(double)yDat[i];
		}
		min = new Minimisation();
		//numPoints = xData.length;
	}


    /** Construct a new CurveFitter. */
    public CurveFitter (double[] xData, double[] yData) {
        this.xData = xData;
        this.yData = yData;
		min = new Minimisation();
        //numPoints = xData.length;
    }


	/**
	*      Perform curve fitting with the simplex method
	*/
    public void doFit(int fitType) {

		fit = fitType;
        if (fit == HEMISPHERIC) hh=new Hemispheric();
        if (fit == SEMI) sm = new SemiModel();

		// SIMPLEX INITIALIZTION
		initialize();
		double ftol = 1e-3;

		// Nelder and Mead minimisation procedure
		min.nelderMead(this, start, step, ftol,1000);

		// get the minimum value
		double minimum = min.getMinimum();

		// get values of y and z at minimum
		double[] param = min.getParamValues();
		bestParams = param;
		// Output the results to screen
		for (int i=0; i<param.length; i++) {
			System.out.println("param_" + i + " : " + param[i]);
		}
		System.out.println("Minimum = " + min.getMinimum());
        System.out.println("Error: " + getError(param));
        if (fit == SEMI) System.out.println("sm tests: "+ sm.counter + " "+ sm.counter/25);
    }



	/**
	*  Here we cannot check our error and move toward the next local minima in error..
	*
	*   we must compare every possible fit.
	*
	*  For now, only 2d fits here..
	*/
    public void doExhaustiveFit(int fitType, double[] param_limits, int[] steps) {

		fit = fitType;
        if (fit == HEMISPHERIC) hh=new Hemispheric();
        if (fit == SEMI) sm = new SemiModel();

		int numParams = getNumParams();
		if (numParams!=2)
			throw new IllegalArgumentException("Invalid fit type");

		double[] params = new double[numParams];
		double bestFit = Math.pow(10,100.0);
		bestParams = new double[numParams];
		double[] startParams = new double[numParams];
		double[] deltas = new double[numParams];

		for (int i=0; i<params.length; i++) {

			// set miminimum of space to search
			params[i]=param_limits[2*i];

			// find deltas of space to search
			deltas[i]=(param_limits[2*i+1]-param_limits[2*i])/steps[i];

			// set the best to the first for now
			bestParams[i]=params[i];
			startParams[i]=params[i];
		}

		System.out.println("deltas: " + deltas[0] + " " + deltas[1]);
		System.out.println("steps: " + steps[0] + " " + steps[1]);
		System.out.println("startParams: " + startParams[0] + " " + startParams[1]);
		//start testing them
		for (int i=0; i<steps[0]; i++) {
			for (int j=0; j<steps[1]; j++) {
				params[0]=startParams[0]+i*deltas[0];
				params[1]=startParams[1]+j*deltas[1];
				double test = getError(params);
			//	System.out.println("prms: " + params[0] + " " + params[1]+ " er: " + test);

				if (test<bestFit) {
					//System.out.println("found one: " + test + " i:" + params[0] + " j:" + params[1]);
					bestFit = test;
					bestParams[0] = params[0];
					bestParams[1] = params[1];
				}
			}
		}
	}


    /**
    *
    *Initialise the simplex
    *
    * Here we put starting point for search in parameter space
    */
    void initialize() {
	   start = new double[getNumParams()];
	   step = new double[getNumParams()];
	   java.util.Arrays.fill(step,1.0);

       double firstx = xData[0];
	   double firsty = yData[0];
	   double lastx = xData[xData.length-1];
	   double lasty = yData[xData.length-1];
	   double xmean = (firstx+lastx)/2.0;
	   double ymean = (firsty+lasty)/2.0;
	   double slope;
	   if ((lastx - firstx) != 0.0)
		   slope = (lasty - firsty)/(lastx - firstx);
	   else
		   slope = 1.0;
       double yintercept = firsty - slope * firstx;


       switch (fit) {
            case STRAIGHT_LINE:
                start[0] = yintercept;
                start[1] = slope;
                break;
            case POLY2:
                start[0] = yintercept;
                start[1] = slope;
                start[2] = 0.0;
                break;
            case POLY3:
                start[0] = yintercept;
                start[1] = slope;
                start[2] = 0.0;
                start[3] = 0.0;
                break;
            case POLY4:
                start[0] = yintercept;
                start[1] = slope;
                start[2] = 0.0;
                start[3] = 0.0;
                start[4] = 0.0;
                break;
            case EXPONENTIAL:
                start[0] = 0.1;
                start[1] = 0.01;
                break;
            case POWER:
                start[0] = 0.0;
                start[1] = 1.0;
                break;
            case LOG:
                start[0] = 0.5;
                start[1] = 0.05;
                break;
            case RODBARD:
                start[0] = firsty;
                start[1] = 1.0;
                start[2] = xmean;
                start[3] = lasty;
                break;
            case GAMMA_VARIATE:
                //  First guesses based on following observations:
                //  t0 [b] = time of first rise in gamma curve - so use the user specified first limit
                //  tm = t0 + a*B [c*d] where tm is the time of the peak of the curve
                //  therefore an estimate for a and B is sqrt(tm-t0)
                //  K [a] can now be calculated from these estimates
                start[0] = firstx;
                double ab = xData[getMax(yData)] - firstx;
                start[2] = Math.sqrt(ab);
                start[3] = Math.sqrt(ab);
                start[1] = yData[getMax(yData)] / (Math.pow(ab, start[2]) * Math.exp(-ab/start[3]));
                break;
            case LOG2:
                start[0] = 0.5;
                start[1] = 0.05;
                start[2] = 0.0;
                break;
            case STEP:
            	start[0] = yData[getMax(yData)];
            	start[1] = 2.0;
            	break;
            case HEMISPHERIC:
            	start[0] = 1.0;
            	start[1] = yData[getMax(yData)];
            	break;
            case QUAD_ORIGIN:
            	start[0] = yData[getMax(yData)]/2;
            	start[1] = 1.0;
            	break;
            case GAUSSIAN:
            	start[0] = yData[getMax(yData)];
            	start[1] = xData[getMax(yData)];
            	start[2] = Math.abs((lastx-firstx)/2);
            	System.out.println(""+start[0]+" "+start[1]+" "+start[2]);
            	break;
            case SEMI:
            	start[0] = 1.0E10;
            	start[1] = 0.000005;
            	//start[2] = 1.5;
            	step[0] = 1E9;
            	step[1] = 0.000001;
            	//step[2] = 1;
            	min.addConstraint(0,-1,0);
            	min.addConstraint(1,-1,0);
            	min.addConstraint(1,1,0.001);
            	//min.addConstraint(2,-1,0);
            	break;
        }
    }


    /** Get number of parameters for current fit function */
    public int getNumParams() {
        switch (fit) {
            case STRAIGHT_LINE: return 2;
            case POLY2: return 3;
            case POLY3: return 4;
            case POLY4: return 5;
            case EXPONENTIAL: return 2;
            case POWER: return 2;
            case LOG: return 2;
            case RODBARD: return 4;
            case GAMMA_VARIATE: return 4;
            case LOG2: return 3;
            case STEP: return 2;
            case HEMISPHERIC: return 2;
            case SEMI: return 2;
            case QUAD_ORIGIN: return 2;
            case GAUSSIAN: return 3;
        }
        return 0;
    }

    /**
    *Returns "fit" function value for parametres "p" at "x"
    *
    *  Define function to fit to here!!
    */
    public double f(int fit, double[] p, double x) {
        switch (fit) {
            case STRAIGHT_LINE:
                return p[0] + p[1]*x;
            case POLY2:
                return p[0] + p[1]*x + p[2]* x*x;
            case POLY3:
                return p[0] + p[1]*x + p[2]*x*x + p[3]*x*x*x;
            case POLY4:
                return p[0] + p[1]*x + p[2]*x*x + p[3]*x*x*x + p[4]*x*x*x*x;
            case EXPONENTIAL:
                return p[0]*Math.exp(p[1]*x);
            case POWER:
                if (x == 0.0)
                    return 0.0;
                else
                    return p[0]*Math.exp(p[1]*Math.log(x)); //y=ax^b
            case LOG:
                if (x == 0.0)
                    x = 0.5;
                return p[0]*Math.log(p[1]*x);
            case RODBARD:
                double ex;
                if (x == 0.0)
                    ex = 0.0;
                else
                    ex = Math.exp(Math.log(x/p[2])*p[1]);
                double y = p[0]-p[3];
                y = y/(1.0+ex);
                return y+p[3];
            case GAMMA_VARIATE:
                if (p[0] >= x) return 0.0;
                if (p[1] <= 0) return -100000.0;
                if (p[2] <= 0) return -100000.0;
                if (p[3] <= 0) return -100000.0;

                double pw = Math.pow((x - p[0]), p[2]);
                double e = Math.exp((-(x - p[0]))/p[3]);
                return p[1]*pw*e;
            case LOG2:
                double tmp = x-p[2];
                if (tmp<0.001) tmp = 0.001;
                return p[0]+p[1]*Math.log(tmp);
            case STEP:
            	if (x>p[1]) return 0.0;
            	else return p[0];
            case HEMISPHERIC:
            	return hh.eflux(p[0],p[1],x);
            case SEMI:
            	return sm.f(p[0],p[1],x);
            	//return sm.f(p[0],p[1],p[2],x);
            case QUAD_ORIGIN:
            	return p[0]*x + p[1]*x*x;
            case GAUSSIAN:
            	return p[0]/p[2]/Math.sqrt(Math.PI*2)*Math.exp(-(x-p[1])*(x-p[1])/p[2]/p[2]/2);
            default:
                return 0.0;
        }
    }



	/** Returns sum of squares of residuals */
	public double getError(double[] params_) {
		double tbr = 0.0;
		for (int i = 0; i < xData.length; i++) {
			tbr += sqr(yData[i] - f(fit, params_, xData[i]));
		}
		//System.out.println("error: " + tbr);
		return tbr;
    }

    /** Here's the one to minimize!!  */
    public double function(double[] params) {
		return getError(params);
	}

    public static double sqr(double d) { return d * d; }


    /**
	* Gets index of highest value in an array.
	*
	* @param              Double array.
	* @return             Index of highest value.
	*/
	public static int getMax(double[] array) {
		double max = array[0];
		int index = 0;
		for(int i = 1; i < array.length; i++) {
			if(max < array[i]) {
				max = array[i];
				index = i;
			}
		}
		return index;
    }


    /**
    *  Use this to fit a curve or
    * for testing...
    *
    *  Reads a file for data and fits to a curve at command line
    *
    */
    public static final void main(String[] args) {
		int numLines = 25;
		int linesToSkip = 0;

		//double[] x = {0,1,2,3,4,5,6,7,8,9};
		//double[] y = {1,3,5,7,9,11,13,15,17,19};
		//CurveFitter cf = new CurveFitter(x,y);
		//cf.doFit(CurveFitter.STRAIGHT_LINE);

		file f = new file(args[0]);
		f.initRead();
		double[] x = new double[numLines];
		double[] y = new double[numLines];
		String line = "";

		for (int i=0; i<linesToSkip; i++) {
			line = f.readLine();
		}

		for (int i=0; i<numLines; i++) {
			line = f.readLine();
			StringTokenizer st = new StringTokenizer(line);
			x[i]=Double.parseDouble(st.nextToken());
			x[i]-=1.0; // IMPORTANT _ SHIFT BY 1.0 TO MOVE TO HELIOCENTRIC FRAME!!
			y[i]=Double.parseDouble(st.nextToken());
			System.out.println("row: " + i + " x: " + x[i] + " y: " + y[i]);
		}
		f.closeRead();

		CurveFitter cf = new CurveFitter(x,y);
		cf.doFit(CurveFitter.SEMI);


		//double[] lims = {1E6,1E10, 0.00000001, 0.0001};
		//int[] steps = {4,4};
		//cf.doExhaustiveFit(CurveFitter.SEMI,lims,steps);
		//System.out.println("param[0]: " + cf.bestParams[0]);
		//System.out.println("param[1]: " + cf.bestParams[1]);


		//cf.setRestarts(100);
		//Date d1 = new Date();
		//cf.doFit(type);
		//Date d2 = new Date();

		//System.out.println(cf.getResultString()+"\n\n");
		//System.out.println("param[0]: " + cf.getParams()[0]);
		//System.out.println("param[1]: " + cf.getParams()[1]);
		//System.out.println("took: " + (d2.getTime()-d1.getTime()));


		//int max = getMax(y);
		//System.out.println("y_max: " + y[max]);
		//double[] lims = {0.0 , 10.0, y[max]/2.0, 4*y[max]};
		//System.out.println("lims: " + lims[0] + " " + lims[1] + " " + lims[2] + " " + lims[3]);
		//int[] steps = {256,128};
		//Date d3 = new Date();
		//cf.doExhaustiveFit(CurveFitter.QUAD_ORIGIN,lims,steps);
		//cf.doFit(type);
		//Date d4 = new Date();
		//double[] ans = cf.bestParams;
		//System.out.println("a[0]: " + ans[0]);
		//System.out.println("a[1]: " + ans[1]);
		//System.out.println(cf.getResultString());
		//System.out.println("took: " + (d4.getTime()-d3.getTime()));

	}

}

