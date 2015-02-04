import java.lang.Math;
import java.util.StringTokenizer;

/**  Utility class for transforming coordinates, etc.
*
*
*
*  (this is a general vector class, formulated for use in heliosphere)
*  (GSE not really implemented yet)
*
* Lukas Saul - Warsaw - July 2000
*  Last updated May, 2001
*/
public class HelioPoint {

	/*
	* These static ints are for discerning coordinate systems
	*/
	public static final int CARTESIAN = 1;
	public static final int SPHERICAL = 2;
	public static final int PLANAR = 3;
	public static final int GSE = 4;

	private double x,y,z;
	private double r,theta,phi;

	// phi=azimuth, theta=polar
	//private double xGSE, yGSE, zGSE;
	//private double psi, chi, ptheta;
	//private HelioPoint hp1, hp2, hp3;

	/*
	* these booleans are so we know when we need to calculate the conversion
	*/
	private boolean haveX, haveY, haveZ, haveR, havePhi, haveTheta;

	/**Default constructor - creates zero vector
	*/
	public HelioPoint() {
		haveX=true; haveY=true; haveZ=true; haveR=true; haveTheta=true; havePhi=true;
		x=0; y=0; z=0; r=0; theta=0; phi=0;
	}

	/**
	* constructor sets up SPHERICAL and CARTESIAN coordinates
	* CARTESIAN heliocentric coordinates. x axis = jan 1 00:00:00.
	*/
	public HelioPoint(int type, double a, double b, double c) {
		if (type == CARTESIAN) {
			haveX=true; haveY=true; haveZ=true; haveR=false; haveTheta=false; havePhi=false;
			x = a; y = b; z = c;
		}
		if (type == SPHERICAL) {
            if (c<0) System.out.println("WARNING: Theata < 0 ???????????????");
			haveX=false; haveY=false; haveZ=false; haveR=true; haveTheta=true; havePhi=true;
			r = a;	phi = b; theta = c;  //syntax=  give azimuth first!
		}
	}

	/**
	* Constructor to use when given an r and theta for a given plane (psi, chi)
	*  psi is the angle of the intersection in the xy plane to x axis
	*  chi is the closest angle of the plane to the z axis
	*/
	/*public HelioPoint(int type, double a, double b, double c, double d) {
		if (type  == PLANAR) {
			haveX=true; haveY=true; haveZ=true; haveR=true; haveTheta=false; havePhi=false;
			psi=a; chi=b, r=c; ptheta=d;
			hp1 = new HelioPoint( SPHERICAL,
	*/

	/**
	* Use this to save overhead by eliminating "new" statements, same as constructors
	*/
	public void setCoords(int type, double a, double b, double c) {
		if (type == CARTESIAN) {
			haveX=true; haveY=true; haveZ=true; haveR=false; haveTheta=false; havePhi=false;
			x = a; y = b; z = c;
		}
		if (type == SPHERICAL) {
			haveX=false; haveY=false; haveZ=false; haveR=true; haveTheta=true; havePhi=true;
			r = a;	phi = b; theta = c;  //syntax=  give azimuth first!
		}
	}

	/**
	* Use this to save overhead by eliminating "new" statements
	*/
	public void setCoords(HelioPoint hp) {
		haveX=true; haveY=true; haveZ=true; haveR=false; haveTheta=false; havePhi=false;
		x = hp.getX(); y = hp.getY(); z = hp.getZ();
	}

