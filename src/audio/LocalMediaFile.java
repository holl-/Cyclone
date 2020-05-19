package audio;

import java.io.*;
import java.net.URI;

public class LocalMediaFile implements MediaFile, Serializable
{
	private static final long serialVersionUID = 2991942925546104334L;
	
	
	private File file;
	
	public LocalMediaFile(File file) {
		if(file == null) throw new IllegalArgumentException("file = null");
		this.file = file.getAbsoluteFile();
	}
	
	
	public File getFile() {
		return file;
	}

//	@Override
//	public boolean canDelete() {
//		return true;
//	}
//
//	@Override
//	public void delete() throws IOException {
//		Path path = file.toPath();
//		Files.delete(path);
//	}
//
//
//	@Override
//	public boolean available() {
//		return file.exists();
//	}
	
	
	@Override
	public InputStream openStream() throws IOException {
		return new FileInputStream(file);
	}
	
	
	@Override
	public String getFileName() {
		return file.getName();
	}
	
	@Override
	public URI toURI() {
		return file.toURI();
	}

	/**
	 * Returns the file size in bytes or -1 if not known.
	 */
	@Override
	public long getFileSize() {
		return file.length();
	}
	

	@Override
	public boolean equals(Object o) {
		if(o instanceof LocalMediaFile) {
			LocalMediaFile m = (LocalMediaFile) o;
			return m.file.equals(file);
		}
		return false;
	}
	
	@Override
	public int hashCode() {
		return file.hashCode();
	}

}
