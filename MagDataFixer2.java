import java.util.StringTokenizer;
import java.io.*;

/** The output form: int, short, short, short
* the int is date in seconds
* the short is phi*10
* the short is theta*10
* the short is mag*10
*/
public class MagDataFixer2 {

	public static final void main(String[] args) {
		try {String CRLF = System.getProperty("line.separator");
			DataOutputStream dos = new DataOutputStream(new FileOutputStream("magdata2.bin"));
			file inf = new file("magdata.dat");
			inf.initRead();
			String line = "";
			boolean eof = false;
			while (!eof) {
				if ((line = inf.readLine()) == null) eof = true;
				else {
					StringTokenizer st = new StringTokenizer(line);
					int date = Integer.parseInt(st.nextToken());
					float phif = Float.parseFloat(st.nextToken());
					float thef = Float.parseFloat(st.nextToken());
					float magf = Float.parseFloat(st.nextToken());

					int phi = Math.round(phif*10);
					int the = Math.round(thef*10);
					int mag = Math.round(magf*10);

					dos.writeInt(date);
					dos.writeShort(phi);
					dos.writeShort(the);
					dos.writeShort(mag);

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