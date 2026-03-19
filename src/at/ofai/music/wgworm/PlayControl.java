package at.ofai.music.wgworm;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.net.URLClassLoader;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * A JPanel with a Play/Pause button, a Stop button, and a JSlider.
 * <p>
 * Has key shortcuts implemented: The WinAmp like controls<br> Z/Y- Back to
 * beginning of file X -- Play (or SPACE) C -- Pause (or SPACE) V -- Stop B --
 * To the end of file
 *
 * @author Werner Goebl, Sept./Nov. 2005, April 2011
 *
 */
public class PlayControl extends JPanel {

    private static final long serialVersionUID = 1L;
    public static final int STOPPED = 0, PLAYING = 1, PAUSED = 2, RECORDING = 3, NOTRECORDING = 4;
    protected JButton playButton, stopButton, recordButton;
    protected JSlider slider;
    private int state = STOPPED;
    private MyPlayer myPlayer;
    private int slMin, slMax;
    private boolean v = false; // verbose output
    private final String playIcon = "icon/Play16.gif";
    private final String pauseIcon = "icon/Pause16.gif";
    private final String stopIcon = "icon/Stop16.gif";
    private final String recordIcon = "icon/Record16.gif";
    private final String recordIconRed = "icon/Record16-red.gif";
    protected PlayControlKeyListener pckl;
    boolean recordButtonPressed = false;
    public static final String defaultStopToolTip = "STOP (V)";
    public static final String defaultPlayToolTip = "PLAY/PAUSE (SPACE or X/C)";
    public static final String defaultRecordToolTip = "RECORD";

    /**
     * Constructor without passing a player. Do this using the method
     * setSlMin(), setSlMax(), and call setPlayer() (which calls init()),
     */
    public PlayControl() {
    }

    public PlayControl(MyPlayer player) {
        this(player, 100);
    } // constructor

    public PlayControl(MyPlayer player, int sliderMax) {
        this(player, 0, sliderMax);
    } // constructor

    /**
     * Creates a play control with PLAY/PAUSE and STOP buttons as well as a
     * slider to navigate within a media file.
     *
     * @param player The player to be controlled
     * @param sliderMin the beginning of the media file (if necessary)
     * @param sliderMax the length of the media file
     */
    public PlayControl(MyPlayer player, int sliderMin, int sliderMax) {
        myPlayer = player;
        slMin = sliderMin;
        slMax = sliderMax;
        init();
    } // constructor

