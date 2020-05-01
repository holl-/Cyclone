/**
 * A virtual distributed platform (VDP) allows sharing of data and files between
 * multiple machines.
 *
 * <p>
 * Each participating systemcontrol uses an instance of {@link vdp.VDP}
 * to share data or allow file sharing. Connected machines are represented as
 * {@link vdp.Peer}s and can be obtained through the
 * {@link vdp.VDP} class.
 * </p>
 * <p>
 * <b>File sharing</b> is enabled by peers making local files available to the
 * network. This process is called <i>mounting</i>. All files mounted by one
 * peer through one of {@link vdp.VDP}'s mount methods are seen as
 * root files. Directories implicitly mount all contained files and folders.
 * Other peers can then access these files as
 * {@link vdp.RemoteFile}s through the corresponding instance of
 * {@link vdp.Peer}.
 * </p>
 * <p>
 * <b>Data sharing</b> allows synchronization of java objects across all peers.
 * Each shared object must extend {@link vdp.Distributed} and has
 * a unique ID. All shared data is available through the
 * {@link vdp.VDP}. Changes made to one peer's instance are
 * synchronized to all other peers by the library.
 * </p>
 * <p>
 * <b>Messages</b> can also be sent to a specific peer using
 * <code>Peer.send()</code> and received using
 * <code>VDP.setOnMessageReceived()</code>.
 * </p>
 *
 * @author API design: Philipp Holl
 */
package vdp;