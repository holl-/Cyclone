package vdp;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * A file or directory mounted on any {@link Peer}.
 *
 * <p>
 * Local files can be mounted using one of {@link VDP}'s mount methods. To
 * access a remote file, use {@link Peer#getRootFiles()} or
 * {@link Peer#getFile(String)}.
 * </p>
 * <p>
 * Mounted directories indirectly mount all contained files and folders. The
 * path of indirectly mounted files then starts with the directly mounted
 * directory, see {@link #getPath()}.
 * </p>
 *
 * <p>
 * Once mounted, files should not be changed until unmounted. However, the
 * contents of directories are allowed to change.
 * </p>
 *
 * @author Philipp Holl
 *
 */
public abstract class RemoteFile {
	private Peer peer;

	public RemoteFile(Peer client) {
		this.peer = client;
	}

	/**
	 * Returns the peer which hosts this file.
	 *
	 * @return the peer which hosts this file
	 */
	public Peer getPeer() {
		return peer;
	}

	/**
	 * Returns the file name.
	 *
	 * @see File#getName()
	 * @return the file name
	 */
	public abstract String getName();

	/**
	 * Returns the absolute pathname to the file on the host peer.
	 *
	 * @see File#getAbsolutePath()
	 * @return the absolute pathname on the host peer
	 */
	public abstract String getAbsolutePath();

	/**
	 * Gets the path to this file under which it is mounted.
	 * <p>
	 * Example: The folder C:/music which contains the file song.mp3 was mounted
	 * using {@link VDP#mountFile(File)}. Then the path to song.mp3 will be
	 * music/song.mp3.
	 * </p>
	 *
	 * @return the path to this file under which it is mounted
	 */
	public abstract String getPath();

	/**
	 * Returns the parent file if present. A parent file is present unless this
	 * file was directly mounted using one of {@link VDP}'s mount methods. Else
	 * {@link Optional#empty()} is returned.
	 *
	 * This method also returns a valid {@link RemoteFile} even if the hosting
	 * peer is not available anymore.
	 *
	 * @return the parent file if present
	 */
	public abstract Optional<RemoteFile> getParentFile();

	/**
	 * Returns true if this object represents a directory. This method may not
	 * check if the path is still available. If not, the result of this method
	 * is undefined.
	 *
	 * @return true if this object represents a directory
	 */
	public abstract boolean isDirectory();

	/**
	 * Tests if this file still exists on the remote peer. If the hosting peer
	 * is not available, this method throws an {@link IOException}.
	 *
	 * @return true if the file still exists, false if it does not exist
	 * @throws IOException
	 *             if the hosting peer is not available
	 */
	public abstract boolean exists() throws IOException;

	/**
	 * Returns the modification date of this file.
	 *
	 * <p>
	 * Files should not be modified while they are being mounted by a
	 * {@link VDP}. Therefore this method also returns a valid date if the
	 * hosting peer is not available.
	 * </p>
	 *
	 * @return the modification date of this file
	 * @throws UnsupportedOperationException
	 *             if this {@link RemoteFile} represents a directory
	 */
	public abstract long lastModified() throws UnsupportedOperationException;

	/**
	 * Returns the size of this file in bytes.
	 * <p>
	 * Files should not be modified while they are being mounted by a
	 * {@link VDP}. Therefore this method also returns a valid size if the
	 * hosting peer is not available.
	 * </p>
	 *
	 * @return the size of this file in bytes
	 * @throws UnsupportedOperationException
	 *             if this {@link RemoteFile} represents a directory
	 */
	public abstract long length() throws UnsupportedOperationException;

	/**
	 * Obtains a list of files contained in this directory by the remote peer.
	 *
	 * @return files contained in this directory
	 * @throws UnsupportedOperationException
	 *             if this file is not a directory
	 * @throws IOException
	 *             if the remote peer is not available
	 */
	public abstract Stream<RemoteFile> list() throws UnsupportedOperationException, IOException;

	/**
	 * Copies this {@link RemoteFile} to a local file. This method blocks until
	 * the transfer is complete.
	 *
	 * @param localFile
	 *            file to be written to
	 * @throws IOException
	 *             if the remote peer is not available or transferring the file
	 *             fails
	 * @throws UnsupportedOperationException
	 *             if this is a directory
	 */
	public abstract void copyTo(File localFile) throws IOException, UnsupportedOperationException;

	/**
	 * Opens an <code>InputStream</code> for this file.
	 *
	 * @return an <code>InputStream</code> for this file
	 * @throws IOException
	 *             if the hosting peer is not available or the connection is
	 *             interrupted
	 * @throws UnsupportedOperationException
	 *             if this is a directory
	 */
	public abstract InputStream openStream() throws IOException, UnsupportedOperationException;

	/**
	 * If this file is stored on the local systemcontrol, this method returns the
	 * associated {@link File}. To test if a file is local, use
	 * {@link #isLocal()}.
	 *
	 * @return the associated File if available
	 * @throws NoSuchElementException
	 *             if this file is stored on a remote systemcontrol
	 */
	public abstract File localFile() throws NoSuchElementException;

	/**
	 * A file is local, it its associated peer is the local peer.
	 *
	 * @return if this file is stored locally
	 * @see Peer#isLocal()
	 */
	public boolean isLocal() {
		return getPeer().isLocal();
	}
}
