package edu.utd.aos.mutex.utils;
import java.net.InetAddress;
import java.net.UnknownHostException;

import org.tinylog.Logger;

import edu.utd.aos.mutex.exception.MutexException;

/**
 * For all server/client details.
 * 
 * @author pankaj
 */
public class Host {
	
	private static String localhost;
	
	public static void initialize() throws MutexException {
		Logger.info("Local host details initialization...");
		setLocalHost();
		Logger.info("Local host initialization complete...");
	}
	
	private static void setLocalHost() throws MutexException {		
		InetAddress ip;        
		try {
			ip = InetAddress.getLocalHost();
			localhost = ip.getHostName();
		} catch (UnknownHostException e) {
			throw new MutexException("Error while fetching the hostname. " + e);
		}		
	}
	
	public static String getLocalHost() {
		return localhost;
	}
	
	/**
	 * Constructor for utility class.
	 */
	private Host() {
		
	}
}
