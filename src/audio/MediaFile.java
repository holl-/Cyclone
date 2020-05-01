package audio;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

/**
 * A <code>MediaFile</code> represents a media, usually sound, file 
 * either stored locally or accessible on some server.
 * The audio data in the file is stored in an encoded format like
 * <i>MP3, WAVE</i>, etc.
 * 
 * <p>
 * Only file information as well as a file {@link InputStream} 
 * can be obtained through the <code>MediaFile</code> interface.
 * 
 * In situations where the media format is already known, the
 * {@link MediaStream} interface should be used instead.
 * </p>
 * 
 * @author Philipp Holl
 *
 */
public interface MediaFile {

	
//	/**
//	 * Returns true, if the media can be deleted.
//	 * @see #delete()
//	 * @return true, if the media can be deleted
//	 */
//	public abstract boolean canDelete();
//	/**
//	 * Deletes all files or resources associated with this <code>MediaFile</code>.
//	 * @throws IOException if deleting failed
//	 * @throws UnsupportedOperationException if deleting is not supported
//	 * @see #canDelete()
//	 */
//	public abstract void delete() throws IOException, UnsupportedOperationException;
//	
//	/**
//	 * Tests if the media is still available, meaning that
//	 * {@link #openStream()} will return a valid stream if requested.
//	 * Usually, but not necessarily, this means that the storing file exists.
//	 * @return true if the media is still available.
//	 * @see #openStream()
//	 * @see #canDelete()
//	 */
//	public abstract boolean available();

	/**
	 * Reads the media as a file, ignoring all format information.
	 * @return an <code>InputStream</code> for the media file
	 * @throws IOException if the media is not available
	 */
	InputStream openStream() throws IOException;
	
	/**
	 * Returns a local file in which the media is stored or 
	 * <code>null</code> if none exists.
	 * @return a local file in which the media is stored or 
	 * <code>null</code> if none exists
	 */
	File getFile();

	/**
	 * Returns the name of the file that stores the media.
	 * This method never returns <code>null</code>, even if the
	 * media is not stored anywhere.
	 * @return the filename
	 */
	String getFileName();
	
	/**
	 * If the media is stored locally or on an openly accessible server,
	 * this method returns the location, otherwise <code>null</code>.
	 * @return the media location or <code>null</code> if not available.
	 */
	URI toURI();

	/**
	 * Returns the file size in bytes or -1 if unknown.
	 * @return the file size in bytes or -1 if unknown
	 */
	long getFileSize();


}