package com.mp3player.mediacommand;

import java.util.EventListener;

public interface MediaCommandListener extends EventListener {

	void commandReceived(MediaCommand command);
	
}
