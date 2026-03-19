package at.ofai.music.wgworm;

import at.ofai.music.plot.U;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FileDialog;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Iterator;
import java.util.List;

import javax.media.ControllerEvent;
import javax.media.ControllerListener;
import javax.media.EndOfMediaEvent;
import javax.media.Manager;
import javax.media.NoPlayerException;
import javax.media.Player;
import javax.media.PrefetchCompleteEvent;
import javax.media.RealizeCompleteEvent;
import javax.media.Time;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.WindowConstants;
import at.ofai.music.wgworm.PlayControl.PlayControlKeyListener;
import java.awt.image.BufferStrategy;

/**
 * Extended worm player with playback controls (play, stop, pause, slider to
 * navigate in the sound files). Uses JAVA Media framework
 * {@see java.media.Player}. Reads and plays wormfiles, displays tempo and
 * loudness curves (parallel unsmoothed wormfile required).
 * <p>
 * REMARK: Does not make use of the wgplot package.
 *
 * @author Werner Goebl, Dec. 2004--April 2007, Dec 2008, April 2011, Sept 2012
 */
public class WormPlay implements ControllerListener, Runnable, MyPlayer {

    //private static final String version = "0.60; 7 April 2011";
    //private static final String version = "0.61; 19 August 2012";
    //private static final String version = "0.62; 13 September 2012";
    //private static final String version = "0.63; 27 September 2012";//fileChooser added; visualDelay adjustable by arrow keys; 
    //private static final String version = "0.64; 29 September 2012";//statusText display added (with fade-out) 
    //private static final String version = "0.65; 16 Oktober 2012";// FileDialog for MacOSX 
    //private static final String version = "0.66; 18 Oktober 2012";// Display wormFile in separate window (shortcuts W/U)
    private static final String version = "0.67; 18 May 2015";// video frame export faster; wobbling worm face with DoublePrecision calmed (reset to integer resolution); not perfect.
    // TODO finally merge with MidiWormPlay
    // TODO: in Mac OSX, the two TPO/INT windows overlap (7. April 2011); Line 266 worked around (Sept 2012)
    private boolean v = false;
    private URL wormURL;
    protected MyWormFile wormData;
    protected MyWormFile uw;
    JFrame wormFrame, tpoFrame, intFrame;
    private Player player = null;
    private Thread playThread = null;
    private double currTime; // same as filePosition, but in Seconds
    private String wavName;
    private int startFrame;
    protected boolean dispTpo = false;
    protected boolean dispInt = false;
    protected boolean dispWrm = true;
    private boolean showTpoLate = false;
    protected DrawWorm dw = null;
    protected DrawData ddt = null;
    protected DrawData ddl = null;
    protected boolean ddPlotSecondaryData = false; // displays smoothed data in DrawData windows (TPO, INT)
    PlayControl playControl;
    private int visualDelay; // delay of visualisation comp to audio (ms)
    private final double beforeWormStart = .25; // time before the worm starts 
    protected boolean mouseButtonPressed = false;
    Dimension scrSz = null;
    int wormFrameSize = 430;
    Dimension wormSize;
    private int threadSleepTime;
    private static final int defaultThreadSleepTime = 20; // ms thread sleep time
    private boolean antiAl = true; // antiAliasing for drawData
    private boolean showRefreshStats = false; // shows refresh statistics every 3 seconds
    protected boolean usWormFileRead = false; // whether or not the unsmoothed worm file was already read
    private Container cp;
    protected WormPlayKeyListener wpkl;
    protected PlayControlKeyListener pckl;
    protected boolean createMovie = false;
    boolean IS_MAC = false;
    private String imageFormat = "gif"; // png, gif, jpg (for video export)
    public BufferStrategy strategy;

    public WormPlay(MyWormFile w) { // constructor
        wormData = w;
    } // WormPlay constructor

