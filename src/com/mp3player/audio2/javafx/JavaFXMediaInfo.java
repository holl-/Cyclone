package com.mp3player.audio2.javafx;

import java.util.HashMap;

import com.mp3player.desktopaudio.AudioDataFormat;
import com.mp3player.desktopaudio.AudioEngine;
import com.mp3player.desktopaudio.MediaFile;
import com.mp3player.desktopaudio.MediaFormat;
import com.mp3player.desktopaudio.MediaInfo;

import javafx.scene.media.Track;

public class JavaFXMediaInfo extends MediaInfo {
	private double duration;
	
	public JavaFXMediaInfo(javafx.scene.media.Media fxMedia, MediaFile media, AudioEngine engine) {
		super(null);
		AudioDataFormat audioDataFormat = null;
		if(!fxMedia.getTracks().isEmpty()) {
			Track track = fxMedia.getTracks().get(0);
			String encoding;
			if(track.getMetadata().containsKey("encoding")) {
				encoding = (String) track.getMetadata().get("encoding");
			} else {
				encoding = track.getName();
			}
			audioDataFormat = new AudioDataFormat(encoding, null, -1, -1, -1, -1, -1, false,
					new HashMap<String, Object>(track.getMetadata()));
		}
		
		format = new MediaFormat(engine, engine.getMediaType(media), -1,
				new HashMap<String, Object>(fxMedia.getMetadata()), audioDataFormat);
		
		duration = fxMedia.getDuration().toSeconds();
	}

	@Override
	public String getTitle() {
		return (String) format.getProperty("title");
	}

	@Override
	public double getDuration() {
		return duration;
	}

	@Override
	public double estimateDuration() {
		if(getDuration() > 0) return getDuration();
		else return -1;
	}

}
