package player.model;

import java.io.IOException;
import java.io.Serializable;
import java.util.Optional;

import vdp.RemoteFile;
import vdp.VDP;

/**
 * Points to a RemoteFile but can exist without the peer being connected.
 * Use {@link Identifier#lookup(VDP)} to fetch the corresponding file.
 */
public class Identifier implements Serializable {
	private static final long serialVersionUID = 7773145268758867752L;


	private String peerID;
	private String path;

	public Identifier(RemoteFile file) {
		this(file.getPeer().getID(), file.getPath());
	}

	public Identifier(String peerID, String path) {
		this.peerID = peerID;
		this.path = path;
	}


	public String getPeerID() {
		return peerID;
	}


	public String getPath() {
		return path;
	}

	@Override
	public String toString() {
		return path;
	}


	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((path == null) ? 0 : path.hashCode());
		result = prime * result + ((peerID == null) ? 0 : peerID.hashCode());
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
		Identifier other = (Identifier) obj;
		if (path == null) {
			if (other.path != null)
				return false;
		} else if (!path.equals(other.path))
			return false;
		if (peerID == null) {
			if (other.peerID != null)
				return false;
		} else if (!peerID.equals(other.peerID))
			return false;
		return true;
	}

	public String getFileName() {
		String name = this.path;
		if(name.contains("/")) name = name.substring(name.lastIndexOf('/')+1);
		if(name.contains("\\")) name = name.substring(name.lastIndexOf('\\')+1);
		return name;
	}

	public String inferTitle() {
		String title = getFileName();
		if(title.contains(".")) title = title.substring(0, title.lastIndexOf('.'));
		return title;
	}

	public Optional<RemoteFile> lookup(VDP vdp) {
		return Optional.ofNullable(vdp.getPeer(peerID)).map(peer -> {
			try {
				return peer.getFile(path);
			} catch (IOException e) {
				return null;
			}
		});
	}

	public static Optional<RemoteFile> lookup(Optional<Identifier> optionalMedia, VDP vdp) {
		return optionalMedia.flatMap(media -> media.lookup(vdp));
	}



}
