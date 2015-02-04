/**
*  This object parses a String to give a Date.
*  Lukas Saul, 1999
*
*   Also caches last date - therefore not a static method.
*   use:md2 = new MyDate2(); Date d = md2.parse(someString);
*/

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;
import javax.swing.text.DefaultEditorKit;
import java.net.*;
import java.io.*;
import java.util.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

// Now keeps the last dates in memory to avoid parsing again.
// Also remembers most recent format and uses it, rather than trying with each one.
public class MyDate2 {
	private int lastPattern=0;
	private String lastDateString="";
	private Date lastDate=null;
	private Date toBeReturned=null;
	private boolean didIt = false;
	private int counter = 0;
	private SimpleDateFormat sdf;
	private String pattern[] = {

					"dd/MM/yyyy HH:mm:ss",
					"G dd/MM/yyyy HH:mm:ss"

					};
	// Only one date format for now- keep it less confusing
	//NOTE KEY HERE: under 4 letters - look for abbreviation. 4 0r more for written out.
	// H - 24 hr scale h - 12 hr scale a - am/pm E - day of week - see API

	// I don't think I can use this!!!
	public int secondsSince1996(int secondsSince1970) {
		Date d1 = new Date((long)secondsSince1970 * 1000);
		Date d2 = parse("01/01/1996 00:00:00");
		System.out.println(d1 + " " + d2);
		int tbr = (int)(d1.getTime()/1000 - d2.getTime()/1000);
		if (tbr < 0) System.out.println("error getting secs since 1996");
		return tbr;
	}

	//here's the main routine:
	public Date parse(String date) {

		if (date.equals(lastDateString)) return lastDate;
		// if so, we audi.

		sdf = new SimpleDateFormat(pattern[lastPattern]);
		sdf.setLenient(false); // lenient my ass!
		try {
			toBeReturned = sdf.parse(date);
			didIt = true;
			lastDateString = date;
			lastDate = toBeReturned;
		}
		catch (Exception e) {didIt = false; }
		if (didIt) return toBeReturned;
		// if so, we audi again


		// worst case scenario, loop through all patterns
		while (!didIt) {
			sdf = new SimpleDateFormat(pattern[counter]);
			sdf.setLenient(false); // lenient my ass!

			try {
				toBeReturned = sdf.parse(date);
				didIt = true;
				lastDateString = date;
				lastDate = toBeReturned;
			}
			catch (Exception e) {didIt = false; }
			if (didIt) return toBeReturned;
			// we audi

			if (counter==pattern.length-1) { // so we couldn't parse it!
				System.out.println("FIX THIS!!  UNABLE TO PARSE: " + date);
				toBeReturned = null;
				return toBeReturned;
			}
			else counter++;
		}
		// We should never get here
		System.out.println("problems in the date parser of a serious nature!!");
		return toBeReturned; //required for compilation
	}

	/*public static final void main(String[] args) {
		MyDate2 md2 = new MyDate2();
		Date d1 = md2.parse("BC 01/01/4713 12:00:00");
		System.out.println(d1);
		System.out.println(""+d1.getTime());
	}*/

	public static final void main(String[] args) {
		int test = (int)(8.27366347*Math.pow(10,8));
		MyDate2 md2 = new MyDate2();
		System.out.println("seconds since 1996(test) " + md2.secondsSince1996(test));
		System.out.println("test "+ test);
		System.out.println("Date 0 " + new Date(0));
		Date d2 = md2.parse("01/01/1996 00:00:00");
		System.out.println("try this " + (d2.getTime()/1000));
		long test2 = (long)(8.29699129*Math.pow(10,11));
		System.out.println(new Date(test2));

		System.out.println("Start: " + new Date((long)(8.27428764*Math.pow(10,11))));

		Date d4 = md2.parse("01/01/1958 00:00:00");
		System.out.println("try this 1958: " + (d4.getTime()/1000));
		Date d5 = md2.parse("01/01/1996 00:00:00");
		System.out.println("or this: " + (d5.getTime()-d4.getTime())/1000);
		System.out.println("answer: " + d5.getTime()/1000);
		System.out.println(""+d5);
	}
}