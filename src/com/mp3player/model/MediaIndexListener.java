package com.mp3player.model;

import java.util.EventListener;

public interface MediaIndexListener extends EventListener {

	void onAdded(MediaIndexEvent e);
	void onRemoved(MediaIndexEvent e);
}
