package audio;

import java.util.EventListener;

public interface MarkerListener extends EventListener
{

	void markerPassed(MarkerEvent e);
	
}
