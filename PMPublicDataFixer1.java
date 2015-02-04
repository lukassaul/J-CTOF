import java.util.StringTokenizer;
public class PMPublicDataFixer1 {

	public static final void main(String[] args) {
		String CRLF = System.getProperty("line.separator");
		file of = new file("pm_pub_data.dat");
		file inf;
		String line = "";
		String trash = "";
		float hr_frac=1/24;
		of.initWrite(false);
		int i = 1905;
		while (i<1916) {
			//if (i<100) inf = new file("p960"+i+".pmv");
			inf = new file("CRN_"+i+".htm");
			if (inf.exists()) {
				inf.initRead();
				for (int j=0; j<28; j++) line = inf.readLine();

				boolean eof = false;
				while (!eof) {
					if ((line = inf.readLine()) == null) eof = true;
					else {
						StringTokenizer st = new StringTokenizer(line," \t\n\r\f:");
						trash = st.nextToken();
						if (trash.indexOf("<")!=-1) break;
						trash = st.nextToken();
						trash = st.nextToken();
						// now DOY with colons
						int doy = Integer.parseInt(st.nextToken());
						int hr = Integer.parseInt(st.nextToken());
						float f_doy = doy+hr*hr_frac;
						trash = st.nextToken();
						trash = st.nextToken();
						int v = Integer.parseInt(st.nextToken());
						float n = Float.parseFloat(st.nextToken());
						int vth = Integer.parseInt(st.nextToken());

						of.write(f_doy + "\t" +
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
