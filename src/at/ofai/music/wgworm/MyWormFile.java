package at.ofai.music.wgworm;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;

/**
 * MyWormFile is a stand-alone copy and modification of Simon's WormFile class.
 * Reads in performance worm data.
 *
 */
public class MyWormFile {

    public double outFramePeriod, inFramePeriod;
    public int length, beatsPerBar, startBarNumber, tempoLate, tailLength;
    public String loudnessUnits, version,
            composer, piece, performer, key, year,
            indication, audioFile, audioPath, smoothing,
            trackLevel, beatLevel, upbeat,
            wormFileName, wormName,
            xLabel, yLabel;
    public double[] axis;
    public double[] time, tempo, intensity;
    public int[] flags, melody;
    public boolean hasMelodyInfo;
    public static final int TRACK = 1, BEAT = 2, BAR = 4, SEG1 = 8, SEG2 = 16,
            SEG3 = 32, SEG4 = 64;
    public static final double defaultFramePeriod = 0.1; // 10 FPS

    public MyWormFile() { // constructor
        clear();
    } // constructor

    public final void clear() {
        composer = "Unknown composer";
        piece = "Unknown piece";
        performer = "Unknown performer";
        key = "";  // A major
        year = ""; // 2009
        indication = ""; // Allegro
        beatLevel = "1/4";
        trackLevel = "1.0";
        upbeat = "0";
        beatsPerBar = 4;
        length = 0;
        audioFile = "";
        audioPath = "";
        wormFileName = "";
        wormName = ""; // just the Name of the wormFile (without path)
        smoothing = "";
        axis = new double[4];
        Arrays.fill(axis, Double.NaN);
        startBarNumber = -999;
        tempoLate = 0;
        version = "1.0";
        inFramePeriod = defaultFramePeriod;
        outFramePeriod = defaultFramePeriod;
        loudnessUnits = "sone";
        tailLength = 200;
        xLabel = "Tempo (bpm)";
        yLabel = "Loudness (sone)";
        hasMelodyInfo = false;
    } // clear()

