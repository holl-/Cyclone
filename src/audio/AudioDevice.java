package audio;

public interface AudioDevice {

	/**
	 * Returns null if name not known.
	 * @return the name of the device
	 */
	String getName();
	String getID();
	boolean isDefault();
	

	double getMaxGain(); // 0 if not known, must be available when track is loaded
	double getMinGain(); // 0 if not known, must be available when track is loaded
	

	/**
	 * Obtains the approximate maximum of players that can be active
	 * simultaneously on this device.
	 * If no such maximum exists, this method returns
	 * {@link #UNLIMITED}.
	 * @return the approximate maximum of players that can be active
	 * simultaneously
	 */
	int getMaxActivePlayers();
	
	int UNLIMITED = -1;
}
