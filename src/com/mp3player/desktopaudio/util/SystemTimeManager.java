package com.mp3player.desktopaudio.util;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class SystemTimeManager implements Runnable
{
	private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
	private ScheduledFuture<?> scheduleHandler;

	private List<SystemTimeJumpListener> timeJumpListeners = new CopyOnWriteArrayList<SystemTimeJumpListener>();

	private long lastTime = -1;
	private int updatePeriod;
	private int minJump = 2000;
	
	public SystemTimeManager() {
	}
	
	public void start(int durationMillis) {
		if(scheduleHandler != null) {
			throw new IllegalStateException("Already running");
		}
		
		updatePeriod = durationMillis;
		scheduleHandler = scheduler.scheduleAtFixedRate(this, durationMillis, durationMillis, TimeUnit.MILLISECONDS);
	}


	@Override
	public void run() {
		long time = System.currentTimeMillis();
		if(lastTime > 0) {
			long dif = time - lastTime;
			if(dif > updatePeriod + minJump) {
				for(SystemTimeJumpListener l : timeJumpListeners) {
					l.timeJumped(dif);
				}
			}
		}
		lastTime = time;
	}
	

	public void dispose() {
		scheduleHandler.cancel(false);
	}
	
	
	public void addSystemTimeJumpListener(SystemTimeJumpListener l) {
		timeJumpListeners.add(l);
	}
	
	public void removeSystemTimeJumpListener(SystemTimeJumpListener l) {
		timeJumpListeners.remove(l);
	}
	
	public interface SystemTimeJumpListener {

		void timeJumped(long timeDifference);
	}
}
