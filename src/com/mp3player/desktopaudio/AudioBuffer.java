package com.mp3player.desktopaudio;


public interface AudioBuffer {
	
	
	/**
	 * Returns the memory allocated by this buffer.
	 * If the exact size is not known this method returns
	 * either an estimate or <code>-1</code>.
	 * @return the allocated memory in bytes
	 */
	long getAllocatedMemory();
	
	
	/**
	 * Returns the start position of the buffer in seconds.
	 * @return the start position of the buffer in seconds
	 */
	double getStartPosition();
	/**
	 * Returns the end position of the buffer in seconds.
	 * @return the end position of the buffer in seconds
	 */
	double getEndPosition();
}
