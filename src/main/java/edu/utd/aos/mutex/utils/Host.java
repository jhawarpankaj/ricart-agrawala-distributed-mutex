package edu.utd.aos.mutex.utils;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.tinylog.Logger;

import edu.utd.aos.mutex.dto.ApplicationConfig;
import edu.utd.aos.mutex.dto.ClientDetails;
import edu.utd.aos.mutex.dto.NodeDetails;
import edu.utd.aos.mutex.dto.ServerDetails;
import edu.utd.aos.mutex.exception.MutexException;
import edu.utd.aos.mutex.references.MutexConfigHolder;
import edu.utd.aos.mutex.references.MutexReferences;

/**
 * For all server/client details.
 * 
 * @author pankaj
 */
public class Host {
	
	private static String localhost;
	private static String port;
	private static int id;
	private static boolean isServer = false;
	private static ArrayList<ArrayList<String>> allServers = new ArrayList<ArrayList<String>>();
	private static ArrayList<String[]> allOtherClients = new ArrayList<String[]>();
	private static ArrayList<String> cachedFiles = new ArrayList<String>();
	
	/**
	 * Initialization for all host details.
	 * 
	 * @throws MutexException
	 */
	public static void initialize() throws MutexException {
		Logger.info("Local host details initialization...");
		setLocalHostPort();
		setAllServers();
		setAllOtherClients();
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
				id = server.getId();
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
					id = client.getId();
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
	private static void setAllOtherClients() {
		ApplicationConfig applicationConfig = MutexConfigHolder.getApplicationConfig();
		NodeDetails nodeDetails = applicationConfig.getNodeDetails();
		List<ClientDetails> clientDetails = nodeDetails.getClientDetails();
		for(ClientDetails client: clientDetails) {
			String[] temp = new String[2];
			temp[0] = client.getName();
			temp[1] = client.getPort();
			if(!client.getName().equals(localhost)) {
				allOtherClients.add(temp);
			}
		}		
	}
	
	public static void setCachedFiles(String listOfFiles) {
		String[] temp = listOfFiles.split(MutexReferences.SEPARATOR);
		cachedFiles = new ArrayList<String>(Arrays.asList(temp));
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
	public static ArrayList<String[]> getAllOtherClients(){
		return allOtherClients;
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
	 * @return Id of the localhost.
	 */
	public static int getId() {
		return id;
	}
	
	/**
	 * @return One of the servers randomly.
	 */
	public static ArrayList<String> getARandomServer() {
		Random rand = new Random();
		ArrayList<ArrayList<String>> allServersList = getAllServers();		
		return allServersList.get(rand.nextInt(allServersList.size()));
	}
	
	/**
	 * @return all cached files.
	 */
	public static ArrayList<String> getCahcedFiles() {
		return cachedFiles;
	}
	
	/**
	 * Constructor for utility class.
	 */
	private Host() {
		
	}
}
