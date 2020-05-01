package com.mp3player.audio2.javasound.lib;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import com.mp3player.desktopaudio.AudioBuffer;


/**
 * A buffer that can be filled and read from asynchronously.
 * Internally, the data is split into chunks of byte[].
 * <p>The buffer can be filled using {@link #fill(byte[], int, int)}
 * which writes a piece of data or {@link #fill(AudioInputStream)}
 * or {@link #startFilling(AudioInputStream, Runnable)} which fills the
 * buffer until the input stream is depleted and automatically
 * closes the buffer.
 * When using {@link #fill(byte[], int, int)} make sure to {@link #close()}
 * the buffer. If not closed, reading methods will keep blocking. </p>
 * To read the buffer use {@link #stream()} or {@link #audioStream()}.
 * @author Philipp Holl
 *
 */
public class MemoryAudioBuffer implements AudioBuffer
{
	private long frameLength;
	private AudioFormat format;

	private ArrayList<byte[]> data;
	private int arraySize;

	private int filledBytes;
	private boolean closed;

	private Object ioMonitor = this;


	public MemoryAudioBuffer(AudioFormat f, long frameLength) {
		this(f, frameLength, 64*1024);
	}

	public MemoryAudioBuffer(AudioFormat f, long frameLength, int chunkSize) {
		if(frameLength == 0) throw new IllegalArgumentException("frameLength cannot be 0.");
		format = f;
		this.frameLength = frameLength;
		arraySize = chunkSize;
		int arrayCount = (int) (frameLength * format.getFrameSize() / arraySize);
		data = new ArrayList<byte[]>(arrayCount);
		filledBytes = 0;
		closed = false;
	}


	/**
	 *
	 * @param buffer
	 * @param off
	 * @param len
	 * @return the number of bytes actually written
	 */
	public synchronized int fill(byte[] buffer, int off, int len) throws IllegalStateException
	{
		if(closed) throw new IllegalStateException();

		int written = 0;
		while(written < len) {
			int localFilled = filledBytes % arraySize;
			int currentArray = filledBytes / arraySize;
			if(localFilled > 0) {
				int writing = Math.min(arraySize-localFilled, len-written);
				System.arraycopy(buffer, written+off, data.get(currentArray), localFilled, writing);
				written += writing;
				filledBytes += writing;
			}
			else {
				byte[] localArray;
				// Create array if doesn't exist
				boolean arrayExists = data.size() > currentArray;
				if(arrayExists) {
					localArray = data.get(currentArray);
				}else {
					localArray = new byte[arraySize];
					data.add(localArray);
				}
				int writing = Math.min(arraySize, len-written);
				System.arraycopy(buffer, written+off, localArray, 0, writing);
				written += writing;
				filledBytes += writing;
			}
		}
		notifyAll();
		return written;
	}

	public void startFilling(AudioInputStream in, Runnable onBufferFilled, Runnable onBufferClosed) {
		new Thread(() -> {
			try {
				fill(in);
				if(onBufferFilled != null) onBufferFilled.run();
			}catch(IllegalStateException exc) {
				if(onBufferClosed != null) onBufferClosed.run();
			}
		}, "Fill Audio Buffer").start();
	}

