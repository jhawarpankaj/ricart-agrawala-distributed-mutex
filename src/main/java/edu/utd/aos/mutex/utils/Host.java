package edu.utd.aos.mutex.utils;
import java.io.File;
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
	private static String localPort;
	private static int id;
	private static boolean isServer = false;
	private static ArrayList<ArrayList<String>> allServers = new ArrayList<ArrayList<String>>();
	private static ArrayList<ArrayList<String>> allOtherClients = new ArrayList<ArrayList<String>>();
	private static ArrayList<String> cachedFiles = new ArrayList<String>();
	private static ArrayList<String> listOfFiles = new ArrayList<String>();
	private static String filePath;
	
	/**
	 * Initialization for all host details.
	 * 
	 * @throws MutexException
	 */
	public static void initialize() throws MutexException {
		Logger.info("Initializing all local host related configuration");
		setLocalHostPort();
		setAllServers();
		setAllOtherClients();
		Logger.info("All localhost initialization complete.");
	}

	/**
	 * Set the current host name.
	 * 
	 * @throws MutexException
	 */
	private static void setLocalHostPort() throws MutexException {
		Logger.info("Setting local host and port...");
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
				localPort = server.getPort();
				id = server.getId();
				filePath = server.getFilePath() + id + "/";
				flag = true;
				break;
			}
		}
		if(!flag) {
			for(ClientDetails client: clientDetails) {
				if(localhost.equalsIgnoreCase(client.getName())) {
					flag = true;
					isServer = false;
					localPort = client.getPort();
					id = client.getId();
					filePath = client.getFilePath() + id + "/";
					break;
				}
			}
		}
		if(!flag) {
			throw new MutexException("The code is being run on unkown machines.");
		}
		Logger.info("Set local host and port...");
	}
	
	/**
	 * Set all server names list.
	 */
	private static void setAllServers(){
		Logger.info("Set all servers list.");
		ApplicationConfig applicationConfig = MutexConfigHolder.getApplicationConfig();
		NodeDetails nodeDetails = applicationConfig.getNodeDetails();
		List<ServerDetails> serverDetails = nodeDetails.getServerDetails();
		for(ServerDetails server: serverDetails) {
			ArrayList<String> temp = new ArrayList<String>();
			temp.add(server.getName());
			temp.add(server.getPort());
			allServers.add(new ArrayList<String>(temp));
		}
		Logger.info("Initialized server list: " + allServers);
	}
	
	/**
	 * Set all clients list.
	 */
	private static void setAllOtherClients() {
		Logger.info("Initialize all client list.");
		ApplicationConfig applicationConfig = MutexConfigHolder.getApplicationConfig();
		NodeDetails nodeDetails = applicationConfig.getNodeDetails();
		List<ClientDetails> clientDetails = nodeDetails.getClientDetails();
		Logger.info("Client Details list: " + clientDetails);
		for(ClientDetails client: clientDetails) {
			ArrayList<String> temp = new ArrayList<String>();
			temp.add(client.getName());
			temp.add(client.getPort());
			Logger.info("temp: " + temp);
			if(!client.getName().equalsIgnoreCase(localhost)) {
				allOtherClients.add(new ArrayList<String>(temp));
			}
			Logger.info("Other clients: " + allOtherClients);
		}
		Logger.info("Client list initialized: " + allOtherClients);
	}
	
	/**
	 * Cache all the file names (only names and not full path).
	 * 
	 * @param listOfFiles
	 */
	public static void setCachedFiles(String listOfFiles) {
		String[] temp = listOfFiles.split(MutexReferences.SEPARATOR);
		cachedFiles = new ArrayList<String>(Arrays.asList(temp));
	}
	
	/**
	 * Set list of files for the server.
	 */
	public static void setListOfFiles() {
		ApplicationConfig applicationConfig = MutexConfigHolder.getApplicationConfig();
		NodeDetails nodeDetails = applicationConfig.getNodeDetails();
		List<ServerDetails> serverDetails = nodeDetails.getServerDetails();
		for(ServerDetails server: serverDetails) {
			if(Host.getLocalHost().equalsIgnoreCase(server.getName())) {
				String filePath = server.getFilePath() + server.getId();
				File[] files = new File(filePath).listFiles();
				for(File file: files) {
					if(file.isFile()) {
						listOfFiles.add(file.getName());
					}
				}
				break;
			}
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
	public static ArrayList<ArrayList<String>> getAllOtherClients(){
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
		return localPort;
	}
	
	/**
	 * @return Id of the localhost.
	 */
	public static int getId() {
		return id;
	}
	
	/**
	 * Get root directory.
	 * @return
	 */
	public static String getFilePath() {
		return filePath;
	}
	
	/**
	 * @return One of the servers randomly.
	 */
	public static ArrayList<String> getARandomServer() {
		Random rand = new Random();
		ArrayList<ArrayList<String>> allServersList = getAllServers();		
		return allServersList.get(rand.nextInt(allServersList.size()));
	}
	
	public static int getPortNumber(String host) {
		ApplicationConfig applicationConfig = MutexConfigHolder.getApplicationConfig();
		NodeDetails nodeDetails = applicationConfig.getNodeDetails();
		List<ServerDetails> serverDetails = nodeDetails.getServerDetails();
		List<ClientDetails> clientDetails = nodeDetails.getClientDetails();
		if(Host.isServer()) {
			for(ServerDetails server: serverDetails) {
				if(server.getName().equalsIgnoreCase(host)) {
					return (Integer.parseInt(server.getPort()));
				}
			}
		}
		else {
			for(ClientDetails client: clientDetails) {
				if(client.getName().equalsIgnoreCase(host)) {
					return (Integer.parseInt(client.getPort()));
				}
			}
		}
		return -1;
	}
	
	/**
	 * @return all cached files.
	 */
	public static ArrayList<String> getCahcedFiles() {
		return cachedFiles;
	}
	
	/**
	 * @return List of files name.
	 */
	public static ArrayList<String> getListOfFiles() {
		return listOfFiles;
	}
	
	/**
	 * Constructor for utility class.
	 */
	private Host() {
		
	}

	
}
