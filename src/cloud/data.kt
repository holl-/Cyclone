package cloud

import java.io.Serializable

/**
 * [Data] can be uploaded to the cloud.
 * Unlike [SynchronizedData], [Data] instances are owned by the device that uploaded them.
 *
 * The synchronization is implemented using serialization to transfer the data
 * and reflection to copy the data to the isLocal object. This procedure ensures
 * that any references to the shared object stay valid after synchronization.
 */
abstract class Data : Serializable
{
    /**
     * Two data objects are identical if their serialized forms are identical.
     */
    open fun identical(other: Data): Boolean {
        return this === other
    }
}


/**
 * Only one instance per class implementing SynchronizedData is allowed.
 * Sharing other objects of the same class will replace the old one.
 *
 * SynchronizedData must have an empty constructor to create a default object.
 */
abstract class SynchronizedData : Serializable {

    /**
     * This method is called on the object that has existed in the cloud the longest.
     * This method must return equal objects no matter on what peer it is invoked.
     *
     * @param other out-of-sync version by another peer, instance of same class as this
     * @return object to keep, must be instance of same class as this
     */
    open fun resolveConflict(other: SynchronizedData): SynchronizedData {
        return this
    }

    /**
     * This method is called on an object that was read from disc.
     * The return value of this object will be pushed to the cloud.
     * If null, nothing will be pushed to the cloud.
     */
    open fun fromFile(): SynchronizedData? {
        return this
    }
}