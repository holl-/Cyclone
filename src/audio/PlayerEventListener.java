package audio;

import java.util.EventListener;

public interface PlayerEventListener extends EventListener {

	void onEvent(PlayerEvent e);
}
