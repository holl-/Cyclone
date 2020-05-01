package audio.javasound.lib;

import static java.lang.Math.PI;
import static java.lang.Math.sin;

import java.io.IOException;
import java.io.InputStream;

import javax.sound.sampled.AudioFormat;

public class SineInputStream extends InputStream
{
	private long pos;
	private long mark;
	private double frequency;
	private int sampleRate;
	private AudioFormat format;
	private byte[] sample;
	private long length;
	
	
	public SineInputStream(double sineFrequency, int sampleRate, long length) {
		this.frequency = sineFrequency;
		this.sampleRate = sampleRate;
		format = new AudioFormat(sampleRate, 8, 1, true, false);
//		format = new AudioDataFormat("PCM_SIGNED", null, sampleRate, 8, 1, 8, sampleRate, false, null);
		sample = samplePhase(10);
		pos = 0;
		mark = 0;
		this.length = length;
	}
	
	
	public AudioFormat getFormat() {
		return format;
	}


	private byte[] samplePhase(int phases) {
		int length = (int) (sampleRate / frequency * phases);
		byte[] bytes = new byte[length];
		for(int i = 0; i < bytes.length; i++) {
			bytes[i] = valueAt(i);
		}
		return bytes;
	}

	

	private byte valueAt(long pos) {
		return (byte) (127 * sin(pos * frequency / format.getSampleRate() * 2 * PI));
	}


	@Override
	public int read() throws IOException {
		return valueAt(pos++);
	}
	
	@Override
	public int read(byte[] buffer, int offset, int length) {
		long remaining = this.length - pos;
		if(remaining <= 0) return -1;
		if(remaining < length) length = (int) remaining;
		
		int filled = 0;
		while(filled < length) {
			int sampleOffset = (int) (pos % sample.length);
			int maxRead = sample.length - sampleOffset;
			int copyBytes = Math.min(length-filled, maxRead);
			System.arraycopy(sample, sampleOffset, buffer, offset, copyBytes);
			filled += copyBytes;
		}
		pos += filled;
		return filled;
	}
	
	
	public long skip(long n) {
		pos += n;
		return n;
	}
	
	public int available() {
		return Integer.MAX_VALUE;
	}
	
	public synchronized void mark(int readlimit) {
		mark = pos;
	}
	
	public synchronized void reset() throws IOException {
        pos = mark;
    }
	
    public boolean markSupported() {
        return true;
    }
}
