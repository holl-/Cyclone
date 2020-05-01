package com.mp3player.vdp.internal;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Stream;

import com.mp3player.vdp.Peer;
import com.mp3player.vdp.RemoteFile;

public class LocalFile extends RemoteFile {
	private LocalFile root;

	/**
	 * If this file is not a root, this String represents
	 * the relative path including the root.
	 * Else, this is the root's name.
	 */
	private String relativePath;

	private File file;


	public static LocalFile createRoot(Peer localClient, File file, String name) {
		return new LocalFile(localClient, null, name, file);
	}

	public static LocalFile createChild(LocalFile root, String relativePath) {
		return new LocalFile(root.getPeer(), root, relativePath, new File(root.localFile().getParent(), relativePath));
	}


	public LocalFile(Peer client, LocalFile root, String relativePath, File file) {
		super(client);
		this.root = root != null ? root : this;
		this.relativePath = relativePath;
		this.file = file;
	}

	public boolean isRoot() {
		return root == this;
	}


	@Override
	public String getName() {
		if(isRoot()) return relativePath;
		else return file.getName();
	}

	@Override
	public String getAbsolutePath() {
		return file.getAbsolutePath();
	}

	@Override
	public String getPath() {
		return relativePath;
	}

	@Override
	public Optional<RemoteFile> getParentFile() {
		if(isRoot()) return Optional.empty();
		else {
			String relParent = new File(relativePath).getParent();
			if(relParent == null || relParent.isEmpty()) return Optional.of(root);
			else return Optional.of(createChild(root, relParent));
		}
	}

	@Override
	public boolean isDirectory() {
		return file.isDirectory();
	}

	@Override
	public boolean exists() throws IOException {
		return file.exists();
	}

	@Override
	public long lastModified() {
		return file.lastModified();
	}

	@Override
	public long length() {
		return file.length();
	}

	@Override
	public Stream<RemoteFile> list() throws UnsupportedOperationException, IOException {
		return Files.list(file.toPath()).map(f -> createChild(root, new File(relativePath, f.getFileName().toString()).toString()));
	}

	@Override
	public void copyTo(File localFile) throws IOException {
		InputStream in = null;
		OutputStream out = null;
		try {
			in = new FileInputStream(file);
			out = new FileOutputStream(localFile);
			byte[] buffer = new byte[1024*8];
			int len;
			while((len = in.read(buffer)) >= 0) {
				out.write(buffer, 0, len);
			}

		} finally {
			if(in != null) in.close();
			if(out != null) out.close();
		}
	}

	@Override
	public InputStream openStream() throws IOException, UnsupportedOperationException {
		return new FileInputStream(file);
	}

	@Override
	public File localFile() throws NoSuchElementException {
		return file;
	}

	@Override
	public String toString() {
		return file.getPath();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((relativePath == null) ? 0 : relativePath.hashCode());
		if(!isRoot()) result = prime * result + ((root == null) ? 0 : root.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		LocalFile other = (LocalFile) obj;
		if (relativePath == null) {
			if (other.relativePath != null)
				return false;
		} else if (!relativePath.equals(other.relativePath))
			return false;
		if(!isRoot()) {
			if(other.isRoot()) return false;
			return root.equals(other.root);
		}
		return true;
	}


}
