package edu.utd.aos.mutex.utils;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import org.tinylog.Logger;

import edu.utd.aos.mutex.dto.ApplicationConfig;
import edu.utd.aos.mutex.dto.ClientDetails;
import edu.utd.aos.mutex.dto.NodeDetails;
import edu.utd.aos.mutex.dto.ServerDetails;
import edu.utd.aos.mutex.exception.MutexException;
import edu.utd.aos.mutex.references.MutexConfigHolder;

/**
 * For all server/client details.
 * 
 * @author pankaj
 */
public class Host {
	
	private static String localhost;
	private static String port;
	private static boolean isServer = false;
	private static ArrayList<ArrayList<String>> allServers = new ArrayList<ArrayList<String>>();
	private static ArrayList<ArrayList<String>> allClients = new ArrayList<ArrayList<String>>();
	
	/**
	 * Initialization for all host details.
	 * 
	 * @throws MutexException
	 */
	public static void initialize() throws MutexException {
		Logger.info("Local host details initialization...");
		setLocalHostPort();
		setAllServers();
		setAllClients();
		Logger.info("Local host initialization complete...");
		
	}

	/**
	 * Set the current host name.
	 * 
	 * @throws MutexException
	 */
	private static void setLocalHostPort() throws MutexException {
		InetAddress ip;
		ApplicationConfig applicationConfig = MutexConfigHolder.getApplicationConfig();
		NodeDetails nodeDetails = applicationConfig.getNodeDetails();
		List<ServerDetails> serverDetails = nodeDetails.getServerDetails();
		List<ClientDetails> clientDetails = nodeDetails.getClientDetails();		
		try {
			ip = InetAddress.getLocalHost();
			localhost = ip.getHostName();
		} catch (UnknownHostException e) {
			throw new MutexException("Error while fetching the hostname. " + e);
		}
		boolean flag = false;
		for(ServerDetails server: serverDetails) {
			if(localhost.equalsIgnoreCase(server.getName())) {
				isServer = true;
				port = server.getPort();
				flag = true;
				break;
			}
		}
		if(!flag) {
			for(ClientDetails client: clientDetails) {
				if(localhost.equalsIgnoreCase(client.getName())) {
					flag = true;
					isServer = false;
					port = client.getPort();
					break;
				}
			}
		}
		if(!flag) {
			throw new MutexException("The code is being run on unkown machines.");
		}
	}
	
	/**
	 * Set all server names list.
	 */
	private static void setAllServers(){
		ApplicationConfig applicationConfig = MutexConfigHolder.getApplicationConfig();
		NodeDetails nodeDetails = applicationConfig.getNodeDetails();
		List<ServerDetails> serverDetails = nodeDetails.getServerDetails();
		for(ServerDetails server: serverDetails) {
			ArrayList<String> temp = new ArrayList<String>();
			temp.add(server.getName());
			temp.add(server.getPort());
			allServers.add(new ArrayList<String>(temp));
		}
	}
	
	/**
	 * Set all clients list.
	 */
	private static void setAllClients() {
		ApplicationConfig applicationConfig = MutexConfigHolder.getApplicationConfig();
		NodeDetails nodeDetails = applicationConfig.getNodeDetails();
		List<ClientDetails> clientDetails = nodeDetails.getClientDetails();
		for(ClientDetails client: clientDetails) {
			ArrayList<String> temp = new ArrayList<String>();
			temp.add(client.getName());
			temp.add(client.getPort());
			allClients.add(new ArrayList<String>(temp));			
		}		
	}
	
	/**
	 * @return All server nodes list.
	 */
	public static ArrayList<ArrayList<String>> getAllServers(){
		return allServers;
	}
	
	/**
	 * @return All client nodes list.
	 */
	public static ArrayList<ArrayList<String>> getAllClients(){
		return allClients;
	}
	 
	/**
	 * To check if current node is a server or not.
	 * 
	 * @return True if current node is a server else false.
	 */
	public static boolean isServer() {
		return isServer;
	}
	
	/**
	 * Getter method to get current local host.
	 * @return Current host's name.
	 */
	public static String getLocalHost() {
		return localhost;
	}
	
	/**
	 * @return Local port.
	 */
	public static String getLocalPort() {
		return port;
	}
	
	/**
	 * Constructor for utility class.
	 */
	private Host() {
		
	}
}
