
/**
* Use this to shift a dataset with a .dat file
* This uses shiftInterpolator
*
*/
public class DataConvector {

	private ShiftInterpolator si;
	private String shiftFileName;

	public DataConvector(String fileName) {
		shiftFileName = fileName;

		file f = new file(shiftFileName);

		int numlines = f.readShitNumLines();

		si = new ShiftInterpolator(shiftFileName,numlines);

	}

	/**
	* Use this to determine the convected date from the file
	*  in the constructor
	*/
	public int getDate(int date) {

		int shift = si.getShift(date);

		return date+shift;
	}

}