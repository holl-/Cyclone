package player.model;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import player.status.PlaybackStatus;
import vdp.Distributed;
import vdp.RemoteFile;
import vdp.VDP;

public class MediaIndex {
	private VDP vdp;
	private List<Identifier> allRoots;
	private List<Identifier> localRoots;
	private MediaSet recentlyUsed;

	/**
	 * Contains all indexed files and directories.
	 * Only media-files are added, other files are ignored.
	 * Directories which don't contain any media files are still added.
	 */
	private MediaSet localIndex; // files and directories

	/**
	 * Lookup-table. This map also contains non-indexed media files.
	 */
	private Map<Identifier, MediaInfo> infoMap;

	private ExecutorService indexService;

	private List<MediaIndexListener> listeners = new CopyOnWriteArrayList<>();

	private File savefile;



	public MediaIndex(VDP vdp, File savefile) {
		this.vdp = vdp;
		this.savefile = savefile;
		allRoots = new ArrayList<>();
		localRoots = new ArrayList<>();
		recentlyUsed = new MediaSet();
		recentlyUsed.setWorking(false);

		localIndex = new MediaSet();

		infoMap = new HashMap<>();

		Optional<Distributed> playback = vdp.getData(PlaybackStatus.VDP_ID);
		if(!playback.isPresent()) throw new IllegalStateException("playback must be present in VDP");
		playback.get().addDataChangeListener(e -> ((PlaybackStatus)e.getData()).getCurrentMedia().ifPresent(m -> addToRecentlyUsed(m)));

		indexService = Executors.newSingleThreadExecutor(r -> new Thread(r, "Index Service"));

		try {
			load(savefile, true);
		} catch (ClassNotFoundException | IOException e1) {
			e1.printStackTrace();
			addDefaultRoots();
		}
	}


	private void load(File file, boolean elseLoadDefault) throws IOException, ClassNotFoundException {
		if(!file.exists()) {
			if(elseLoadDefault) addDefaultRoots();
		}
		else {
			ObjectInputStream in = new ObjectInputStream(new BufferedInputStream(new FileInputStream(file)));
			ExternalForm f = (ExternalForm) in.readObject();
			in.close();
			f.restore(this);
		}
	}

	private void save(File file) throws IOException {
		ObjectOutputStream out = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(file)));
		out.writeObject(new ExternalForm(this));
		out.close();
	}

	private static class ExternalForm implements Serializable
	{
		private static final long serialVersionUID = -1138185882296825505L;


		private List<String> localRoots;
//		private List<String> recentlyUsed;

		public ExternalForm(MediaIndex index) {
			localRoots = new ArrayList<>(index.localRoots.stream()
					.map(id -> id.lookup(index.vdp))
					.filter(Optional::isPresent)
					.map(Optional::get)
					.filter(RemoteFile::isLocal)
					.map(RemoteFile::localFile)
					.map(File::getAbsolutePath)
					.collect(Collectors.toList()));
		}

		public void restore(MediaIndex index) {
			localRoots.forEach(path -> {
				index.addLocalRoot(new File(path), false);
			});
		}
	}

	private void addDefaultRoots() {
		File music = new File(System.getProperty("user.home"), "Music");
		if(music.exists() && music.isDirectory()) {
			addLocalRoot(music, true);
		}
	}


	public Optional<MediaInfo> getInfo(Identifier id) {
		return Optional.ofNullable(infoMap.get(id));
	}

	public Optional<MediaInfo> getInfo(RemoteFile file) {
		return Optional.ofNullable(infoMap.get(new Identifier(file)));
	}

	public MediaSet startSearch(MediaFilter filter) {
		MediaSet set = new MediaSet();
		new Thread(() -> {
			List<MediaInfo> searchList = localIndex.getItems().stream().filter(m -> filter.applyAsDouble(m) > 0).collect(Collectors.toList());
			set.add(searchList);
			set.setWorking(false);
		}).start();
		return set;
	}

	public MediaSet startSearch(String pattern) {
		String lowerCase = pattern.toLowerCase();
		return startSearch(media -> media.getRelativePath().toLowerCase().contains(lowerCase) ? 1 : 0);
	}

	public boolean isIndexed(RemoteFile file) {
		return isIndexed(new Identifier(file));
	}

	public boolean isIndexed(Identifier id) {
		boolean inLocalIndex = localIndex.contains(id);
		return inLocalIndex;
	}

	private void addToRecentlyUsed(Identifier id) {
		Optional<MediaInfo> opMedia = id.lookup(vdp).map(file -> getOrAdd(file));
		if(!opMedia.isPresent()) return;
		MediaInfo media = opMedia.get();
		recentlyUsed.remove(Arrays.asList(media));
		recentlyUsed.add(0, Arrays.asList(media));
	}

	/**
	 *
	 * @param dir
	 * @return true if any file was added
	 */
	private void addToIndex(RemoteFile dir) {
		if(isIndexed(dir)) return;

		List<MediaInfo> mediaList = new ArrayList<>();
		mediaList.add(getOrAdd(dir));

		List<RemoteFile> files;
		try {
			files = dir.list().collect(Collectors.toList());
		} catch (UnsupportedOperationException | IOException e) {
			return;
		}

		for(RemoteFile file : files) {
			if(file.isDirectory()) {
				addToIndex(file);
			}
			else if(AudioFiles.isAudioFile(file.getName())){
				mediaList.add(getOrAdd(file));
			}
		}

		localIndex.add(mediaList);
	}


	public MediaInfo getOrAdd(RemoteFile file) {
		return infoMap.computeIfAbsent(new Identifier(file), f -> new MediaInfo(file));
	}


	public VDP getVdp() {
		return vdp;
	}

	public List<Identifier> listAllRoots() {
		return Collections.unmodifiableList(allRoots);
	}

	public List<Identifier> localRoots() {
		return Collections.unmodifiableList(localRoots);
	}

	public MediaSet getRecentlyUsed() {
		return recentlyUsed;
	}

	public void addLocalRoot(File file) {
		addLocalRoot(file, true);
	}

	private void addLocalRoot(File file, boolean save) {
		// TODO do nothing if already indexed

		RemoteFile rfile = vdp.mountFile(file);
		Identifier id = new Identifier(rfile);
		localRoots.add(id);
		allRoots.add(id);

		MediaIndexEvent e = new MediaIndexEvent(this, rfile);
		listeners.forEach(l -> l.onAdded(e));

		indexService.execute(() -> {
			localIndex.setWorking(true);
			addToIndex(rfile);
			localIndex.setWorking(false);
			if(save) {
				try {
					save(savefile);
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		});
	}

	public void removeLocalRoot(RemoteFile file) {
		if(localRoots.remove(file)) {
			allRoots.remove(file);

			MediaIndexEvent e = new MediaIndexEvent(this, file);
			listeners.forEach(l -> l.onRemoved(e));
		}
	}

	public void addMediaIndexListener(MediaIndexListener l) {
		listeners.add(l);
	}

	public void removeMediaIndexListener(MediaIndexListener l) {
		listeners.remove(l);
	}



}
