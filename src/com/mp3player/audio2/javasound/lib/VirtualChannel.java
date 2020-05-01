package com.mp3player.audio2.javasound.lib;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicLong;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.BooleanControl;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;

public class VirtualChannel
{
	private InputStream audioInputStream;
	private SourceDataLine line;
	private AudioFormat format;

	// Status
	private volatile boolean running;
	private volatile boolean alive;

	/**
	 * Frame position in the current input stream.
	 * @see #getFrameLag()
	 */
	private volatile AtomicLong bytesRead;

	private ByteQueue recentlyWritten; // TODO only use if input stream doesn't support mark

	private WritingThread thread;
	private FloatControl masterGain;
	private BooleanControl muteControl;
	private FloatControl balanceControl;

	private volatile Runnable onInputStreamEnded, onPlaybackEnded;


	public VirtualChannel(AudioFormat format) {
		this.format = format;

		running = false;
		alive = true;
		bytesRead = new AtomicLong(0);

		recentlyWritten = new ByteQueue();

		thread = new WritingThread();
		thread.start();
	}



	public synchronized void start() {
		if(line == null) throw new IllegalStateException("no output line specified");
		if(audioInputStream == null) throw new IllegalStateException("no input specified");
		if(running) return;

		line.start();
		running = true;

		thread.continueSynchronized();
	}


	public synchronized void stop() {
		if(!running) return;

		line.stop();
		running = false;

		// WritingThread automatically pauses
	}


	public void setOnInputStreamEnded(Runnable r) {
		onInputStreamEnded = r;
	}

	public void setOnPlaybackEnded(Runnable r) {
		onPlaybackEnded = r;
	}


	public AudioFormat getFormat() {
		return format;
	}


	public boolean isRunning() {
		return running;
	}


	public long getReadBytes() {
		return bytesRead.get();
	}

	/**
	 * Returns the number of frames read from the input stream.
	 * @return
	 * @see #getFrameLag()
	 */
	public long getReadFrames() {
		return bytesRead.get() / format.getFrameSize();
	}

	public int getFrameLag() {
		if(line == null) return -1;
		return line.getBufferSize() / format.getFrameSize();
	}


