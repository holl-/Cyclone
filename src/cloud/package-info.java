/**
 * A virtual distributed platform (DistributedPlatform) allows sharing of data and files between
 * multiple machines.
 *
 * <p>
 * Each participating systemcontrol uses an instance of {@link cloud.Cloud}
 * to share data or allow file sharing. Connected machines are represented as
 * {@link cloud.Peer}s and can be obtained through the
 * {@link cloud.Cloud} class.
 * </p>
 * <p>
 * <b>File sharing</b> is enabled by peers making isLocal files available to the
 * network. This process is called <i>mounting</i>. All files mounted by one
 * peer through one of {@link cloud.Cloud}'s mount methods are seen as
 * root files. Directories implicitly mount all contained files and folders.
 * Other peers can then access these files as
 * {@link cloud.CloudFile}s through the corresponding instance of
 * {@link cloud.Peer}.
 * </p>
 * <p>
 * <b>Data sharing</b> allows synchronization of java objects across all peers.
 * Each shared object must extend {@link cloud.Distributed} and has
 * a unique ID. All shared data is available through the
 * {@link cloud.Cloud}. Changes made to one peer's instance are
 * synchronized to all other peers by the library.
 * </p>
 * <p>
 * <b>Messages</b> can also be sent to a specific peer using
 * <code>Peer.send()</code> and received using
 * <code>DistributedPlatform.setOnMessageReceived()</code>.
 * </p>
 *
 * @author API design: Philipp Holl
 */
package cloud;