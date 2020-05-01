package vdp;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

import vdp.internal.LocalPeer;

/**
 * Main class for setting up a virtual distributed platform.
 *
 */
public class VDP {
	private LocalPeer localPeer;

	private List<ConnectionListener> connectionListeners = new CopyOnWriteArrayList<>();
	private List<DataListener> dataListeners = new CopyOnWriteArrayList<>();
	private Consumer<Serializable> onMessageReceived;

	private Map<String, Distributed> localData = new HashMap<>();

	private ExecutorService eventHandler;

	/**
	 * Creates a new virtual distributed platform. The created platform will
	 * only know the local peer and does not actively connect to other peers or
	 * receive connections from such until a <code>connect</code> method is
	 * called.
	 */
	public VDP() {
		localPeer = new LocalPeer();

		eventHandler = Executors.newSingleThreadExecutor();
	}

	/**
	 * Connects to a virtual IP address and returns immediately. If other peers
	 * are found at that address or later connect to it, they are added to the
	 * lists of peers, returned by {@link #getAllPeers()},
	 * {@link #getRemotePeers()} and the <code>onPeerConnected</code>-listener
	 * is informed.
	 *
	 * @param multicastAddress
	 *            a multicast address (in the range 224.0.0.0 to 239.255.255.255
	 *            for IPv4)
	 * @throws IOException
	 *             if the address cannot be connected to
	 * @see #disconnectFromMulticastAddress(String)
	 * @see #addConnectionListener(ConnectionListener)
	 */
	public void connectToMulticastAddress(String multicastAddress) throws IOException {

	}

	public void connectToExternal(String address) throws IOException {

	}

	public void disconnectFromMulticastAddress(String multicastAddress) throws IOException {

	}

	public void disconnectFromExternal(String address) throws IOException {

	}

	public void disconnectAll(BiConsumer<String, IOException> errorHandler) {

	}

	/**
	 * Allows other peers to read the given file. The file will be available
	 * using {@link Peer#getRootFiles()} or {@link Peer#getFile(String)} where
	 * the path equals the returned filename. If the file is a directory, all
	 * contained files and folders are also shared.
	 * <p>
	 * <i>Warning:</i> While mounted, files should not be modified.
	 * </p>
	 *
	 * @return the mounted filename with which other peers can access the file.
	 *         This may be different from the real file name if a file with that
	 *         name has already been mounted before.
	 * @param file
	 *            file to share
	 * @see #mountFile(String, File)
	 * @see #unmountFile(String)
	 */
	public RemoteFile mountFile(File file) {
		return localPeer.mount(file);
	}

	/**
	 * Mounts a file with a given name. The same file can be mounted with
	 * different names. See {@link #mountFile(File)} for further details.
	 *
	 * @param name
	 *            the name by which the file can be found
	 * @param file
	 *            the file to mount
	 * @return the mounted file
	 * @throws IllegalArgumentException
	 *             if the given name is already in use
	 * @see #mountFile(File)
	 * @see #unmountFile(String)
	 */
	public RemoteFile mountFile(String name, File file) throws IllegalArgumentException {
		return localPeer.mount(name, file);
	}

	public RemoteFile mountVirtual(String name, long length, long lastModified, Supplier<InputStream> streamSupplier)
			throws IllegalArgumentException {
		return null;
	}

	public void unmountFile(String name) {

	}

	public void putData(Distributed data) {
		if (data.vdp != null)
			throw new IllegalArgumentException("data is already bound");
		data.vdp = this;

		localData.put(data.getID(), data);

		long time = System.currentTimeMillis();
		DataEvent e = new DataEvent(data, localPeer, localPeer, time, time);
		dataListeners.forEach(l -> l.onDataAdded(e));
	}

	public void removeData(Distributed data) {
		if (data.vdp != this)
			throw new IllegalArgumentException();
		data.vdp = null;

		long time = System.currentTimeMillis();
		DataEvent e = new DataEvent(data, localPeer, localPeer, time, time);
		dataListeners.forEach(l -> l.onDataRemoved(e));
	}

	void changed(Distributed data) {
		eventHandler.execute(() -> {
			long time = System.currentTimeMillis();
			DataEvent e = new DataEvent(data, localPeer, localPeer, time, time);
			dataListeners.forEach(l -> l.onDataAdded(e));
			data._fireChanged(e);
		});
	}

	public Optional<Distributed> getData(String id) {
		// TODO stub implementation
		return Optional.ofNullable(localData.get(id));
	}

	@SuppressWarnings("unchecked")
	public <T extends Distributed> T getOrAddData(T addIfNotPresent) {
		Optional<Distributed> p = getData(addIfNotPresent.getID());
		if (p.isPresent()) {
			return (T) p.get();
		} else {
			putData(addIfNotPresent);
			return addIfNotPresent;
		}
	}

	public Collection<Distributed> getAllData() {
		// TODO stub implementation
		return localData.values();
	}

	public void saveAllData(File saveFile) {

	}

	public void loadAllData(File saveFile) {

	}

	public Peer getLocalPeer() {
		return localPeer;
	}

	public List<Peer> getRemotePeers() {
		return null;
	}

	public List<Peer> getAllPeers() {
		return null;
	}

	public Peer getPeer(String id) {
		if(localPeer.getID().equals(id)) return localPeer;

		return null;
	}

	public void addConnectionListener(ConnectionListener l) {
		connectionListeners.add(l);
	}

	public void removeConnectionListener(ConnectionListener l) {
		connectionListeners.remove(l);
	}

	public void addDataListener(DataListener l) {
		dataListeners.add(l);
	}

	public void removeDataListener(DataListener l) {
		dataListeners.remove(l);
	}

	public Consumer<Serializable> getOnMessageReceived() {
		return onMessageReceived;
	}

	public void setOnMessageReceived(Consumer<Serializable> onMessageReceived) {
		this.onMessageReceived = onMessageReceived;
	}

}
