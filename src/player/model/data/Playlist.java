package player.model.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import distributed.DFile;
import distributed.Conflict;
import distributed.Distributed;

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

	private List<DFile> list = new ArrayList<>();

	public Playlist() {
		super(true, false);
	}

	@Override
	public Distributed resolveConflict(Conflict conflict) {
		// TODO Auto-generated method stub
		return null;
	}

	public List<DFile> list() {
		return new ArrayList<>(list);
	}

	public int size() {
		return list.size();
	}

	public Optional<DFile> first() {
		if(isEmpty()) return Optional.empty();
		return Optional.of(list.get(0));
	}

	public Optional<DFile> last() {
		if(isEmpty()) return Optional.empty();
		return Optional.of(list.get(size()-1));
	}

	public Optional<DFile> getNext(Optional<DFile> current, boolean loop) throws IllegalArgumentException {
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

	public Optional<DFile> getPrevious(Optional<DFile> mediaID, boolean loop) throws IllegalArgumentException {
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

	public DFile setAll(List<DFile> files, int returnIDIndex, boolean shuffle, boolean firstStayFirst) {
		_clear();
		DFile returnID = null;
		if(!files.isEmpty()) {
			list.clear();
			list.addAll(files);
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

	public DFile addAll(List<DFile> files, int returnIDIndex, boolean shuffle, Optional<DFile> shuffleToFirst) {
		list.addAll(files);
		DFile returnID = returnIDIndex >= 0 ? files.get(returnIDIndex) : null;
		if(shuffle) _shuffle(shuffleToFirst);
		fireChangedLocally();
		return returnID;
	}

	public void shuffle(Optional<DFile> makeFirst) {
		_shuffle(makeFirst);
		fireChangedLocally();
	}

	private void _shuffle(Optional<DFile> makeFirst) {
		Collections.shuffle(list);
		makeFirst.ifPresent(first -> {
			if(list.remove(first)) list.add(0, first);
			else throw new IllegalArgumentException("makeFirst is not contained in playlist");
		});
	}

	public void add(DFile file) {
		list.add(file);
		fireChangedLocally();
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

	public void setAll(List<DFile> newList) {
		list.clear();
		list.addAll(newList);
		fireChangedLocally();
	}

	public void remove(DFile identifier) {
		boolean removed = list.remove(identifier);
		if(removed) fireChangedLocally();
	}

}
