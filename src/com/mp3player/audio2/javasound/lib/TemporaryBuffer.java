package com.mp3player.audio2.javasound.lib;

public class TemporaryBuffer {
	private byte[] data;
	private int len; // current number of valid bytes
	
	
	public TemporaryBuffer(int length) {
		data = new byte[length];
		len = 0;
	}
	

	public int writeToOffset() {
		return len;
	}

	public int writeToLength() {
		return data.length - len;
	}
	
	public int readLength() {
		return len;
	}

	public void bytesWritten(int byteCount) {
		len += byteCount;
		if(len > data.length) throw new IllegalStateException();
	}

	public void bytesRead(int byteCount) {
		if(byteCount > len)
			throw new IllegalStateException("more bytes read than available");
		
		if(byteCount == len) {
			// All bytes were written
			len = 0;
		}
		else {
			// Only the beginning x bytes were written
			for(int i = 0; i < (len-byteCount); i++) {
				data[i] = data[i+byteCount];
			}
			len = len - byteCount;
		}
	}


	public byte[] data() {
		return data;
	}


	public void clear() {
		len = 0;
	}


	public boolean bytesAvailable() {
		return len > 0;
	}
	
	
	
}
