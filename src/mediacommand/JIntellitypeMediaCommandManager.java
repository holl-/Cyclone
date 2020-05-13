package mediacommand;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

import com.melloware.jintellitype.IntellitypeListener;
import com.melloware.jintellitype.JIntellitype;
import com.melloware.jintellitype.JIntellitypeException;

public class JIntellitypeMediaCommandManager extends MediaCommandManager implements IntellitypeListener {

	private JIntellitype jIntellitype;
	private Map<Integer, MediaCommand> commandMap;
	
	
	public JIntellitypeMediaCommandManager() {
		File lib = getLibraryFile();
		if(!lib.exists()) System.err.println(""+lib.getAbsolutePath()+" does not exist");
		JIntellitype.setLibraryLocation(lib.getPath()); // path to JIntellitype(64).dll
		try {
			jIntellitype = JIntellitype.getInstance();
		}catch(JIntellitypeException exc) {
			exc.printStackTrace();
			return;
		}
		
		commandMap = new HashMap<Integer, MediaCommand>();
		commandMap.put(JIntellitype.APPCOMMAND_MEDIA_PLAY_PAUSE, MediaCommand.PLAY_PAUSE);
		commandMap.put(JIntellitype.APPCOMMAND_MEDIA_STOP, MediaCommand.STOP);
		commandMap.put(JIntellitype.APPCOMMAND_MEDIA_NEXTTRACK, MediaCommand.NEXT);
		commandMap.put(JIntellitype.APPCOMMAND_MEDIA_PREVIOUSTRACK, MediaCommand.PREVIOUS);
		commandMap.put(JIntellitype.APPCOMMAND_VOLUME_UP, MediaCommand.VOLUME_UP);
		commandMap.put(JIntellitype.APPCOMMAND_VOLUME_DOWN, MediaCommand.VOLUME_DOWN);
		commandMap.put(JIntellitype.APPCOMMAND_VOLUME_MUTE, MediaCommand.MUTE);
		
		jIntellitype.addIntellitypeListener(this);
	}
	
	
	@Override
	public void cleanUp() {
		jIntellitype.cleanUp();
	}


	@Override
	public void onIntellitype(int commandCode) {
		if(commandMap.containsKey(commandCode)) {
			MediaCommand command = commandMap.get(commandCode);
			fireCommandReceived(command);
		}
	}

	public static File getBinaryApplicationFile(Class<?> owningClass, String relativePath) {
		// Location when deployed
		File dir = null;
		try {
			dir = new File(owningClass.getProtectionDomain().getCodeSource().getLocation().toURI()).getParentFile();
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		File deployedFile = new File(dir, relativePath);
		if(deployedFile.exists()) return deployedFile;

		// Non-deployed - Use file from src/
		File srcFile = null;
		try {
			srcFile = new File(owningClass.getResource(relativePath).toURI());
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		if (srcFile.exists()) return srcFile;

		throw new RuntimeException("Cannot find file "+relativePath);
	}
	
	private static File getLibraryFile() {
		String bitnessJVM = System.getProperty("sun.arch.data.model");
		String filename;
		if(bitnessJVM.equals("64")) {
			filename = "JIntellitype64.dll";
		}else {
			filename = "JIntellitype.dll";
		}
		return getBinaryApplicationFile(JIntellitypeMediaCommandManager.class, filename);

		
//		File extracted = new File(filename);
//		if(!extracted.exists()) {
//			// Extract
//			InputStream in = JIntellitypeMediaCommandManager.class.getResourceAsStream(filename);
//			try {
//				Files.copy(in, extracted.toPath());
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
//		}
//		return extracted;
	}
	
	public static boolean isSupported() {
		return isPlatformSupported() && getLibraryFile().exists();
	}

	public static boolean isPlatformSupported() {
		try {
			String osName = System.getProperty("os.name").toLowerCase();
			return osName.toLowerCase().contains("windows");
	      } catch (SecurityException ex) {
	         // we are not allowed to look at this property
	         System.err.println("Caught a SecurityException reading the system property "
	                  + "'os.name'; the SystemUtils property value will default to null.");
	         return false;
	      }
	}
	
}
