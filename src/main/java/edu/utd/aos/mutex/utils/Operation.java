package edu.utd.aos.mutex.utils;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.tinylog.Logger;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

import edu.utd.aos.mutex.dto.ApplicationConfig;
import edu.utd.aos.mutex.dto.ClientDetails;
import edu.utd.aos.mutex.dto.NodeDetails;
import edu.utd.aos.mutex.dto.ServerDetails;
import edu.utd.aos.mutex.exception.MutexException;
import edu.utd.aos.mutex.references.MutexConfigHolder;
import edu.utd.aos.mutex.references.MutexReferences;
import edu.utd.aos.mutex.references.OperationEnum;

public class Operation {
	
	/**
	 * key is Filename and Value is list of requests timestamp.
	 */
	private static Table<String, String, ArrayList<Long>> myRequestsMap = HashBasedTable.create();
	
	/**
	 * Row is file name, Column is operation and data is a map of host names and count.
	 */
	private static Table<String, String, Map<String, Integer>> myRepliesMap = HashBasedTable.create();
	
	/**
	 * Key is hostname and value is list of files.
	 */
	private static Table<String, String, ArrayList<String>> myDeferredReplies = HashBasedTable.create();
	
	/**
	 * To check if currently in critical section.
	 */
	private static Table<String, String, Boolean> inCriticalSection = HashBasedTable.create();
	
	/**
	 * READ: "READ||file1||timestamp";
	 * WRITE: "WRITE||file1||timestamp||<id, timestamp>";
	 * @return formatted string for requests.
	 */
	public static String generateClientRequest() {
		ArrayList<String> cachedFiles = Host.getCahcedFiles();		
		ArrayList<String> availableRequests = new ArrayList<String>();
		availableRequests.add(OperationEnum.READ.toString());
		availableRequests.add(OperationEnum.WRITE.toString());		
		Random rand = new Random();
		String file = cachedFiles.get(rand.nextInt(cachedFiles.size()));
		String operation = availableRequests.get(rand.nextInt(availableRequests.size()));
		String result = "";
		long now = Instant.now().toEpochMilli();
		if(operation.equalsIgnoreCase(OperationEnum.READ.toString())) {
			result = result + operation + MutexReferences.SEPARATOR_TEXT + file + MutexReferences.SEPARATOR_TEXT + now;
		}
		else {
			String content = "<" + Host.getId() + ", " + now + ">"; 			
			result = result + operation + MutexReferences.SEPARATOR_TEXT + file + MutexReferences.SEPARATOR_TEXT + now + MutexReferences.SEPARATOR_TEXT + content;
		}
		setMyRequestsMap(file, operation, now);
		return result;
	}
	
	/**
	 * Generate critical section read/write req for the servers.
	 */
	public static String generateCriticalSectionRequest(String opn, String file) {
		if(opn.equalsIgnoreCase(OperationEnum.READ.toString())) {
			return OperationEnum.READ.toString() + MutexReferences.SEPARATOR_TEXT + file;
		}
		else {
			return OperationEnum.WRITE.toString() + MutexReferences.SEPARATOR_TEXT + file;
		}
	}

	/**
	 * Set/Update the requests map.
	 * @param file File to access.
	 * @param timestamp Current time.
	 */
	private static void setMyRequestsMap(String file, String operation, long timestamp) {
		if(!(myRequestsMap.get(file, operation) == null)){
			ArrayList<Long> temp = myRequestsMap.get(file, operation);
			temp.add(timestamp);
			myRequestsMap.put(file, operation, new ArrayList<Long>(temp));
		}
		else {
			ArrayList<Long> temp = new ArrayList<Long>();
			temp.add(timestamp);
			myRequestsMap.put(file, operation, new ArrayList<Long>(temp));
		}
	}
	
	/**
	 * Set/Update the replies map on receiving replies from other clients.
	 * 
	 * @param response
	 */
	public static void setMyRepliesMap(String file, String opn, String host) {
		
		Map<String, Integer> status = myRepliesMap.get(file, opn);
		
		if(status == null || !status.containsKey(host)) {
			Map<String, Integer> updatedMap = new HashMap<String, Integer>();
			updatedMap.put(host, 1);
			myRepliesMap.put(file, opn, new HashMap<String, Integer>(updatedMap));
		}
		else {
			int count = status.get(host);
			status.put(host, count + 1);
			myRepliesMap.put(file, opn, new HashMap<String, Integer>(status));
		}
	}
	
