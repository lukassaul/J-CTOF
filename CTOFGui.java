import java.util.*;
import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import javax.swing.*;
import java.awt.*;
import java.awt.print.*;
import java.awt.event.*;
import javax.swing.event.*;


/**
*   This class is the main GUI - and the common block for shared variables.
*   Lukas Saul  Nov 2000
*
*   This thing is kind of turning into a list of all analyses as checkboxes...
*
*   However actualy analysis takes places elsewhere (CTOFHitogrammer.java)
*/
public class CTOFGui extends JFrame implements ActionListener{
	public String user_dir = System.getProperty("user.dir");
	public String file_sep = System.getProperty("file.separator");
	public String save_dir = user_dir; // for now...
	public String CRLF = System.getProperty("line.separator");
	public String backupFileName = ".ctofSettings";
	public String outputFileName = "";

	// GUI Objects...
	private Container contentPane;
	private JButton runButton, stopButton, clearButton, timesButton;
	private JButton shocksButton, bButton, swButton, corrButton, resultsButton;
	public JButton nextIntervalButton, spectraButton;
	private JPanel eastPanel, westPanel, buttonPanel;

	// these guys are public because they are "common block" - useful stuff
	public JTextField polarMinField, polarMaxField, polarMinField2, polarMaxField2;
	public JTextField azimuthMinField, azimuthMaxField, azimuthMinField2, azimuthMaxField2;
	public JTextField magnitudeMinField, magnitudeMaxField, bSmoothingField;
	public JTextField magFileField;
	public JTextField radialMinField, radialMaxField;
	public JTextField vswMinField, vswMaxField, swSmoothingField;
	public JTextField densityMinField, densityMaxField, vthMinField, vthMaxField;
	public JTextField timeStartField, timeEndField, timeSplitField;
	public JTextField timeBinField, vvBinField, outputFileField;
	public JTextField shockTimeField, binStartField, binFinishField;
	public JTextField alfvenMinField, alfvenMaxField;
	public JTextField alfvenRatioMinField, alfvenRatioMaxField;
	public JTextField gammaField, tailStartField;
	public JTextField corrDeltaField, corrNumField, corrXField, corrYField;
	public JTextField intervalsField;
	public JLabel statusLabel;
	// checkboxes for the selection menus (mag, sw, shocks, etc.)
	public JCheckBox bCheckBox, shockCheckBox, strongShockCheckBox, allShockCheckBox;
	public JCheckBox swCheckBox, timesCheckBox, timeProfileCheckBox;
	public JCheckBox energyHistogramCheckBox, correlationCheckBox, pcorrelationCheckBox;//, diffCheckBox;
	public JCheckBox monochromeCheckBox, accelCheckBox;

	// new checkbox paradigm now:
	// we are going to set up for scattering any of the parameters
	// note, this "histogram" is more of an averaging than a histogram really...
	public JCheckBox doScatCheckBox, histScatCheckBox;
	public JTextField scatXField, scatYField, scatBinsField;
	public JButton showInfoButton;

	public boolean doTI;

	private String currentGUI;
	private JLabel label1, label2, label3, label4;

	private CtofHistogrammer ch;

	public JTextField floatingAverageFileField, etaMaxField, etaMinField;
	public JTextField averageBMinField, averageBMaxField;

	public JComboBox histTypeComboBox;
	//public String comboString =

	// "catalog" creating gui items (Jan. 2003)
	private JButton catalogButton;
	public JTextField c_outputFileField, c_numFilesField;
	public JCheckBox catalogCheckBox;;

	public JCheckBox specCheckBox;
	public JTextField spDivMinBinField,spDivMaxBinField,spDivMinField,spDivMaxField;
	public JTextField spCurlMinBinField, spCurlMaxBinField, spCurlMinField, spCurlMaxField;
	public JTextField spCurlFileField, spDivFileField;
 	// better to compare to a fit:
 	public JTextField parInterceptMinField, parInterceptMaxField, traceInterceptMinField;
 	public JTextField traceInterceptMaxField, parIndexMinField, parIndexMaxField;
 	public JTextField traceIndexMinField, traceIndexMaxField;
 	public JTextField ratioMinField, ratioMaxField;

 	// e- impact gui stuff
 	public JButton electronButton;
 	public JCheckBox electronCheckBox;
 	public JTextField electronFileField;
 	public JTextField electronMinField, electronMaxField;

	public JButton energyCalibrationButton;
	public JCheckBox kallenbachCheckBox;

	public JTextField heFileField;

	// diffusion index stuff
	public JTextField d_vvMinField, d_vvMaxField;

	public JCheckBox intensityCheckBox;
	public JCheckBox totalEFluxCheckBox;
	public JCheckBox dontNormalizeCheckBox;
	public JCheckBox removeShiftCheckBox;

	//public JCheckBox timeProfile3DCheckBox;
	//public JTextField numSegmentsField;
	/**
	* the constructor for the GUI.  Constructing this object starts the program.
	*/
	public CTOFGui () {
		if (user_dir == null) user_dir = "";
		if (save_dir == null) save_dir = "";
		doTI = false;

		// Setting up GUI...
		setTitle("SOHO CTOF He+ Pickup Analysis GUI");
		contentPane = getContentPane();
		// our window listener...
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e){
				saveSettings();
				System.exit(0);
			}
		});

