package org.pokenet.server.network;

import org.pokenet.server.backend.entity.PlayerChar;

/**
 * A class which kicks players if they've been idle for too long
 */
public class IdleTimer implements Runnable {
	private boolean m_isRunning = false;
	
	@Override
	public void run() {
		while(m_isRunning) {
			/*
			 * Loop through all players and check for idling
			 * If they've idled, disconnect them
			 */
			try {
				for(PlayerChar p : ConnectionManager.getPlayers().values()) {
					if(System.currentTimeMillis() - p.lastPacket >= 300000)
						p.forceLogout();
				}
				Thread.sleep(5000);
			} catch (Exception e) {}
			
		}
	}
	
	/**
	 * Starts the idle timer
	 */
	public void start() {
		m_isRunning = true;
		new Thread(this).start();
	}
	
	/**
	 * Stops the idle timer
	 */
	public void stop() {
		m_isRunning = false;
	}
}