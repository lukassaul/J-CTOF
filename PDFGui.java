

/**
*  This is a GUI for doing PDF analysis
*
*
*  Uses spectrum.java for computations - see that class for comments
*/
public class PDFGui extends JFrame implements ActionListener {

	private JPanel contentPane, buttonPanel;
	private JTextField fileNameField,

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

	}

	public void actionPerformed(ActionEvent e) {


		Object source = e.getActionCommand();
		System.out.println("processing event " + e);

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