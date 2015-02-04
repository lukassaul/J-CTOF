
import java.util.StringTokenizer;
public class MagDataFixer1 {

	public static final void main(String[] args) {
		String CRLF = System.getProperty("line.separator");
		file of = new file("magdata.dat");
		file inf;
		String line = "";
		of.initWrite(false);
		int i = 79;
		while (i<230) {
			if (i<100) inf = new file("bext960"+ i + ".bxt");
			else inf = new file("bext96" + i + ".bxt");
			if (inf.exists()) {
				inf.initRead();
				for (int j=0; j<10; j++) line = inf.readLine();
				boolean eof = false;
				while (!eof) {
					if ((line = inf.readLine()) == null) eof = true;
					else {
						StringTokenizer st = new StringTokenizer(line);
						of.write(st.nextToken() + "\t" +
								st.nextToken() + "\t" +
								st.nextToken() + "\t" +
								st.nextToken() + "\t" + CRLF);
						line = inf.readLine();
					}
				}
				inf.closeRead();
			}
			i++;
		}
		of.closeWrite();

	}
}