/*
		//This code is to override the system.out.println to logfile.txt
		System.out.println("Trying to convert output to log file...");
		try {
			FileOutputStream str = new FileOutputStream(new File(user_dir + file_sep + ".ctoflog.txt"));
		    System.setOut(new PrintStream(str));
	        System.setErr(new PrintStream(str));
        }
        catch (Exception ex) { ex.printStackTrace(); }
        System.out.println("Output log for SohoCTOF.java");
*/
		//**** --------------------- Done Initialization----------------------------------------****/


		// Create GUI Objects...

		runButton = new JButton("RUN");
		runButton.addActionListener(this);
		stopButton = new JButton("Stop");
		stopButton.addActionListener(this);
		clearButton = new JButton("Clear");
		clearButton.addActionListener(this);
		shocksButton = new JButton("Shocks...");
		shocksButton.addActionListener(this);
		bButton = new JButton("Magnetic Fields...");
		bButton.addActionListener(this);
		swButton = new JButton("SW Properties...");
		swButton.addActionListener(this);
		corrButton = new JButton("Correlation Integral...");
		corrButton.addActionListener(this);
		resultsButton = new JButton("Output...");
		resultsButton.addActionListener(this);
		timesButton = new JButton("Times...");
		timesButton.addActionListener(this);
		statusLabel = new JLabel("Status... ");
		showInfoButton = new JButton("Show Info");
		showInfoButton.addActionListener(this);
		nextIntervalButton = new JButton("Next Histogram");
		nextIntervalButton.addActionListener(this);
		catalogButton = new JButton("Catalog...");
		catalogButton.addActionListener(this);
		spectraButton = new JButton("Wave Spectra...");
		spectraButton.addActionListener(this);
		energyCalibrationButton = new JButton("Energy Calibration...");
		energyCalibrationButton.addActionListener(this);


		//BField Stuff:
		alfvenMinField = new JTextField();
		alfvenRatioMinField = new JTextField();
		alfvenMaxField = new JTextField();
		alfvenRatioMaxField = new JTextField();
		polarMinField = new JTextField();
		polarMaxField = new JTextField();
		//polarMinField2 = new JTextField();
		//polarMaxField2 = new JTextField();
		azimuthMaxField = new JTextField();
		azimuthMinField = new JTextField();
		//azimuthMaxField2 = new JTextField();
		//azimuthMinField2 = new JTextField();
		magnitudeMinField = new JTextField();
		magnitudeMaxField = new JTextField();
		bSmoothingField = new JTextField();
		radialMinField = new JTextField();
		radialMaxField = new JTextField();
		floatingAverageFileField = new JTextField("magdatafloat.bin");
		etaMaxField = new JTextField();
		etaMinField = new JTextField();
		averageBMaxField = new JTextField();
		averageBMinField = new JTextField();
		bCheckBox = new JCheckBox("Use Mag Constraints");
		magFileField = new JTextField();

		//shocks:
		shockCheckBox = new JCheckBox("Throw away shock events");
		shockTimeField = new JTextField();
		allShockCheckBox = new JCheckBox("Use only shock events");
		accelCheckBox = new JCheckBox("Throw away accel. regions");

		//strongShockCheckBox = new JCheckBox("Discard only strong shock events");

		//SW:
		swCheckBox = new JCheckBox("Check SW Properties");
		vswMinField = new JTextField();
		vswMaxField = new JTextField();
		swSmoothingField = new JTextField();
		//swDensityCheckBox = new JCheckBox("Use all densities");
		densityMinField = new JTextField();
		densityMaxField = new JTextField();
		//densitySmoothingField = new JTextField();
		vthMinField = new JTextField();
		vthMaxField = new JTextField();

		//output..
		timeProfileCheckBox = new JCheckBox("Make a time profile");
		timeBinField = new JTextField();
		vvBinField = new JTextField();
		outputFileField = new JTextField();
		timeStartField = new JTextField();
		timeEndField = new JTextField();
		energyHistogramCheckBox = new JCheckBox("Make V/Vsw Histogram");
		vvBinField = new JTextField();
		doScatCheckBox = new JCheckBox("Make a Scatter Plot");
		histScatCheckBox = new JCheckBox("and Histogram the Scatter Plot");
		tailStartField = new JTextField();
		scatXField = new JTextField();
		scatYField = new JTextField();
		scatBinsField = new JTextField();
		binStartField = new JTextField();
		binFinishField = new JTextField();
		intervalsField = new JTextField();
		intensityCheckBox = new JCheckBox("Calculate Max Intensity");
		totalEFluxCheckBox = new JCheckBox("Calculate Tot.EFlux");
		dontNormalizeCheckBox = new JCheckBox("don't norm. time spec");
		removeShiftCheckBox = new JCheckBox("Remove Upwind Shift");
		//diffCheckBox = new JCheckBox("Use a w range (Diff. Flux)");


		//efficiency
		gammaField = new JTextField();
		kallenbachCheckBox = new JCheckBox("Use Kallenbach Ion-Optical Curve");

		monochromeCheckBox = new JCheckBox("Output only monochrome");
		monochromeCheckBox.setSelected(false);

		// correlation integral...
		corrNumField = new JTextField();
		corrDeltaField = new JTextField();
		correlationCheckBox = new JCheckBox("Do Correlation Integral");
		pcorrelationCheckBox = new JCheckBox("Do Pearson's Corr. Coeff.");
		corrXField = new JTextField();
		corrYField = new JTextField();

		histTypeComboBox = new JComboBox();
		histTypeComboBox.addItem("EFlux");
		histTypeComboBox.addItem("Flux");
		histTypeComboBox.addItem("Distribution Function");
		histTypeComboBox.addItem("Log Distribution Function");
		histTypeComboBox.setSelectedIndex(0);

		// catalog items
		c_outputFileField = new JTextField();
		catalogCheckBox = new JCheckBox("Create Catalog File(s)");
		c_numFilesField = new JTextField();


		//spectral analysis gui objects construction
		specCheckBox = new JCheckBox("Use MfiSpec Constraints");
		specCheckBox.setSelected(false);
		spDivMinBinField = new JTextField();
		spDivMaxBinField = new JTextField();
		spDivMinField = new JTextField();
		spDivMaxField = new JTextField();
		spCurlMinBinField = new JTextField();
		spCurlMaxBinField = new JTextField();
		spCurlMinField = new JTextField();
		spCurlMaxField = new JTextField();
		spDivFileField = new JTextField();
		spCurlFileField = new JTextField();
		// and the new fit fields:
		parInterceptMinField = new JTextField();
		parInterceptMaxField = new JTextField();
		traceInterceptMinField = new JTextField();
		traceInterceptMaxField = new JTextField();
		parIndexMinField = new JTextField();
		parIndexMaxField = new JTextField();
 		traceIndexMinField = new JTextField();
 		traceIndexMaxField = new JTextField();
 		ratioMinField = new JTextField();
 		ratioMaxField = new JTextField();

 		heFileField = new JTextField();

 		// electron items
 		electronCheckBox = new JCheckBox("use electron data");
 		electronMinField = new JTextField();
 		electronMaxField = new JTextField();
 		electronFileField = new JTextField();
 		electronButton = new JButton("Electron Data...");
 		electronButton.addActionListener(this);

		d_vvMinField = new JTextField();
		d_vvMaxField = new JTextField();


		//tailVswCheckBox = new JCheckBox("Make Tail vs. Vsw Histogram");
		//tailBCheckBox = new JCheckBox("Make Tail vs. B Histogram");
		//tailAlfvenCheckBox = new JCheckBox("Make Tail vs. Valfven Histogram");
		//fluxVswCheckBox = new JCheckBox("Flux vs. Vsw Histogram");
		//fluxBCheckBox = new JCheckBox("Flux vs |B| Histogram");
		//fluxBDirCheckBox = new JCheckBox("Flux vs B angle Histogram");
		//fluxAlfvenCheckBox = new JCheckBox("Flux vs Va/Vsw Histogram");

		// times..
		timesCheckBox = new JCheckBox("Use all events");
		timeStartField = new JTextField();
		timeEndField = new JTextField();
		//timeSplitField = new JTextField();

	//	numSegmentsField = new JTextField();



		westPanel = new JPanel();
		westPanel.setLayout(new GridLayout(9,1));
		westPanel.add(shocksButton);
		westPanel.add(bButton);
		westPanel.add(swButton);
		westPanel.add(corrButton);
		westPanel.add(resultsButton);
		westPanel.add(catalogButton);
		westPanel.add(spectraButton);
		westPanel.add(energyCalibrationButton);
		westPanel.add(electronButton);
		//westPanel.add(timesButton);

		eastPanel = new JPanel(); // empty for now...

		buttonPanel = new JPanel();
		buttonPanel.add(statusLabel);
		buttonPanel.add(runButton);
		buttonPanel.add(stopButton);
		//buttonPanel.add(clearButton);
		buttonPanel.add(showInfoButton);
		//buttonPanel.add(nextIntervalButton);

		contentPane.add(buttonPanel, "South");
		contentPane.add(westPanel, "West");
		contentPane.add(eastPanel, "East");

		//initialize time segments:
		pack();
		show();

		file f = new file(user_dir + file_sep + backupFileName);
		if (f.exists()) {
			loadSettings();
		}
	}

	/** Here's where all the button presses and actions go...
	*
	*/
	public void actionPerformed(ActionEvent e) {


		Object source = e.getActionCommand();
		//System.out.println("processing event " + e);

		/*bCheckBox.fireStateChanged();
		shockCheckBox.fireStateChanged();
		strongShockCheckBox.fireStateChanged();
		swCheckBox.fireStateChanged();
		timesCheckBox.fireStateChanged();
		timeProfileCheckBox.fireStateChanged();
		energyHistogramCheckBox.fireStateChanged();
		*/ // problems here with the damned checkboxes!!


		if (source == "Magnetic Fields...") {
			eastPanel.removeAll();
			eastPanel.setLayout(new GridLayout(11,3));
			eastPanel.add(bCheckBox);
			eastPanel.add(new JLabel("File:"));
			eastPanel.add(magFileField);
			eastPanel.add(new JLabel(""));
			eastPanel.add(new JLabel("Min"));
			eastPanel.add(new JLabel("Max"));
			eastPanel.add(new JLabel("Polar"));
			eastPanel.add(polarMinField);
			eastPanel.add(polarMaxField);
			eastPanel.add(new JLabel("Azimuth"));
			eastPanel.add(azimuthMinField);
			eastPanel.add(azimuthMaxField);
			//eastPanel.add(new JLabel("Polar2"));
			//eastPanel.add(polarMinField2);
			//eastPanel.add(polarMaxField2);
			//eastPanel.add(new JLabel("Azimuth2"));
			//eastPanel.add(azimuthMinField2);
			//eastPanel.add(azimuthMaxField2);
			eastPanel.add(new JLabel("Magnitude"));
			eastPanel.add(magnitudeMinField);
			eastPanel.add(magnitudeMaxField);
			//eastPanel.add(new JLabel("Smoothing (min)"));
			//eastPanel.add(bSmoothingField);
			//eastPanel.add(new JLabel(""));
			eastPanel.add(new JLabel("angle to radial"));
			eastPanel.add(radialMinField);
			eastPanel.add(radialMaxField);
			eastPanel.add(new JLabel("Alfven Speed: "));
			eastPanel.add(alfvenMinField);
			eastPanel.add(alfvenMaxField);
			eastPanel.add(new JLabel("Valfven / Vsw"));
			eastPanel.add(alfvenRatioMinField);
			eastPanel.add(alfvenRatioMaxField);
			eastPanel.add(new JLabel("Averaged DataFile: "));
			eastPanel.add(floatingAverageFileField);
			eastPanel.add(new JLabel(""));
			eastPanel.add(new JLabel("Eta - "));
			eastPanel.add(etaMinField);
			eastPanel.add(etaMaxField);
			eastPanel.add(new JLabel("Average BMag "));
			eastPanel.add(averageBMinField);
			eastPanel.add(averageBMaxField);
			pack();
			show();
		}
		else if (source == "Shocks...") {
			eastPanel.removeAll();
			eastPanel.setLayout(new GridLayout(3,2));
			eastPanel.add(shockCheckBox);
			eastPanel.add(allShockCheckBox);
			eastPanel.add(new JLabel("deltaT around shocks (hr):"));
			eastPanel.add(shockTimeField);
			eastPanel.add(accelCheckBox);
			eastPanel.add(new JLabel(""));
			//eastPanel.add(strongShockCheckBox);
			pack();
			show();
		}
		else if (source == "SW Properties...") {
			eastPanel.removeAll();
			eastPanel.setLayout(new GridLayout(6,3));
			eastPanel.add(swCheckBox);
			eastPanel.add(new JLabel(""));
			eastPanel.add(new JLabel(""));
			eastPanel.add(new JLabel(""));
			eastPanel.add(new JLabel("Min"));
			eastPanel.add(new JLabel("Max"));
			eastPanel.add(new JLabel("|Vsw|"));
			eastPanel.add(vswMinField);
			eastPanel.add(vswMaxField);
			eastPanel.add(new JLabel("(1/cm^3)"));
			eastPanel.add(densityMinField);
			eastPanel.add(densityMaxField);
			eastPanel.add(new JLabel("V(thermal)"));
			eastPanel.add(vthMinField);
			eastPanel.add(vthMaxField);
			eastPanel.add(new JLabel("Smoothing (min)"));
			eastPanel.add(swSmoothingField);
			eastPanel.add(new JLabel(""));
			pack();
			show();
		}
		else if (source == "Output...") {
			eastPanel.removeAll();
			eastPanel.setLayout(new GridLayout(15,2));
			eastPanel.add(doScatCheckBox);
			//eastPanel.add(histScatCheckBox);
			eastPanel.add(intensityCheckBox);
			eastPanel.add(new JLabel("X axis quantity: (int)"));
			eastPanel.add(scatXField);
			eastPanel.add(new JLabel("Y axis quantity: (int)"));
			eastPanel.add(scatYField);
			//eastPanel.add(new JLabel("#of bins for scatter historam"));
			//eastPanel.add(scatBinsField);
			eastPanel.add(new JLabel("Tail start (cutoff): "));
			eastPanel.add(tailStartField);

			//eastPanel.add(timeProfileCheckBox);
			//eastPanel.add(showInfoButton);
			//eastPanel.add(fluxBDirCheckBox);
			//eastPanel.add(new JLabel("Averaging interval (hrs)"));
			//eastPanel.add(timeBinField);
			eastPanel.add(new JLabel("Start time (DOY): "));
			eastPanel.add(timeStartField);
			eastPanel.add(new JLabel("End Time (DOY) : "));
			eastPanel.add(timeEndField);
			eastPanel.add(energyHistogramCheckBox);
			eastPanel.add(histTypeComboBox);
			eastPanel.add(new JLabel("Number of V/Vsw bins: "));
			eastPanel.add(vvBinField);
			eastPanel.add(new JLabel("Output File: "));
			eastPanel.add(outputFileField);
			eastPanel.add(new JLabel("Helium File: "));
			eastPanel.add(heFileField);

			//eastPanel.add(diffCheckBox);
			eastPanel.add(totalEFluxCheckBox);
			eastPanel.add(new JLabel("Integral EFlux v/vsw range:"));
			eastPanel.add(binStartField);
			eastPanel.add(binFinishField);
			eastPanel.add(new JLabel("# of intervals per histogram: "));
			eastPanel.add(intervalsField);

			eastPanel.add(timeProfileCheckBox);
			eastPanel.add(monochromeCheckBox);
			eastPanel.add(dontNormalizeCheckBox);
			eastPanel.add(removeShiftCheckBox);
			//eastPanel.add(new JLabel("Averaging Interval (minutes):"));
			//eastPanel.add(numSegmentsField);

			pack();
			show();
		}
		else if (source == "Correlation Integral...") {
			eastPanel.removeAll();
			eastPanel.setLayout(new GridLayout(5,2));
			eastPanel.add(correlationCheckBox);
			eastPanel.add(pcorrelationCheckBox);
			eastPanel.add(new JLabel("Corr. delta step: "));
			eastPanel.add(corrDeltaField);
			eastPanel.add(new JLabel("Corr. # of pos. steps: "));
			eastPanel.add(corrNumField);
			eastPanel.add(new JLabel("Corr. x(t) unit: "));
			eastPanel.add(corrXField);
			eastPanel.add(new JLabel("Corr. y(t+del) unit: "));
			eastPanel.add(corrYField);

			//eastPanel.add(new JButton("Show Data"));
			pack();
			show();
		}
		else if (source == "Catalog...") {
			eastPanel.removeAll();
			eastPanel.setLayout(new GridLayout(3,2));
			eastPanel.add(catalogCheckBox);
			eastPanel.add(new JLabel(""));
			eastPanel.add(new JLabel("Output file pathname: "));
			eastPanel.add(c_outputFileField);
			eastPanel.add(new JLabel("Time per page (days): "));
			eastPanel.add(c_numFilesField);
			pack();
			show();
		}
		else if (source == "Wave Spectra...") {
			eastPanel.removeAll();
			eastPanel.setLayout(new GridLayout(8,3));
			eastPanel.add(specCheckBox);
			eastPanel.add(new JLabel("Min"));
			eastPanel.add(new JLabel("Max"));
			eastPanel.add(new JLabel("PAR comp. .1hZ-intercept: "));
			eastPanel.add(parInterceptMinField);
			eastPanel.add(parInterceptMaxField);
			eastPanel.add(new JLabel("TRACE comp. .1hZ-intercept: "));
			eastPanel.add(traceInterceptMinField);
			eastPanel.add(traceInterceptMaxField);
			eastPanel.add(new JLabel("PAR comp. spec index: "));
			eastPanel.add(parIndexMinField);
			eastPanel.add(parIndexMaxField);
			eastPanel.add(new JLabel("TRACE comp. spec index: "));
			eastPanel.add(traceIndexMinField);
			eastPanel.add(traceIndexMaxField);
			eastPanel.add(new JLabel("TRACE / PAR power ratio"));
			eastPanel.add(ratioMinField);
			eastPanel.add(ratioMaxField);
			eastPanel.add(new JLabel("D_VV index"));
			eastPanel.add(d_vvMinField);
			eastPanel.add(d_vvMaxField);
			eastPanel.add(new JLabel("filenames: "));
			eastPanel.add(spDivFileField);
			eastPanel.add(spCurlFileField);
			/*eastPanel.add(new JLabel("Div Bins: "));
			eastPanel.add(spDivMinBinField);
			eastPanel.add(spDivMaxBinField);
			eastPanel.add(new JLabel("Div Range: "));
			eastPanel.add(spDivMinField);
			eastPanel.add(spDivMaxField);
			eastPanel.add(new JLabel("Curl Bins: "));
			eastPanel.add(spCurlMinBinField);
			eastPanel.add(spCurlMaxBinField);
			eastPanel.add(new JLabel("Curl Range: "));
			eastPanel.add(spCurlMinField);
			eastPanel.add(spCurlMaxField);*/
			pack();
			show();
		}
		else if (source == "Energy Calibration...") {
			eastPanel.removeAll();
			eastPanel.setLayout(new GridLayout(2,2));
			eastPanel.add(kallenbachCheckBox);
			eastPanel.add(new JLabel(""));
			eastPanel.add(new JLabel("Efficiency adjust (E^gamma): "));
			eastPanel.add(gammaField);
			pack();
			show();
		}
		else if (source == "Electron Data...") {
			eastPanel.removeAll();
			eastPanel.setLayout(new GridLayout(4,2));
			eastPanel.add(electronCheckBox);
			eastPanel.add(new JLabel(""));
			eastPanel.add(new JLabel("file:  "));
			eastPanel.add(electronFileField);
			eastPanel.add(new JLabel("Min. e- param: "));
			eastPanel.add(electronMinField);
			eastPanel.add(new JLabel("Max. e- param: "));
			eastPanel.add(electronMaxField);
			pack();
			show();
		}

		else if (source == "Times...") {
			/*eastPanel.removeAll();
			eastPanel.setLayout(new GridLayout(3,3));
			eastPanel.add(timesCheckBox);
			eastPanel.add(new JLabel(""));
			eastPanel.add(new JLabel("Superposed Epoch Analysis:"));
			eastPanel.add(new JLabel(""));
			eastPanel.add(new JLabel("Start"));
			eastPanel.add(new JLabel("Finish"));
			eastPanel.add(new JLabel("DOY"));
			eastPanel.add(timeStartField);
			eastPanel.add(timeFinishField);
			pack();
			show();*/
		}
		else if (source == "Clear") {
			//clearAllFields();
		}
		else if (source == "Stop") {
			ch.stop = true;
			//r.stopIt();
		}
        else if (source == "Show Info") {
			JDialog jd = new JDialog(this, "Dataset Parameter List", false);
			Container ofBeer = jd.getContentPane();
			ofBeer.add(new JTextArea(CtofHistogrammer.UNITS_DESCRIPTION));
			jd.setSize(200,200);
            jd.show();
            //JOptionPane.showMessageDialog(this, CtofHistogrammer.UNITS_DESCRIPTION);
        }
        else if (source == "Next Histogram") {
			if (ch==null) {
				JOptionPane.showMessageDialog(this, "Not initialized yet... do a run first");
			}
			else {
				ch.doNextHistogram();
			}
		}
		else if (source == "RUN") {
			boolean doIt = true;
			if (shockCheckBox.isSelected() & allShockCheckBox.isSelected()) {
				JOptionPane.showMessageDialog(this, "Error with shock parameters!");
				doIt = false;
			}
			//outputFileName = getOutputFileName();
			file q = new file(outputFileField.getText());
			if (q.exists()) {
				if (JOptionPane.showConfirmDialog(this,"Overwrite file?") !=
					JOptionPane.OK_OPTION)  doIt = false;
			}
			if (outputFileField.getText().length() < 3) doIt = false;
			if (doIt) {
				System.out.println("Trying to load CtofHistogrammer");
				System.out.println(timeProfileCheckBox.isSelected()+"");
				ch = new CtofHistogrammer(this);
				ch.start();
				//Runner r = new Runner(this);
				//r.start();
				System.out.println("Should be running");
			}
		}
		//System.out.println(e+"");
	}

	// just keep tacking new components on at the bottom that you want to save
	private void saveSettings() {
		file f = new file(user_dir + file_sep + backupFileName);
		f.saveShit(
				polarMinField.getText() + CRLF +
				polarMaxField.getText() + CRLF +
				azimuthMinField.getText() + CRLF +
				azimuthMaxField.getText() + CRLF +
				magnitudeMinField.getText() + CRLF +
				magnitudeMaxField.getText() + CRLF +
				radialMinField.getText() + CRLF +
				radialMaxField.getText() + CRLF +
				vswMinField.getText() + CRLF +
				vswMaxField.getText() + CRLF +
				densityMinField.getText() + CRLF +
				densityMaxField.getText() + CRLF +
				vthMinField.getText() + CRLF +
				vthMaxField.getText() + CRLF +
				timeStartField.getText() + CRLF +
				timeEndField.getText() + CRLF +
				timeBinField.getText() + CRLF +
				outputFileField.getText() + CRLF +
			    vvBinField.getText() + CRLF +
				bCheckBox.isSelected() + CRLF +
				shockCheckBox.isSelected() + CRLF +
				allShockCheckBox.isSelected() + CRLF +
				shockTimeField.getText() + CRLF +
				swCheckBox.isSelected() + CRLF +
				timesCheckBox.isSelected() + CRLF +
				timeProfileCheckBox.isSelected() + CRLF +
				energyHistogramCheckBox.isSelected() + CRLF +
				gammaField.getText() + CRLF +
				alfvenMinField.getText() + CRLF +
				alfvenMaxField.getText() + CRLF +
				alfvenRatioMinField.getText() + CRLF +
				alfvenRatioMaxField.getText() + CRLF +
				doScatCheckBox.isSelected() + CRLF +
				histScatCheckBox.isSelected() + CRLF +
				scatXField.getText() + CRLF +
				scatYField.getText() + CRLF +
				scatBinsField.getText() + CRLF +
				correlationCheckBox.isSelected() + CRLF +
				pcorrelationCheckBox.isSelected() + CRLF +
				corrDeltaField.getText()+ CRLF +
				corrNumField.getText()+ CRLF +
				corrXField.getText()+ CRLF +
				corrYField.getText()+ CRLF +
				//diffCheckBox.isSelected() + CRLF +
				binStartField.getText()+ CRLF +
				binFinishField.getText() + CRLF +
				intervalsField.getText() + CRLF +
				c_outputFileField.getText() + CRLF +
				c_numFilesField.getText() + CRLF +
				catalogCheckBox.isSelected() + CRLF +

				specCheckBox.isSelected() + CRLF +
				spDivMinBinField.getText() + CRLF +

				spDivMaxBinField.getText() + CRLF +
				spDivMinField.getText() + CRLF +
				spDivMaxField.getText() + CRLF +
				spCurlMinBinField.getText() + CRLF +
				spCurlMaxBinField.getText() + CRLF +
				spCurlMinField.getText() + CRLF +
				spCurlMaxField.getText() + CRLF +
				spDivFileField.getText() + CRLF +
				spCurlFileField.getText() + CRLF +
				tailStartField.getText() + CRLF +
				magFileField.getText() + CRLF +


				parInterceptMinField.getText() + CRLF +
				parInterceptMaxField.getText() + CRLF +
				traceInterceptMinField.getText() + CRLF +
				traceInterceptMaxField.getText() + CRLF +
				parIndexMinField.getText() + CRLF +
				parIndexMaxField.getText() + CRLF +
				traceIndexMinField.getText() + CRLF +
				traceIndexMaxField.getText() + CRLF +
				ratioMinField.getText() + CRLF +
				ratioMaxField.getText() + CRLF +
				d_vvMinField.getText() + CRLF +
				d_vvMaxField.getText() + CRLF +
				heFileField.getText() + CRLF +

				electronCheckBox.isSelected() + CRLF +
				electronMinField.getText() + CRLF +
				electronMaxField.getText() + CRLF +
				electronFileField.getText()
				//numSegmentsField.setText(f.readLine());

				//numSegmentsField.getText() + CRLF +
				//histTypeComboBox.getSelectedIndex()
				//tailVswCheckBox.isSelected() + CRLF +
				//tailBCheckBox.isSelected() + CRLF +
				//tailAlfvenCheckBox.isSelected()
		);
	}

	private void loadSettings() {
		file f = new file(user_dir + file_sep + backupFileName);
		if (f.exists()) {
			f.initRead();
			polarMinField.setText(f.readLine());
			polarMaxField.setText(f.readLine());
			azimuthMinField.setText(f.readLine());
			azimuthMaxField.setText(f.readLine());
			magnitudeMinField.setText(f.readLine());
			magnitudeMaxField.setText(f.readLine());
			radialMinField.setText(f.readLine());
			radialMaxField.setText(f.readLine());
			vswMinField.setText(f.readLine());
			vswMaxField.setText(f.readLine());
			densityMinField.setText(f.readLine());
			densityMaxField.setText(f.readLine());
			vthMinField.setText(f.readLine());
			vthMaxField.setText(f.readLine());
			timeStartField.setText(f.readLine());
			timeEndField.setText(f.readLine());
			timeBinField.setText(f.readLine());
			outputFileField.setText(f.readLine());
			vvBinField.setText(f.readLine());
			bCheckBox.setSelected(parseB(f.readLine()));
			shockCheckBox.setSelected(parseB(f.readLine()));
			allShockCheckBox.setSelected(parseB(f.readLine()));
			shockTimeField.setText(f.readLine());
			swCheckBox.setSelected(parseB(f.readLine()));
			timesCheckBox.setSelected(parseB(f.readLine()));
			timeProfileCheckBox.setSelected(parseB(f.readLine()));
			energyHistogramCheckBox.setSelected(parseB(f.readLine()));
			gammaField.setText(f.readLine());
			alfvenMinField.setText(f.readLine());
			alfvenMaxField.setText(f.readLine());
			alfvenRatioMinField.setText(f.readLine());
			alfvenRatioMaxField.setText(f.readLine());
			doScatCheckBox.setSelected(parseB(f.readLine()));
			histScatCheckBox.setSelected(parseB(f.readLine()));
			scatXField.setText(f.readLine());
			scatYField.setText(f.readLine());
			scatBinsField.setText(f.readLine());
			//tailVswCheckBox.setSelected(parseB(f.readLine()));
			//tailBCheckBox.setSelected(parseB(f.readLine()));
			//tailAlfvenCheckBox.setSelected(parseB(f.readLine()));
			correlationCheckBox.setSelected(parseB(f.readLine()));
			pcorrelationCheckBox.setSelected(parseB(f.readLine()));
			corrDeltaField.setText(f.readLine());
			corrNumField.setText(f.readLine());
			corrXField.setText(f.readLine());
			corrYField.setText(f.readLine());
			//diffCheckBox.setSelected(parseB(f.readLine()));
			binStartField.setText(f.readLine());
			binFinishField.setText(f.readLine());
			intervalsField.setText(f.readLine());
			c_outputFileField.setText(f.readLine());
			c_numFilesField.setText(f.readLine());
			catalogCheckBox.setSelected(parseB(f.readLine()));
			specCheckBox.setSelected(parseB(f.readLine()));
			spDivMinBinField.setText(f.readLine());
			spDivMaxBinField.setText(f.readLine());
			spDivMinField.setText(f.readLine());
			spDivMaxField.setText(f.readLine());
			spCurlMinBinField.setText(f.readLine());
			spCurlMaxBinField.setText(f.readLine());
			spCurlMinField.setText(f.readLine());
			spCurlMaxField.setText(f.readLine());
			spDivFileField.setText(f.readLine());
			spCurlFileField.setText(f.readLine());
			tailStartField.setText(f.readLine());
			magFileField.setText(f.readLine());
			parInterceptMinField.setText(f.readLine());
			parInterceptMaxField.setText(f.readLine());
			traceInterceptMinField.setText(f.readLine());
			traceInterceptMaxField.setText(f.readLine());
			parIndexMinField.setText(f.readLine());
			parIndexMaxField.setText(f.readLine());
			traceIndexMinField.setText(f.readLine());
			traceIndexMaxField.setText(f.readLine());
			ratioMinField.setText(f.readLine());
			ratioMaxField.setText(f.readLine());
			d_vvMinField.setText(f.readLine());
			d_vvMaxField.setText(f.readLine());
			heFileField.setText(f.readLine());
			electronCheckBox.setSelected(parseB(f.readLine()));
			electronMinField.setText(f.readLine());
			electronMaxField.setText(f.readLine());
			electronFileField.setText(f.readLine());
			//numSegmentsField.setText(f.readLine());
			//histTypeComboBox.setSelectedIndex(Integer.parseInt(f.readLine()));
			f.closeRead();
		}
	}

	private boolean parseB(String s) {
		if (s.equals("true")) return true;
		if (s.equals("false")) return false;
		else {
			System.out.println("Error parsing boolean: " + s);
		}
		return false;
	}


	/*private String getOutputFileName() {
		String line = "soho_ctof_java_";
		boolean gotIt = false;
		int i = 1;
		while (!gotIt) {
			File f = new File(user_dir + file_sep + line + i + ".dat");
			if (f.exists()) i++;
			else {
				gotIt = true;
			}
		}
		return line + i + ".dat";
	}*/


	/** Fills an array with Integers from 0, 1, ... to length-1
	*
	*/
	public void fillArray(Integer[] ar) {
		for (int i=0; i<ar.length; i++) {
			ar[i]=new Integer(i);
		}
	}

	/** Fills an array with ints from 0, 1, ... to length-1
	*
	*/
	public void fillArray(int[] ar) {
		for (int i=0; i<ar.length; i++) {
			ar[i]=i;
		}
	}

	/** FROM JAVA FORUMS
	* Although JFileChooser won't give us this information,
	* we need it...
	*/
	public static File[] getSelectedFiles(JFileChooser chooser) {
		// FROM JAVA FORUMS
		// Although JFileChooser won't give us this information,
		// we need it...
		Container c1 = (Container)chooser.getComponent(3);
		JList list = null;
		while (c1 != null) {
	 		Container c = (Container)c1.getComponent(0);
			if (c instanceof JList) {
				list = (JList)c;
				break;
			}
			c1 = c;
		}
		Object[] entries = list.getSelectedValues();
		File[] files = new File[entries.length];
		for (int k=0; k<entries.length; k++) {
			if (entries[k] instanceof File)
				files[k] = (File)entries[k];
			}
		return files;
	}


	/** For our thermometer.  Only updates if the string is new.
	*
	*/
	public void updateStatus(String s) {
		if (!s.equals(statusLabel.getText())) {
			statusLabel.setText(s);
			// this takes time!
		}
	}

	/**
	* Use this method to get a nice STring
	* that describes all the settings
	* used in theMain.
	*/
	public String getParamString() {
		String outputString = "CTOF Gui Settings for CTOFHistogrammer output" + CRLF;
		Date d = new Date();
		outputString += "Date of Analysis: " + d + CRLF;
		// add the magnetic field parameters
		if (bCheckBox.isSelected()) {
			outputString += "Mag. angle to radial range: "
					+radialMinField.getText()+" "
					+radialMaxField.getText()+CRLF;
			outputString += "Azimuth range 1: "
					+azimuthMinField.getText()+" "
					+azimuthMaxField.getText()+ CRLF;
			outputString += "Polar range 1: "
					+polarMinField.getText()+" "
					+polarMaxField.getText()+ CRLF;
			outputString += "Magnitude range: "
					+magnitudeMinField.getText()+" "
					+magnitudeMaxField.getText()+ CRLF;
			outputString += "Angle to Radial Range: "
					+radialMinField.getText()+" "
					+radialMaxField.getText()+ CRLF;
			outputString += "Alfven Speed Range: "
					+alfvenMinField.getText() + " "
					+alfvenMaxField.getText() + CRLF;
			outputString += "Alfven speed to Vsw Ratio Range: "
					+alfvenRatioMinField.getText() + " "
					+alfvenRatioMaxField.getText() + CRLF;
		}
		else { outputString += "No Magnetic field constraints" + CRLF; }
		// add the solar wind parameters
		if (swCheckBox.isSelected()) {
			outputString += "Vsw range: "
					+vswMinField.getText()+" "
					+vswMaxField.getText()+ CRLF;
			outputString += "Density range: "
					+densityMinField.getText()+" "
					+densityMaxField.getText()+ CRLF;
			outputString += "Vth range: "
					+vthMinField.getText()+" "
					+vthMaxField.getText()+ CRLF;
		}
		else { outputString += "No Solar Wind Constraints" + CRLF; }
		// add shock parameters
		if (shockCheckBox.isSelected()) outputString += "Shock events discarded" + CRLF;
		if (allShockCheckBox.isSelected()) outputString += "Using only shock events" + CRLF;

		// output paramters
		outputString += "Efficiency adjust (E^gamma) gamma = "
				+gammaField.getText() + CRLF;
		outputString += "Using time range (days) : "
				+timeStartField.getText() + " - " + timeEndField.getText() + CRLF;
		outputString += "Tail Start (Cutoff) = "
				+tailStartField.getText() + CRLF;
		outputString += "Averaging interval (hrs) : "
				+timeBinField.getText() + CRLF;
		// that's all the info I think
		return outputString;
	}


	/**
	*  use this method to run this shit
	*/
	public static void main(String[] args) {
		CTOFGui pr = new CTOFGui();
	}

	/*
	public void outputError() {
		System.out.println("Command line ASCII dump for Cluster Level 1 Data");
		System.out.println("Currently supports product 3 & product 28");
		System.out.println("Usage: java ASCIIDump fileToRead fileToWrite (options)\n");
		System.out.println("fileToRead must be in official format with official name");
		System.out.println("Warning: fileToWrite will be overwritten!");
		System.out.println("Options:  ");
		System.out.println("-o  :  (for product 3) - make ascii in original form");
		System.out.println("Without this option, the data will be presented like PHA data.");
		System.exit(0);
	}*/

}
