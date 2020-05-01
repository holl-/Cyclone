package com.mp3player.fx.playerwrapper;

import com.mp3player.model.MediaFilter;
import com.mp3player.model.MediaIndex;
import com.mp3player.vdp.RemoteFile;

import javafx.collections.ObservableList;

public class MediaIndexWrapper {
	private MediaIndex index;
	private MediaSetWrapper recentlyUsed;

	private ObservableList<RemoteFile> localRoots;
	private ObservableList<RemoteFile> allRoots;


	public MediaIndexWrapper(MediaIndex index) {
		this.index = index;

		recentlyUsed = new MediaSetWrapper(index.getRecentlyUsed());
	}


	public MediaIndex getIndex() {
		return index;
	}


	public MediaSetWrapper getRecentlyUsed() {
		return recentlyUsed;
	}

	public MediaSetWrapper startSearch(MediaFilter filter) {
		return new MediaSetWrapper(index.startSearch(filter));
	}

	public MediaSetWrapper startSearch(String pattern) {
		return new MediaSetWrapper(index.startSearch(pattern));
	}

}
