import java.util.StringTokenizer;
import java.io.*;

/**
* This takes the ASCII output of PHADataFixer1 and creates a binary data file of events
*
* The output form: int, short
* the int is date in seconds
* the short is e/q bin
*/
public class PHADataFixer2 {

	public static final void main(String[] args) {
		try {
			int numberWithoutSpeed = 0;
			int numberOutsideRange = 0;
			int numberOfEvents = 0;
			int numberAbove4 = 0;
			String CRLF = System.getProperty("line.separator");
			SwReader swr = new SwReader();

			DataOutputStream dos = new DataOutputStream(new FileOutputStream("phadata17.bin"));
			file inf = new file("phadata.dat");
			inf.initRead();
			String line = "";
			boolean eof = false;
			while (!eof) {
				if ((line = inf.readLine()) == null) eof = true;
				else {
					StringTokenizer st = new StringTokenizer(line);
					int date = Integer.parseInt(st.nextToken());
					int eq = Integer.parseInt(st.nextToken());

					int vsw = swr.getV(date);
					//o(vsw+"");
					if (vsw == -1) {
						numberWithoutSpeed++;
					}
					else {
						// these formulas are from "matrix rates" from ctof
						//o(eq+"");
						double eqMax = 34.673 * Math.pow(105, (double)(0 - eq)/(double)116);
						double eqMin = 34.673 * Math.pow(105, (double)(1 - eq)/(double)116);
						//o(eqMax + " " + eqMin);
						// this one is just E/q = .5mv^2 (m = 4 amu)
						double vMaxd = Math.sqrt(eqMax*4.8)*100;
						double vMind = Math.sqrt(eqMin*4.8)*100;
						//o(vMaxd + " " + vMind);
						double vvMax = vMaxd/vsw;
						double vvMin = vMind/vsw;
						//o(vvMax + " " + vvMin);
						if ( (vvMin >= (double)1.7) && (vvMax <= 4) ) {
							dos.writeInt(date);
							dos.writeShort(eq);
							numberOfEvents++;
						}else {
							if (vvMin < (double)1.7) numberAbove4++;
							numberOutsideRange++;
						}
					}
				}
			}
			dos.close();
			inf.closeRead();
			o("events without vsw: " + numberWithoutSpeed);
			o("events outside range: " + numberOutsideRange);
			o("events below 1.7: " + numberAbove4);
			o("total good events: " + numberOfEvents);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void o(String s) {
		System.out.println(s);
	}
}