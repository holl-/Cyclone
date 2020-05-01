package com.mp3player.appinstance;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.BindException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Optional;
import java.util.function.Consumer;

public class InstanceManager {
	public static final int PORT = 44362;

	private ApplicationParameters parameters;

	private ServerSocket serverSocket;
	private Consumer<ApplicationParameters> onNewInstance;

	public InstanceManager(ApplicationParameters parameters, Consumer<ApplicationParameters> onNewInstance) {
		this.parameters = parameters;
		this.onNewInstance = onNewInstance;
	}

	/**
	 * If no other instance of this application exists, opens a port to listen
	 * for other applications and returns {@link Optional#empty()}.
	 *
	 * Else, sends the {@link ApplicationParameters} of this application to the
	 * main instance and returns the existing application's parameters.
	 *
	 * @return the application parameters of the existing application if one
	 *         exists or empty if this application was registered.
	 */
	public Optional<ApplicationParameters> registerIfFirst() throws IOException {
		try {
			serverSocket = new ServerSocket(PORT, 10, InetAddress.getLocalHost());
			handleClients();
			return Optional.empty();
		} catch (BindException e) {
			// there is already someone there
			ApplicationParameters remoteParams = exchangeWithMainInstance();
			if (!remoteParams.getApplicationID().equals(parameters.getApplicationID())) {
				throw new BindException("Port bound to application with ID " + remoteParams.getApplicationID());
			}
			return Optional.of(remoteParams);
		}
	}

	public void shutdown() {
		if (serverSocket == null)
			return;
		try {
			serverSocket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private ApplicationParameters exchangeWithMainInstance() throws IOException {
		if (serverSocket != null)
			throw new IllegalStateException("This application is the main instance");

		try (Socket clientSocket = new Socket(InetAddress.getLocalHost(), PORT)) {
			return exchangeWith(clientSocket);
		}
	}

	private ApplicationParameters exchangeWith(Socket socket) throws IOException {
		ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
		out.writeObject(parameters);
		out.flush();
		ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
		ApplicationParameters remoteParameters;
		try {
			remoteParameters = (ApplicationParameters) in.readObject();
		} catch (ClassNotFoundException e) {
			throw new IOException(e);
		}
		return remoteParameters;
	}

	private void handleClients() {
		Thread t = new Thread(() -> {
			while (!serverSocket.isClosed()) {
				try (Socket client = serverSocket.accept()) {
					ApplicationParameters params = exchangeWith(client);
					if (params.getApplicationID().equals(parameters.getApplicationID())) {
						onNewInstance.accept(params);
					}
				} catch (IOException exc) {
					if (!serverSocket.isClosed())
						exc.printStackTrace();
				}

			}
		});
		t.setDaemon(true);
		t.start();
	}
}
