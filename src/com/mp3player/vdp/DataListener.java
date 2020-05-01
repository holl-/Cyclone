package com.mp3player.vdp;

public interface DataListener {
	void onDataAdded(DataEvent e);
	void onDataRemoved(DataEvent e);
	void onDataChanged(DataEvent e);
}
