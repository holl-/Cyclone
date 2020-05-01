package com.mp3player.audio2.javasound.lib;

import javax.sound.sampled.Line;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;

import com.mp3player.audio2.javasound.JavaSoundEngine;
import com.mp3player.desktopaudio.AudioDevice;

public class JavaSoundMixer implements AudioDevice
{
	private JavaSoundEngine engine;
	private Mixer mixer;
	private boolean isDefault;
    private double[] minMaxGain;
	
	
	public JavaSoundMixer(JavaSoundEngine engine, Mixer mixer, boolean isDefault) {
		this.engine = engine;
		this.mixer = mixer;
		this.isDefault = isDefault;
		minMaxGain = null;
	}
	
	protected void loadMinMaxGain() {
		if(minMaxGain != null) return;
		
		try {
			minMaxGain = AudioSystem2.getMinMaxLineGain(mixer);
		} catch (LineUnavailableException e) {
			minMaxGain = new double[] { 0, 0};
			engine.errorOccurred(e, "cannot retrieve min/max gain");
		}
	}

	@Override
	public String getName() {
		return mixer.getMixerInfo().getName();
	}

	@Override
	public boolean isDefault() {
		return isDefault;
	}
	
	public void setDefault(boolean def) {
		isDefault = def;
	}
	
	public Mixer getMixer() {
		return mixer;
	}
	
	public Mixer.Info getInfo() {
		return mixer.getMixerInfo();
	}

	@Override
	public boolean equals(Object o) {
		if(o instanceof JavaSoundMixer) {
			JavaSoundMixer d = (JavaSoundMixer) o;
			return mixer == d.mixer;
		}
		else return false;
	}
	
	@Override
	public String toString() {
		return getName();
	}
	
	@Override
	public int hashCode() {
		return mixer.hashCode();
	}

	@Override
	public String getID() {
		Mixer.Info info = getInfo();
		return info.getName()+";"+info.getVendor()+";"+info.getVersion();
	}

	@Override
	public double getMaxGain() {
		loadMinMaxGain();
		return minMaxGain[1];
	}

	@Override
	public double getMinGain() {
		loadMinMaxGain();
		return minMaxGain[0];
	}

	@Override
	public int getMaxActivePlayers() {
		return mixer.getMaxLines(new Line.Info(SourceDataLine.class));
	}
}
