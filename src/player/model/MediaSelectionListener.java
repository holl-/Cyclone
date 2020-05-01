package player.model;

import java.util.EventListener;

public interface MediaSelectionListener extends EventListener {
	void onAdded(MediaSetEvent e);
	void onRemoved(MediaSetEvent e);

	void onWorkingChanged(MediaSetEvent e);
}
