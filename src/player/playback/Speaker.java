package player.playback;

public class Speaker {
	private String peerID;
	private String id;
	private String name;
	private double minGain, maxGain;
	private boolean peerDefault;


	public Speaker(String peerID, String id, String name, double minGain, double maxGain, boolean peerDefault) {
		this.peerID = peerID;
		this.id = id;
		this.name = name;
		this.minGain = minGain;
		this.maxGain = maxGain;
		this.peerDefault = peerDefault;
	}


	public String getPeerID() {
		return peerID;
	}


	public String getId() {
		return id;
	}


	public String getName() {
		return name;
	}


	public double getMinGain() {
		return minGain;
	}


	public double getMaxGain() {
		return maxGain;
	}


	public boolean isPeerDefault() {
		return peerDefault;
	}

	@Override
	public String toString() {
		return name;
	}


	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((peerID == null) ? 0 : peerID.hashCode());
		return result;
	}


	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Speaker other = (Speaker) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (peerID == null) {
			if (other.peerID != null)
				return false;
		} else if (!peerID.equals(other.peerID))
			return false;
		return true;
	}


}
