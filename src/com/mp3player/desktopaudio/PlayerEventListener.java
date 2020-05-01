package com.mp3player.desktopaudio;

import java.util.EventListener;

public interface PlayerEventListener extends EventListener {

	void onEvent(PlayerEvent e);
}
