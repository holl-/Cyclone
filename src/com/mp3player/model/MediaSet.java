package com.mp3player.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class MediaSet {
	private List<MediaInfo> list;
	private boolean working;

	private List<MediaSelectionListener> listeners = new CopyOnWriteArrayList<>();


	public MediaSet() {
		list = new ArrayList<>();
		working = true;
	}


	public List<MediaInfo> getItems() {
		return Collections.unmodifiableList(list);
	}

	public boolean isWorking() {
		return working;
	}

	public void addMediaSelectionListener(MediaSelectionListener l) {
		listeners.add(l);
	}

	public void removeMediaSelectionListener(MediaSelectionListener l) {
		listeners.remove(l);
	}

	void setWorking(boolean working) {
		this.working = working;
		MediaSetEvent e = new MediaSetEvent(this, -1, null);
		listeners.forEach(l -> l.onWorkingChanged(e));
	}

	void add(int index, List<MediaInfo> newList) {
		list.addAll(index, newList);
		MediaSetEvent e = new MediaSetEvent(this, index, Collections.unmodifiableList(newList));
		listeners.forEach(l -> l.onAdded(e));
	}

	// lots of boilerplate objects
	void add(List<MediaInfo> newList) {
		int index = list.size();
		list.addAll(newList);
		MediaSetEvent e = new MediaSetEvent(this, index, Collections.unmodifiableList(newList));
		listeners.forEach(l -> l.onAdded(e));
	}

	void remove(List<MediaInfo> newList) {
		if(list.removeAll(newList)) {
			MediaSetEvent e = new MediaSetEvent(this, -1, Collections.unmodifiableList(newList));
			listeners.forEach(l -> l.onRemoved(e));
		}
	}


	boolean contains(MediaInfo media) {
		return list.contains(media);
	}


	// TODO slow, implement via private HashSet<Identifier>
	public boolean contains(Identifier id) {
		return list.stream().anyMatch(media -> media.getIdentifier().equals(id));
	}
}