	/** Access methods - use these to get coordinates of this point
	*/
	public double getX() {
		if (haveX) return x;
		else {
			x = getR()*Math.cos(getPhi())*Math.sin(getTheta());
			haveX = true;
			return x;
		}
	}
	public double getY(){
		if (haveY) return y;
		else {
			y = getR()*Math.sin(getPhi())*Math.sin(getTheta());
			haveY = true;
			return y;
		}
	}
	public double getZ() {
		if (haveZ) return z;
		else {
			z = getR()*Math.cos(getTheta());
			haveZ = true;
			return z;
		}
	}
	public double getR() {
		if (haveR) return r;
		else {
			r = Math.sqrt(getX()*x + getY()*y + getZ()*z);
			haveR = true;
			return r;
		}
	}
	public double getPhi() {
		if (havePhi) return phi;
		else {
			// this stuff is because Math.atrig(arg) returns -pi/2<theta<pi/2 :
			// we want 0<phi<2pi
			if (getX()>0) phi = Math.atan(getY()/x);
			else if (x<0) phi = Math.atan(y/x) + Math.PI;
			else if (x==0 & y>0) phi = Math.PI/2;
			else if (x==0 & y<0) phi = 3*Math.PI/2;
			else if (x==0 & y==0) phi = 0;
			havePhi = true;
			if (phi<0) phi+=Math.PI*2;
			else if (phi>Math.PI*2) phi-=Math.PI*2;

			return phi;
		}
	}
	public double getTheta() {
		if (haveTheta) return theta;
		else if(getR()==0) { theta=0; haveTheta=true; return 0; }
		else {
			// we want theta>=0 & theta<= PI
			theta = Math.acos(z/getR());
			//else if (z<0) theta = Math.PI + Math.acos(z/r);
			//else if (z==0) theta = Math.PI/2;
			haveTheta = true;
			//if (theta<0) theta+=|(theta>Math.PI)) System.out.println("theta? " + theta);
			return theta;
		}
	}

	// some vector operations here:

	/**
	* Add two vectors easily
	*/
	public HelioPoint sum(HelioPoint hp) {
		return new HelioPoint( CARTESIAN,getX()+hp.getX(),getY()+hp.getY(),getZ()+hp.getZ() );
	}
	public static void sum(HelioPoint x, HelioPoint y) {
		x.setCoords(CARTESIAN, x.getX()+y.getX(), x.getY()+y.getY(), x.getZ()+y.getZ());
	}

	/**
	* Subtract two vectors easily
	*/
	public HelioPoint difference(HelioPoint hp) {
		return new HelioPoint( CARTESIAN,getX()-hp.getX(),getY()-hp.getY(),getZ()-hp.getZ() );
	}
	public static void difference(HelioPoint x, HelioPoint y) {
		x.setCoords( CARTESIAN, x.getX()-y.getY(), x.getY()-y.getY(), x.getZ()-y.getZ() );
	}

	/**
	* Multiply a vector by a scalar
	*/
	public HelioPoint product(double d) {
		return new HelioPoint( SPHERICAL, d*getR(), getPhi(), getTheta() );
	}
	public static void product(HelioPoint x, double d) {
		x.setCoords( SPHERICAL, d*x.getR(), x.getPhi(), x.getTheta() );
	}

	/**
	* returns vector of same size pointing in opposite direction
	*
	*/
	public HelioPoint invert() {
		return new HelioPoint(CARTESIAN, -getX(), -getY(), -getZ());
	}
	public static void invert(HelioPoint x) {
		x.setCoords( CARTESIAN, -x.getX(), -x.getY(), -x.getZ() );
	}

	/**
	* returns unit vector in same direction
	*
	*/
	public HelioPoint normalize() {
		return new HelioPoint(SPHERICAL, 1, getPhi(), getTheta());
	}
	public static void normalize(HelioPoint x) {
		x.setCoords( SPHERICAL, 1, x.getPhi(), x.getTheta() );
	}

	/**
	*  returns standard dot product
	*/
	public double dotProduct(HelioPoint hp) {
		return getX()*hp.getX() + getY()*hp.getY() + getZ()*hp.getZ();
	}

	/**
	*  returns standard cross product
	*/
	public HelioPoint crossProduct(HelioPoint hp) {
		return new HelioPoint( CARTESIAN,
					(getY()*hp.getZ() - getZ()*hp.getY()),
					(getZ()*hp.getX() - getX()*hp.getZ()),
					(getX()*hp.getY() - getY()*hp.getX())   );
	}
	public static void crossProduct(HelioPoint x, HelioPoint y) {
		x.setCoords( CARTESIAN,
					(x.getY()*y.getZ() - x.getZ()*y.getY()),
					(x.getZ()*y.getX() - x.getX()*y.getZ()),
					(x.getX()*y.getY() - x.getY()*y.getX())   );
	}

