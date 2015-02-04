/**
 * Title: MultipleJPaneTest
 *
 * Description: Test code for SGT problem example. I am trying to
 * display two data sets one above the other using two separate
 * JPanes. My ultimate goal is to have multiple layers stacked on each
 * of the JPanes. When I display just the axes and no data set attached
 * to the graphs, the axes are fine. When I add the top data set to
 * the graph, the axes are fine and the data is displayed. When I
 * add the bottom data set to the graph or both data sets the bottom
 * axes appear to be clipped. Any ideas on how to solve this. I have
 * tried to follow the logic in the draw() method but have not found
 * a solution.
 *
 * @author Paul Raab
 */

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;

import gov.noaa.pmel.sgt.*;
import gov.noaa.pmel.util.*;
import gov.noaa.pmel.sgt.dm.Collection;
import gov.noaa.pmel.sgt.dm.SimplePoint;
import gov.noaa.pmel.sgt.demo.TestData;

public class MultipleJPaneTest extends JPanel {

  private Dimension panelSize;

  private JPane paneTop;
  private JPane paneBottom;
  private boolean useTopData = true;
  private boolean useBottomData = true;

  /*
   * Global X-axis data
   */
  private SoTRange.Double xRange;

  private double xSize;
  private double xStart;
  private double xEnd;

  private double yTopSize;
  private double yTopStart;
  private double yTopEnd;

  private double yBottomSize;
  private double yBottomStart;
  private double yBottomEnd;

  public MultipleJPaneTest() {

    /**
     * Initialize main JPane
     */
    super();
    panelSize = new Dimension(1000, 800);
    this.setSize(panelSize);
    setBackground(Color.white);
    setForeground(Color.black);

    /*
     * Compute plot sizes for top and bottom JPanes
     */
    computePlotSize();

    /**
     * Set GridLayout on main JPanel, create and add
     * two child JPanes
     */
    setLayout(new GridLayout(2, 1));
    paneTop = new JPane("Top Pane",
      new Dimension((int)panelSize.getWidth(),
        (int)(panelSize.getHeight()/2.0)));
    paneTop.setBatch(true);
    paneTop.setBackground(Color.white);
    paneTop.setForeground(Color.black);
    paneTop.setLayout(new StackedLayout());
    add(paneTop);

    paneBottom = new JPane("Bottom Pane",
      new Dimension((int)panelSize.getWidth(),
        (int)(panelSize.getHeight()/2.0)));
    paneBottom.setBatch(true);
    paneBottom.setBackground(Color.white);
    paneBottom.setForeground(Color.black);
    paneBottom.setLayout(new StackedLayout());
    add(paneBottom);
  }

  /*
   * Set the flag for whether to plot the top JPane's data
   */
  public void setTopData(boolean flag) {
    useTopData = flag;
  }

  /*
   * Set the flag for whether to plot the bottom JPane's data
   */
  public void setBottomData(boolean flag) {
    useBottomData = flag;
  }

