import gsfc.nssdc.cdf.*;
import java.util.*;
import java.io.*;


/**
*  Let's make sure we have what we think we do in these binary files...
*
*
*  Lukas Saul, December 2002
*/
public class BinaryFileTester {
	private int numTests = 1000;
	private int numBetweenDates = 3;
	DataInputStream dis;


	public static int GOODS = (int)(8.20368000 * Math.pow(10,8)); // 1996 - (12/30/69 19:00:00)


	public BinaryFileTester(String filename) {
		File f = new File(filename);
		if (f.exists()) {
			try {
				dis = new DataInputStream(new FileInputStream(filename));
				o("using file : " + f.getName());
				for (int i=0; i<numTests; i++) {

					//  first point - get date for output file
					int u = dis.readInt();
					//o(u+" "+(float)u/10.0f/60.0f/60.0f/24.0f);
					o(u+" "+(float)(u-GOODS)/60.0f/60.0f/24.0f);
					for (int j=0; j<numBetweenDates; j++) {
						o(dis.readShort()+"");
					}
				}

			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public static void o(String s) {
		System.out.println(s);
	}

	public final static void main(String[] args) {
		BinaryFileTester bft = new BinaryFileTester(args[0]);
	}
}