    void init() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e1) {
        }
        String lcOSName = System.getProperty("os.name").toLowerCase();
        IS_MAC = lcOSName.startsWith("mac os x");

        scrSz = Toolkit.getDefaultToolkit().getScreenSize();
        currTime = wormData.time[0] - beforeWormStart;
        if (currTime < 0) {
            currTime = 0.0; //visDelay / 1000.0;
        }
        if (!"".equals(wormData.audioPath)) {
            wavName = wormData.audioPath + "/" + wormData.audioFile;
        } else {
            wavName = wormData.audioFile;
        }
        wormSize = new Dimension(wormFrameSize, wormFrameSize);
        dw = new DrawWorm(wormData, wormSize);

        // PlayControl
        int controlMin = 0, controlMax = 0;
        controlMax = wormData.length;
        playControl = new PlayControl((MyPlayer) this, controlMin, controlMax);
        // HelpButton
        JButton helpButton = new JButton(new ImageIcon(getClass().getClassLoader().getResource("icon/Help16.gif")));
        helpButton.setPreferredSize(new Dimension(20, 18));
        helpButton.setFocusable(false);
        helpButton.setToolTipText("Help (H)");
        helpButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!WormPlayHelp.isVisible) {
                    WormPlayHelp.showHelp();
                } else {
                    WormPlayHelp.hideHelp();
                }
            }
        });
        cp = new Container(); //container for playControl + helpButton
        cp.setLayout(new BoxLayout(cp, BoxLayout.X_AXIS));
        cp.add(playControl);
        cp.add(helpButton);
        cp.validate();

        if ((dispTpo || dispInt) && !usWormFileRead) {
            // read in unsmoothed worm data
            usWormFileRead = readUSWormFile();
        }
        if (dispTpo && usWormFileRead) { // construct tempo DrawData
            initTPO();
        }
        if (dispInt && usWormFileRead) { // construct loudness DrawData
            initINT();
        }
        // worm display construction
        int del = wormData.performer.indexOf(" ");
        int ll = 1;
        while (wormData.performer.length() > del + ll && ll < 4) {
            ll++;
        }
        String acro = wormData.performer.substring(del + 1, del + ll).toUpperCase();
        if (dispWrm) {
            wormFrame = new JFrame(acro + ": " + wormData.composer + ", " + wormData.piece);
            dw.setFrame(wormFrame);
            dw.setRefreshStats(showRefreshStats);
            wormFrame.setSize(dw.xSize, dw.ySize + cp.getPreferredSize().height);
            Image img = wormFrame.getToolkit().getImage(URLClassLoader.getSystemResource("icon/wormIcon48.jpg"));
            wormFrame.setIconImage(img);
            wormFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            wormFrame.getContentPane().add(BorderLayout.CENTER, dw);
        }

        if (dispWrm) {
            wormFrame.getContentPane().add(BorderLayout.SOUTH, cp);
            wormFrame.validate();
            wormFrame.setVisible(true);
            wormFrame.createBufferStrategy(2);
            strategy = wormFrame.getBufferStrategy();
            dw.setStrategy(strategy);
            System.out.println("Bufferstragy: " + strategy);
        } else if (dispTpo) {
            tpoFrame.getContentPane().add(BorderLayout.SOUTH, cp);
            tpoFrame.validate();
        } else {
            intFrame.getContentPane().add(BorderLayout.SOUTH, cp);
            intFrame.validate();
        }

        startFrame = (int) Math.round(wormData.time[0]
                / wormData.outFramePeriod) - 1;
        wpkl = new WormPlayKeyListener(this);
        pckl = playControl.getPlayControlKeyListener();
        playControl.addKeyListener(wpkl);
        playControl.requestFocusInWindow();

        if (dispTpo && showTpoLate) { // radio buttons for tpo early/late //DEBUG!!
            JRadioButton tpoEarlyButton = new JRadioButton("Tempo");
            tpoEarlyButton.setMnemonic(KeyEvent.VK_E);
            tpoEarlyButton.setActionCommand("early");
            tpoEarlyButton.setPreferredSize(new Dimension(68, 18));
            JRadioButton tpoLateButton = new JRadioButton("");
            tpoLateButton.setMnemonic(KeyEvent.VK_L);
            tpoLateButton.setActionCommand("late");
            tpoLateButton.setPreferredSize(new Dimension(18, 18));
            if (uw.tempoLate == 0) {
                tpoEarlyButton.setSelected(true);
            } else {
                tpoLateButton.setSelected(true);
            }
            ButtonGroup group = new ButtonGroup();
            group.add(tpoEarlyButton);
            group.add(tpoLateButton);
            JPanel radioPanel = new JPanel();
            radioPanel.setLayout(new BoxLayout(radioPanel, BoxLayout.X_AXIS));
            radioPanel.add(tpoEarlyButton);
            radioPanel.add(tpoLateButton);
            cp.add(radioPanel);
            cp.validate();
            tpoLateButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (uw.tempoLate == 0) {
                        for (int i = uw.length - 1; i >= 1; i--) {
                            uw.tempo[i] = uw.tempo[i - 1];
                        }
                        uw.tempoLate = 1;
                        ddt.update(currTime);
                    }
                }
            });
            tpoEarlyButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (uw.tempoLate == 1) {
                        for (int i = 0; i < uw.length - 1; i++) {
                            uw.tempo[i] = uw.tempo[i + 1];
                        }
                        uw.tempoLate = 0;
                        ddt.update(currTime);
                    }
                }
            });
        }
        if (createMovie) {
            createMovieFrames(50); // 
        }
    } // init()

    public boolean readUSWormFile() {
        int ind = wormData.wormFileName.lastIndexOf(".");
        String unsmWormFileName = wormData.wormFileName.substring(0, ind)
                + "_unsmoothed.worm";
        if (v) {
            System.out.println("unsmWormFileName: " + unsmWormFileName);
        }
        File file = new File(unsmWormFileName);
        boolean itisthere = file.isFile();
        outn("itFile = " + itisthere);
        URL unsmURL;
        try {
            unsmURL = new URL(unsmWormFileName);
            unsmURL.openStream();
        } catch (MalformedURLException e) {
            outn("unsmWormFile not correct: " + unsmWormFileName);
            return false;
        } catch (IOException e) {
            outn("unsmWormFile not found: " + unsmWormFileName);
            return false;
        }
        uw = new MyWormFile();
        uw.read(unsmWormFileName);
        return true;
    } // readUSWormFile()

    public void initTPO() {
        ddt = new DrawData(uw, true, DrawData.getDefaultsize(), false, wormData);
        ddt.setAntiAliasing(antiAl);
        ddt.setRefreshStats(showRefreshStats);
        tpoFrame = new JFrame("TEMPO (bpm) against TIME (s): "
                + wormData.performer + ", " + wormData.year);
        if (!dispWrm) {
            tpoFrame.setSize(ddt.xSize, ddt.ySize + cp.getPreferredSize().height);
        } else {
            tpoFrame.setSize(ddt.xSize, ddt.ySize);
        }
        tpoFrame.setLocation(dw.xSize, (int) (dw.ySize / 2) + 22); // ATTENTION: this is a workaround for MacOSX placing the windows differently
        tpoFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        tpoFrame.getContentPane().add(BorderLayout.CENTER, ddt);
        tpoFrame.setVisible(true);
        ddt.addKeyListener(wpkl);
        ddt.addKeyListener(pckl);
        ddt.requestFocusInWindow();
    } // initTPO()

    public void initINT() {
        ddl = new DrawData(uw, false, DrawData.getDefaultsize(), false, wormData);
        ddl.setAntiAliasing(antiAl);
        ddl.setRefreshStats(showRefreshStats);
        intFrame = new JFrame("LOUDNESS (sone) against TIME (s): "
                + wormData.performer + ", " + wormData.year);
        if (!dispWrm && !dispTpo) {
            intFrame.setSize(ddl.xSize, ddl.ySize + cp.getPreferredSize().height);
        } else {
            intFrame.setSize(ddl.xSize, ddl.ySize);
        }
        intFrame.setLocation(dw.xSize, 0);
        intFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        intFrame.getContentPane().add(BorderLayout.CENTER, ddl);
        intFrame.setVisible(true);
        ddl.addKeyListener(wpkl);
        ddl.addKeyListener(pckl);
        ddl.requestFocusInWindow();
    } // initINT()

    public static void main(String[] args) {
        boolean verbose = false;
        boolean showTL = false;
        boolean dspWrm = true;
        boolean dspTpo = false;
        boolean dspInt = false;
        boolean antiAl = true;
        boolean refreshStats = false;
        boolean createMovie = false;
        String imageFormat = "gif";
        int wormFrameSize = 430;
        MyWormFile w;
        int visualDelay = 0; // ms
        String wormName = "";
        URL wormURL;
        int sleepT = WormPlay.defaultThreadSleepTime; // ms 
        //if (args.length == 0) printUsageAndExit();
        int i = 0;
        while (i < args.length) { // goes through arguments
            System.out.println(i + ": " + args[i]);
            if (args[i].equalsIgnoreCase("-h") || args[i].equals("-help")) {
                printUsageAndExit();
            }
            if (args[i].equalsIgnoreCase("noworm")) {
                dspWrm = false;
            }
            if (args[i].equalsIgnoreCase("tpo")) {
                dspTpo = true;
            }
            if (args[i].equalsIgnoreCase("int")) {
                dspInt = true;
            }
            if (args[i].equalsIgnoreCase("showTL")) {
                showTL = true;
            }
            if (args[i].equalsIgnoreCase("noAntiAl")) {
                antiAl = false;
            }
            if (args[i].equalsIgnoreCase("movie")) {
                createMovie = true;
            }
            if (args[i].equalsIgnoreCase("png")) {
                imageFormat = "png";
            }
            if (args[i].equalsIgnoreCase("gif")) {
                imageFormat = "gif";
            }
            if (args[i].equalsIgnoreCase("jpg")) {
                imageFormat = "jpg";
            }
            if (args[i].equalsIgnoreCase("-v")) {
                verbose = true;
            }
            if (args[i].equalsIgnoreCase("-r")) {
                refreshStats = true;
            }
            if (args[i].equalsIgnoreCase("-d")) {// set Visual Delay (comp to Audio) in ms
                i++;
                if (i >= args.length) {
                    printUsageAndExit();
                }
                try {
                    visualDelay = Integer.parseInt(args[i]);
                    System.out.println("visualDelay: " + visualDelay + " ms");
                } catch (NumberFormatException e) {
                    printUsageAndExit();
                }
            }
            if (args[i].equalsIgnoreCase("-z")) {// set size of worm window
                i++;
                if (i >= args.length) {
                    printUsageAndExit();
                }
                try {
                    wormFrameSize = Integer.parseInt(args[i]);
                    System.out.println("wormFrameSize: " + wormFrameSize + " px");
                } catch (NumberFormatException e) {
                    printUsageAndExit();
                }
            }
            if (args[i].equals("-s")) {// sets sleep time of the vis thread (ms)
                i++;
                if (i >= args.length) {
                    printUsageAndExit();
                }
                try {
                    sleepT = Integer.parseInt(args[i]);
                    if (sleepT < 0) {
                        sleepT = 0;
                    }
                    System.out.println("threadSleepTime: " + sleepT + " ms");
                } catch (NumberFormatException e) {
                    printUsageAndExit();
                }
            }
            if (args[i].endsWith(".worm")) { // read worm file
                wormName = args[i];
            }
            i++;
        }
        JFileChooser fch;
        FileDialog fd;
        if (wormName.equals("")) {
            String osName = System.getProperty("os.name");
            if (osName.equalsIgnoreCase("mac os x")) {
                fd = new FileDialog(new JFrame(""), "WormPlay: Please, choose a wormFile to play.");
                System.setProperty("apple.awt.fileDialogForDirectories", "false");
                fd.setFilenameFilter((File directory, String fileName) -> (fileName.endsWith(".worm") || fileName.endsWith(".WORM")));
                fd.setVisible(true);
                if (fd.getFile() != null) {
                    wormName = fd.getDirectory();
                    wormName += fd.getFile();
                } else {
                    System.exit(0);
                }
            } else {
                fch = new JFileChooser(); // create fileChooser, if no wormFile is given
                fch.setDialogTitle("WormPlay: Please, choose a wormFile to play.");
                fch.setFileFilter(new WormFilter());
                fch.setApproveButtonText("Open");
                fch.setMultiSelectionEnabled(false);
                fch.setCurrentDirectory(new File("."));
                while (!(wormName.endsWith(".worm") || wormName.endsWith(".WORM"))) {
                    int returnVal = fch.showOpenDialog(fch);
                    if (returnVal == JFileChooser.APPROVE_OPTION) {
                        wormName = fch.getSelectedFile().getAbsolutePath();
                    } else if (returnVal == JFileChooser.CANCEL_OPTION) {
                        System.exit(0);
                    }
                }
            }
            System.out.println("wormName:" + wormName);
        }
        w = new MyWormFile();
        wormURL = w.read(wormName);
        System.out.println(w.performer + ", " + w.year + "; "
                + w.composer + ": " + w.piece + "; Length: " + w.length);

        System.out.println("dispWorm= " + dspWrm + "dispTpo = " + dspTpo + ", dispInt = " + dspInt);
        WormPlay wp = new WormPlay(w);
        wp.dispWrm = dspWrm;
        wp.dispTpo = dspTpo;
        wp.dispInt = dspInt;
        wp.showTpoLate = showTL;
        wp.wormURL = wormURL;
        wp.v = verbose;
        wp.threadSleepTime = sleepT;
        wp.antiAl = antiAl;
        wp.showRefreshStats = refreshStats;
        wp.createMovie = createMovie;
        wp.wormFrameSize = wormFrameSize;
        wp.visualDelay = visualDelay;
        wp.imageFormat = imageFormat;
        wp.init();
    } // main

    public void createMovieFrames(double fps) {
        int waitingTime = 75; // 50
        System.out.println("t(0) = " + wormData.time[0] + "; startFrame = " + startFrame);
        int currFrame;
        for (int i = 0; i < wormData.length; i++) {
            currFrame = i - startFrame; // auch ohne + 1
            if (currFrame < 0) {
                currFrame = 0;
            }
            dw.setCurrSlice(currFrame);
            dw.repaint();
            do {
                //System.out.println("WormPlay repainting frame "+i);
                try {
                    Thread.sleep(waitingTime);
                } catch (InterruptedException e) {
                }
            } while (dw.isReallyPainting());
            dw.captureScreen(imageFormat, i);
            //dw.captureScreen("gif", i);
            //dw.captureScreen("jpg", i);
            do {
                //System.out.println("WormPlay capturing frame "+i);
                try {
                    Thread.sleep(waitingTime);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } while (dw.isReallyPainting());
        }
        System.out.println("Generating movie done.");
        System.exit(0);
    } // creatMovieFrames

    @Override
    public void startPlaying() {
        if (player == null) {
            try {
                // TODO check out the URL stuff and replace the toURL() method.
                File file = new File(U.getBase(wormURL.getPath()) + wavName);
                if (v) {
                    System.out.println("Corrected fileName: " + file);
                }
                try {
                    player = Manager.createPlayer(file.toURL());
                } catch (NoPlayerException ex) {
                    errorMessage("play() NoPlayerException. " + ex.getMessage());
                }
                player.addControllerListener(this);
                player.realize();
            } catch (MalformedURLException ex) {
                errorMessage("play() MalformedURLException. " + ex.getMessage());
            } catch (IOException ex) {
                errorMessage("play() IOException. " + ex.getMessage());
            }
        } else {
            if (v) {
                System.out.print("play() ");
            }
            setTime();
            player.start();
            if (v) {
                System.out.println(", continue playing: " + wavName);
            }
            /*
             * System.out.println( "getStartLatency: " +
             * player.getStartLatency() + ", getMediaTime: " +
             * player.getMediaTime() + ", getRate: " + player.getRate());
             */
        }
    } // play()

    public void setTime() {
        if (player != null) {
            double tmp = currTime + visualDelay / 1000.0;
            if (tmp > 0) {
                player.setMediaTime(new Time(tmp));
            } else {
                player.setMediaTime(new Time(0));
            }
            if (v) {
                System.out.print("setTime() " + String.format("%3.3f", tmp) + " s");
            }
        }
    } // setTime()

    @Override
    public void pausePlaying() {
        if (player != null) {
            player.stop();
        }
    } // pause()

    @Override
    public void stop() {
        pausePlaying();
        if (player != null) {
            player.removeControllerListener(this);
            player.close();
            player = null;
        }
        if (playThread != null) {
            playThread = null;
        }
        playControl.sliderReset();
    } // stop()

    @Override
    public void controllerUpdate(ControllerEvent ev) {
        // if (ev instanceof ConfigureCompleteEvent) {
        // p.setContentDescriptor(null);
        // p.realize();
        // }
        if (ev instanceof RealizeCompleteEvent) {
            player.prefetch();
            if (v) {
                System.out.print("controllerUpdate: Prefetching...");
            }
        }
        if (ev instanceof PrefetchCompleteEvent) {
            Time time = player.getDuration();
            if (v) {
                System.out.println("complete.");
            }
            if (v) {
                System.out.println("Now playing: " + wavName + ", duration= "
                        + time.getSeconds());
            }
            // System.out.println("Priority: " + playThread.getPriority());
            player.getGainControl().setLevel((float) 0.75); // workaround von Andreas Arzt; 10. Sept 2010
            setTime();
            if (playThread == null) {
                playThread = new Thread(this);
                playThread.start();
            } else {
                if (v) {
                    System.out.println("PlayThread almost started twice!");
                }
            }
            player.start();
        }
        if (ev instanceof EndOfMediaEvent) {
            //pause();
            player.removeControllerListener(this);
            player.stop();
            player.close();
            player = null;
            if (playThread != null) {
                playThread = null;
            }
            playControl.playSetPlay();
            currTime = 0;
        }
    } // controllerUpdate

    @Override
    public void run() { // thread to update graphics and sleep a while
        while (playThread != null) {
            if (player != null) {
                double time = getAudioTime();
                // System.out.println("run() state: " + state + ", Frames: " +
                // Math.round(time.getSeconds() / wormData.outFramePeriod));
                if (playControl.getState() == PlayControl.PLAYING) {
                    // System.out.println("run() Time: " + time.getSeconds() +
                    // ", SliderRange=" +
                    // playControl.getMinimum() + "--" +
                    // playControl.getMaximum() + "; Value=" + tmp);
                    int tmp = (int) (Math.round(time / wormData.outFramePeriod) - startFrame);
                    if (tmp > playControl.getSliderValue()) {
                        if (dispWrm) {
                            dw.redraw(tmp);
                        }
                        if (tmp >= playControl.getSlMin()
                                && tmp <= playControl.getSlMax()) {
                            playControl.setSliderValue(tmp); //update slider position
                        }
                    }
                    if (dispTpo) {
                        ddt.update(time);
                    }
                    if (dispInt) {
                        ddl.update(time);
                    }
                }
                try {
                    Thread.sleep(threadSleepTime);
                } catch (InterruptedException ex) {
                }
            } // if
        } // while
    } // run

    public void errorMessage(String s) {
        JOptionPane.showMessageDialog(null, s, "Error",
                JOptionPane.ERROR_MESSAGE);
    } // errorMessage()

    private static void printUsageAndExit() {
        outn("WormPlay");
        outn("by Werner Goebl, Version " + version + " (DrawWorm " + DrawWorm.getVersion() + ")");
        outn(" ");
        outn("Usage: java WormPlay <OPTIONS> <wormfile>");
        outn(" ");
        outn("  OPTIONS:");
        outn("      tpo        displays tempo curves    (from unsmoothed wormFile)");
        outn("      int        displays loudness curves (from unsmoothed wormFile)");
        outn("                   [same as wormFile, but with '_unsmoothed' at");
        outn("                    the end of the name.]");
        outn("      noworm     hides worm display (still requires wormFile)");
        outn("      showTL     shows tempo late/early radio buttons (default: off)");
        outn("      noAntiAl   avoids antialiasing for tpo and int (default: on)");
        outn("      -v         verbose output");
        outn("      -d <DELAY> sets the delay of the display (in ms ");
        outn("                 compared to sound); e.g., Brahms: 200; ");
        outn("                 MenDELLssohn: -50, Orchids -150, Machaut Mac: -100");
        outn("      -s <TIME>  sets the sleep time of the visual ");
        outn("                 thread (in ms), default 20 ms; might be too low a");
        outn("                 value at slow machines or bad graphics.");
        outn("      -r         displays mean repaint time for each panel (to check");
        outn("                 graphics capability (MenDELLssohn: DrawWorm:38ms;DrawData:7ms)");
        outn("      -z         sets the x/y size of the worm frame (default 430px)");
        outn("      movie      creates all the frames of the worm for a video clip as");
        outn("      gif        gif files,");
        outn("      png        png files, or");
        outn("      jpg        jpg files.");
        System.exit(1);
    } // printUsageAndExit

    //private static void out(String s) {System.out.print(s);} // out
    private static void outn(String s) {
        System.out.println(s);
    } // outn

    public void sliderMoved(int sliderPosition) {
        currTime = (startFrame + sliderPosition) * wormData.outFramePeriod;
        // System.out.println("CurrTime = " + currTime);
        if (player == null || playControl.getState() != PlayControl.PLAYING) {
            // update display if not playing
            dw.redraw(sliderPosition);
            if (dispTpo) {
                ddt.update(currTime);
            }
            if (dispInt) {
                ddl.update(currTime);
            }
        }
        if (playControl.getState() == PlayControl.STOPPED) {
            // first scroll, then play...
            pausePlaying();
        }
    } // sliderMoved()

    public static String getVersion() {
        return version;
    }

    @Override
    public double getAudioTime() {
        return player.getMediaTime().getSeconds() - (double) visualDelay / 1000.0;
    }

    public int getThreadSleepTime() {
        return threadSleepTime;
    }

    public void setThreadSleepTime(int threadSleepTime) {
        this.threadSleepTime = threadSleepTime;
    }

    @Override
    public void startRecording() { // there won't be a record button in WormPlay ever!
        // Auto-generated method stub
    }

    public int getVisDelay() {
        return visualDelay;
    }

    public void setVisDelay(int visDelay) {
        this.visualDelay = visDelay;
    }

    public void showWormFile(MyWormFile wf) {
        JFrame wFrame = new JFrame(wf.wormName);
        wFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        JTextArea textArea = new JTextArea(55, 40);
        textArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(textArea);
        wFrame.add(scrollPane);

        List<String> lines = wf.arrangeWormFile();
        Iterator<String> it = lines.iterator();
        while (it.hasNext()) {
            String value = it.next();
            textArea.append(value);
            textArea.append(System.getProperty("line.separator"));
            // System.out.println("Value :"+value);
        }

        wFrame.pack();
        wFrame.setVisible(true);
    }

} // WormPlay

