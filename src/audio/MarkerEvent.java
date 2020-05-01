package audio;


public class MarkerEvent {

	private Player player;
	private double markerPosition;
	private boolean passedForward;
	private boolean skipped;
	
	
	public MarkerEvent(Player player, double position,
			boolean passedForward, boolean jumped) {
		this.player = player;
		this.markerPosition = position;
		this.passedForward = passedForward;
		this.skipped = jumped;
	}


	public Player getPlayer() {
		return player;
	}


	public double getMarkerPosition() {
		return markerPosition;
	}


	public boolean wasPassedForward() {
		return passedForward;
	}


	public boolean wasSkipped() {
		return skipped;
	}
	
	
	
}
