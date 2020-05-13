package systemcontrol;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static mediacommand.JIntellitypeMediaCommandManager.getBinaryApplicationFile;


public abstract class LocalMachine {

	public static LocalMachine getLocalMachine() {
		String os = System.getProperty("os.name");
		if(os.toLowerCase().contains("windows")) {
			return new WindowsMachine();
		}
		return null;
	}
	
	
	public abstract boolean enterStandby();
	
	public abstract boolean turnOffMonitors();
	
	
	
	private static class WindowsMachine extends LocalMachine
	{
		@Override
		public boolean enterStandby()
		{
			String windir = System.getenv("windir");
			String command = windir+"/System32/rundll32.exe powrprof.dll,SetSuspendState";
			try {
				Runtime.getRuntime().exec(command);
				return true;
			} catch (IOException e) {
				e.printStackTrace();
				return false;
			}
		}

		@Override
		public boolean turnOffMonitors() {
			File exe = getBinaryApplicationFile(LocalMachine.class, "Turn Off Monitor.exe");
			try {
				Runtime.getRuntime().exec(exe.getAbsolutePath());
				return true;
			} catch (IOException e) {
				e.printStackTrace();
				return false;
			}
		}
	}
	
}
