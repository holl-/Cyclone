package player.playback;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import player.model.Identifier;
import vdp.Conflict;
import vdp.Distributed;
import vdp.RemoteFile;

/**
 * The playlist contains all currently shared media files with their IDs and
 * respective locations. It resembles the {@link PlayerTarget} in that it
 * defines what the playback engine should play next. However it is not part
 * thereof to reduce network traffic as the playlist is not changed that often.
 *
 * @author Philipp Holl
 *
 */
public class Playlist extends Distributed {
	private static final long serialVersionUID = 7881884218273201562L;

	public static final String VDP_ID = "playlist";

	private List<Identifier> list = new ArrayList<>();

	public Playlist() {
		super(VDP_ID, true, false);
	}

	@Override
	public Distributed resolveConflict(Conflict conflict) {
		// TODO Auto-generated method stub
		return null;
	}

	public List<Identifier> list() {
		return new ArrayList<>(list);
	}

	public int size() {
		return list.size();
	}

	public Optional<Identifier> first() {
		if(isEmpty()) return Optional.empty();
		return Optional.of(list.get(0));
	}

	public Optional<Identifier> last() {
		if(isEmpty()) return Optional.empty();
		return Optional.of(list.get(size()-1));
	}

	public Optional<Identifier> getNext(Optional<Identifier> current, boolean loop) throws IllegalArgumentException {
		if(isEmpty()) return Optional.empty();
		if(!current.isPresent()) return first();

		int index = list.indexOf(current.get());
		if (index < 0)
			throw new IllegalArgumentException("media ID " + current.get() + " is not contained in playlist.");
		if (index < size() - 1)
			return Optional.of(list.get(index + 1));
		else if (loop)
			return first();
		else
			return Optional.empty();
	}

	public Optional<Identifier> getPrevious(Optional<Identifier> mediaID, boolean loop) throws IllegalArgumentException {
		if(isEmpty()) return Optional.empty();
		if(!mediaID.isPresent()) return first();

		int index = list.indexOf(mediaID.get());
		if (index < 0)
			throw new IllegalArgumentException("media ID " + mediaID.get() + " is not contained in playlist.");
		if (index > 0)
			return Optional.of(list.get(index + -1));
		else if (loop)
			return last();
		else
			return Optional.empty();
	}

	public Identifier setAll(List<RemoteFile> files, int returnIDIndex, boolean shuffle, boolean firstStayFirst) {
		_clear();
		Identifier returnID = null;
		if(!files.isEmpty()) {
			list = files.stream().map(file -> _add(file)).collect(Collectors.toList());
			returnID = returnIDIndex >= 0 ? list.get(returnIDIndex) : list.get(0);
			if(shuffle) {
				if(firstStayFirst)
					_shuffle(Optional.of(returnID));
				else
					_shuffle(Optional.empty());
			}
			returnID = list.get(0);
		}
		fireChangedLocally();
		return returnID;
	}

	public Identifier addAll(List<RemoteFile> files, int returnIDIndex, boolean shuffle, Optional<Identifier> shuffleToFirst) {
		List<Identifier> newList = files.stream().map(file -> _add(file)).collect(Collectors.toList());
		Identifier returnID = returnIDIndex >= 0 ? newList.get(returnIDIndex) : null;
		if(shuffle) _shuffle(shuffleToFirst);
		fireChangedLocally();
		return returnID;
	}

	public void shuffle(Optional<Identifier> makeFirst) {
		_shuffle(makeFirst);
		fireChangedLocally();
	}

	private void _shuffle(Optional<Identifier> makeFirst) {
		Collections.shuffle(list);
		makeFirst.ifPresent(first -> {
			if(list.remove(first)) list.add(0, first);
			else throw new IllegalArgumentException("makeFirst is not contained in playlist");
		});
	}

	private Identifier _add(RemoteFile file) {
		Identifier media = new Identifier(file.getPeer().getID(), file.getPath());
		list.add(media);
		return media;
	}

	public Identifier add(RemoteFile file) {
		Identifier media = _add(file);
		fireChangedLocally();
		return media;
	}

	private void _clear() {
		list.clear();
	}

	public void clear() {
		_clear();
		fireChangedLocally();
	}

	public boolean isEmpty() {
		return list.isEmpty();
	}

	public void setAll(List<Identifier> newList) {
		list.clear();
		list.addAll(newList);
		fireChangedLocally();
	}

	public void remove(Identifier identifier) {
		boolean removed = list.remove(identifier);
		if(removed) fireChangedLocally();
	}

}
