package audio.javafx;

import audio.MediaFile;
import audio.UnsupportedMediaFormatException;
import javafx.scene.media.Media;
import javafx.scene.media.MediaException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.concurrent.CountDownLatch;

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
		if(uri == null) {
			File file = File.createTempFile("stream", mediaFile.getFileName());
			try(FileOutputStream out = new FileOutputStream(file); InputStream in = mediaFile.openStream()) {
				in.transferTo(out);
			}
			uri = file.toURI();
		}
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
