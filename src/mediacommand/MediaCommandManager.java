package mediacommand;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public abstract class MediaCommandManager {

	private static MediaCommandManager instance;
	
	public static boolean isPlatformSupported() {
		return JIntellitypeMediaCommandManager.isPlatformSupported();
	}
	public static boolean isSupported() {
		return JIntellitypeMediaCommandManager.isSupported();
	}
	public static MediaCommandManager getInstance() {
		if(instance == null && isSupported()) {
			instance = new JIntellitypeMediaCommandManager();
		}
		return instance;
	}
	
	
	
	
	protected List<MediaCommandListener> listeners;
	
	
	public MediaCommandManager() {
		listeners = new CopyOnWriteArrayList<MediaCommandListener>();
	}
	
	public abstract void cleanUp();
	
	
	public void addMediaCommandListener(MediaCommandListener l) {
		listeners.add(l);
	}
	
	public void removeMediaCommandListener(MediaCommandListener l) {
		listeners.remove(l);
	}
	
	protected void fireCommandReceived(MediaCommand command) {
		for(MediaCommandListener l : listeners) {
			l.commandReceived(command);
		}
	}
}
