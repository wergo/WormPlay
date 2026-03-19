/*
 TODOs: (17 May 2015)

 Problem: for video export, screen shots are taken. Draw all stuff to an internal 
 image and then write it to GIF file. -- done!

 TODO:    change text into double/float text (entire font rendering has to be 
 done differently, as in plot/text.java). -- half done. I don't know why the double 
 version wobbles around (even the 2-digit bar numbers have changing font distance)
 Now: for screen display, double resolution switched off.

 */
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
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

/**
 * DrawWorm displays the Performance Worm nicely and very fast. Independent of
 * the plot package and faster. <br>
 * <br>
 * <b>Latest version:</b> 14 April 2007: uses background image to draw gray
 * portion of worm (=a lot !! faster, especially for long worms).
 * <br>
 * <br>
 * printScreen implemented (CTRL + P to produce a EPS file from worm).
 * <b>Latest version:</b> 19 August 2012: Drawing in Double precision added (why
 * not earlier?). With "D", you can switch between Integer and Double precision
 * in painting.
 * <br>
 * <br>
 *
 * @author Werner Goebl, 2005--2015
 */
public class DrawWorm extends JPanel {

    //private static final String version = "0.57; 16 Dec 2008"; 
    //private static final String version = "0.58; 19 August 2012"; 
    //private static final String version = "0.59; 28 September 2012"; // statusText eingebaut 
    private static final String version = "0.60; 20 May 2015"; // video export accelerated; worm face double precision bug eased, but not solved
    // TODO: Bug: nach einer Weile funktioniert der Status Anzeiger nicht mehr. Nach STOP Taste gehts wieder. 
    private static final long serialVersionUID = 1L;
    protected JFrame frame = null;
    protected MyWormFile wormData;
    protected int currSlice;
    protected int oldSlice;
    int xSize;
    int ySize;
    private int width = 0, oldWidth = 0;
    private int height = 0, oldHeight = 0;
    protected Graphics2D gImage;
    protected BufferedImage img;
    private double xScale, yScale;
    private double xShift, yShift;
    private double xMargin, yMargin;
    private final int tickHeight = 5; // lenght/height of ticks (pixels)
    private double wDiameter;
    private int tailLength;
    final Color[] red, black;
    private FontMetrics fm;
    private double maxTpo, maxInt, minTpo, minInt;
    private int axisFontSize, barNumberFontSize, titleFontSize, labelFontSize, statusTextFontSize;
    private boolean xAxisLog;
    private LinkedList<Double> xAxisLabels, yAxisLabels;
    private int[] barNumbers;
    private boolean printScreen = false; // for EPS output
    private boolean captureScreen = false; // for PNG/GIF/JPG output (screen shot of JPanel)
    private BufferedImage outputImage;
    private boolean refreshStats = false; // shows refresh statistics every 3 seconds
    private final long[] repaintTimes = new long[50];
    private int rptInd = 0; // repaintTimesIndex
    private boolean whiteBackground = false;
    //private Figure fig = new Figure("Check Times.");
    private String outputFormatString = "gif";
    boolean painting = false;
    boolean aliasingOn = true;
    private int frameLabel = -1; // label the output frame (gif,jpg) 
    private boolean drawDoublePrecision = true;
    private String statusText = ""; // displays status messages (such as "visDelay= -30 ms") 
    private float statusTextShade = 0f; // black, fades away, when not needed, down to 0.0
    private final int defaultCount = 20; // number of cycles nothing changes
    private int statusTextcount = defaultCount; // 
    private final float fadeThreshold = Color.lightGray.getRed() / 255f;
    private int threadSleepTime = -999;
    static final Locale l = Locale.ENGLISH;

    public DrawWorm(MyWormFile w) {
        this(w, new Dimension(430, 430));
    }

    public DrawWorm(MyWormFile w, Dimension d) {
        this(w, d, true);
    }

