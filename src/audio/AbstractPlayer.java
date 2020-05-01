package audio;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import audio.PlayerEvent.EventType;

public abstract class AbstractPlayer implements Player {

	protected List<PlayerEventListener> endOfMediaListeners = new CopyOnWriteArrayList<PlayerEventListener>();
	protected List<PlayerEventListener> activationListeners = new CopyOnWriteArrayList<PlayerEventListener>();
	protected List<PlayerEventListener> stateListeners = new CopyOnWriteArrayList<PlayerEventListener>();
	
	


	@Override
	public void addActivationListener(PlayerEventListener l) {
		activationListeners.add(l);
	}

	@Override
	public void removeActivationListener(PlayerEventListener l) {
		activationListeners.remove(l);
	}

	@Override
	public void addStateListener(PlayerEventListener l) {
		stateListeners.add(l);
	}

	@Override
	public void removeStateListener(PlayerEventListener l) {
		stateListeners.remove(l);
	}

	@Override
	public void addEndOfMediaListener(PlayerEventListener l) {
		endOfMediaListeners.add(l);
	}

	@Override
	public void removeEndOfMediaListener(PlayerEventListener l) {
		endOfMediaListeners.remove(l);
	}
	
	
	
	protected void fireActivated(double position, Object cause) {
		PlayerEvent event = new PlayerEvent(this, EventType.ACTIVATE, position, System.currentTimeMillis(), cause);
		for(PlayerEventListener l : activationListeners) {
			l.onEvent(event);
		}
	}
	
	protected void fireDeactivated(double position, Object cause) {
		PlayerEvent event = new PlayerEvent(this, EventType.DEACTIVATE, position, System.currentTimeMillis(), cause);
		for(PlayerEventListener l : activationListeners) {
			l.onEvent(event);
		}
	}
	
	protected void fireStarted(double position, Object cause) {
		PlayerEvent event = new PlayerEvent(this, EventType.START, position, System.currentTimeMillis(), cause);
		for(PlayerEventListener l : stateListeners) {
			l.onEvent(event);
		}
	}
	
	protected void fireStopped(double position, Object cause) {
		PlayerEvent event = new PlayerEvent(this, EventType.STOP, position, System.currentTimeMillis(), cause);
		for(PlayerEventListener l : stateListeners) {
			l.onEvent(event);
		}
	}
	
	protected void firePositionChanged(double oldPosition, Object cause) {
		PlayerEvent event = new PlayerEvent(this, EventType.POSITION, oldPosition, System.currentTimeMillis(), cause);
		for(PlayerEventListener l : stateListeners) {
			l.onEvent(event);
		}
	}
	
	protected void fireEndOfMedia(double positionMillis) {
		PlayerEvent event = new PlayerEvent(this, EventType.STOP, positionMillis, System.currentTimeMillis(), PlayerEvent.END_OF_MEDIA);
		for(PlayerEventListener l : endOfMediaListeners) {
			l.onEvent(event);
		}
	}
}