	public void fill(AudioInputStream in) throws IllegalStateException
	{
		int available = 4*1024;
		try {
			int realAvailable = in.available();
			if(realAvailable > 0) available = realAvailable;
		} catch(IOException exc) {
			exc.printStackTrace();
		}
		byte[] buffer = new byte[Math.max(Math.min(8*1024, available), 2*1024)]; // 2KB < available < 8 KB
		int len;
		try {
			while((len = in.read(buffer)) != -1) {
				int written = fill(buffer, 0, len);
				if(written != len) {
					System.err.println("Buffer overflowing, length="+len+", wrote "+written);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		close();
	}

	public synchronized void close() {
		frameLength = filledBytes / format.getFrameSize();
		closed = true;
		notifyAll();
	}

	public boolean isClosed() {
		return closed;
	}

	public AudioFormat getFormat() {
		return format;
	}

	public int getFrameLength() {
		return (int) frameLength;
	}

	public long getFramesFilled() {
		return filledBytes / format.getFrameSize();
	}

	public int getFrame(int positionMillis) {
		return (int) ((positionMillis / 1000.0) * format.getFrameRate());
	}

	public double getDuration() {
		if(frameLength == AudioSystem.NOT_SPECIFIED) return AudioSystem.NOT_SPECIFIED;
		return frameLength / format.getFrameRate();
	}

	public int getDurationMillis() {
		if(frameLength == AudioSystem.NOT_SPECIFIED) return AudioSystem.NOT_SPECIFIED;
		return (int) Math.round(frameLength * 1000.0 / format.getFrameRate());
	}

	public long getDurationMicros() {
		if(frameLength == AudioSystem.NOT_SPECIFIED) return AudioSystem.NOT_SPECIFIED;
		return (int) Math.round(frameLength * 1_000_000.0 / format.getFrameRate());
	}

	public long getAllocatedMemory() {
		if(data == null) return 0;
		return data.size() * (long) arraySize;
	}

	public int getAllocatedMemoryMB() {
		return (int) (getAllocatedMemory() / (1024*1024));
	}

	@Override
	public String toString() {
		double durationSec = Math.round(getDuration()*10.0) / 10.0;
		return "("+durationSec+"sec, "+getFrameLength()+" frames, "+getAllocatedMemoryMB()+"MB)";
	}

	public void dealloc(boolean runGC) {
		if(!closed) close();
		data.clear();
		data = null;
		if(runGC) System.gc();
	}

	public boolean exists() {
		return data != null;
	}

	public int getChunkSize() {
		return arraySize;
	}

	/**
	 * Returns an <code>InputStream</code> that starts from
	 * the beginning of the array.
	 * Methods that access parts of the data that have not been filled,
	 * the stream blocks until they are available.
	 * @return a new <code>InputStream</code> from the start.
	 */
	public ByteStream stream() {
		return new ByteStream();
	}

	public AudioInputStream audioStream() {
		int frameLength = (int) this.frameLength;
		return new AudioInputStream(stream(), format, frameLength);
	}

	public AudioInputStream audioStreamFrom(int posMillis) {
		int startFrame = (int) ((posMillis / 1000.0) * format.getFrameRate());
		return audioStreamFromFrame(startFrame);
	}
	public AudioInputStream audioStreamFromFrame(int startFrame) {
		if(startFrame < 0) throw new IllegalArgumentException("startFrame < 0");
		int frameLength = (int) this.frameLength;
		ByteStream in = stream();
		in.skip(startFrame * format.getFrameSize());
		int restFrames = frameLength == -1 ? -1 : frameLength-startFrame;
		return new AudioInputStream(in, format, restFrames);
	}


	@Override
	public double getStartPosition() {
		return 0;
	}

	@Override
	public double getEndPosition() {
		return getFramesFilled() / format.getFrameRate();
	}




	private class ByteStream extends InputStream
	{
		private int position;
		private int mark = 0;

		@Override
		public synchronized int available() {
			return filledBytes - position;
		}

		@Override
		public void close() {}

		@Override
		public synchronized void mark(int readLimit) {
			mark = position;
		}

		@Override
		public boolean markSupported() {
			return true;
		}

		public synchronized boolean endReached() {
			if(closed) return position >= filledBytes;
			else return false;
		}

		@Override
		public synchronized int read() throws IOException {
			if(endReached()) return -1;
			block();
			if(endReached()) return -1;
			int arrayIndex = position / arraySize;
			int localPosition = position % arraySize;
			int b = data.get(arrayIndex)[localPosition];
			position ++;
			return b;
		}

		@Override
		public synchronized int read(byte[] b, int off, int len) {
			if(endReached()) return -1;
			block();
			if(endReached()) return -1;
			len = Math.min(len, available());

			int written = 0;
			while(written < len) {
				int arrayIndex = position / arraySize;
				int localPosition = position % arraySize;
				int writing = Math.min(len-written, arraySize-localPosition);
				System.arraycopy(data.get(arrayIndex), localPosition, b, written+off, writing);
				written += writing;
				position += writing;
			}
			return len;
		}

		@Override
		public synchronized void reset() {
			position = mark;
		}

		@Override
		public synchronized long skip(long n) {
			if(n < 0) throw new IllegalArgumentException();
			if(endReached()) return -1;

			while(available() < n) {
				block();
				if(endReached()) return -1;
				if(closed) break;
			}

			if(closed) {
				n = Math.min(n, available());
			}

			position += n;
			return n;
		}

		/**
		 *
		 * @return false if interrupted
		 */
		private synchronized boolean block() {
			if(filledBytes > position) return true;
			synchronized (ioMonitor) {
				try {
					ioMonitor.wait();
					return true;
				} catch (InterruptedException e) {
					return false;
				}
			}
		}

	}





}
