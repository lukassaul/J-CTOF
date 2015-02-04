import gsfc.nssdc.cdf.*;
import java.util.*;
import java.io.*;


/**
*  Let's make sure we have what we think we do in these binary files...
*    this time by machine test (as opposed to screen display)
*
*  Lukas Saul, April 2003
*/
public class PHABinaryFileChecker {
	public static int GOODS = (int)(8.20368000 * Math.pow(10,8)); // 1996 - (12/30/69 19:00:00)
	DataInputStream dis;
	int debug;
	int lastDate=0;

	public PHABinaryFileChecker(String filename) {
		debug = 0;
		float[] fluxes = new float[62];
		int[] stats = new int[62];
		File f = new File(filename);
		if (f.exists()) {
			try {
				dis = new DataInputStream(new FileInputStream(filename));
				o("using file : " + f.getName());
				while (true) {

					//  first point - get date for output file
					int u = dis.readInt();
					float date = (float)(u-GOODS)/60.0f/60.0f/24.0f;
					//float date=(float)u/10.0f/60.0f/60.0f/24.0f;
					for(int i=0; i<62; i++) {
						fluxes[i]=dis.readFloat();
						stats[i]=dis.readInt();
					}

					if (lastDate>u) {
						o("BIG PROBLEMS: " + lastDate + u);
					}
					lastDate = u;

					if (debug < 5) {
						o("\n\n"+ u + " " + date);
						for (int i=0; i<62; i++) {
							o(fluxes[i] + " " + stats[i]);
						}
					}
					debug++;
					//if (!check(a) | !check(b) | !check(c)) {
					//	System.out.println("probs: "+date+" "+a+" "+b+" "+c);
					//}
				}
			}
			catch (Exception e) {e.printStackTrace();}
		}
	}

	private static boolean check(float f) {
		if (f>200|f<-200)return false;
		else return true;
	}

	public static void o(String s) {
		System.out.println(s);
	}

	public final static void main(String[] args) {
		PHABinaryFileChecker bft = new PHABinaryFileChecker(args[0]);
	}
}