	/**
	 * Set/Update deferred map.
	 * @param request
	 */
	public static void setMyDeferredRepliesMap(String[] input, String host, int port) { 
		String operation = input[0];
		String file = input[1];
		String content = null;
		ArrayList<String> arrayList = myDeferredReplies.get(file, operation);
		if(arrayList == null) {
			ArrayList<String> temp = new ArrayList<String>();
			temp.add(String.join(host + MutexReferences.SEPARATOR_TEXT + port + MutexReferences.SEPARATOR_TEXT, input));
			myDeferredReplies.put(file, operation, new ArrayList<String>(temp));
		}
		else {
			arrayList.add(String.join(host + MutexReferences.SEPARATOR_TEXT + port + MutexReferences.SEPARATOR_TEXT, input));
			myDeferredReplies.put(file, operation, new ArrayList<String>(arrayList));
		}
	}
	
	
	/**
	 * @param file
	 * @return no of replies for any file.
	 */
	public static boolean gotRequiredReplies(String file, String opn) {
		
		Logger.debug("Current status of myRepliesMap: " + myRepliesMap);
		ApplicationConfig applicationConfig = MutexConfigHolder.getApplicationConfig();
		NodeDetails nodeDetails = applicationConfig.getNodeDetails();
		List<ClientDetails> clientDetails = nodeDetails.getClientDetails();
		
		int count = 0;
		
		if(!(myRepliesMap.get(file, opn) == null)) {
			Map<String, Integer> replyFromHosts = myRepliesMap.get(file, opn);
			for(Map.Entry<String, Integer> status: replyFromHosts.entrySet()) {
				if(status.getValue() >= 1) {
					count++;
				}
			}
		}
		return count == clientDetails.size() - 1 ? true: false;
	}
	
	public static void updateRepliesMap(String file, String opn) {
		Logger.debug("Updating replies map.");
		Map<String, Integer> replyMap = myRepliesMap.get(file, opn);
		Map<String, Integer> updateMap = new HashMap<String, Integer>();
		for(Map.Entry<String, Integer> status: replyMap.entrySet()) {
			String host = status.getKey();
			int val = status.getValue();
			updateMap.put(host, val - 1);
		}
		myRepliesMap.put(file, opn, new HashMap<String, Integer>(updateMap));
	}
	
	
	public static boolean isMyTimeStampLarger(String file, String opn, long timestamp) {
		
		if(myRequestsMap.get(file, opn) == null || myRequestsMap.get(file, opn).isEmpty()) {
			return true;
		}
		else {
			Long myTimestamp = myRequestsMap.get(file, opn).get(0);
			return myTimestamp > timestamp ? true: false;
		}
	}
	
	public static void enterCriticalSection(String file, String opn) {
		Logger.info("Entering critical section.");
		inCriticalSection.put(file, opn, true);
	}
	
	public static void exitCriticalSection(String file, String opn) {
		Logger.info("Exiting critical section.");
		inCriticalSection.put(file, opn, false);
	}
	
	public static boolean inCriticalSectionStatus(String file, String opn) {
		if(inCriticalSection.get(file, opn) == null) {
			return false;
		}else {
			return inCriticalSection.get(file, opn);
		}
	}
	
	
	/** 
	 * REPLY||file1||READ or
	 * REPLY||file1||WRITE
	 * @param address
	 * @param port
	 * @param file
	 * @param operation
	 * @throws MutexException
	 */
	public static void sendReadReply(String address, int port, String file, String operation) throws MutexException {
		Logger.info("Attempting to send REPLY for READ request to node: " + address + ", port: " + port + ", for file: " + file + ", from client: " + Host.getLocalHost());
		port = Host.getPortNumber(address);
		Socket socket = null; 
	    DataOutputStream out = null;
	    try {
			socket = new Socket(address, port);			
	        out = new DataOutputStream(socket.getOutputStream());
	        out.writeUTF(OperationEnum.REPLY.toString() + MutexReferences.SEPARATOR_TEXT + file + MutexReferences.SEPARATOR_TEXT + operation);
	        socket.close();
	    }catch(Exception e) {
	    	throw new MutexException("Error while sending a REPLY for READ message to the client. Error: " + e);
	    }
		
	}
	
	public static void sendWriteReply(String address, int port, String file, String operation, String content) throws MutexException {
		Logger.info("Attempting to send REPLY for WRITE request to node: " + address + " for file: " + file + ", from client: " + Host.getLocalHost());
		port = Host.getPortNumber(address);
		Socket socket = null; 
	    DataOutputStream out = null;
	    try {
			socket = new Socket(address, port);			
	        out = new DataOutputStream(socket.getOutputStream());
	        out.writeUTF(OperationEnum.REPLY.toString() + MutexReferences.SEPARATOR_TEXT + file + MutexReferences.SEPARATOR_TEXT + operation + MutexReferences.SEPARATOR_TEXT + content);
	        socket.close();
	    }catch(Exception e) {
	    	throw new MutexException("Error while sending a REPLY for READ message to the client. Error: " + e);
	    }
		
	}

