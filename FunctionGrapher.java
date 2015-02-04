//import gov.noaa.noaaserver.sgt.awt.*;
//import gov.noaa.noaaserver.sgt.*;
//import gov.noaa.noaaserver.sgt.datamodel.*;
import gov.noaa.pmel.sgt.swing.*;
import gov.noaa.pmel.sgt.dm.*;
import gov.noaa.pmel.util.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.print.*;


/**
*  This class graphs a function for test purposes
*
*  Adapted Mar. 03 LAS
*/
public class FunctionGrapher extends PrintableDialog implements ActionListener{

	private Container contentPane;
	private long[] array;
	private JButton printButton, closeButton;
	private JTextArea outputArea;
	private JPanel buttonPanel;
	private JScrollPane scrollPane;
	private JPlotLayout lpl;


	/** Send in the return array from the histogrammer, and a pointer to the GUI
	*    - also send in the filename just written
	*/
	public FunctionGrapher() {
		setTitle("Function Grapher");

		//super(parent, "CIS Histogram", true);
		//this.array = array;
		//theMain = parent;

		contentPane = getContentPane();
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e){
				setVisible(false);
				System.exit(0);
			}
		});

		printButton = new JButton("Print");
		printButton.addActionListener(this);
		closeButton = new JButton("Close");
		closeButton.addActionListener(this);
		buttonPanel = new JPanel();
		buttonPanel.add(printButton);
		buttonPanel.add(closeButton);
		contentPane.add(buttonPanel, "South");

		double[] dummy = {0.0};
		SimpleLine sl = new SimpleLine(dummy,dummy,"dummy");
		lpl=new JPlotLayout(sl);
		lpl.clear();
		lpl.setAutoRange(true,true);


		//lpl.setYRange(new Range2D(max(),0.0,1.0), true);

		//lpl.setSize(new Dimension(450,600));

		contentPane.add(lpl, "Center");
		lpl.setTitles(" " + " "+ " " + " ",
		              "Use shift to zoom- ctrl to reset-",
		              "");

		pack();
		show();
	}

	public void addLine(Function f, double min_x, double max_x) {
		addLine(f,min_x,max_x,100) ;
	}

	public void addLine(Function f, double min_x, double max_x, int numPoints) {
		lpl.clear();

		if (numPoints<=1) return;
		double[] ypoints = new double[numPoints];
		double[] xpoints = new double[numPoints];
		double delta = ((max_x - min_x)/(numPoints-1));
		for (int i=0; i<numPoints; i++) {
			ypoints[i] = f.function(min_x + i*delta);
			xpoints[i] = min_x + i*delta;
		}

		SimpleLine sl = new SimpleLine(xpoints, ypoints, "Function Line");
		sl.setXMetaData(new SGTMetaData("", ""));
		sl.setYMetaData(new SGTMetaData("", ""));


		lpl.addData(sl);
		pack();
		show();
		lpl.show();
		lpl.draw();
	}


	public void actionPerformed(ActionEvent evt) {
		Object source = evt.getActionCommand();
		if (source == "Close") {
			setVisible(false);
			System.exit(0);
		}
		else if (source == "Print") {
			System.out.println("Trying to print...");
			//setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			PrinterJob job = PrinterJob.getPrinterJob();
			job.setPrintable(this);
			if (job.printDialog()) {
				try {
					job.print();
				}
				catch (Exception ex) {
					ex.printStackTrace();
				}
			}
			System.out.println("Supposedly printed...");
			//setCursor(Cursor.getDefaultCursor());
		}
	}

	/*private double max() {
		long tbr = 0;
		for (int i=0; i<array.length; i++) {
			if (array[i] > tbr) tbr = array[i];
		}
		return tbr;
	}*/

	private void setDefaultSizes() {
		int width = (int)Toolkit.getDefaultToolkit().getScreenSize().getWidth()*1/3;
		int height = (int)Toolkit.getDefaultToolkit().getScreenSize().getHeight()*2/3;
		Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
		Point centerPoint = new Point((screen.width-width)/2,(screen.height-height)/2);
		setLocation(centerPoint);
		setSize(width,height);
	}

	public static final void main(String[] args) {

		Function f = new Function () {
			public double function(double x) {
				return (float)KallenbachEnergyCorrection.efficiency((float)x);
			}
		};
		FunctionGrapher fg = new FunctionGrapher();

		fg.addLine(f,0.0, 32.0);

	}

}




