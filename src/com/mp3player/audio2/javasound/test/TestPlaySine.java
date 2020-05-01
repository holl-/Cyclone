package com.mp3player.audio2.javasound.test;

import java.io.IOException;

import com.mp3player.audio2.javasound.JavaSoundEngine;
import com.mp3player.audio2.javasound.lib.AudioSystem2;
import com.mp3player.audio2.javasound.lib.SineInputStream;
import com.mp3player.desktopaudio.AudioEngineException;
import com.mp3player.desktopaudio.MediaStream;
import com.mp3player.desktopaudio.Player;
import com.mp3player.desktopaudio.UnsupportedMediaFormatException;

public class TestPlaySine {

	public static void main(String[] args) throws UnsupportedMediaFormatException, IOException, AudioEngineException {
		// Initialize AudioEngine
		JavaSoundEngine engine = new JavaSoundEngine();
		
		// Test
		SineInputStream sine = new SineInputStream(440, 44100,  100_000);
		Player player = engine.newPlayer(new MediaStream(
				sine, -1, -1, -1, 
				AudioSystem2.toAudioDataFormat(sine.getFormat()),
				null));
		player.prepare();
		player.activate(engine.getDefaultDevice());
		player.start();
				
	}
	
	
	

}