	/**
	 * Call server with appropriate READ/WRITE messages.
	 * READ||file1
	 * WRITE||file2||content
	 * 
	 * @param input 
	 */
	public static void executeCriticalSection(String[] input) {
		
		Logger.info("Executing critical section.");
		String file = input[1];
		String operation = input[2];
		String content = null;
		
		Socket serverSocket = null;
		DataOutputStream dos = null;
		DataInputStream dis = null;
				
		if(OperationEnum.READ.toString().equalsIgnoreCase(operation)) {
			ArrayList<String> aRandomServer = Host.getARandomServer();
			String address = aRandomServer.get(0);
			int port = Integer.parseInt(aRandomServer.get(1));
			try {
				serverSocket = new Socket(address, port);
				Logger.info("Requesting server: " + address + ", to perform operation: " + operation + ", for file: " + file);
				dis = new DataInputStream(serverSocket.getInputStream());
		        dos = new DataOutputStream(serverSocket.getOutputStream());		        
		        dos.writeUTF(OperationEnum.READ.toString() + MutexReferences.SEPARATOR_TEXT + file);
		        String response = null;
		        response = dis.readUTF();
		        Instant currentTimestamp = Instant.now();
		        long timeout = 10000;
		        while((response == null) && currentTimestamp.plusMillis(timeout).isAfter(Instant.now())) {
		        	response = dis.readUTF();
		        }
		        if(response == null) {
		        	serverSocket.close();
		        	throw new MutexException("The server did not complete the read request of client: " + Host.getLocalHost());
		        }
		        Logger.info("Read last line of file: " + file + ", send from server: " + address + ". Text Read: " + response);
		        Logger.info("Exiting critical section");
		        serverSocket.close();
		    }catch(Exception e) {
		    	Logger.error("Error while sending request to server: " + address + ". Error: " + e);
		    }
		}
		else {
			content = input[3];
			ArrayList<ArrayList<String>> allServers = Host.getAllServers();
			ArrayList<String> replyFromServers = new ArrayList<String>();
			for(ArrayList<String> servers: allServers) {
				String address = servers.get(0);
				int port = Integer.parseInt(servers.get(1));
				try {
					serverSocket = new Socket(address, port);
					Logger.info("Requesting server: " + address + ", to perform operation: " + operation);
					dis = new DataInputStream(serverSocket.getInputStream());
			        dos = new DataOutputStream(serverSocket.getOutputStream());
			        dos.writeUTF(OperationEnum.WRITE.toString() + MutexReferences.SEPARATOR_TEXT + file + MutexReferences.SEPARATOR_TEXT + content);			        
			        String response = null;
			        response = dis.readUTF();
			        Instant currentTimestamp = Instant.now();
			        long timeout = 10000;
			        while((response == null) && currentTimestamp.plusMillis(timeout).isAfter(Instant.now())) {
			        	response = dis.readUTF();
			        }
			        if(response == null) {
			        	serverSocket.close();
			        	throw new MutexException("The server did not complete the read request of client: " + Host.getLocalHost());
			        }
			        replyFromServers.add(address);			        
			        serverSocket.close();
			    }catch(Exception e) {
			    	Logger.error("Error while sending request to server: " + address + ". Error: " + e);
			    }
			}
			if(replyFromServers.size() == Host.getAllServers().size()) {
				Logger.info("Successfully updated all replica of file: " + file);
				Logger.info("Exiting critical section");
			}
		}		
	}

	public static void sendDeferredReply(String file, String readWriteOpn) {
		
		try {
			ArrayList<String> arrayList = myDeferredReplies.get(file, readWriteOpn);
			Logger.debug("Sending all deferred replies for: " + arrayList);
			if(arrayList == null) {
				return;
			}
			else {
				Logger.info("Start sending all deferred replies.");
				for(String pipedString: arrayList) {				
					if(OperationEnum.READ.toString().equalsIgnoreCase(readWriteOpn)) {
						String[] input = pipedString.split(MutexReferences.SEPARATOR);
						String host = input[0];
						int port = (Integer.parseInt(input[1]));					
						sendReadReply(host, port, file, readWriteOpn);
					}
					else {
						String[] input = pipedString.split(MutexReferences.SEPARATOR);
						String host = input[0];
						String content = input[4];
						int port = (Integer.parseInt(input[1]));
						sendWriteReply(host, port, file, readWriteOpn, content);
					}
				}
			}
		}catch(Exception e) {
			Logger.error("Error while sending deferred replies: " + e);
		}
	}
}







