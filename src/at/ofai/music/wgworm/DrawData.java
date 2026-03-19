package at.ofai.music.wgworm;

import at.ofai.music.plot.EpsGraphics2D;
import at.ofai.music.plot.M;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Line2D;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import javax.swing.JPanel;

/**
 * Draws 2-dim data as provided by unsmoothed worm files (i.e., tempo and
 * loudness info).
 * <br>[Does not use the plot package.]
 *
 * @author Werner Goebl
 *
 */
public class DrawData extends JPanel {

    //private static final String version = "0.55; 9 April 2007";
    private static final String version = "0.56; 11 Sept 2012";
    private static final long serialVersionUID = 1L;
    protected MyWormFile wormData, wormData2;
    protected double currTime = 0;
    int xSize, ySize;
    private static final Dimension defaultSize = new Dimension(590, 215);
    private double xScale, yScale;
    private double xShift, yShift;
    private double xMarginL, xMarginR, yMargin;
    private FontMetrics fm;
    private double maxData, minData, minTime, maxTime;
    private int axisFontSize, barNumberFontSize;
    private boolean yAxisLog; // TODO: BUG: does not work, not used (Sept 2012)
    private boolean plotSecondaryData = false;
    private LinkedList<Double> xAxisLabels, yAxisLabels;
    private int[] barNumbers;
    private double xWindow; // seconds
    private boolean plotTpo;
    private Color mColor, accColor, acc2Color, gray;
    private boolean showMelodyInfo = false;
    private boolean printScreen = false; // for EPS output
    private boolean antiAli = true; // antiAliasing
    private boolean refreshStats = false; // shows refresh statistics every 3 seconds
    private long[] repaintTimes = new long[50];
    private int rptInd = 0; // repaintTimesIndex

    public DrawData(MyWormFile w) { // constructor
        this(w, true);
    }

    public DrawData(MyWormFile w, boolean plTpo) { // constructor
        this(w, plTpo, defaultSize);
    }

    public DrawData(MyWormFile w, boolean plTpo, Dimension d) { // constructor
        this(w, plTpo, d, false);
    }

    public DrawData(MyWormFile w, boolean plTpo, Dimension d, boolean log) {
        this(w, plTpo, d, log, null);
    }

    public DrawData(MyWormFile w, boolean plTpo, Dimension d, boolean log, MyWormFile w2) {
        yAxisLog = log; // true: at.ofai.music.plot yAxis logarithmically
        plotTpo = plTpo;
        wormData = w; // the whole worm data as read by MyWormFile.read
        wormData2 = w2; // secondary wormData, usually the smoothed info
        currTime = 0; // index of the current time
        xWindow = 10.0;
        xSize = d.width;
        ySize = d.height;
        xScale = 1.0;
        yScale = 1.0;
        xShift = 0.0;
        yShift = 0.0;
        xAxisLabels = new LinkedList<Double>();
        yAxisLabels = new LinkedList<Double>();
        barNumbers = new int[wormData.length]; // create an array of barNumbers
        int barNumber = wormData.startBarNumber - 1;
        for (int i = 0; i < wormData.length; i++) {
            if ((wormData.flags[i] & MyWormFile.BAR) != 0) {
                barNumber++;
            }
            barNumbers[i] = barNumber;
        }
        mColor = new Color(230, 30, 30); // melody color
        accColor = new Color(30, 30, 30); // accomp color
        acc2Color = new Color(230, 130, 130); // accomp color
        gray = new Color(170, 170, 170);
    } // DrawData constructor

    public void update(double t) {
        currTime = t;
        repaint();
    }

    public void paint(Graphics g1) {
        long startTime = 0l;
        if (refreshStats) {
            startTime = System.nanoTime();
        }
        double ct = currTime;
        Graphics2D g = (Graphics2D) g1;
        if (antiAli) {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON); // drawing nicer
        }
        xAxisLabels.clear();
        yAxisLabels.clear();
        int width = getWidth();
        int height = getHeight();
        float scaleRef = (width + height) / 2;
        int r = (int) Math.round(scaleRef / 192.0); // marker radius

