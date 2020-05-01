package player.model;

import java.util.List;

public class MediaSetEvent {
	private MediaSet set;
	private int index;
	private List<MediaInfo> sublist;

	public MediaSetEvent(MediaSet set, int index, List<MediaInfo> sublist) {
		this.set = set;
		this.index = index;
		this.sublist = sublist;
	}

	public MediaSet getSet() {
		return set;
	}

	/**
	 * If a sublist was added, returns the position at which it was inserted.
	 *
	 * @return the position of the inserted sublist
	 */
	public int getIndex() {
		return index;
	}

	public List<MediaInfo> getSublist() {
		return sublist;
	}

}
