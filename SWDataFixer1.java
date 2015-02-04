import java.util.StringTokenizer;

/**
* Read SW data from small files
*  and creates a single ASCII file with the data we want
*
*/
public class SWDataFixer1 {

	public static final void main(String[] args) {
		String CRLF = System.getProperty("line.separator");
		file of = new file("swdata.dat");
		file inf;
		String line = "";
		String trash = "";
		of.initWrite(false);
		int i = 81;
		while (i<230) {
			if (i<100) inf = new file("p960"+i+".pmv");
			else inf = new file("p96"+i+".pmv");
			if (inf.exists()) {
				inf.initRead();
				for (int j=0; j<10; j++) line = inf.readLine();

				boolean eof = false;
				while (!eof) {
					if ((line = inf.readLine()) == null) eof = true;
					else {
						StringTokenizer st = new StringTokenizer(line);
						String date = st.nextToken();
						trash = st.nextToken();
						trash = st.nextToken();
						String v = st.nextToken();
						trash = st.nextToken();
						String n = st.nextToken();
						trash = st.nextToken();
						String vth = st.nextToken();

						of.write(date + "\t" +
								v + "\t" +
								n + "\t" +
								vth + "\t" + CRLF);
					}
				}
				inf.closeRead();
			}
			i++;
		}
		of.closeWrite();

	}
}