  /*
   * Create and draw all of the top JPane's graphic objects
   */
  public void drawTop() {

    /*
     * Set batch mode on
     */
    paneTop.setBatch(true);

    /*
     * Set x and y ranges
     */
    xRange = new SoTRange.Double(0.0, 1000.0);
    SoTRange.Double topYRange = new SoTRange.Double(0.0, 100.0);

    /*
     * Setup XY transformations
     */
    LinearTransform xTransform = new LinearTransform(xStart, xEnd,
      xRange.start, xRange.end);
    LinearTransform yTransform = new LinearTransform(yTopStart, yTopEnd,
      topYRange.start, topYRange.end);

    /*
     * Create top graph
     */
    CartesianGraph topGraph = new CartesianGraph("Top Graph");
    topGraph.setXTransform(xTransform);
    topGraph.setYTransform(yTransform);

    /*
     * Create the bottom axis, set its range in user units
     * and its origin. Add the axis to the graph.
     */
    SoTPoint origin = new SoTPoint(xRange.start, topYRange.start);
    PlainAxis xAxis = new PlainAxis("Top JPane Bottom Axis");
    xAxis.setRangeU(xRange);
    xAxis.setLocationU(origin);
    Font xAxisFont = new Font("Helvetica", Font.PLAIN, 14);
    xAxis.setLabelFont(xAxisFont);
    xAxis.setTicPosition(Axis.POSITIVE_SIDE);
    xAxis.setLabelPosition(Axis.POSITIVE_SIDE);
    topGraph.addXAxis(xAxis);

    /*
     * Create the left axis, set its range in user units
     * and its origin. Add the axis to the graph.
     */
    origin = new SoTPoint(xRange.start, topYRange.start);
    PlainAxis yAxis = new PlainAxis("Top JPane Left Axis");
    yAxis.setRangeU(topYRange);
    yAxis.setLocationU(origin);
    Font yAxisFont = new Font("Helvetica", Font.PLAIN, 14);
    yAxis.setLabelFont(yAxisFont);
    topGraph.addYAxis(yAxis);

    /*
     * Remove previous top layer
     */
    try {
      if (paneTop.getLayer("Top Layer") != null) {
 paneTop.remove(paneTop.getLayer("Top Layer"));
      }
    }
    catch (Exception e) {
    }

    /*
     * Create and add layer
     */
    Layer topLayer = new Layer("Top Layer",
      new Dimension2D(xSize, yTopSize));
    topLayer.setGraph(topGraph);

    /*
     * Build test data set of points
     */
    if (useTopData) {
      TestData td;
      Collection col;
      Range2D xr = new Range2D(xRange.start, xRange.end);
      Range2D er = new Range2D(topYRange.start, topYRange.end);
      td = new TestData(xr, er, 20);
      col = td.getCollection();
      PointAttribute pattr = new PointAttribute(10, Color.red);
      topGraph.setData(col, pattr);
    }

    /*
     * Add layer to top JPane
     */
    paneTop.add(topLayer);
    paneTop.setBatch(false);
  }

  /*
   * Create and draw all of the bottom JPane's graphic objects
   */
  public void drawBottom() {

    /*
     * Set batch mode on
     */
    paneBottom.setBatch(true);

    /*
     * Set x and y ranges
     */
    xRange = new SoTRange.Double(10.0, 40.0);
    SoTRange.Double bottomYRange = new SoTRange.Double(-2000.0, 500.0);

    /*
     * Setup XY transformations
     */
    LinearTransform xTransform = new LinearTransform(xStart, xEnd,
      xRange.start, xRange.end);
    LinearTransform yTransform = new LinearTransform(yBottomStart, yBottomEnd,
      bottomYRange.start, bottomYRange.end);

    /*
     * Create bottom graph
     */
    CartesianGraph bottomGraph = new CartesianGraph("Bottom Graph");
    bottomGraph.setXTransform(xTransform);
    bottomGraph.setYTransform(yTransform);

    /*
     * Create the bottom axis, set its range in user units
     * and its origin. Add the axis to the graph.
     */
    SoTPoint origin = new SoTPoint(xRange.start, bottomYRange.end);
    PlainAxis xAxis = new PlainAxis("Bottom JPane Bottom Axis");
    xAxis.setRangeU(xRange);
    xAxis.setLocationU(origin);
    Font xAxisFont = new Font("Helvetica", Font.PLAIN, 14);
    xAxis.setLabelFont(xAxisFont);
    xAxis.setTicPosition(Axis.NEGATIVE_SIDE);
    xAxis.setLabelPosition(Axis.NEGATIVE_SIDE);
    bottomGraph.addXAxis(xAxis);

    /*
     * Create the left axis, set its range in user units
     * and its origin. Add the axis to the graph.
     */
    origin = new SoTPoint(xRange.start, bottomYRange.start);
    PlainAxis yAxis = new PlainAxis("Bottom JPane Left Axis");
    yAxis.setRangeU(bottomYRange);
    yAxis.setLocationU(origin);
    Font yAxisFont = new Font("Helvetica", Font.PLAIN, 14);
    yAxis.setLabelFont(yAxisFont);
    bottomGraph.addYAxis(yAxis);

    /*
     * Remove previous bottom layer
     */
    try {
      if (paneBottom.getLayer("Bottom Layer") != null) {
 paneBottom.remove(paneBottom.getLayer("Bottom Layer"));
      }
    }
    catch (Exception e) {
    }

    /*
     * Create and add layer
     */
    Layer bottomLayer = new Layer("Bottom Layer",
      new Dimension2D(xSize, yBottomSize));
    bottomLayer.setGraph(bottomGraph);

    /*
     * Build test data set of points
     */
    if (useBottomData) {
      TestData td;
      Collection col;
      Range2D xr = new Range2D(xRange.start, xRange.end);
      Range2D er = new Range2D(bottomYRange.start, bottomYRange.end);
      td = new TestData(xr, er, 20);
      col = td.getCollection();
      PointAttribute pattr = new PointAttribute(20, Color.blue);
      bottomGraph.setData(col, pattr);
    }

    /*
     * Add layer to bottom JPane
     */
    paneBottom.add(bottomLayer);
    paneBottom.setBatch(false);
  }