    public DrawWorm(MyWormFile w, Dimension d, boolean log) { // constructor
        xAxisLog = log; // true: at.ofai.music.plot xAxis logarithmically
        wormData = w; // the whole worm data as read by MyWormFile.read
        currSlice = 0; // index of the current worm slice
        oldSlice = 0; // index of the previous worm slice
        tailLength = wormData.tailLength;
        red = ColorFade.fadeRed(tailLength, -0.0125);
        black = ColorFade.fadeBlack(tailLength, -0.005);
        xSize = d.width; // size of window (JFrame) in pixels
        ySize = d.height;
        xScale = 1.0;
        yScale = 1.0;
        xShift = 0.0;
        yShift = 0.0;
        if (Double.isNaN(wormData.axis[0])) {
            // scale automatically to maxs
            minTpo = Double.MAX_VALUE;
            maxTpo = 0.0;
            minInt = Double.MAX_VALUE;
            maxInt = 0.0;
            for (int i = 0; i < wormData.tempo.length; i++) {
                maxTpo = Math.max(maxTpo, wormData.tempo[i]);
                maxInt = Math.max(maxInt, wormData.intensity[i]);
                minTpo = Math.min(minTpo, wormData.tempo[i]);
                minInt = Math.min(minInt, wormData.intensity[i]);
            }
            double roundTo = 5.0;
            minTpo = Math.floor(minTpo / roundTo) * roundTo;
            maxTpo = Math.ceil(maxTpo / roundTo) * roundTo;
            minInt = Math.floor(minInt / roundTo) * roundTo;
            maxInt = Math.ceil(maxInt / roundTo) * roundTo;
        } else {
            minTpo = wormData.axis[0];
            maxTpo = wormData.axis[1];
            minInt = wormData.axis[2];
            maxInt = wormData.axis[3];
        }
        if (xAxisLog) {
            minTpo = log2(minTpo);
            maxTpo = log2(maxTpo);
        }
        int maxTicks = 11; // max number of ticks per axis
        xAxisLabels = new LinkedList<>();
        double s1, s2; // determine xtmp Axes Labels
        if (xAxisLog) {
            s1 = Math.pow(2, minTpo);
            s2 = Math.pow(2, maxTpo);
        } else {
            s1 = minTpo;
            s2 = maxTpo;
        }
        double stepSize = Math.ceil((s2 - s1) / maxTicks);
        for (int i = (int) (Math.ceil(s1)); i <= s2; i++) {
            if (i % stepSize == 0) {
                xAxisLabels.add((double) i);
            }
        }
        yAxisLabels = new LinkedList<>();
        s1 = minInt;
        s2 = maxInt;
        stepSize = Math.ceil((s2 - s1) / maxTicks);
        for (int i = (int) (Math.ceil(s1)); i <= s2; i++) {
            if (i % stepSize == 0) {
                yAxisLabels.add((double) i);
            }
        }
        // pre-calculate barnumbers
        barNumbers = new int[wormData.tempo.length];
        int barNumber = wormData.startBarNumber - 1;
        for (int i = 0; i < wormData.tempo.length; i++) {
            if ((wormData.flags[i] & MyWormFile.BAR) != 0) {
                barNumber++; // update barNumber
            }
            barNumbers[i] = barNumber;
        }
    } // DrawWorm constructor


    /*
     * redraws the worm at the current slice (int)
     */
    public void redraw(int i) {
        //if (i < 1) i = 1;
        if (i > wormData.length) {
            i = wormData.length;
        }
        currSlice = i;
        //render(); // to try out double buffer, but it did not seem right...
        repaint();
    } // addSlice

    BufferStrategy myStrategy;

    public void setStrategy(BufferStrategy _strategy) {
        myStrategy = _strategy;
    }

    /**
     * Try out to use double buffering; but without success (23/5/2015)
     */
    public void render() {
        Graphics2D g2 = (Graphics2D) myStrategy.getDrawGraphics();
        try {
            drawWorm(g2);
        } finally {
            g2.dispose();
        }
        myStrategy.show();
    }

    @Override
    public void paint(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        drawWorm(g2);
    }

