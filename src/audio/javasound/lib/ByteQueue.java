package audio.javasound.lib;

import java.util.LinkedList;

/**
 * Synchronized method for storing a queue of bytes.
 * @author Philipp Holl
 *
 */
public class ByteQueue {
	private int minLength; // Minimum length in bytes
	private LinkedList<byte[]> queue; // most recent last
	private int bytesReceived;
	
	private byte[] removedArray;
	
	
	public ByteQueue() {
		this(0);
	}
	
	public ByteQueue(int length) {
		this.minLength = length;
		queue = new LinkedList<byte[]>();
		bytesReceived = 0;
	}

	
	
	public synchronized void putOriginal(byte[] bytes) {
		queue.add(bytes);
		removeOld();
		bytesReceived += bytes.length;
	}
	
	/**
	 * This method performs best if the arrays all have the same length.
	 * @param bytes
	 * @param offset
	 * @param length
	 */
	public synchronized void putCopy(byte[] bytes, int offset, int length) {
		if(offset != 0 || length != bytes.length) {
			byte[] chunk = new byte[length];
			System.arraycopy(bytes, offset, chunk, 0, length);
			putOriginal(chunk);
		}
		else {
			if(removedArray != null && removedArray.length == length) {
				System.arraycopy(bytes, offset, removedArray, 0, length);
				putOriginal(removedArray);
			}
			else {
				putOriginal(bytes.clone());
			}
		}
	}
	
	public synchronized void changeMinLength(int length) {
		minLength = length;
		removeOld();
	}
	
	private synchronized void removeOld() {
		int len = 0;
		for(int i = queue.size()-1; i >= 0; i--) {
			len += queue.get(i).length;
			if(len >= minLength) {
				// Remove all with index < i
				removeOld(i);
				break;
			}
		}
	}
	
	private synchronized void removeOld(int count) {
		for(int i = 0; i < count; i++) {
			removedArray = queue.removeFirst();
		}
	}
	
	
	/**
	 * Returns an array of the minimal length or less if not filled.
	 * @return
	 */
	public synchronized byte[] getQueue() {
		return getMostRecent(minLength);
	}
	
	/**
	 * Returns an array of all buffered bytes.
	 * The length of the array can be more or less than the minimal
	 * length.
	 * @return
	 */
	public synchronized byte[] getAllBytes() {
		return getMostRecent(Integer.MAX_VALUE);
	}

	public synchronized byte[] getQueueMultipleOf(int common) {
		int moduloEnd = bytesReceived % common;
		int cLength = getCurrentLength();
		int end = cLength - moduloEnd;
		// TODO
//		int prefLength = Math.min(minLength, getCurrentLength());
//		int modulo = prefLength % common;
//		int throwAwayBytes = modulo == 0 ? 0 : common - modulo;
//		return getMostRecent(prefLength - throwAwayBytes);
		return getQueue();
	}
	
	public synchronized byte[] getMostRecent(int byteCount) {
		int currentLength = getCurrentLength();
		byte[] bytes = new byte[Math.min(byteCount, currentLength)];
		
		int pos = bytes.length;
		for(int i = queue.size()-1; i >= 0; i--) {
			byte[] chunk = queue.get(i);
			pos -= chunk.length;
			int overflow = 0;
			if(pos < 0) {
				overflow = -pos;
				pos = 0;
			}
			System.arraycopy(chunk, overflow, bytes, pos, chunk.length-overflow);
			if(pos == 0) break;
		}
		
		return bytes;
	}
	

	public synchronized void reset() {
		queue.clear();
		bytesReceived = 0;
	}
	
	
	public boolean isFilled() {
		return getCurrentLength() >= getMinLength();
	}
	
	public synchronized int getCurrentLength() {
		int len = 0;
		for(byte[] chunk : queue) {
			len += chunk.length;
		}
		return len;
	}
	
	public int getMinLength() {
		return minLength;
	}
	
	public int getBytesReceived() {
		return bytesReceived;
	}
}