        xMarginL = width * .025;
        xMarginR = width * .05;
        yMargin = height * .1;
        axisFontSize = (int) Math.round(2 + scaleRef * .03);
        barNumberFontSize = (int) Math.round(2 + scaleRef * .03);
        //titleFontSize = (int) Math.round(7 + scaleRef * .03);
        //labelFontSize = (int) Math.round(4 + scaleRef * .03);
        minTime = ct - xWindow;
        maxTime = ct; // seconds
        File epsFile = null;
        if (printScreen) { // print to EPS output instead of screen
            String what = "-INT";
            if (plotTpo) {
                what = "-TPO";
            }
            String fileName = wormData.wormName.substring(0,
                    wormData.wormName.lastIndexOf(".")) + what + ".worm.eps";
            epsFile = new File(fileName);
            System.out.print("DrawData; EPS OutputFile: " + fileName + " ... ");
            try {
                g = new EpsGraphics2D(epsFile.getName(),
                        epsFile, 0, 0, width, height);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // recalc with real extent of window
        minData = Double.MAX_VALUE;
        maxData = 0.0;
        int startIndex = 0; // first relevant line in wormData
        for (int i = 0; i < wormData.length; i++) {
            if (wormData.time[i] > minTime - 2
                    && wormData.time[i] < maxTime + 2) {
                if (startIndex == 0) {
                    startIndex = i; // remember first relevant line in wormData
                }
                if (plotTpo && wormData.tempo[i] > maxData) {
                    maxData = wormData.tempo[i];
                }
                if (plotTpo && wormData.tempo[i] < minData) {
                    minData = wormData.tempo[i];
                }
                if (!plotTpo && wormData.intensity[i] > maxData) {
                    maxData = wormData.intensity[i];
                }
                if (!plotTpo && wormData.intensity[i] < minData) {
                    minData = wormData.intensity[i];
                }
            }
            if (wormData.time[i] > maxTime + 2) {
                break;
            }
        }
        minData -= (maxData - minData) * .05;
        maxData += (maxData - minData) * .05;
        if (yAxisLog && !plotTpo) {
            minData = log2(minData);
            maxData = log2(maxData);
        }
        xScale = ((double) (width) - xMarginL - xMarginR) / (maxTime - minTime);
        yScale = ((double) (height) - 2.0 * yMargin) / (maxData - minData);
        xShift = (minTime * xScale) - xMarginL;
        yShift = (minData * yScale) - yMargin;

        // draw frames and background
        g.clearRect(0, 0, width, height);
        if (printScreen) {
            g.setColor(Color.white);
        } else {
            g.setColor(Color.lightGray);
        }

        g.fillRect(0, 0, width, height);
        g.setColor(Color.white);
        g.fillRect((int) xMarginL, (int) (yMargin),
                (int) (width - xMarginL - xMarginR),
                (int) (height - 2 * yMargin));
        g.setColor(Color.black);
        g.drawRect((int) xMarginL, (int) (yMargin),
                (int) (width - xMarginL - xMarginR),
                (int) (height - 2 * yMargin));

        // draw axis ticks and labels
        int maxTicks = 11; // max number of ticks per axis
        int tickHeight = 5; // lenght/height of ticks (pixels)
        g.setFont(new Font("Arial", Font.PLAIN, axisFontSize));
        fm = g.getFontMetrics();
        double s1, s2;
        // calc for x axis
        s1 = minTime;
        s2 = maxTime; // min max Time (shifts over Time)
        double stepSize = Math.ceil((s2 - s1) / maxTicks);
        for (int i = (int) (Math.ceil(s1)); i <= s2; i++) {
            if (i % stepSize == 0) {
                xAxisLabels.add((double) i);
            }
        }
        // draw x Axis Labels
        for (Iterator<Double> i = xAxisLabels.iterator(); i.hasNext();) {
            double j = i.next();
            String label = Integer.toString((int) j);
            g.drawLine((int) (j * xScale - xShift), (int) yMargin, (int) (j
                    * xScale - xShift), (int) (yMargin + tickHeight));
            g.drawLine((int) (j * xScale - xShift),
                    (int) (height - yMargin), (int) (j * xScale - xShift),
                    (int) (height - yMargin - tickHeight));
            int wd = fm.stringWidth(label);
            g.drawString(label, (int) (j * xScale - xShift - wd / 2),
                    (int) (height - yMargin + fm.getAscent()));
        }
        // calc for y axis
        maxTicks = 7;
        if (yAxisLog) {
            s1 = Math.pow(2, minData);
            s2 = Math.pow(2, maxData);
        } else {
            s1 = minData;
            s2 = maxData;
        }
        stepSize = Math.ceil((s2 - s1) / maxTicks);
        for (int i = (int) (Math.ceil(s1)); i <= s2; i++) {
            if (i % stepSize == 0) {
                yAxisLabels.add((double) i);
            }
        }
        // draw y Axis Labels
        for (Iterator<Double> i = yAxisLabels.iterator(); i.hasNext();) {
            double j;
            double tmp = i.next();
            if (yAxisLog) {
                j = log2(tmp);
            } else {
                j = tmp;
            }
            String label = Integer.toString((int) tmp);
            g.drawLine((int) (xMarginL),
                    (int) (height - (j * yScale - yShift)),
                    (int) (xMarginL + tickHeight), (int) (height - (j
                    * yScale - yShift)));
            g.drawLine((int) (width - xMarginR), (int) (height - (j
                    * yScale - yShift)),
                    (int) (width - xMarginR - tickHeight),
                    (int) (height - (j * yScale - yShift)));
			// draw y labels left
			/*int wd = fm.stringWidth(label);
             g.drawString(label, (int) (xMarginL - wd * 1.1),
             (int)(maxHeight - ((j * yScale - yShift) - 
             (fm.getAscent() - fm.getDescent()) / 2)));*/
            // draw y labels right
            g.drawString(label, (int) (width - xMarginR * .9),
                    (int) (height - ((j * yScale - yShift)
                    - (fm.getAscent() - fm.getDescent()) / 2)));
        }

        g.setFont(new Font("Arial", Font.BOLD, barNumberFontSize));
        fm = g.getFontMetrics();

        int currX, currY;
        int oldX = -1, oldY = -1;
        int oldMX = -1, oldMY = -1;

        if (plotTpo) { // TEMPO
            oldX = (int) (wormData.time[0] * xScale - xShift);
            oldY = height - (int) (wormData.tempo[0] * yScale - yShift);
        } else { //       INTENSITY
            oldX = (int) (wormData.time[0] * xScale - xShift);
            oldY = height - (int) (wormData.intensity[0] * yScale - yShift);
            if (wormData.melody[0] > 0 && showMelodyInfo) {
                oldMX = (int) (wormData.time[0] * xScale - xShift);
                oldMY = height - (int) (wormData.intensity[0] * yScale - yShift);
                oldX = -1;
            }
        }
        g.setColor(accColor);
        if (wormData.melody[0] > 0 && !plotTpo && showMelodyInfo) {
            g.setColor(mColor); // melody color
        }
        g.drawOval(oldX - r, oldY - r, 2 * r, 2 * r); // first data point

        // draw data curve and barlines
        for (int j = startIndex; j < wormData.length &&// go through worm file
                wormData.time[j] <= ct; j++) { //   from relevant line on.
            if (wormData.time[j] > (ct - xWindow - 2)) {
                currX = (int) (wormData.time[j] * xScale - xShift);
                if (plotTpo) {
                    currY = height - (int) (wormData.tempo[j] * yScale - yShift);
                } else {
                    currY = height - (int) (wormData.intensity[j] * yScale - yShift);
                }
                g.setColor(accColor);
                if (wormData.melody[j] > 0 && !plotTpo && showMelodyInfo) {
                    g.setColor(mColor); // melody color
                }
                if (wormData.melody[j] == 0 && oldX >= 0 || plotTpo || !showMelodyInfo) {
                    g.drawLine(oldX, oldY, currX, currY); // draw acc data curve
                }
                if (wormData.melody[j] > 0 && oldMX >= 0 && !plotTpo) {
                    g.drawLine(oldMX, oldMY, currX, currY); // draw acc data curve
                }
                g.drawOval(currX - r, currY - r, 2 * r, 2 * r); // draw data markers

                if ((wormData.flags[j] & MyWormFile.BAR) != 0) { // bar lines
                    String bn = Integer.toString(barNumbers[j]);
                    int wd = fm.stringWidth(bn);
                    g.setColor(gray);
                    g.drawLine(currX, (int) (height - yMargin), currX,
                            (int) (yMargin));
                    g.setColor(Color.black);
                    int ovalWidth = (int) (yMargin);
                    if (wd * 1.2 > yMargin) {
                        ovalWidth = (int) (wd * 1.25);
                    }
                    g.drawOval((int) (currX - ovalWidth / 2), 0, ovalWidth,
                            (int) (yMargin));
                    g.drawString(bn, currX - wd / 2, (int) ((yMargin
                            + fm.getAscent() - fm.getDescent()) / 2));
                }
                oldX = (int) (wormData.time[j] * xScale - xShift);
                if (plotTpo) {
                    oldY = height - (int) (wormData.tempo[j] * yScale - yShift);
                } else {
                    oldY = height - (int) (wormData.intensity[j] * yScale - yShift);
                    if (wormData.melody[j] == 1 && showMelodyInfo) {
                        oldMX = (int) (wormData.time[j] * xScale - xShift);
                        oldMY = height - (int) (wormData.intensity[j] * yScale - yShift);
                        oldX = -1;
                    }
                    if (wormData.melody[j] == 2 && showMelodyInfo) {
                        oldMX = -1;
                        oldX = -1;
                    }
                }
            }
        }

        if (plotSecondaryData) {
            Double currX2 = -1.0, currY2 = -1.0, oldX2 = null, oldY2 = null;
            // draw data curve and barlines of secondary worm file info (i.e. smoothed file)
            for (int j = 0; j < wormData2.length &&// go through worm file
                    wormData2.time[j] <= maxTime; j++) { //   from relevant line on.
                //System.out.println("oldXY/currXY: "+oldX2 +"-"+oldY2+"/"+currX2+"-"+currY2+"; j="+j+"/"+wormData2.length);
                if (wormData2.time[j] > (minTime - 2)) {
                    currX2 = (wormData2.time[j] * xScale - xShift);
                    if (plotTpo) {
                        currY2 = height - (wormData2.tempo[j] * yScale - yShift);
                    } else {
                        currY2 = height - (wormData2.intensity[j] * yScale - yShift);
                    }
                    g.setColor(acc2Color);
                    if (oldX2 != null) {
                        g.draw(new Line2D.Double(oldX2, oldY2, currX2, currY2));
                    }
                    oldX2 = currX2;
                    oldY2 = currY2;
                }
            }
        }

        if (printScreen) {
            try {
                ((EpsGraphics2D) g).flush();
                ((EpsGraphics2D) g).close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            printScreen = false;
            System.out.println("done.");
            repaint();
            return;
        }
        if (refreshStats) {
            repaintTimes[rptInd++] = System.nanoTime() - startTime;
            if (rptInd >= repaintTimes.length) {
                System.out.println("Mean draw time last 50 repaints "
                        + "DrawData(Tpo=" + plotTpo + "):\t"
                        + M.mean(repaintTimes) / 1000000.0 + "ms");
                rptInd = 0;
            }
        }

    } // paint

    private static double log2(double x) {
        return (Math.log(x) / Math.log(2.0));
    } // log2

    public static String getVersion() {
        return version;
    } // getVersion()

    /**
     * Shows any melody information stored in the 5th column of the worm file in
     * the intensity graph.
     *
     * @param showMelodyInfo
     */
    public void setShowMelodyInfo(boolean showMelodyInfo) {
        this.showMelodyInfo = showMelodyInfo;
    }

    public boolean isShowMelodyInfo() {
        return showMelodyInfo;
    }

    public void printScreen() {
        printScreen = true;
        repaint();
    }

    public double getXWindow() {
        return xWindow;
    }

    public void setXWindow(double window) {
        xWindow = window;
    }

    public void increaseXWindow() {
        xWindow++;
    }

    public void decreaseXWindow() {
        if (xWindow > 1.0) {
            xWindow--;
        }
    }

    public void setAntiAliasing(boolean antiAl) {
        this.antiAli = antiAl;
    }

    public void setRefreshStats(boolean refreshStats) {
        this.refreshStats = refreshStats;
    }

    public boolean isRefreshStats() {
        return refreshStats;
    }

    public static Dimension getDefaultsize() {
        return defaultSize;
    }

    public boolean isPlotSecondaryData() {
        return plotSecondaryData;
    }

    public void setPlotSecondaryData(boolean plotSecondaryData) {
        this.plotSecondaryData = plotSecondaryData;
    }

} // DrawData