    /**
     * This is the general rendering of the worm (incl axes, etc)
     * @param g2 
     */
    public void drawWorm(Graphics2D g2) {
        painting = true;
        long startTime = 0l;
        if (refreshStats) {
            startTime = System.nanoTime();
        }
        int cs = currSlice;
        //System.out.println("Text AntiAliasing: " + g2.getRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING) + 
        //", Text Fractional Metricts: " + g2.getRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS));

        File epsFile;
        String outputName = "";
        if (printScreen) { // print to EPS output instead of screen
            if (frameLabel < 0) {
                outputName = wormData.wormName + "_" + String.format("%04d", currSlice) + ".eps";
            } else {
                outputName = wormData.wormName + "_" + String.format("%04d", frameLabel) + ".eps";
            }
            epsFile = new File(outputName);
            System.out.print("Writing EPS OutputFile: " + outputName + " ... ");
            try {
                g2 = new EpsGraphics2D(epsFile.getName(), epsFile, 0, 0, width, height);
            } catch (IOException e) {
            }
        }
        if (captureScreen) { // print to PNG/GIF/JPG output instead of screen
            String subdir = wormData.wormName.substring(0, wormData.wormName.length() - 5);
            System.out.println(subdir);
            if (!(new File(subdir).isDirectory())) {
                new File(subdir).mkdir();
            }
            if (frameLabel < 0) {
                outputName = subdir + File.separatorChar + new File(wormData.wormName).getName() + "_" + String.format("%04d", currSlice) + "." + outputFormatString;
            } else {
                outputName = subdir + File.separatorChar + new File(wormData.wormName).getName() + "_" + String.format("%04d", frameLabel) + "." + outputFormatString;
            }
            outputImage = getGraphicsConfiguration().createCompatibleImage(width, height);
            g2 = (Graphics2D) outputImage.getGraphics();
            System.out.print("Writing Image OutputFile: " + outputName + " ... ");
        }
        if (aliasingOn) {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); // drawing nicer
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        }
        //g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        //g2.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_ENABLE);
        //g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        width = getWidth();
        height = getHeight();

        // check if resized or rewinded
        if (width != oldWidth || height != oldHeight || oldSlice > cs || printScreen || captureScreen) {
            oldSlice = 0;
            img = null;
        }

        float scaleRef = (width + height) / 2;
        xMargin = width * .1;
        yMargin = height * .1;
        wDiameter = 16.0 + scaleRef * .03;// worm diameter(pxls)
        axisFontSize = (int) Math.round(2 + scaleRef * .03);
        barNumberFontSize = (int) Math.round(7 + scaleRef * .02);
        titleFontSize = (int) Math.round(7 + scaleRef * .03);
        labelFontSize = (int) Math.round(4 + scaleRef * .03);
        statusTextFontSize = (int) Math.round(8 + scaleRef * .005);

        // recalc with real extent of window
        xScale = ((double) (width) - 2.0 * xMargin) / (maxTpo - minTpo);
        yScale = ((double) (height) - 2.0 * yMargin) / (maxInt - minInt);
        xShift = (minTpo * xScale) - xMargin;
        yShift = (minInt * yScale) - yMargin;
        /*
         * System.out.println("TpoRange: min=" + minTpo + ", max=" + maxTpo + ";
         * IntRange: min=" + minInt + ", max=" + maxInt + "; Scale: xtmp=" + xScale + ",
         * ytmp=" + yScale + "; Shift: xtmp=" + xShift + ", ytmp=" + yShift);
         */
        g2.clearRect(0, 0, width, height); // clear window

