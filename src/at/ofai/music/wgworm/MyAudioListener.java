package at.ofai.music.wgworm;

import javax.sound.sampled.SourceDataLine;

public class MyAudioListener implements Runnable {

	private SourceDataLine line; // instance variable declaration
	private int sleepTime; // milliseconds
	protected MyWormFile wormData;
	protected int currSlice;
	private DrawWorm dw;

	public MyAudioListener(SourceDataLine ln, MyWormFile wd) {
		// MyAudioListener(ln,wd,new DrawWorm.createWorm(wormData));
		line = ln;
		Thread t = new Thread(this);
		t.start();
		wormData = wd;
		currSlice = 0;
		dw = DrawWorm.createWorm(wormData);
		sleepTime = 50; // ms
	} // constructor

	public MyAudioListener(SourceDataLine ln, MyWormFile wd, DrawWorm drwm) {
		line = ln;
		Thread t = new Thread(this);
		t.start();
		wormData = wd;
		currSlice = 0;
		dw = drwm;
		sleepTime = 50; // ms
		System.out.println("MyAudioListener");
	} // constructor

	public void run() {
		while (true) {
			try { // that's three lines for sleeping a while
				Thread.sleep(sleepTime); // milliseconds
			} catch (InterruptedException e) {
			}

			/*
			 * System.out.println( (line.getMicrosecondPosition()/1000.0) + " " +
			 * (line.getLongFramePosition()/8192) + " " + line.available());
			 */
			if (line.getMicrosecondPosition() / 1000000.0 >= wormData.time[currSlice]
					&& currSlice < wormData.time.length) {
				/*
				 * System.out.println(line.getMicrosecondPosition()/1000000.0 + ";
				 * wormData: " + wormData.time[currSlice] + "; " + currSlice +
				 * "/" + wormData.time.length);
				 */
				dw.redraw(currSlice);
				// System.out.println(line.getMicrosecondPosition()/1000000.0
				// + ", Sleeptime = " + sleepTime
				// + ", currSlice = " + currSlice
				// + ", WormLength = " + (wormData.time.length-1));
				if (currSlice < wormData.time.length - 1)
					currSlice++;
			}
			if (currSlice >= wormData.time.length - 1) {
				sleepTime = 5000; // ms; to reduce senseless computing
			}
		} // while
	} // run
} // MyAudioListener
