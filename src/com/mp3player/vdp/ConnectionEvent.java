package com.mp3player.vdp;

public class ConnectionEvent {
	private Peer peer;
	private long time;
	private Cause cause;

	enum Cause
	{
		/**SchmorgUs*/
		USER,
		TIMEOUT
	}

	public ConnectionEvent(Peer peer, long time, Cause cause) {
		this.peer = peer;
		this.time = time;
		this.cause = cause;
	}

	public Peer getPeer() {
		return peer;
	}

	public long getTime() {
		return time;
	}

	public Cause getCause() {
		return cause;
	}




}