  /*
   * Compute nice plot dimensions
   */
  public void computePlotSize() {

    /**
     * Set XY dimensions in physical units
     *
     * xSize, ySize are the width and height in physical units
     * of the Layer graphics region.
     *
     * xStart, xEnd are the start and end points for the X axis
     * yStart, yEnd are the start and end points for the Y axis
     */

  }

  /**
   * Main method for illustrating the problem. A loop is used. To
   * see how I would like the axes to be displayed, enter "NEITHER" or
   * "TOP". To see how the bottom axes become obscured, enter "BOTH"
   * or "BOTTOM". To quit, enter "END".
   */
  public static void main(String[] args) {
    MultipleJPaneTest test = new MultipleJPaneTest();
    JFrame frame = new JFrame("Multiple JPane Test");
    frame.setSize(new Dimension(1000, 800));
    frame.setLocation(0, 0);
    frame.getContentPane().setLayout(new BorderLayout());
    frame.addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent event) {
        System.exit(0);
      }
    });
    frame.getContentPane().add(test, BorderLayout.CENTER);
    frame.setVisible(true);
    System.out.println(
     "Enter type of display for testing\n" +
     "NEITHER(default) - display only the axes for top and bottom JPanes\n" +
     "BOTH - display axes and data in both top and bottom JPanes\n" +
     "TOP - display axes in both and data in only the top JPane\n" +
     "BOTTOM - display axes in both and data in only the bottom JPane\n" +
     "END - terminate test program\n");
    String answer = readLine("Enter type: ");
    while (true) {
      if (answer.trim().toUpperCase().equals("BOTH")) {
        test.setTopData(true);
        test.setBottomData(true);
      }
      else if (answer.trim().toUpperCase().equals("NEITHER") ||
        answer.trim().length() == 0) {
        test.setTopData(false);
        test.setBottomData(false);
      }
      else if (answer.trim().toUpperCase().equals("TOP")) {
        test.setTopData(true);
        test.setBottomData(false);
      }
      else if (answer.trim().toUpperCase().equals("BOTTOM")) {
        test.setTopData(false);
        test.setBottomData(true);
      }
      else if (answer.trim().toUpperCase().equals("END")) {
        frame.dispose();
        System.exit(0);
      }
      test.drawTop();
      test.drawBottom();
      test.revalidate();
      answer = readLine("Enter type: ");
    }
  }

 /**
  * An easy interface to read strings from
  * standard input.
  * Derived from: Console.java in Core Java 2 - Vol. 1
  * @version 1.10 10 Mar 1997
  * @author Cay Horstmann
  */
  public static String readLine(String prompt) {
    System.out.print(prompt + " ");
    System.out.flush();
    int ch;
    String r = "";
    boolean done = false;
    while (!done) {
      try {
        ch = System.in.read();
        if (ch < 0 || (char)ch == '\n') {
          done = true;
        }
        else if ((char)ch != '\r') {
          r = r + (char)ch;
        }
      }
      catch(java.io.IOException e) {
        done = true;
      }
    }
    return r;
  }

}