class WormPlayHelp extends JFrame {

    static WormPlayHelp wph = null;
    static boolean isVisible = false;
    private static final long serialVersionUID = 1L;
    WormPlay wormPlay;
    static final String helpText
            = "<html>\n"
            + "<h1>WormPlay</h1>"
            + "<p>A simple player for the OFAI worm file format "
            + "with play back controls by Werner Goebl, 2005--2012<br>"
            + "Version: " + WormPlay.getVersion()
            + " (DrawWorm: " + DrawWorm.getVersion() + "; DrawData: "
            + DrawData.getVersion() + ")</p>"
            + "<br>"
            + "<p><b>Keyboard Shortcuts</b>"
            + "<ul>Playback controls:"
            + "<li>SPACE -- Play/Pause"
            + "<li>Z/Y -- Back (to worm begin); "
            + "X -- Play; "
            + "C -- Pause; "
            + "V -- Stop; "
            + "B -- Forward (to worm end)"
            + "<li>left/right arrows -- scroll backwards/forwards. "
            + "Use SHIFT or/and CTRL to speed up scrolling"
            + "</ul>"
            + "<ul>Window Size Controls (only Worm Window)"
            + "<li>1, 2, 3, 4, 5 -- scale to 1/1, 1/2, 1/3... of window width"
            + "<li>CTRL + 1, 2, 3, 4, 5 -- scale to 1/1, 1/2, 1/3... "
            + "of window height"
            + "</ul>"
            + "<ul>Data Windows"
            + "<li>T -- show tempo curves (only if '_unsmoothed' file is available)</li>"
            + "<li>S -- show smoothed data in Data Windows</li></ul>"
            + "<li>I -- show intensity curves (only if '_unsmoothed' file is available)</li></ul>"
            + "<ul>Window x-Excerpt (only Data Windows)"
            + "<li>Minus or Comma to decrease excerpt; Equals or Period to increase it.</li>"
            + "<li>M (for intensity display) shows melody separately (if supported by worm file)"
            + "</li></ul>"
            + "<ul>Export--Printing"
            + "<li>Press CTRL + P to produce EPS screen shot with white background "
            + "(and if present of tempo or loudness curves)</li>"
            + "<li>SHIFT-CTRL + P to produce EPS screen shot with grey background</li>"
            + "<li>Press CTRL + G to produce GIF screen shot with grey background</li>"
            + "<li>Press CTRL + J to produce JPG screen shot with grey background</li>"
            + "</ul>"
            + "<ul>"
            + "<li>R to show repaint times of all open windows.</li>"
            + "<li>A Switch on/off antialiasing for plotting the worm.</li>"
            + "<li>D Switch between Double and Integer precision for plotting the worm.</li>"
            + "<li>H or ESCAPE to close this window.</li>"
            + "<li>CTRL (or CTRL+ALT) + arrows up/down  in/decrease repaint sleep time by 10 ms.</li>"
            + "<li>CTRL + SHIFT + arrows up/down  in/decrease repaint sleep time by 30 ms.</li>"
            + "<li>CTRL+ALT + arrows left/right  in/decrease visual delay by 10 ms.</li>"
            + "<li>W  Show contents of wormFile in a separate window.</li>"
            + "<li>U  Show contents of unsmoothed wormFile in a separate window.</li>"
            + "</ul>"
            + "</p>"
            + "</html>";

