package com.mp3player.audio;


public class PlayerEvent {
	private Player player;
	private EventType type;
	private double position;
	private long time;
	private Object cause; // Throwable, END_OF_MEDIA, EXTERNAL_INTERRUPT, USER_COMMAND
	
	
	public enum EventType
	{
		START, STOP,
		ACTIVATE, DEACTIVATE,
		POSITION,
	}
	
	public static final Object END_OF_MEDIA = "END_OF_MEDIA";
	/**
	 * This could be an unknown error or some other unknown reason for the
	 * player to stop or deactivate.
	 */
	public static final Object EXTERNAL_INTERRUPT = "EXTERNAL_INTERRUPT";
	public static final Object USER_COMMAND = "USER_COMMAND";
	
	
	public PlayerEvent(Player player, EventType type, double position, long time,
			Object cause) {
		this.player = player;
		this.type = type;
		this.position = position;
		this.time = time;
		this.cause = cause;
	}

	
	public Player getPlayer() {
		return player;
	}
	
	public double getPosition() {
		return position;
	}
	
	public long getTime() {
		return time;
	}
	
	public Object getCause() {
		return cause;
	}
	
	public boolean isError() {
		return cause instanceof Throwable;
	}
	
	public Throwable getError() {
		return (Throwable) cause;
	}
	
	public boolean isEndOfMedia() {
		return END_OF_MEDIA.equals(cause);
	}
	
	public boolean isExternalInterrupt() {
		return EXTERNAL_INTERRUPT.equals(cause);
	}
	
	public boolean isUserCommand() {
		return USER_COMMAND.equals(cause);
	}
	
	public EventType getEventType() {
		return type;
	}
}
