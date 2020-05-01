package com.mp3player.desktopaudio;

import java.util.EventListener;

public interface MarkerListener extends EventListener
{

	void markerPassed(MarkerEvent e);
	
}
