package audio;

import java.io.Serializable;

public class MediaType implements Serializable
{
	private static final long serialVersionUID = 1784450261867960105L;
	
	
	private String name;
	private String fileExtension;
	
	
	public MediaType(String name, String fileExtension) {
		this.name = name;
		this.fileExtension = fileExtension;
	}


	public String getName() {
		return name;
	}


	/**
	 * Returns the file extension in lower case.
	 * Examples include "mp3", "wav" etc.
	 * @return the file extension in lower case
	 */
	public String getFileExtension() {
		return fileExtension;
	}
	


	@Override
	public String toString() {
		return name+" ("+fileExtension+")";
	}


	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((fileExtension == null) ? 0 : fileExtension.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
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
		MediaType other = (MediaType) obj;
		if (fileExtension == null) {
			if (other.fileExtension != null)
				return false;
		} else if (!fileExtension.equals(other.fileExtension))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}
	
	
	
}
