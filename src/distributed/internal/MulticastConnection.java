package distributed.internal;

import java.io.IOException;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;

public class MulticastConnection {
	private MulticastSocket socket;


	public MulticastConnection(String ipAddress) throws UnknownHostException, IOException {
		socket = new MulticastSocket();
		socket.joinGroup(InetAddress.getByName(ipAddress));

	}
}
