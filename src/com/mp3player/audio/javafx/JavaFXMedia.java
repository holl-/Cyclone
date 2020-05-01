package com.mp3player.audio.javafx;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.CountDownLatch;

import com.mp3player.audio.MediaFile;
import com.mp3player.audio.UnsupportedMediaFormatException;

import javafx.scene.media.Media;
import javafx.scene.media.MediaException;

public class JavaFXMedia
{
	private MediaFile mediaFile;
	private Media fxMedia;
	private CountDownLatch durationKnownLatch;
	
	
	public JavaFXMedia(MediaFile media) {
		this.mediaFile = media;
		durationKnownLatch = new CountDownLatch(1);
	}
	
	
	
	public synchronized void prepare() throws IOException, UnsupportedMediaFormatException {
		if(fxMedia != null) return;
		
		URI uri = mediaFile.toURI();
		if(uri == null) throw new IOException("media must support one of the following URI formats: HTTP, FILE, JAR");
		try {
			fxMedia = new Media(uri.toString());
		} catch(MediaException exc) {
			if(exc.getType() == MediaException.Type.MEDIA_UNAVAILABLE ||
					exc.getType() == MediaException.Type.MEDIA_INACCESSIBLE)
				throw new IOException(exc);
			throw new UnsupportedMediaFormatException(exc);
		} catch(UnsupportedOperationException exc) {
			throw new IOException(exc);
		}
		
		if(fxMedia.getDuration().isUnknown()) {
			fxMedia.durationProperty().addListener((duration, oldValue, newValue) -> durationKnownLatch.countDown());
		} else {
			durationKnownLatch.countDown();
		}
	}
	
	public void waitForDurationKnown() throws InterruptedException {
		durationKnownLatch.await();
	}
	
	public synchronized Media getFXMedia() {
		return fxMedia;
	}
	
	public synchronized boolean isPrepared() {
		return fxMedia != null;
	}
	
	
	public MediaFile getMediaFile() {
		return mediaFile;
	}
}
