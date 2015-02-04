import java.util.StringTokenizer;
import java.util.Date;

public class ShockReader {

	public static int  NO_SHOCK = -1;
	public static int  NOT_A_SHOCK = 0; // ??  Some event  I guess... we'll chuck it
	public static int  WEAK_SHOCK = 1;
	public static int  MEDIUM_SHOCK = 2;
	public static int  STRONG_SHOCK = 3; // actually none of these!

	public static int preShockTime = 100;
	public static int postShockTime = 1000;

	private file f;
	private String line;
	private StringTokenizer st;
	private file logFile;
	private MyDate2 md2;

	private int currentShock, eventsInShock;
	private TimeInterval currentTI;

	public int numberOfTries, numberOfHits;

	public ShockReader() {

		md2 = new MyDate2();
		logFile = new file("shockReaderLog.txt");
		f = new file("shocklist.dat");
		f.initRead();
		line = "";
		loadNext();
		eventsInShock = 0;
		numberOfTries = 0;
		numberOfHits = 0;
	}


	public int getShock(int date) {
		numberOfTries++;
		if (currentTI.check(date)) {
			numberOfHits++;
			eventsInShock++;
			return currentShock;
		}
		else if (date < currentTI.startDate) return -1;
		else {
			while (loadNext()) {
				if (currentTI.check(date)) {
					numberOfHits++;
					eventsInShock++;
					return currentShock;
				}
				else if (date < currentTI.startDate) return -1;
			}
		}
		return -1;
	}

	private boolean loadNext() {
		if ((line = f.readLine()) != null) {
			st = new StringTokenizer(line);
			Date d = md2.parse(st.nextToken() + " " + st.nextToken());
			int date = (int)(d.getTime() / 1000);
			currentTI = new TimeInterval(date-preShockTime, date+postShockTime);
			currentShock = Integer.parseInt(st.nextToken());
			return true;
		}
		else {
			logFile.saveShit("events in shock = " + eventsInShock);
			return false;
		}
	}
}
