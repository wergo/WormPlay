package at.ofai.music.wgworm;

/**
 * Interface of a Player with its subfunctions.
 * @author wernerg
 * @see PlayControl
 */
public interface MyPlayer {
	public void startPlaying();
	public void pausePlaying();
	public void stop();
	public void sliderMoved(int sliderPosition);
	public double getAudioTime();
	public void startRecording() ;
} // Interface MyPlayer