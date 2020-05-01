package mediacommand;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CombinationManager {

	private long lastInput = -1;
	private List<MediaCommand> currentCombination;
	
	private int maxDelay = 500;
	private Map<MediaCommand[], CombinationListener> combinations;
	
	
	public CombinationManager() {
		combinations = new HashMap<>();
		currentCombination = new ArrayList<MediaCommand>();
	}
	
	
	public void addCombination(MediaCommand[] combination, CombinationListener listener) {
		combinations.put(combination, listener);
	}
	
	public void register() throws IllegalStateException
	{
		if(!MediaCommandManager.isSupported()) {
			throw new IllegalStateException("not supported");
		}
		register(MediaCommandManager.getInstance());
	}
	
	public void register(MediaCommandManager m) {
		m.addMediaCommandListener(command -> input(command));
	}
	
	
	private void input(MediaCommand command) {
		long now = System.currentTimeMillis();
		long last = lastInput;
		lastInput = now;
		
		if(last < 0 || now - last > maxDelay) {
			currentCombination.clear();
		}
		
		currentCombination.add(command);
		boolean isRegisteredCombination = false;
		for(MediaCommand[] combination : combinations.keySet()) {
			if(equal(currentCombination, combination)) {
				fire(combination);
				isRegisteredCombination = true;
			}
		}
		
		if(!isRegisteredCombination) {
			// execute current command
			for(MediaCommand[] combination : combinations.keySet()) {
				if(equal(command, combination)) {
					fire(combination);
				}
			}
		}
	}
	
	protected void fire(MediaCommand[] combination) {
		combinations.get(combination).onCombination(combination);
	}


	private static <T> boolean equal(List<T> list, T[] array) {
		if(list.size() != array.length) return false;
		for(int i = 0; i < array.length; i++) {
			if(list.get(i) != array[i]) return false;
		}
		return true;
	}
	
	private static <T> boolean equal(T element, T[] array) {
		if(array.length != 1) return false;
		return array[0].equals(element);
	}
}
