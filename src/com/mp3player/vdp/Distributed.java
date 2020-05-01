package com.mp3player.vdp;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * All objects that are shared among clients extend <code>Distributed</code>.
 * <p>
 * Each distributed object has a unique ID which must be provided in the
 * constructor. If multiple objects with the same ID are detected
 * {@link #resolveConflict(Conflict)} is called to determine which one will be
 * kept. The other instance is then discarded.
 * </p>
 * <p>
 * When a distributed object is changed locally, it must call
 * {@link #fireChangedLocally()} which triggers the synchronization process.
 * After each change (by the local peer or a connected peer) all
 * <code>changeListeners</code> receive a {@link DataEvent}.
 * </p>
 * <p>
 * The synchronization is implemented using serialization to transfer the data
 * and reflection to copy the data to the local object. This procedure ensures
 * that any references to the shared object stay valid after synchronization.
 * </p>
 *
 * @author Philipp Holl
 *
 */
public abstract class Distributed implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * Unique data ID
	 */
	private String id;
	/**
	 * This object will only be serialized and written to file if this flag is
	 * set to true.
	 */
	private boolean permanent;
	/**
	 * Specifies whether the data is valid only as long as it's owner is
	 * connected to the network. Also, if true, the data will only be written to
	 * file on the owner's machine.
	 */
	private boolean ownerBound;

	/**
	 * Listeners are not sent through the network and will not be present at
	 * deserialization. Therefore
	 * {@link #copyNonTransientFieldsFrom(Distributed)} is called after
	 * deserialization before listeners are informed.
	 */
	private transient List<Consumer<DataEvent>> changeListeners = new CopyOnWriteArrayList<>();
	/**
	 * Associated VDP, set when {@link VDP#putData(Distributed)} is invoked.
	 */
	VDP vdp;

	/**
	 * Creates a new {@link Distributed} object which is not yet bound to any
	 * network. Binding to a network happens automatically when
	 * {@link VDP#putData(Distributed)} is called.
	 *
	 * @param id
	 *            unique data ID with which the object can be found
	 * @param permanent
	 *            whether the data should be saved to file
	 * @param ownerBound
	 *            whether the data is only valid as long as it's owner is
	 *            available
	 */
	public Distributed(String id, boolean permanent, boolean ownerBound) {
		this.id = id;
		this.permanent = permanent;
		this.ownerBound = ownerBound;
	}

	/**
	 * Called on the local copy when two objects with the same ID are detected.
	 * A conflict can arise when two peers connect which both have a shared
	 * object with the same ID. This method then resolves the conflict by
	 * providing the object to keep.
	 *
	 * @param conflict
	 *            detailed information about the conflict
	 * @return the object to keep with this ID. This can be <code>this</code>,
	 *         <code>other</code> or a newly created object. All other objects
	 *         are discarded.
	 */
	public abstract Distributed resolveConflict(Conflict conflict);

	void _fireChanged(DataEvent e) {
		for (Consumer<DataEvent> l : changeListeners)
			l.accept(e);
	}

	/**
	 * This method must be called by inheriting classes after a modification to
	 * the data has been performed. If this object is bound to a {@link VDP}, it
	 * triggers the synchronization process and fires {@link DataEvent}s to all
	 * <code>changeListeners</code> at all peers.
	 *
	 * @see #addDataChangeListener(Consumer)
	 */
	protected void fireChangedLocally() {
		if (vdp != null)
			vdp.changed(this);
		else {
			DataEvent e = new DataEvent(this, null, null, System.currentTimeMillis(), System.currentTimeMillis());
			for (Consumer<DataEvent> l : changeListeners)
				l.accept(e);
		}
	}

	/**
	 * Adds a listener that will be informed whenever part of this object's data
	 * is modified. The modification could have happened locally or by a
	 * different peer.
	 *
	 * @param l
	 *            listener
	 */
	public void addDataChangeListener(Consumer<DataEvent> l) {
		changeListeners.add(l);
	}

	/**
	 * Removes a change listener.
	 *
	 * @param l
	 *            listener to remove
	 */
	public void removeDataChangeListener(Consumer<DataEvent> l) {
		changeListeners.remove(l);
	}

	/**
	 * Returns the unique ID of this distributed object.
	 *
	 * @return the unique ID
	 */
	public final String getID() {
		return id;
	}

	/**
	 * This object will only be serialized and written to file if the permanent
	 * flag is set to true.
	 *
	 * @return whether the permanent flag is set
	 */
	public boolean isPermanent() {
		return permanent;
	}

	/**
	 * Specifies whether the data is valid only as long as it's owner is
	 * connected to the network. Also, if true, the data will only be written to
	 * file on the owner's machine.
	 *
	 * @return whether the owner-bound flag is set
	 */
	public boolean isOwnerBound() {
		return ownerBound;
	}

	void copyListenersFrom(Distributed other) {
		changeListeners = new CopyOnWriteArrayList<>(other.changeListeners);
	}

	void copyNonTransientFieldsFrom(Distributed other) throws IllegalArgumentException, IllegalAccessException {
		for (Field field : getClass().getFields()) {
			if (!field.isAccessible())
				field.setAccessible(true);
			if (!Modifier.isTransient(field.getModifiers())) {
				Object value = field.get(other);
				field.set(this, value);
			}
		}
	}
}
