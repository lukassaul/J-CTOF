import java.util.StringTokenizer;
import java.io.*;

/** The output form: int, short, short, short
* the int is date in seconds
* the short is phi*10
* the short is theta*10
* the short is mag*10
*/
public class MagFloatDataFixer {

	public static final void main(String[] args) {
		try {String CRLF = System.getProperty("line.separator");
			DataOutputStream dos = new DataOutputStream(new FileOutputStream("magdatafloat.bin"));
			file inf = new file("SLIDING_MAGDATA.dat");
			inf.initRead();
			String line = "";
			boolean eof = false;
			while (!eof) {
				if ((line = inf.readLine()) == null) eof = true;
				else {
					StringTokenizer st = new StringTokenizer(line);
					int date = Integer.parseInt(st.nextToken());
					float magf = Float.parseFloat(st.nextToken());
					float etaf = Float.parseFloat(st.nextToken());

					//int eta = Math.round(etaf*100);
					int mag = Math.round(magf*10);

					dos.writeInt(date);
					dos.writeShort(mag);
					dos.writeFloat(etaf);

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