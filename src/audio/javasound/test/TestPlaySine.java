package audio.javasound.test;

import audio.AudioEngineException;
import audio.MediaStream;
import audio.Player;
import audio.UnsupportedMediaFormatException;
import audio.javasound.JavaSoundEngine;
import audio.javasound.lib.AudioSystem2;
import audio.javasound.lib.SineInputStream;

import java.io.IOException;

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
		player.activate(engine.getDefaultDevice(), 0.2);
		player.start();
				
	}
	
	
	

}
