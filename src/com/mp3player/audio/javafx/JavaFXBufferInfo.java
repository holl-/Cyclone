package com.mp3player.audio.javafx;

import com.mp3player.audio.AudioBuffer;

import javafx.scene.media.MediaPlayer;

public class JavaFXBufferInfo implements AudioBuffer {
	private MediaPlayer player;
	
	
	public JavaFXBufferInfo(MediaPlayer player) {
		this.player = player;
	}
	
	

	@Override
	public long getAllocatedMemory() {
		return -1;
	}

	@Override
	public double getStartPosition() {
		return 0;
	}

	@Override
	public double getEndPosition() {
		return player.getBufferProgressTime().toSeconds();
	}

}
