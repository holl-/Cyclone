package cloud

import cloud.Peer.Companion.getLocal
import java.io.Serializable

/**
 * All objects that are shared among clients extend `Distributed`.
 *
 *
 * Each distributed object has a unique ID which must be provided in the
 * constructor. If multiple objects with the same ID are detected
 * [.resolveConflict] is called to determine which one will be
 * kept. The other instance is then discarded.
 *
 *
 *
 * When a distributed object is changed locally, it must call
 * [.fireChangedLocally] which triggers the synchronization process.
 * After each change (by the isLocal peer or a connected peer) all
 * `changeListeners` receive a [DataEvent].
 *
 *
 *
 * The synchronization is implemented using serialization to transfer the data
 * and reflection to copy the data to the isLocal object. This procedure ensures
 * that any references to the shared object stay valid after synchronization.
 *
 *
 * @param shared
 * Shared objects are owned by all peers and are kept alive after the creator disconnects.
 * They may also be stored on disk to be loaded on application restart.
 * Only one instance of shared objects can be present at any point in time while unlimitted non-shared objects can be added.
 *
 * @author Philipp Holl
 */
abstract class Data : Serializable
{
    var origin = getLocal()

    /*
     * The cloud through which this data was obtained.
     * This property is set on deserialization.
     */
    @Transient internal var cloud: Cloud? = null


    /**
     * Tests if this data object is still valid.
     * Owner-bound data becomes invalid when the peer that pushed it disconnects from the cloud.
     * [SynchronizedData] instances never become invalid.
     */
    open fun isValid(): Boolean {
        return cloud?.peers?.contains(origin) ?: true
    }

}


/**
 * Only one instance per class implementing SynchronizedData is allowed.
 * Sharing other objects of the same class will replace the old one.
 */
abstract class SynchronizedData : Serializable
{
}