	/**
	 *
	 * The InputStream is expected to begin at the start of one frame.
	 * @param newInput
	 * @param closeOldStream
	 * @param flush
	 * @throws IOException
	 */
	public synchronized void setInputStream(InputStream newInput, boolean closeOldStream, boolean flush) {
		InputStream oldStream = audioInputStream;
		boolean wasRunning = running;

		running = false;
		if(flush && line != null) {
			line.flush();
		}
		thread.flush();

		audioInputStream = newInput;
		recentlyWritten.reset();
		bytesRead.set(0);

		running = wasRunning;
		thread.continueSynchronized();

		if(closeOldStream && oldStream != null) {
			try {
				oldStream.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public synchronized void setLine(Mixer newMixer, double gain, boolean mute, boolean closeOldLine) throws LineUnavailableException {
		SourceDataLine newLine = AudioSystem.getSourceDataLine(format, newMixer.getMixerInfo());
		setLine(newLine, gain, mute, closeOldLine);
	}

	public synchronized void setLine(SourceDataLine newLine, double gain, boolean mute, boolean closeOldLine) throws LineUnavailableException {
		SourceDataLine oldLine = line;
		boolean wasRunning = running;

		if(!newLine.isOpen()) {
			newLine.open(format);
		} else {
			if(!format.matches(newLine.getFormat())) throw new IllegalArgumentException("line is opened with the wrong format");
		}


		// Stop the old line
		running = false;
		if(oldLine != null) oldLine.stop(); // This will cause the thread pause
		thread.synchronizeBuffer();

		line = newLine; // should not overwrite this variable while WritingThread active
		masterGain = (FloatControl) line.getControl(FloatControl.Type.MASTER_GAIN);
		if(line.isControlSupported(BooleanControl.Type.MUTE)) {
			muteControl = (BooleanControl) line.getControl(BooleanControl.Type.MUTE);
		} else muteControl = null;
		if(line.isControlSupported(FloatControl.Type.BALANCE)) {
			balanceControl = (FloatControl) line.getControl(FloatControl.Type.BALANCE);
		} else balanceControl = null;
		masterGain.setValue((float) gain);
		muteControl.setValue(mute);


		// Replay bytes written to buffer of SourceDataLine
		byte[] repeat = recentlyWritten.getQueueMultipleOf(format.getFrameSize()); // Avoid starting in the middle of a frame

		int bufferSize = newLine.getBufferSize();
		recentlyWritten.changeMinLength(bufferSize);


		// Start the new line
		if(wasRunning && !newLine.isRunning()) {
			newLine.start();
			running = wasRunning;
		}
		newLine.write(repeat, 0, repeat.length); // TODO repeat bytes should be written from WritingThread

		// Continue WritingThread
		thread.initLine();
		thread.continueSynchronized();

		// Cleanup
		if(closeOldLine && oldLine != null && oldLine.isOpen()) {
			oldLine.close();
		}
	}



	public boolean isAlive() {
		return alive;
	}


	public synchronized void dispose(boolean closeInput, boolean closeOutput) {
		alive = false;
		thread.exit();

		if(line != null) {
			line.close();
		}
		if(audioInputStream != null) {
			try {
				audioInputStream.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}


	/**
	 * This thread is active while the line is running.
	 * Else, it is in pause mode and can be suspended using {@link #continueSynchronized()}.
	 * @author Philipp Holl
	 *
	 */
	protected class WritingThread extends Thread
	{
		private volatile boolean exit;
		private volatile TemporaryBuffer tmpBuffer;
		private CyclicBarrier barrier;


		public WritingThread() {
			super("VirtualChannel.WritingThread");
			exit = false;
			barrier = new CyclicBarrier(2, () -> {});
		}

		@Override
		public void run() {
			try {
				write();
			} catch (IOException e) {
				if(exit) return;
				else e.printStackTrace(); // TODO
			}
		}

		public void exit() {
			exit = true;
		}

		public void continueSynchronized() {
			try {
				barrier.await();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (BrokenBarrierException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		public void initLine() {
			if(line != null) {
				tmpBuffer = new TemporaryBuffer(line.getBufferSize() / 4);
			} else {
				tmpBuffer = null;
			}
		}

		private void pauseWriting() {
			try {
				barrier.await();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (BrokenBarrierException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		/**
		 * Blocks until the buffer is not in use.
		 */
		public void flush() { // TODO cannot clear buffer while in use
			if(tmpBuffer != null) {
				synchronized (tmpBuffer) {
					tmpBuffer.clear();
				}
			}
		}

		public void synchronizeBuffer() {
			if(tmpBuffer == null) return;
			synchronized (tmpBuffer) {

			}
		}


		/**
		 * Keeps writing bytes to the output until the stream ends.
		 * @throws IOException
		 */
		protected void write() throws IOException {

			while(true) {
				if(exit) return;

				while(!running) {
					pauseWriting();
					continue;
				}

				int readBytes;
				int writtenBytes = 0;

				synchronized (tmpBuffer) {

					// Read some input data
					readBytes = audioInputStream.read(tmpBuffer.data(),
							tmpBuffer.writeToOffset(), tmpBuffer.writeToLength());

					if(readBytes > 0) {
						recentlyWritten.putCopy(tmpBuffer.data(), tmpBuffer.writeToOffset(), readBytes);
						tmpBuffer.bytesWritten(readBytes);
						bytesRead.set(bytesRead.get()+readBytes);

						// Write to line
						if(tmpBuffer.bytesAvailable()) {
							writtenBytes = line.write(tmpBuffer.data(), 0, tmpBuffer.readLength());
							tmpBuffer.bytesRead(writtenBytes);
						}
					}
				}


				// Handle exceptions
				if(readBytes < 0) {
					if(onInputStreamEnded != null) onInputStreamEnded.run();
					line.drain();
					playbackEnded();
				}
				else if(writtenBytes < readBytes) {
					// line was closed, stopped, flushed
					// or input stream ended
					pauseWriting();
				}
			}
		}
	}


	public void setGain(double gain) {
		masterGain.setValue((float) gain);
	}



	public void playbackEnded() {
		line.stop();
		running = false;
		if(onPlaybackEnded != null) onPlaybackEnded.run();
	}



	public void setMute(boolean mute) {
		muteControl.setValue(mute);
	}



	public double getBalance() {
		return balanceControl.getValue();
	}

	public void setBalance(double balance) {
		balanceControl.setValue((float) balance);
	}


}