    public URL read(String fileName) {
        InputStream fis; // fileInputStream
        URL wormURL = null;
        File f = null;
        try {
            try {
                wormURL = new URL(fileName);
                fis = wormURL.openStream();
                System.out.println("Opened URL: " + wormURL);
            } catch (Exception e) {
                f = new File(fileName);
                if (!f.isFile())// a local hack for UNC file names under Windows
                {
                    f = new File("//fichte" + fileName);
                }
                if (!f.isFile()) {
                    throw (new FileNotFoundException("Could not open: "
                            + fileName));
                }
                fis = (InputStream) new FileInputStream(f);
                System.out.println("Opened FILE: " + f);
            }
            InputStreamReader isr = new InputStreamReader(fis, "ISO-8859-1");
            BufferedReader in = new BufferedReader(isr);
            String input = in.readLine();
            if (input == null) {
                throw new Exception("Empty input file: " + fileName);
            }
            if (!input.startsWith("WORM")) {
                throw new Exception("Bad header format in file: " + fileName);
            }
            int delimiter = input.indexOf(':');
            while (delimiter >= 0) { // read in HEADER info
                String attribute = input.substring(0, delimiter).trim();
                String value = input.substring(delimiter + 1).trim();
                if (attribute.equalsIgnoreCase("Worm Version")) {
                    version = value;
                } else if (attribute.equalsIgnoreCase("FrameLength")) {
                    inFramePeriod = Double.parseDouble(value);
                } else if (attribute.equalsIgnoreCase("LoudnessUnits")) {
                    loudnessUnits = value;
                } else if (attribute.equalsIgnoreCase("Length")) {
                    length = Integer.parseInt(value);
                } else if (attribute.equalsIgnoreCase("AudioFile")) {
                    int index = value.lastIndexOf('/');
                    if (index >= 0) {
                        audioPath = value.substring(0, index);
                    }
                    audioFile = value.substring(index + 1);
                } else if (attribute.equalsIgnoreCase("Smoothing")) {
                    smoothing = value;
                } else if (attribute.equalsIgnoreCase("Composer")) {
                    composer = value;
                } else if (attribute.equalsIgnoreCase("Piece")) {
                    piece = value;
                } else if (attribute.equalsIgnoreCase("Performer")) {
                    performer = value;
                } else if (attribute.equalsIgnoreCase("Key")) {
                    key = value;
                } else if (attribute.equalsIgnoreCase("YearOfRecording")) {
                    year = value;
                } else if (attribute.equalsIgnoreCase("Indication")) {
                    indication = value;
                } else if (attribute.equalsIgnoreCase("BeatLevel")) {
                    beatLevel = value;
                } else if (attribute.equalsIgnoreCase("TrackLevel")) {
                    trackLevel = value;
                } else if (attribute.equalsIgnoreCase("Upbeat")) {
                    upbeat = value;
                } else if (attribute.equalsIgnoreCase("BeatsPerBar")) {
                    beatsPerBar = Integer.parseInt(value);
                } else if (attribute.equalsIgnoreCase("StartBarNumber")) {
                    startBarNumber = Integer.parseInt(value);
                } else if (attribute.equalsIgnoreCase("MelodyInfo")) {
                    if (Integer.parseInt(value) == 0) {
                        hasMelodyInfo = false;
                    } else {
                        hasMelodyInfo = true;
                    }
                } else if (attribute.equalsIgnoreCase("TempoLate")) {
                    tempoLate = Integer.parseInt(value);
                } else if (attribute.equalsIgnoreCase("tailLength")) {
                    tailLength = Integer.parseInt(value);
                } else if (attribute.equalsIgnoreCase("xLabel")) {
                    xLabel = value;
                } else if (attribute.equalsIgnoreCase("yLabel")) {
                    yLabel = value;
                } else if (attribute.equalsIgnoreCase("Axis")) {
                    StringTokenizer tk = new StringTokenizer(value);
                    for (int i = 0; i < axis.length; i++) {
                        axis[i] = Double.parseDouble(tk.nextToken());
                    }
                } else {
                    System.err.println("Header data ignored: " + attribute
                            + ":\t" + value);
                }
                input = in.readLine();
                if (input != null) {
                    delimiter = input.indexOf(':');
                } else {
                    break;
                }
            }
            // read in data
            time = new double[length];
            tempo = new double[length];
            intensity = new double[length];
            flags = new int[length];
            melody = new int[length];
            int index = 0;
            while ((input != null) && (index < length)) {
                StringTokenizer tk = new StringTokenizer(input);
                if (inFramePeriod == 0) {
                    time[index] = Double.parseDouble(tk.nextToken());
                }
                tempo[index] = Double.parseDouble(tk.nextToken());
                intensity[index] = Double.parseDouble(tk.nextToken());
                if (tk.hasMoreTokens()) {
                    flags[index] = Integer.parseInt(tk.nextToken());
                } else {
                    flags[index] = 0;
                }
                if (tk.hasMoreTokens()) {
                    melody[index] = Integer.parseInt(tk.nextToken());
                    hasMelodyInfo = true;
                } else {
                    melody[index] = 0;
                }
                input = in.readLine();
                index++;
            }
            if (inFramePeriod == 0) {
                for (int i = 0; i < length - 1; i++) {
                    outFramePeriod = (outFramePeriod + (time[i + 1] - time[i])) / 2;
                }
            }
            // round to 3 digits after the period
            outFramePeriod = (double) Math.round(outFramePeriod * 10000) / 10000;
            in.close();
            // correct missing startBarNumber (no upbeat=1; else=0)
            if (startBarNumber < -100 && Double.parseDouble(upbeat) == 0.0) {
                startBarNumber = 1;
            } else if (startBarNumber < -100) {
                startBarNumber = 0;
            }
        } catch (FileNotFoundException e) {
            System.err.println(e);
        } catch (IOException e) {
            System.err.println("IOException reading " + fileName);
        } catch (Exception e) {
            System.err.println("Error parsing file " + fileName + ": " + e);
            e.printStackTrace();
        }
        if (wormURL == null) {
            try {
                wormURL = f.toURL();
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
        }
        wormFileName = wormURL.toExternalForm();
        wormName = wormURL.getFile();
        return wormURL;
    } // read()

    /**
     * arrangeWormFile() creates a list of String lines with the wormFile in it.
     *
     * @return
     */
    public List<String> arrangeWormFile() {
        List<String> lines = new ArrayList<String>();
        lines.add("WORM Version:\t" + version);
        if (!"".equals(audioFile)) {
            lines.add("AudioFile:\t" + audioFile);
        }
        lines.add("Composer:\t" + composer);
        lines.add("Piece:\t" + piece);
        lines.add("Key:\t" + key);
        lines.add("Performer:\t" + performer);
        lines.add("YearOfRecording:\t" + year);
        if (inFramePeriod >= 0) {
            lines.add("FrameLength:\t" + inFramePeriod);
        }
        if (!"".equals(loudnessUnits)) {
            lines.add("LoudnessUnits:\t" + loudnessUnits);
        }
        if (axis[2] > 0 && axis.length == 4) {
            lines.add("Axis:\t" + axis[0] + " " + axis[1] + " " + axis[2] + " " + axis[3] + " ");
        }
        if (!"".equals(smoothing)) {
            lines.add("Smoothing:\t" + smoothing); // 
        }
        lines.add("StartBarNumber:\t" + startBarNumber);
        lines.add("TempoLate:\t" + tempoLate);
        lines.add("BeatLevel:\t" + beatLevel);
        lines.add("TrackLevel:\t" + trackLevel);
        lines.add("Upbeat:\t" + upbeat);
        lines.add("BeatsPerBar:\t" + beatsPerBar);
        lines.add("MelodyInfo:\t" + hasMelodyInfo);

        if (!"".equals(xLabel)) {
            lines.add("xLabel:\t" + xLabel);
        }
        if (!"".equals(yLabel)) {
            lines.add("yLabel:\t" + yLabel);
        }
        if (length > 0) {
            lines.add("Length:\t" + length);
        }

        // time, tempo, intensity flags, melody;
        String tmp;
        for (int i = 0; i < time.length; i++) {
            tmp = String.format("%.4f", time[i]) + "\t";
            tmp = tmp + String.format("%.4f", tempo[i]) + "\t";
            tmp = tmp + String.format("%.4f", intensity[i]) + "\t";
            tmp = tmp + String.format("%d", flags[i]);
            if (hasMelodyInfo) {
                tmp = tmp + "\t" + melody[i];
            }
            lines.add(tmp);
        }
        return lines;
    } // arrangeWormFile()
} // class MyWormFile