    private void init() {
        PlayControlButtonListener bl = new PlayControlButtonListener();
        Dimension buttonSize = new Dimension(20, 20);
        recordButton = new JButton(new ImageIcon(getClass().getClassLoader().getResource(recordIcon)));
        recordButton.setVisible(false);
        recordButton.setBackground(Color.LIGHT_GRAY);
        recordButton.setPreferredSize(buttonSize);
        recordButton.setActionCommand("Record");
        recordButton.setFocusable(false);
        recordButton.setToolTipText(defaultRecordToolTip);
        recordButton.addActionListener(bl);

        playButton = new JButton(new ImageIcon(getClass().getClassLoader().getResource(playIcon)));
        // "icon/Play16.gif";
        playButton.setToolTipText(defaultPlayToolTip);
        playButton.setPreferredSize(buttonSize);
        playButton.setFocusable(false);
        playButton.setActionCommand("Play");
        playButton.addActionListener(bl);

        stopButton = new JButton(new ImageIcon(getClass().getClassLoader().getResource(stopIcon)));
        stopButton.setToolTipText(defaultStopToolTip);
        stopButton.setPreferredSize(buttonSize); // 20x18
        stopButton.setFocusable(false);
        stopButton.setActionCommand("Stop");
        stopButton.addActionListener(bl);

        slider = new JSlider(slMin, slMax, slMin);
        slider.setToolTipText("(left/right arrows with SHIFT or/and CTRL)");
        slider.setFocusable(false);
        slider.setPreferredSize(new Dimension(slider.getWidth(), 20));
        slider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                int filePosition = ((JSlider) e.getSource()).getValue();
                myPlayer.sliderMoved(filePosition);
            }
        });

        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        add(recordButton);
        add(playButton);
        add(stopButton);
        add(slider);
        this.setFocusable(true);
        this.requestFocusInWindow();
        pckl = new PlayControlKeyListener();
        this.addKeyListener(pckl);
        //this.setEnable(false);
        validate();
    }

    public void sliderReset() {
        slider.setValue(slMin);
    } // sliderReset()

    /**
     * Sets the slider value
     *
     * @param value the slider value to set
     */
    public void setSliderValue(int value) {
        slider.setValue(value);
    } // setValue()

    /**
     * Returns the slider value
     *
     * @return <code>int</code> value of the slider
     */
    public int getSliderValue() {
        return slider.getValue();
    } // getValue()

    /**
     * Sets the play button to play state (and displays a PLAY icon)
     */
    public void playSetPlay() {
        playButton.setIcon(new ImageIcon(getClass().getClassLoader()
                .getResource(playIcon)));
        state = PLAYING;
    } // playSetPlay()

    /**
     * Sets the play button to pause state (and displays a PAUSE icon)
     */
    public void playSetPause() {
        playButton.setIcon(new ImageIcon(getClass().getClassLoader()
                .getResource(pauseIcon)));
        state = PAUSED;
    } // playSetPause()

    /**
     *
     * @return Returns the state.
     */
    public int getState() {
        return state;
    } // getState()

    public void setState(int state) {
        this.state = state;
        if (state != PAUSED) {
            playButton.setActionCommand("Play");
        }
        if (state == PAUSED) {
            playButton.setActionCommand("Pause");
        }
    } // setState()

    /**
     * @return Returns the slMax.
     */
    public int getSlMax() {
        return slMax;
    }

    public void setSlMax(int max) {
        slMax = max;
        slider.setMaximum(slMax);
    }

    /**
     * @return Returns the slMin.
     */
    public int getSlMin() {
        return slMin;
    }

    public void setSlMin(int min) {
        slMin = min;
        slider.setMinimum(slMin);
    }

    public void setPlayButtonVisibility(boolean isVisible) {
        playButton.setVisible(isVisible);
    }

    public void play() {
        if (playButton.isEnabled()) {
            playSetPause();
            playButton.setActionCommand("Pause");
            state = PLAYING;
            if (v) {
                System.out.println("PlayControlButtonListener: PLAY");
            }
            myPlayer.startPlaying();
        }
    } // play

    public void pause() {
        if (playButton.isEnabled()) {
            playButton.setActionCommand("Play");
            playSetPlay();
            if (v) {
                System.out.println("PlayControlButtonListener: PAUSE");
            }
            state = PAUSED;
            myPlayer.pausePlaying();
        }
    } // pause

    public void stop() {
        if (stopButton.isEnabled()) {
            playButton.setActionCommand("Play");
            playButton.setIcon(new ImageIcon(getClass().getClassLoader()
                    .getResource(playIcon)));
            if (v) {
                System.out.println("PlayControlButtonListener: STOPPED");
            }
            state = STOPPED;
            myPlayer.stop();
        }
    } // stop

    public void record() {
        System.out.println("PlayControl.record()");
        if (recordButton.isEnabled() && recordButton.isVisible()) {
            if (!recordButtonPressed) {
                state = RECORDING;
                recordButton.setIcon(new ImageIcon(getClass()
                        .getClassLoader().getResource(recordIconRed)));
                recordButton.setToolTipText("Recording...");
                recordButton.setBackground(Color.RED);
                recordButtonPressed = true;
            } else {
                state = NOTRECORDING;
                recordButton.setIcon(new ImageIcon(getClass()
                        .getClassLoader().getResource(recordIcon)));
                recordButton.setToolTipText("Record");
                recordButton.setBackground(Color.LIGHT_GRAY);
                recordButtonPressed = false;
            }
            if (v) {
                System.out.println("PlayControlButtonListener: RECORDING");
            }
            myPlayer.startRecording();
        }
    } // play

    public void resetRecordButton() {
        state = NOTRECORDING;
        recordButton.setIcon(new ImageIcon(getClass()
                .getClassLoader().getResource(recordIcon)));
        recordButton.setToolTipText("Record");
        recordButton.setBackground(Color.LIGHT_GRAY);
        recordButtonPressed = false;
    } // resetRecordButton()

    /**
     * End of file reached.
     */
    public void ended() {
        playButton.setActionCommand("Play");
        playButton.setIcon(new ImageIcon(getClass().getClassLoader()
                .getResource(playIcon)));
        if (v) {
            System.out.println("PlayControlButtonListener: STOP");
        }
        state = STOPPED;
    } // ended()

    private void scroll(int sliderValue) {
        if (isEnabled()) {
            if (sliderValue > slMax) {
                sliderValue = slMax;
            }
            if (sliderValue < slMin) {
                sliderValue = slMin;
            }
            slider.setValue(sliderValue);
        }
    } // scroll

    /**
     * To be used in conjunction with the simple constructor PlayControl()
     * without arguments.
     *
     * @param _player
     */
    public void setPlayer(MyPlayer _player) {
        myPlayer = _player;
        init();
    }

    class PlayControlButtonListener implements ActionListener {

        public void actionPerformed(ActionEvent e) {
            String name = ((JButton) e.getSource()).getActionCommand();
            if (name.equals("Play")) {
                play();
            } else if (name.equals("Pause")) {
                pause();
            } else if (name.equals("Record")) {
                record();
            } else { // STOP
                stop();
            }
        }
    } // class PlayControlButtonListener

    class PlayControlKeyListener implements KeyListener {

        int inc1 = 1, inc2 = 3, inc3 = 6, inc4 = (int) ((slMax - slMin) / 10);

        public void keyTyped(KeyEvent e) {
        }

        public void keyPressed(KeyEvent e) {
            switch (e.getKeyCode()) {
                case KeyEvent.VK_RIGHT:
                    scroll(slider.getValue() + inc1);
                    break;
                case KeyEvent.VK_LEFT:
                    scroll(slider.getValue() - inc1);
                    break;
            }
            if ((e.getModifiers() & KeyEvent.SHIFT_MASK) != 0) { // SHIFT + key
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_RIGHT:
                        scroll(slider.getValue() + inc2);
                        break;
                    case KeyEvent.VK_LEFT:
                        scroll(slider.getValue() - inc2);
                        break;
                }
            }
            if ((e.getModifiers() & KeyEvent.CTRL_MASK) != 0) { // CTRL + key
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_RIGHT:
                        scroll(slider.getValue() + inc3);
                        break;
                    case KeyEvent.VK_LEFT:
                        scroll(slider.getValue() - inc3);
                        break;
                }
            }
            if ((e.getModifiers() & KeyEvent.CTRL_MASK) != 0 && // SHIFT + CTRL
                    (e.getModifiers() & KeyEvent.SHIFT_MASK) != 0) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_RIGHT:
                        scroll(slider.getValue() + inc4);
                        break;
                    case KeyEvent.VK_LEFT:
                        scroll(slider.getValue() - inc4);
                        break;
                }
            }
        } // keyPressed()

        public void keyReleased(KeyEvent e) {
            switch (e.getKeyCode()) {
                case KeyEvent.VK_Z:
                    pause();
                    slider.setValue(slMin);
                    break;
                case KeyEvent.VK_Y:
                    pause();
                    slider.setValue(slMin);
                    break; // do the same thing, just for German keyboards
                case KeyEvent.VK_X:
                    play();
                    break; // PLAY
                case KeyEvent.VK_C:
                    pause();
                    break; // PAUSE
                case KeyEvent.VK_V:
                    stop();
                    break; // STOP
                case KeyEvent.VK_B:
                    slider.setValue(slMax);
                    break;
                case KeyEvent.VK_SPACE:
                    if (state == PLAYING) {
                        pause();
                    } else {
                        play();
                    }
                    break;
                case KeyEvent.VK_RIGHT:
                    scroll(slider.getValue() + inc1);
                    break;
                case KeyEvent.VK_LEFT:
                    scroll(slider.getValue() - inc1);
                    break;
            }
        } // keyReleased()
    } // class PlayControlKeyListener

    public void setEnable(boolean enabled) {
        playButton.setEnabled(enabled);
        stopButton.setEnabled(enabled);
        slider.setEnabled(enabled);
    } // setEnable()

    public void enableRecordButton(boolean recEnabled) {
        recordButton.setEnabled(recEnabled);
    }

    public PlayControlKeyListener getPlayControlKeyListener() {
        return pckl;
    }

    public void setRecordButtonVisibility(boolean isVisible) {
        recordButton.setVisible(isVisible);
    }

    public void setRecordToolTip(String toolTip) {
        recordButton.setToolTipText(toolTip);
    }

    public void resetRecordToolTip() {
        recordButton.setToolTipText(defaultRecordToolTip);
    }

    public void setStopToolTip(String toolTip) {
        stopButton.setToolTipText(toolTip);
    }

    public void resetStopToolTip() {
        stopButton.setToolTipText(defaultStopToolTip);
    }

    public void setPckl(PlayControlKeyListener pckl) {
        this.pckl = pckl;
    }
} // Class PlayControl
