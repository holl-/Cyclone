package distributed;

import java.util.EventListener;

public interface ConnectionListener extends EventListener {
	void onConnected(ConnectionEvent e);
	void onDisconnected(ConnectionEvent e);
}