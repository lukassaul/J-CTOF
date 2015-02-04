import java.util.StringTokenizer;
import java.io.*;

/**
*  Take SW data as ASCII and make a tidy BIN file, with rounded quantities
*
*  The output form: int, short, short, short
* the int is date in seconds
* the short is vsw (km/s)
* the short is n*10 (1/cm^3)
* the short is vth*10 (1/cm^3)
*/
public class SWDataFixer2 {

	public static final void main(String[] args) {
		try {String CRLF = System.getProperty("line.separator");

			DataOutputStream dos = new DataOutputStream(new FileOutputStream("windswdata.bin"));
			file inf = new file("wind_sme_96.dat");
			inf.initRead();
			String line = "";
			boolean eof = false;
			while (!eof) {
				if ((line = inf.readLine()) == null) eof = true;
				else {
					StringTokenizer st = new StringTokenizer(line);
					int date = Integer.parseInt(st.nextToken());
					float vswf = Float.parseFloat(st.nextToken());
					float nf = Float.parseFloat(st.nextToken());
					float vthf = Float.parseFloat(st.nextToken());

					int vsw = Math.round(vswf);
					int n = Math.round(nf*10);
					int vth = Math.round(vthf*10);

					dos.writeInt(date);
					dos.writeShort(vsw);
					dos.writeShort(n);
					dos.writeShort(vth);

				}
			}
			dos.close();
			inf.closeRead();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
}