	/**
	*   this returns the angle between our vector and inflow as expressed
	*   with elevation and azimuth
	*    (Range = from 0 to PI)
	*/
	public double angleToInflow(double phi0,double theta0) {
		HelioPoint lism = new HelioPoint(HelioPoint.SPHERICAL, 1, phi0, theta0);
		return getAngle(lism);
	}

	/**
	* This calculates the angle between two vectors.
	* Should range from 0 to PI
	*
	*/
	public double getAngle(HelioPoint hp) {
		double cosAngle = this.dotProduct(hp)/(getR()*hp.getR());
		return Math.acos(cosAngle);
	}


	/**
	*  HOPEFULLY DEPRECATED BY NOW
	* these routines for converting to a cylindrical coordinate system
	* note: axis must be non-zero vector!
	*/
	public double cylCoordRad(HelioPoint axis) {
		double tbr = this.dotProduct(axis) / axis.getR(); // ref plane doesnt matter here
		return tbr;
	}

	public double cylCoordTan(HelioPoint axis) {
		return this.crossProduct(axis).getR() / axis.getR(); // lenght of cross product = Vtangent?
	}

	public double cylCoordPhi(HelioPoint axis, HelioPoint refPlaneVector) {
		// a bit trickier here...  using convention from Judge and Wu (sin Psi)
		HelioPoint newThis = moveZAxis(axis);
		HelioPoint newRefPlaneVector = moveZAxis(axis);
		return (newThis.getPhi() - newRefPlaneVector.getPhi() - Math.PI/2);
	}

	/**
	* this routine returns a new helioPoint expressed in terms of new z axis
	* lets try to take the cross product with current z axis and rotate around that.
	*/
	public HelioPoint moveZAxis(HelioPoint newAxis) {
		HelioPoint cp = new HelioPoint(CARTESIAN, getY(), -getX(), 0);
		cp = cp.normalize();
		return rotateAroundArbitraryAxis(cp, newAxis.getTheta()).invert();
	}

	/**
	*  This routine uses a matrix transformation to
	*   return a vector (HelioPoint) which has been rotated about
	*   the axis (axis) by some degree (t).
	*/
	public HelioPoint rotateAroundArbitraryAxis(HelioPoint axis, double t) {
		x=getX(); y=getY(); z=getZ(); // make sure we have coords we need
		double s = Math.sin(t);
		double c=Math.cos(t);// useful quantities
		double oc = 1-c ;  // this is unneccessary but I think I have an extra 16bits around somewhere
		double u = axis.getX();
		double v = axis.getY();
		double w = axis.getZ();

		// this is a matrix transformation
		return new HelioPoint(CARTESIAN,
				x*(c + u*u*oc) + y*(-w*s + u*v*oc) + z*(v*s + u*w*oc),  //x
				x*(w*s + u*v*oc) + y*(c + v*v*oc) + z*(-u*s + v*w*oc),  //y
				x*(-v*s + u*w*oc) + y*(u*s + v*w*oc) + z*(c + w*w*oc)   //z
					);
	}

	public String toString() {
		String tbr = new String("\n");
		tbr += "X = " + getX() + "\n";
		tbr += "Y = " + getY() + "\n";
		tbr += "Z = " + getZ() + "\n";
		tbr += "r = " + getR() + "\n";
		tbr += "phi = " + getPhi() + "\n";
		tbr += "theta = " + getTheta() + "\n";
		return tbr;
	}


