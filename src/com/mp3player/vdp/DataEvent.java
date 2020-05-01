package com.mp3player.vdp;

public class DataEvent {
	private Distributed data;
	private Peer editingClient;
	private Peer localClient;
	private long sourceTime;
	private long timeReceived;


	public DataEvent(Distributed data, Peer editingClient, Peer localClient, long sourceTime, long timeReceived) {
		this.data = data;
		this.editingClient = editingClient;
		this.localClient = localClient;
		this.sourceTime = sourceTime;
		this.timeReceived = timeReceived;
	}


	public Distributed getData() {
		return data;
	}


	public boolean wasChangedLocally() {
		return editingClient.equals(localClient);
	}


	public Peer getEditingClient() {
		return editingClient;
	}


	public Peer getLocalClient() {
		return localClient;
	}


	public long getSourceTime() {
		return sourceTime;
	}


	public long getTimeReceived() {
		return timeReceived;
	}


}
