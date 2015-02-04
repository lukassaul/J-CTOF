import java.util.StringTokenizer;

/**
* This class parses data from .cph files, SOHO CTOF PHA level 1 data.
*
*  It generates an ASCII file with all m/q between 3 and 5, for each event giving
*  only the time and the energy bin.
*
*/
public class PHADataFixer1 {

	public static final void main(String[] args) {
		String CRLF = System.getProperty("line.separator");
		file of = new file("phadata.dat");
		file inf;
		String line = "";
		String trash = "";
		of.initWrite(false);
		int i = 81;
		while (i<230) {
			if (i<100) inf = new file("sw_m2_0"+ i + "_0" + i + "_03eq.cph");
			else inf = new file("sw_m2_"+ i + "_" + i + "_03eq.cph");
			if (inf.exists()) {
				inf.initRead();
				line = inf.readLine();

				boolean eof = false;
				while (!eof) {
					if ((line = inf.readLine()) == null) eof = true;
					else if (line.length() < 2) eof = true;
					else {
						boolean addIt = false;
						//System.out.println(line);
						StringTokenizer st = new StringTokenizer(line);
						String date = st.nextToken();
						if (date.charAt(date.length()-1)==',')
							date = date.substring(0,date.length()-1);
						trash = st.nextToken();
						String eq = st.nextToken();
						if (eq.charAt(eq.length()-1)==',')
							eq = eq.substring(0,eq.length()-1);
						trash = st.nextToken();
						trash = st.nextToken();
						trash = st.nextToken();
						trash = st.nextToken();
						trash = st.nextToken();
						trash = st.nextToken();
						trash = st.nextToken();
						String mq = st.nextToken();
						if (mq.charAt(mq.length()-1)==',')
							mq = mq.substring(0,mq.length()-1);
						float mqf = Float.parseFloat(mq);
						if ( (mqf > 3) && (mqf < 5) ) addIt = true;

						if (addIt) of.write(date + "\t" + eq +  CRLF);
					}
				}
				inf.closeRead();
			}
			i++;
		}
		of.closeWrite();

	}
}
