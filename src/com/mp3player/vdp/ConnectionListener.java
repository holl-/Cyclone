package com.mp3player.vdp;

import java.util.EventListener;

public interface ConnectionListener extends EventListener {
	void onConnected(ConnectionEvent e);
	void onDisconnected(ConnectionEvent e);
}
