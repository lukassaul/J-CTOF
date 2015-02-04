import gsfc.nssdc.cdf.*;
import java.util.*;
import java.io.*;


/**
*  Let's make sure we have what we think we do in these binary files...
*    this time by machine test (as opposed to screen display)
*
*  Lukas Saul, April 2003
*/
public class BinaryFileChecker {
	DataInputStream dis;
	public BinaryFileChecker(String filename) {
		File f = new File(filename);
		if (f.exists()) {
			try {
				dis = new DataInputStream(new FileInputStream(filename));
				o("using file : " + f.getName());
				while (true) {

					//  first point - get date for output file
					int u = dis.readInt();
					float date=(float)u/10.0f/60.0f/60.0f/24.0f;
					float a =dis.readInt()/1000;
					float b =dis.readInt()/1000;
					float c =dis.readInt()/1000;

					if (!check(a) | !check(b) | !check(c)) {
						System.out.println("probs: "+date+" "+a+" "+b+" "+c);
					}
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
		BinaryFileChecker bft = new BinaryFileChecker(args[0]);
	}
}
