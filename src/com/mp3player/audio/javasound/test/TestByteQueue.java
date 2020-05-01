package com.mp3player.audio.javasound.test;

import java.util.Arrays;

import com.mp3player.audio.javasound.lib.ByteQueue;

public class TestByteQueue {

	public TestByteQueue() {
		// TODO Auto-generated constructor stub
	}

	public static void main(String[] args) {
		ByteQueue queue = new ByteQueue(10);
		
		for(byte i = 0; i < 100; i++) {
			byte[] bytes = new byte[]{ i, i, i };
			queue.putCopy(bytes, 0, bytes.length);
		}
		
		System.out.println("Bytes received: "+queue.getBytesReceived());
		System.out.println("Filled: "+queue.isFilled());
		System.out.println("Min Len: "+queue.getMinLength());
		System.out.println("Current: "+queue.getCurrentLength());
		System.out.println("Queue: "+Arrays.toString(queue.getQueue()));
		System.out.println("All: "+Arrays.toString(queue.getAllBytes()));
		System.out.println("Recent 2: "+Arrays.toString(queue.getMostRecent(2)));
		System.out.println("Queue (multiple of 2): "+Arrays.toString(queue.getQueueMultipleOf(2)));
		System.out.println("Queue (multiple of 3): "+Arrays.toString(queue.getQueueMultipleOf(3)));
	}

}