	/**
	* for testing
	*/
	public static final void main(String[] args) {
		// let's test rotation routine
		/*HelioPoint hp1 = new HelioPoint(CARTESIAN, 0,0,1);
		HelioPoint hp2 = new HelioPoint(CARTESIAN, 1,0,0);
		HelioPoint hp3 = hp1.rotateAroundArbitraryAxis(hp2, Math.PI/2);
		System.out.println(hp3.getX()+" "+hp3.getY()+" "+hp3.getZ());

		// ok, let's test this cylindrical coordinate bullshit
		HelioPoint hp4 = new HelioPoint(CARTESIAN, 0,0,1);
		HelioPoint hp5 = new HelioPoint(CARTESIAN, -1,0,0);
		System.out.println(hp5.cylCoordRad(hp4)+"");
		System.out.println(hp5.cylCoordTan(hp4)+"");
		System.out.println(hp5.cylCoordPhi(hp4,new HelioPoint(CARTESIAN,1,0,0)));*/

		// testing our "angle to radial" for GSE theata and phi
		/*HelioPoint HPREF = new HelioPoint(HelioPoint.CARTESIAN, 1,0,0);
		float p = (float)(360-45);
		float t = (float)0.0;
		HelioPoint hp = new HelioPoint(SPHERICAL, 1,p*Math.PI/180 - Math.PI/2, t*Math.PI/180 + Math.PI/2);
		double angle = (float)hp.getAngle(HPREF);
		if (angle>Math.PI/2) {
			angle=Math.PI/2 - (angle-Math.PI/2);
		}
       	angle = angle*180/Math.PI; // convert to degrees
        System.out.println(angle + "");
        */

        // THIS IS FOR FIXING UP THE BMAG FILE
        StringTokenizer st;
        HelioPoint hp = new HelioPoint();
        HelioPoint HPREF = new HelioPoint(HelioPoint.CARTESIAN, -1, 0, 0);
        float bmag,btheta,bphi;
        bmag=0; btheta=0; bphi=0;
        file f = new file("MAGDATA_ascii.txt");
        file outf = new file("ANG.txt");
        f.initRead();
        outf.initWrite(false);
        String line = "";
        // throw out first line
        line = f.readLine();
        while ( (line=f.readLine()) != null) {
			st = new StringTokenizer(line,"\t");
			String word = st.nextToken();
			word = st.nextToken();
			bphi=Float.parseFloat(st.nextToken());
			btheta=Float.parseFloat(st.nextToken());
			bmag=Float.parseFloat(st.nextToken());
			hp.setCoords(HelioPoint.SPHERICAL, 1,bphi*Math.PI/180, btheta*Math.PI/180 + Math.PI/2);
            double angle = (float)hp.getAngle(HPREF);
            if (angle>Math.PI/2) {
                angle=Math.PI/2 - (angle-Math.PI/2);
            }
            angle = angle*180/Math.PI; // convert to degrees
            //System.out.println("Next line: " + angle+ " for: " + bphi + " " + btheta + " " + bmag);
			outf.write(angle+"\n");
		}
		f.closeRead();
		outf.closeWrite();
	}
}


		/*
		// this is probably not the way to go here....
		HelioPoint r1, r2, r3;
		o(getX() + " " + getY() + " " + getZ());
		// first change (spin) to new coordinate frame so newAxis is above x axis
		r1 = new HelioPoint(SPHERICAL, getR(), getPhi()-newAxis.getPhi(), getTheta());
		o(r1.getX() + " " + r1.getY() + " " + r1.getZ());
		// then rename axes so we can do another spin
		r2 = new HelioPoint(CARTESIAN, r1.getZ(), r1.getX(), -r1.getY());
		o(r2.getX() + " " + r2.getY() + " " + r2.getZ());
		// now do the spin so the new axis is along x
		r3 = new HelioPoint(SPHERICAL, getR(), r2.getPhi()-newAxis.getTheta(), r2.getTheta());
		o(r3.getX() + " " + r3.getY() + " " + r3.getZ());
		// finally rename axes so new axis (currently on x) is along z
		return new HelioPoint(CARTESIAN, -r3.getZ(), r3.getY(), r3.getX());*/