        if (printScreen || captureScreen) { // to generate EPS or PNG/JPG output
            drawBackground(g2);     // draw background frames
            drawAxis(g2);           // draw axis ticks and labels
            drawLabelsAndTitle(g2); // draw labels and title
            // do not draw statusText when printed
        } else {
            if (img == null) {// construct background image from scratch 
                img = getGraphicsConfiguration().createCompatibleImage(width, height);
                gImage = (Graphics2D) img.getGraphics();
                gImage.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); // drawing nicer
                drawBackground(gImage);     // draw background frames
                drawAxis(gImage);           // draw axis ticks and labels
                drawLabelsAndTitle(gImage); // draw labels and title
            }
            if (cs < tailLength) { // redraw background image, if no grey worm
                g2.drawImage(img, 0, 0, null);
            }
        }

        g2.setFont(new Font("Arial", Font.BOLD, barNumberFontSize));
        fm = g2.getFontMetrics();

        // draw Worm
        int startI = oldSlice - tailLength;
        if (startI < 0) {
            startI = 0;
        }
        for (int j = startI; j < cs; j++) {
            double wSize = (double) (wDiameter - ((double) (cs - j) * (wDiameter - 1.0)) / (double) tailLength);// size of worm circle
            //System.out.println("Diameter: " + wSize / 2);
            if (wSize < 1.0) {
                wSize = 1.0;
            }
            if ((wormData.flags[j] & MyWormFile.SEG1) != 0) {
                wSize = wSize * 1.2;
            } else if ((wormData.flags[j] & MyWormFile.SEG2) != 0) {
                wSize = wSize * 1.4;
            }
            int xPoint, yPoint; // xy coordinates of current worm slice
            double dxPoint, dyPoint; // xy in double precision
            if (xAxisLog) { // log
                dxPoint = log2(wormData.tempo[j]) * xScale - xShift - wSize / 2.0;
            } else { // lin
                dxPoint = wormData.tempo[j] * xScale - xShift - wSize / 2.0;
            }
            //System.out.println("dxPoint: "+dxPoint);
            dyPoint = (double) height - (wormData.intensity[j] * yScale - yShift + wSize / 2.0);
            //System.out.println("dyPoint: "+dyPoint);
            xPoint = (int) Math.round(dxPoint);
            yPoint = (int) Math.round(dyPoint);

            if (cs - j < tailLength) {// draw red part of the worm (always live!)
                //if (currSlice-j <= 0 || currSlice-j >= red.length) { System.out.println("\n\nDEBUG235 DrawWorm: currSlice="+currSlice+", j="+j+"\n\n");} //  Ich hab keine Ahnung mehr, was das hier soll!
                g2.setColor(red[cs - j]); // draw read fill
                if (wormData.flags[j] > MyWormFile.SEG1) {
                    g2.setColor(Color.black);
                }
                if (drawDoublePrecision) {
                    g2.fill(new Ellipse2D.Double(dxPoint, dyPoint, wSize, wSize));
                } else {
                    g2.fillOval(xPoint, yPoint, (int) Math.round(wSize), (int) Math.round(wSize));
                }
                g2.setColor(black[cs - j]); // black margin
                if (drawDoublePrecision) {
                    g2.draw(new Ellipse2D.Double(dxPoint, dyPoint, wSize, wSize));
                } else {
                    g2.drawOval(xPoint, yPoint, (int) Math.round(wSize), (int) Math.round(wSize));
                }
            } else { // draw black portion of the worm 
                if (printScreen || captureScreen) { // only for EPS output print into g2
                    g2.setColor(Color.white); // white fill
                    if (wormData.flags[j] > MyWormFile.SEG1) {
                        g2.setColor(Color.black);
                    }
                    if (drawDoublePrecision) {
                        g2.fill(new Ellipse2D.Double(dxPoint, dyPoint, wSize, wSize));
                    } else {
                        g2.fillOval(xPoint, yPoint, (int) Math.round(wSize), (int) Math.round(wSize));
                    }
                    g2.setColor(black[tailLength - 1]); // grey matter
                    if (drawDoublePrecision) {
                        g2.draw(new Ellipse2D.Double(dxPoint, dyPoint, wSize, wSize));
                    } else {
                        g2.drawOval(xPoint, yPoint, (int) Math.round(wSize), (int) Math.round(wSize));
                    }
                } else { // into gImage (normally)
                    gImage.setColor(Color.white); // white fill
                    if (wormData.flags[j] > MyWormFile.SEG1) {
                        gImage.setColor(Color.black);
                    }
                    if (printScreen) { // drawDoublePrecision) { // switched off, because it wobbles on screen, 18/5/2015
                        gImage.fill(new Ellipse2D.Double(dxPoint, dyPoint, wSize, wSize));
                    } else {
                        gImage.fillOval(xPoint, yPoint, (int) wSize, (int) wSize);
                    }
                    gImage.setColor(black[tailLength - 1]); // grey matter
                    if (printScreen) { // drawDoublePrecision) { // switched off, because it wobbles on screen, 18/5/2015
                        gImage.draw(new Ellipse2D.Double(dxPoint, dyPoint, wSize, wSize));
                    } else {
                        gImage.drawOval(xPoint, yPoint, (int) wSize, (int) wSize);
                    }
                    if (cs - j == tailLength) {// at the end of grey worm
                        g2.drawImage(img, 0, 0, null); //  draw the image once
                    }
                }
            }

            if (j == cs - 1) { // present point (``worm's face'')
                String time = timeString(wormData.time[j]); // draw time display 
                int twd = fm.stringWidth(time); //                 in the corner				
                g2.drawString(time, (int) (xMargin + (width - 2 * xMargin) * 0.95 - twd),
                        (int) (yMargin + (height - 2 * yMargin) * 0.09));

                String bn = Integer.toString(barNumbers[j]); //  draw bar number  
                //GlyphVector msg = g2.getFont().createGlyphVector(g2.getFontRenderContext(), bn);
                /*AttributedString as1 = new AttributedString(bn);
                 as1.addAttribute(TextAttribute.SIZE, barNumberFontSize);
                 as1.addAttribute(TextAttribute.FAMILY, "Arial");
                 as1.addAttribute(TextAttribute.WEIGHT, TextAttribute.WEIGHT_REGULAR);*/
                double wd = (double) fm.stringWidth(bn); //  at first worm slice
                if (wormData.flags[j] <= MyWormFile.SEG1) {
                    g2.setColor(Color.black);
                } else {
                    g2.setColor(Color.white);
                }
                if (printScreen) { //drawDoublePrecision) { // switched off, because it wobbles on screen, 18/5/2015
                    float xx = (float) (dxPoint + (wSize - wd) / 2.0);
                    float yy = (float) (dyPoint + (wSize + (double) fm.getAscent() - (double) fm.getDescent()) / 2.0);
                    //System.out.println("dx-xtmp: " + (dxPoint - xx) + ", dy-ytmp: " + (dyPoint - yy));
                    //new TextLayout(as1.getIterator(),g2.getFontRenderContext()).draw(g2, xx, yy);
                    //g2.drawString(as1.getIterator(), xx, yy);
                    //g2.drawGlyphVector(msg, xx, yy);
                    g2.drawString(bn, xx, yy);
                } else {
                    g2.drawString(bn, xPoint + (int) Math.round((wSize - wd) / 2.0),
                            yPoint + (int) Math.round((wSize + fm.getAscent() - fm.getDescent()) / 2.0));
                }
            } // if

        } // for

        if (printScreen) {
            try {
                ((EpsGraphics2D) g2).flush();
                ((EpsGraphics2D) g2).close();
            } catch (IOException e) {
            }
            printScreen = false;
            img = null;
            painting = false;
            System.out.println("done.");
            return;
        } else if (captureScreen) {
            try {
                ImageIO.write(outputImage, outputFormatString, new File(outputName));
            } catch (IOException ex) {
                Logger.getLogger(DrawWorm.class.getName()).log(Level.SEVERE, null, ex);
            }
            captureScreen = false;
            img = null;
            painting = false;
            System.out.println("done.");
            return;
        } else {
            drawStatusText(g2);
        }

        /*
         if (captureScreen) {
         Robot robot;
         try {
         robot = new Robot();
         Rectangle ausschnitt = new Rectangle(this.getBounds());
         ausschnitt.setLocation(getLocationOnScreen());
         BufferedImage screenShot = robot.createScreenCapture(ausschnitt);
         ImageIO.write(screenShot, outputFormatString, new File(outputName));
         } catch (AWTException | IOException e) {
         }
         captureScreen = false;
         img = null;
         painting = false;
         System.out.println("done.");
         return;
         }
         */
        oldSlice = cs;
        oldWidth = width;
        oldHeight = height;

        if (refreshStats) {
            repaintTimes[rptInd++] = System.nanoTime() - startTime;
            if (rptInd >= repaintTimes.length) {
                System.out.println("Mean draw time last 50 repaints "
                        + "DrawWorm:\t"
                        + M.mean(repaintTimes) / 1000000.0 + "ms");
                rptInd = 0;
                double[] tmp = new double[repaintTimes.length];
                for (int i = 0; i < repaintTimes.length; i++) {
                    tmp[i] = repaintTimes[i] / 1000000.0;
                    repaintTimes[i] = 0;
                }
                //fig.clear();
                //double[] xtmp = new double[]{-2.5,2.5,7.5,12.5,17.5,22.5,27.5,32.5,37.5,42.5,47.5,52.5,57.5,62.5,67.5};
                //fig.gca().bar(M.histc(tmp,xtmp));
                //fig.repaint();	
            }
        }
        whiteBackground = false;
        painting = false;
    } // paint

    private void drawBackground(Graphics2D g2) {
        if ((printScreen || captureScreen) && whiteBackground) {
            g2.setColor(Color.white);
        } else {
            g2.setColor(Color.lightGray);
        }
        g2.fillRect(0, 0, width, height); // grey background
        g2.setColor(Color.white);
        g2.fill(new Rectangle2D.Double(xMargin, yMargin, (width - 2 * xMargin), (height - 2 * yMargin)));
        g2.setColor(Color.black);
        g2.draw(new Rectangle2D.Double(xMargin, (yMargin), (width - 2 * xMargin), (height - 2 * yMargin)));
    } // drawBackground

    private void drawAxis(Graphics2D g2) {
        g2.setFont(new Font("Arial", Font.PLAIN, axisFontSize));
        fm = g2.getFontMetrics();
        double xtmp, ytmp;
        // draw xtmp-axis ticks & labels
        for (Iterator<Double> i = xAxisLabels.iterator(); i.hasNext();) {
            double j;
            double tmp = i.next();
            if (xAxisLog) {
                j = log2(tmp);
            } else {
                j = tmp;
            }
            String label = Integer.toString((int) tmp);
            xtmp = j * xScale - xShift;
            g2.draw(new Line2D.Double(xtmp, yMargin, xtmp, (yMargin + tickHeight)));
            g2.draw(new Line2D.Double(xtmp, (height - yMargin), xtmp, (height - yMargin - tickHeight)));
            int wd = fm.stringWidth(label);
            g2.drawString(label, (float) (xtmp - wd / 2), (float) (height - yMargin + fm.getAscent()));
        }
        // draw ytmp-axis ticks & labels
        for (double j : yAxisLabels) {
            String label = Integer.toString((int) j);
            ytmp = j * yScale - yShift;
            g2.draw(new Line2D.Double((xMargin), (height - ytmp), (xMargin + tickHeight), (height - ytmp)));
            g2.draw(new Line2D.Double((width - xMargin), (height - ytmp), (width - xMargin - tickHeight), (height - ytmp)));
            int wd = fm.stringWidth(label);
            g2.drawString(label, (float) (xMargin - wd * 1.1), (float) (height - (ytmp - (fm.getAscent() - fm.getDescent()) / 2)));
        }
    } // drawAxis()

    private void drawLabelsAndTitle(Graphics2D g2) {
        g2.setColor(Color.black);
        g2.setFont(new Font("Arial", Font.BOLD, titleFontSize));
        fm = g2.getFontMetrics();
        String title = wormData.performer + ", " + wormData.year;
        int wd = fm.stringWidth(title);
        g2.drawString(title, (int) ((width - wd) / 2), (int) ((yMargin
                + fm.getAscent() - fm.getDescent()) / 2));

        g2.setFont(new Font("Arial", Font.BOLD, labelFontSize));
        fm = g2.getFontMetrics();
        wd = fm.stringWidth(wormData.xLabel);
        g2.drawString(wormData.xLabel, (int) ((width - wd) / 2),
                (int) (height - yMargin / 6));
        wd = fm.stringWidth(wormData.yLabel);
        g2.rotate(-Math.PI / 2.0);
        g2.drawString(wormData.yLabel, -(int) ((height + wd) / 2), fm.getAscent());
        g2.rotate(Math.PI / 2.0);
    } // drawLabelsAndTitle()

    private void drawStatusText(Graphics2D g2) {
        if (statusText.equals("")) {
            //System.out.println("drawStatusText(): NO STATUSTEXT fs: "+statusTextFontSize+" shade: "+statusTextShade+"count: "+statusTextcount+" w:"+width+" h:"+height);
        } else {
            if (statusTextShade == 0f) {
                g2.setColor(Color.BLACK);
            } else if (statusTextShade > 0 && statusTextShade <= fadeThreshold) {
                g2.setColor(new Color(statusTextShade, statusTextShade, statusTextShade));
            }
            g2.setFont(new Font("Arial", Font.PLAIN, statusTextFontSize));
            fm = g2.getFontMetrics();
            int wd = fm.stringWidth(statusText);
            //System.out.println("drawStatusText(): '" + statusText+"' fs: "+statusTextFontSize+" shade: "+statusTextShade+"count: "+statusTextcount + ", wd:" + wd+" w:"+width+" h:"+height+" m:"+yMargin);
            g2.drawString(statusText,
                    (int) Math.round((width - wd - 2)),
                    (int) Math.round(height - yMargin / 6));
            //statusTextShade = statusTextShade - 0.01f;
            statusTextcount = statusTextcount - 1;
            if (statusTextcount < 0) {
                statusTextShade = statusTextShade + (4f * 0.01f + 0.0033f / 4f * threadSleepTime);
                if (statusTextShade > fadeThreshold) {
                    statusTextShade = fadeThreshold;
                    statusText = "";
                }
            }
        }
    } // drawStatusText()

    public static DrawWorm createWorm(MyWormFile wormData) {
        JFrame frame = new JFrame(wormData.composer + ": " + wormData.piece);
        DrawWorm w = new DrawWorm(wormData);
        frame.add(w);
        frame.setSize(w.xSize, w.ySize);
        frame.setLocation(500, 0);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setVisible(true);
        // w.scale(w,wormData);
        return w;
    } // createWorm

    private double log2(double x) {
        return (Math.log(x) / Math.log(2.0));
    } // log2

    /**
     * @return Returns the frame.
     */
    public JFrame getFrame() {
        return frame;
    } // getFrame()

    /**
     * @param frame The frame to set.
     */
    public void setFrame(JFrame frame) {
        this.frame = frame;
    } // setFrame()

    public static String timeString(double time) {
        double seconds = time % 60.0;
        int minutes = (int) ((time / 60) % 60);
        int hours = (int) Math.floor(time / 3600.0);
        String str;
        if (time < 10) {
            str = String.format(l, "%04.2f", seconds);
        } else {
            str = String.format(l, "%05.2f", seconds);
        }
        if (minutes > 0) {
            if (hours <= 0) {
                str = String.format(l, "%2d:", minutes) + str;
            } else {
                str = String.format(l, "%02d:", minutes) + str;
            }
        }
        if (hours > 0) {
            str = String.format(l, "%2d:", hours) + str;
        }
        return str;
    } // timeString()

    public static String getVersion() {
        return version;
    } // getVersion()

    /**
     * Use this method (plus repaint()) to export the current frame into an EPS
     * file to disk.
     */
    public void printScreen() {
        printScreen = true;
        repaint();
    }

    public void captureScreen(String what, int frameLabel) {
        this.frameLabel = frameLabel;
        captureScreen(what);
    }

    /**
     * Use this method (plus repaint()) to export the current frame into a GIF
     * image to disk
     *
     * @param what.
     */
    public void captureScreen(String what) {
        outputFormatString = what;
        captureScreen = true;
        repaint();
    }

    public void setRefreshStats(boolean refreshStats) {
        this.refreshStats = refreshStats;
    }

    public boolean isRefreshStats() {
        return refreshStats;
    }

    public String getOutputFormatString() {
        return outputFormatString;
    }

    public void setOutputFormatString(String outputFormatString) {
        this.outputFormatString = outputFormatString;
    }

    public void setWhiteBackground(boolean whiteBackground) {
        this.whiteBackground = whiteBackground;
    }

    public int getCurrSlice() {
        return currSlice;
    }

    public void setCurrSlice(int currSlice) {
        this.currSlice = currSlice;
    }

    public boolean isCaptureScreen() {
        return captureScreen;
    }

    public boolean isReallyPainting() {
        return painting;
    }

    public boolean isDrawDoublePrecision() {
        return drawDoublePrecision;
    }

    public void setDrawDoublePrecision(boolean drawDoublePrecision) {
        this.drawDoublePrecision = drawDoublePrecision;
    }

    public String getStatusText() {
        return statusText;
    }

    public void setStatusText(String statusText) {
        this.statusText = statusText;
        this.statusTextShade = 0f;
        statusTextcount = defaultCount;
    }

    public int getThreadSleepTime() {
        return threadSleepTime;
    }

    public void setThreadSleepTime(int threadSleepTime) {
        this.threadSleepTime = threadSleepTime;
    }

    public boolean isAliasingOn() {
        return aliasingOn;
    }

    public void setAliasingOn(boolean aliasingOn) {
        this.aliasingOn = aliasingOn;
    }
} // DrawWorm