    public static void showHelp() {
        JFrame.setDefaultLookAndFeelDecorated(false);
        if (wph == null) {
            wph = new WormPlayHelp();
            wph.setTitle("WormPlay HELP");
            wph.setIconImage(wph.getToolkit().getImage(URLClassLoader.getSystemResource("icon/wormIcon48.jpg")));
            wph.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            wph.setSize(680, 750);
            //wph.setLocation(10,10);
            JLabel jl = new JLabel(helpText);
            jl.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createTitledBorder("Help"),
                    BorderFactory.createEmptyBorder(10, 10, 10, 10)));
            jl.setFocusable(true);
            jl.requestFocusInWindow();
            jl.addKeyListener(new KeyListener() {
                @Override
                public void keyTyped(KeyEvent arg0) {
                }

                @Override
                public void keyPressed(KeyEvent arg0) {
                }

                @Override
                public void keyReleased(KeyEvent e) {
                    switch (e.getKeyCode()) {
                        case KeyEvent.VK_ESCAPE:
                            wph.dispose();
                            break;
                        case KeyEvent.VK_H:
                            wph.dispose();
                            break;
                    }
                } // keyReleased			
            });
            wph.add(jl);
            wph.validate();
            wph.repaint();
            wph.setVisible(true);
        } else {
            wph.setVisible(true);
        }
        isVisible = true;
    } // showHelp

    public static void hideHelp() {
        wph.setVisible(false);
        isVisible = false;
    } // hideHelp()
} // class WormPlayHelp

