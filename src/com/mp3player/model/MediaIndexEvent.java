package com.mp3player.model;

import com.mp3player.vdp.RemoteFile;

public class MediaIndexEvent {
	private MediaIndex index;
	private RemoteFile file;

	public MediaIndexEvent(MediaIndex index, RemoteFile file) {
		this.index = index;
		this.file = file;
	}

	public MediaIndex getIndex() {
		return index;
	}

	public RemoteFile getFile() {
		return file;
	}

}
