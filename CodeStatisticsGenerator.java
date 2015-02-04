

import java.util.*;
import java.io.*;

/**
*
* Taken from nelder.jomail.BuildPrepare and modified
*
*   just want to see some stats on the code we have built
*
*   this recurses subdirectories so often you will need to move libraries out of subdirs
*   to avoid counting the .java files therein...
*
*/
public class CodeStatisticsGenerator {

	//file temp = new file("abcdefghijklmnop");
	file f = new file();

	// hey!  lets waste some memory!
	long fileInstances = 0;
	long classInstances = 0;
	long lineInstances = 0;
	long stringInstances = 0;
	long leftBrackets = 0; // why not?
	long commentInstances = 0;
	long systemOutInstances = 0;

	String shit = ""; // this is used for a line of code
	//String newshit = "";
	String ct = ""; // current token
	String linesep = System.getProperty("line.separator");

	/**
	* These strings are used to get rid of stuff that should be commented out at build
	*/
	public static final String rmTagStart = "9Elder Inc. REMOVE THIS CODE START";
	public static final String rmTagFinish = "9Elder Inc. REMOVE THIS CODE FINISH";

	/**
	* this guy takes a directory
	*/
	public CodeStatisticsGenerator(String[] args) {
		Date d3 = new Date(); // go figure
		System.out.println("here's the linesep length " + linesep.length());

		runThis(args);
		System.out.println("fileInstances = "  + fileInstances);
		System.out.println("classInstances = "  + classInstances);
		System.out.println("lineInstances = "  + lineInstances);
		System.out.println("stringInstances = "  + stringInstances);
		System.out.println("leftBrackets = "  + leftBrackets);
		System.out.println("systemOutInstances = " + systemOutInstances);

		Date d1 = new Date();
		System.out.println(d1 + "(for testing o())");
		Date d2 = new Date();
		System.out.println("o() took: " + (d2.getTime()-d1.getTime()));
		System.out.println("whole thing took: " + (d2.getTime() - d3.getTime()));
	}

	public void runThis(String[] args) {
		System.out.println("calling runthis("+args[0]+")");

		File dirF = new File (args[0]);
		File[] subDirs = dirF.listFiles();
		for (int j=0; j<subDirs.length; j++) {
			// recurse like a mothafucka
			if (subDirs[j].isDirectory()) {
				System.out.println(subDirs[j]);
				String[] weak = {""+subDirs[j]+System.getProperty("file.separator")};
				runThis(weak);
			}
		}

		String[] dir = dirF.list(new JavaFileFilter());
		System.out.println("about to go through " + dir.length + " files");
		for (int i=0; i<dir.length; i++) {
			System.out.println("Going through "+args[0]+dir[i]);
			f = new file(args[0]+dir[i]);

			// finally going to do it correct
			f.initRead();

			boolean eof = false;
			String line = "";
			// ok, ready to parse:
			while (!eof) {
				if ((line=f.readLine())==null) eof = true;
				else {
					lineInstances++;
					int index = 0;
					shit = line;
					int len = shit.length();
					boolean inComment = false;
					boolean eol = false;
					while (index < len) {
						if (inComment) {
							if (index<len-3 && shit.substring(index,index+2).equals("*/")) {
								inComment = false;
							}
						}
						else { // ok - not in a comment.   Let's do shit

							// first check if we are going back in a comment
							if (index<len-3 && shit.substring(index,index+2).equals("//")) {
								commentInstances++;
								//newshit+=shit.substring(index,len);
								eol=true; // we are done with this line
								index = len+1;
							}
							else if (index<shit.length()-3 && shit.substring(index,index+2).equals("/*")) {
								commentInstances++;
								inComment = true;
							}


							// other shit to check for
							else if (index<len-6 && shit.substring(index,index+5).equals("class")) {
								classInstances++;
							}
							else if (index<len-5 &&
									shit.substring(index,index+4).equals("file")) fileInstances++;
							else if (index<len-7 &&
									shit.substring(index,index+6).equals("String")) stringInstances++;
							else if (index<len-3 &&
									shit.substring(index,index+1).equals("{")) leftBrackets++;
							else if (index<len-(linesep.length()+1) &&
								shit.substring(index,index+linesep.length()).equals(linesep)) lineInstances++;


							// also check for the dreaded o()
							else if (index<len-20 && shit.substring(index,index+19).equals("System.out.println("))  {
								systemOutInstances++;
							}
						} // done not in comment stuff

						// and of course save the rest
						//if (!eol) newshit+=shit.substring(index,index+1);
						index++;
					}
				}
			}
			f.closeRead();
			String[] m_sheeIt = {"damn that's some good shit"};
		}
	}

	public static final void main(String[] args) {
		CodeStatisticsGenerator bp = new CodeStatisticsGenerator(args);
	}

}

class JavaFileFilter implements FilenameFilter {
	public boolean accept(File dir, String s) {
		int q = s.length();
		if (s.length()>5 && s.substring(q-4,q).equals("java")) {
			return true;
		}
		else return false;
	}
}