class WormPlayKeyListener implements KeyListener {

    WormPlay wp;

    WormPlayKeyListener(WormPlay wormPlay) {
        wp = wormPlay;
    } // constructor	

    @Override
    public void keyTyped(KeyEvent e) {
    }

    @Override
    public void keyPressed(KeyEvent e) {
    }

    @Override
    public void keyReleased(KeyEvent e) {
        String txt;
        int st;
        if (wp.wormFrame != null) {
            if ((e.getModifiers() & KeyEvent.CTRL_MASK) != 0 && // CTRL+ALT
                    (e.getModifiers() & KeyEvent.ALT_MASK) != 0) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_UP:
                        st = wp.getThreadSleepTime() + 10;
                        wp.setThreadSleepTime(st);
                        txt = "threadSleepTime: " + wp.getThreadSleepTime() + " ms.";
                        System.out.println(txt);
                        wp.dw.setStatusText(txt);
                        wp.dw.setThreadSleepTime(st);
                        break;
                    case KeyEvent.VK_DOWN:
                        st = wp.getThreadSleepTime() - 10;
                        if (st < 0) {
                            st = 0;
                        }
                        wp.setThreadSleepTime(st);
                        txt = "threadSleepTime: " + wp.getThreadSleepTime() + " ms.";
                        System.out.println(txt);
                        wp.dw.setStatusText(txt);
                        wp.dw.setThreadSleepTime(st);
                        break;
                    case KeyEvent.VK_RIGHT:
                        wp.setVisDelay(wp.getVisDelay() + 10);
                        txt = "visualDelay: " + wp.getVisDelay() + " ms.";
                        System.out.println(txt);
                        wp.dw.setStatusText(txt);
                        break;
                    case KeyEvent.VK_LEFT:
                        wp.setVisDelay(wp.getVisDelay() - 10);
                        txt = "visualDelay: " + wp.getVisDelay() + " ms.";
                        System.out.println(txt);
                        wp.dw.setStatusText(txt);
                        break;
                }
            } else if ((e.getModifiers() & KeyEvent.CTRL_MASK) != 0 && // CTRL+SHIFT
                    (e.getModifiers() & KeyEvent.SHIFT_MASK) != 0) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_UP:
                        st = wp.getThreadSleepTime() + 30;
                        wp.setThreadSleepTime(st);
                        txt = "threadSleepTime: " + wp.getThreadSleepTime() + " ms.";
                        System.out.println(txt);
                        wp.dw.setStatusText(txt);
                        wp.dw.setThreadSleepTime(st);
                        break;
                    case KeyEvent.VK_DOWN:
                        st = wp.getThreadSleepTime() - 30;
                        if (st < 0) {
                            st = 0;
                        }
                        wp.setThreadSleepTime(st);
                        txt = "threadSleepTime: " + wp.getThreadSleepTime() + " ms.";
                        System.out.println(txt);
                        wp.dw.setStatusText(txt);
                        wp.dw.setThreadSleepTime(st);
                        break;
                    case KeyEvent.VK_G: // prints the DrawWorm screen with grey bkground
                        wp.dw.captureScreen("gif");
                        break;
                }
            } else if ((e.getModifiers() & KeyEvent.CTRL_MASK) != 0) { // CTRL
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_0:
                        wp.wormFrame.setSize(wp.wormSize.width, wp.wormSize.height);
                        break;
                    case KeyEvent.VK_1:
                        wp.wormFrame.setSize(wp.scrSz.height, wp.scrSz.height);
                        break;
                    case KeyEvent.VK_2:
                        wp.wormFrame.setSize(wp.scrSz.height / 2, wp.scrSz.height / 2);
                        break;
                    case KeyEvent.VK_3:
                        wp.wormFrame.setSize(wp.scrSz.height / 3, wp.scrSz.height / 3);
                        break;
                    case KeyEvent.VK_4:
                        wp.wormFrame.setSize(wp.scrSz.height / 4, wp.scrSz.height / 4);
                        break;
                    case KeyEvent.VK_5:
                        wp.wormFrame.setSize(wp.scrSz.height / 5, wp.scrSz.height / 5);
                        break;
                    case KeyEvent.VK_P: // prints the DrawWorm screen
                        wp.dw.setWhiteBackground(true);
                        wp.dw.printScreen();
                        break;
                    case KeyEvent.VK_J: // prints the DrawWorm screen
                        wp.dw.captureScreen("jpg");
                        break;
                    case KeyEvent.VK_G: // prints the DrawWorm screen
                        wp.dw.captureScreen("gif");
                        break;
                    case KeyEvent.VK_UP:
                        st = wp.getThreadSleepTime() + 10;
                        wp.setThreadSleepTime(st);
                        txt = "threadSleepTime: " + wp.getThreadSleepTime() + " ms.";
                        System.out.println(txt);
                        wp.dw.setStatusText(txt);
                        wp.dw.setThreadSleepTime(st);
                        break;
                    case KeyEvent.VK_DOWN:
                        st = wp.getThreadSleepTime() - 10;
                        if (st < 0) {
                            st = 0;
                        }
                        wp.setThreadSleepTime(st);
                        txt = "threadSleepTime: " + wp.getThreadSleepTime() + " ms.";
                        System.out.println(txt);
                        wp.dw.setStatusText(txt);
                        wp.dw.setThreadSleepTime(st);
                        break;
                }
            } else {
                switch (e.getKeyCode()) { //        just keys
                    case KeyEvent.VK_0:
                        wp.wormFrame.setSize(wp.wormSize.width, wp.wormSize.height);
                        break;
                    case KeyEvent.VK_1:
                        wp.wormFrame.setSize(wp.scrSz.width, wp.scrSz.width);
                        break;
                    case KeyEvent.VK_2:
                        wp.wormFrame.setSize(wp.scrSz.width / 2, wp.scrSz.width / 2);
                        break;
                    case KeyEvent.VK_3:
                        wp.wormFrame.setSize(wp.scrSz.width / 3, wp.scrSz.width / 3);
                        break;
                    case KeyEvent.VK_4:
                        wp.wormFrame.setSize(wp.scrSz.width / 4, wp.scrSz.width / 4);
                        break;
                    case KeyEvent.VK_5:
                        wp.wormFrame.setSize(wp.scrSz.width / 5, wp.scrSz.width / 5);
                        break;
                    case KeyEvent.VK_T:
                        if (!wp.usWormFileRead) {
                            wp.usWormFileRead = wp.readUSWormFile();
                        }
                        if (!wp.usWormFileRead) {
                            wp.dispTpo = false;
                        } else {
                            if (wp.tpoFrame != null) {
                                if (wp.tpoFrame.isVisible()) {
                                    wp.tpoFrame.setVisible(false);
                                    wp.dispTpo = false;
                                } else {
                                    wp.tpoFrame.setVisible(true);
                                    wp.dispTpo = true;
                                }
                            } else {
                                wp.initTPO();
                                wp.dispTpo = true;
                            }
                        }
                        break;
                    case KeyEvent.VK_I:
                        if (!wp.usWormFileRead) {
                            wp.usWormFileRead = wp.readUSWormFile();
                        }
                        if (!wp.usWormFileRead) {
                            wp.dispInt = false;
                        } else {
                            if (wp.intFrame != null) {
                                if (wp.intFrame.isVisible()) {
                                    wp.intFrame.setVisible(false);
                                    wp.dispInt = false;
                                } else {
                                    wp.intFrame.setVisible(true);
                                    wp.dispInt = true;
                                }
                            } else {
                                wp.initINT();
                                wp.dispInt = true;
                            }
                        }
                        break;
                    case KeyEvent.VK_H:
                        if (!WormPlayHelp.isVisible) {
                            WormPlayHelp.showHelp();
                        } else {
                            WormPlayHelp.hideHelp();
                        }
                        break;
                    case KeyEvent.VK_S:
                        wp.ddPlotSecondaryData = !wp.ddPlotSecondaryData;
                        if (wp.dispTpo) {
                            wp.ddt.setPlotSecondaryData(wp.ddPlotSecondaryData);
                        }
                        if (wp.dispInt) {
                            wp.ddl.setPlotSecondaryData(wp.ddPlotSecondaryData);
                        }
                        break;
                    case KeyEvent.VK_D:
                        if (wp.dw.isDrawDoublePrecision()) {
                            wp.dw.setDrawDoublePrecision(false);
                        } else {
                            wp.dw.setDrawDoublePrecision(true);
                        }
                        break;
                    case KeyEvent.VK_A:
                        if (wp.dw.isAliasingOn()) {
                            wp.dw.setAliasingOn(false);
                        } else {
                            wp.dw.setAliasingOn(true);
                        }
                        break;
                    case KeyEvent.VK_W:
                        wp.showWormFile(wp.wormData);
                        break;
                    case KeyEvent.VK_U:
                        if (wp.uw != null) {
                            wp.showWormFile(wp.uw);
                        }
                        break;
                }
            }
            wp.wormFrame.validate();
            wp.wormFrame.repaint();
        }
        if ((e.getModifiers() & KeyEvent.CTRL_MASK) != 0) { // plot if ddl,ddt
            if (e.getKeyCode() == KeyEvent.VK_P) {
                if (wp.ddl != null) {
                    wp.ddl.printScreen();
                }
                if (wp.ddt != null) {
                    wp.ddt.printScreen();
                }
            }
        }
        if (e.getKeyCode() == KeyEvent.VK_M && wp.ddl != null) { // show Melody
            if (wp.ddl.isShowMelodyInfo()) //                      only for ddl
            {
                wp.ddl.setShowMelodyInfo(false);
            } else {
                wp.ddl.setShowMelodyInfo(true);
            }
            wp.ddl.repaint();
        }
        if (e.getKeyCode() == KeyEvent.VK_R) {
            if (wp.dw != null) {
                if (wp.dw.isRefreshStats()) {
                    wp.dw.setRefreshStats(false);
                } else {
                    wp.dw.setRefreshStats(true);
                }
            }
            if (wp.ddt != null) {
                if (wp.ddt.isRefreshStats()) {
                    wp.ddt.setRefreshStats(false);
                } else {
                    wp.ddt.setRefreshStats(true);
                }
            }
            if (wp.ddl != null) {
                if (wp.ddl.isRefreshStats()) {
                    wp.ddl.setRefreshStats(false);
                } else {
                    wp.ddl.setRefreshStats(true);
                }
            }
        }
        if (e.getKeyCode() == KeyEvent.VK_MINUS || e.getKeyCode() == KeyEvent.VK_COMMA) {
            if (wp.ddl != null) {
                wp.ddl.decreaseXWindow();
            }
            if (wp.ddt != null) {
                wp.ddt.decreaseXWindow();
            }
        }
        if (e.getKeyCode() == KeyEvent.VK_EQUALS || e.getKeyCode() == KeyEvent.VK_PERIOD) {
            if (wp.ddl != null) {
                wp.ddl.increaseXWindow();
            }
            if (wp.ddt != null) {
                wp.ddt.increaseXWindow();
            }
        }
    }
} // WormPlayKeyListener
