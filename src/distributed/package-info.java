/**
 * A virtual distributed platform (DistributedPlatform) allows sharing of data and files between
 * multiple machines.
 *
 * <p>
 * Each participating systemcontrol uses an instance of {@link distributed.DistributedPlatform}
 * to share data or allow file sharing. Connected machines are represented as
 * {@link distributed.Peer}s and can be obtained through the
 * {@link distributed.DistributedPlatform} class.
 * </p>
 * <p>
 * <b>File sharing</b> is enabled by peers making isLocal files available to the
 * network. This process is called <i>mounting</i>. All files mounted by one
 * peer through one of {@link distributed.DistributedPlatform}'s mount methods are seen as
 * root files. Directories implicitly mount all contained files and folders.
 * Other peers can then access these files as
 * {@link distributed.DFile}s through the corresponding instance of
 * {@link distributed.Peer}.
 * </p>
 * <p>
 * <b>Data sharing</b> allows synchronization of java objects across all peers.
 * Each shared object must extend {@link distributed.Distributed} and has
 * a unique ID. All shared data is available through the
 * {@link distributed.DistributedPlatform}. Changes made to one peer's instance are
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
package distributed;