package player.status;

import java.util.Optional;

import player.model.Identifier;
import vdp.Peer;
import vdp.VDP;

public class PlayerStatus {
	private VDP vdp;

	private PlaybackStatus playback;
	private PlayerTarget target;
	private Playlist playlist;


	public PlayerStatus(VDP vdp) {
		this.vdp = vdp;

		playback = vdp.getOrAddData(new PlaybackStatus());
		target = vdp.getOrAddData(new PlayerTarget());
		playlist = vdp.getOrAddData(new Playlist());
	}


	public VDP getVdp() {
		return vdp;
	}


	public PlaybackStatus getPlayback() {
		return playback;
	}


	public PlayerTarget getTarget() {
		return target;
	}

	public Optional<MachineInfo> getInfo(Peer peer) {
		return vdp.getData(MachineInfo.id(peer)).map(data -> (MachineInfo)data);
	}


	public Playlist getPlaylist() {
		return playlist;
	}

	public Optional<Identifier> getNext() {
		return playlist.getNext(playback.getCurrentMedia(), target.isLoop());
	}
	public void next() {
		target.setTargetMedia(getNext(), true);
	}

	public Optional<Identifier> getPrevious() {
		return playlist.getPrevious(playback.getCurrentMedia(), target.isLoop());
	}
	public void previous() {
		target.setTargetMedia(getPrevious(), true);
	}



}
