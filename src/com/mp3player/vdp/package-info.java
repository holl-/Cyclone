/**
 * A virtual distributed platform (VDP) allows sharing of data and files between
 * multiple machines.
 *
 * <p>
 * Each participating machine uses an instance of {@link com.mp3player.vdp.VDP}
 * to share data or allow file sharing. Connected machines are represented as
 * {@link com.mp3player.vdp.Peer}s and can be obtained through the
 * {@link com.mp3player.vdp.VDP} class.
 * </p>
 * <p>
 * <b>File sharing</b> is enabled by peers making local files available to the
 * network. This process is called <i>mounting</i>. All files mounted by one
 * peer through one of {@link com.mp3player.vdp.VDP}'s mount methods are seen as
 * root files. Directories implicitly mount all contained files and folders.
 * Other peers can then access these files as
 * {@link com.mp3player.vdp.RemoteFile}s through the corresponding instance of
 * {@link com.mp3player.vdp.Peer}.
 * </p>
 * <p>
 * <b>Data sharing</b> allows synchronization of java objects across all peers.
 * Each shared object must extend {@link com.mp3player.vdp.Distributed} and has
 * a unique ID. All shared data is available through the
 * {@link com.mp3player.vdp.VDP}. Changes made to one peer's instance are
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
package com.mp3player.vdp;