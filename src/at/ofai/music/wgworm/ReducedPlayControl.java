package at.ofai.music.wgworm;

/**
 * A PlayControl without Slider (set invisible)
 * @author Werner Goebl
 * @see PlayControl
 */
public class ReducedPlayControl extends PlayControl {
	private static final long serialVersionUID = 1L;

	public ReducedPlayControl(MyPlayer player) {
		super(player, 2);
		super.slider.setVisible(false);
	}
	
	public void setEnabled(Boolean enabled) {
		super.setEnable(enabled);
	}
} // ReducedPlayControl
