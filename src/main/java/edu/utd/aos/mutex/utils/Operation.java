package edu.utd.aos.mutex.utils;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.commons.io.FileUtils;
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
	private static Table<String, String , Integer> myRepliesMap = HashBasedTable.create();
	
	/**
	 * Key is hostname and value is list of files.
	 */
	private static Table<String, String, ArrayList<String>> myDeferredReplies = HashBasedTable.create();
	
	/**
	 * To check if currently in critical section.
	 */
	private static HashMap<String, Boolean> inCriticalSection = new HashMap<String, Boolean>();
	
	public static Table<String, String, Boolean> cachedReply = HashBasedTable.create();
	
	public static Table<String, String, Boolean> alreadyGeneratedRequest = HashBasedTable.create();
	
	/**
	 * READ: "READ||file1||timestamp";
	 * WRITE: "WRITE||file1||timestamp||<id, timestamp>";
	 * @return formatted string for requests.
	 */
	public static String generateClientRequest() {
		ArrayList<String> cachedFiles = Host.getCahcedFiles();		
		ArrayList<String> availableRequests = new ArrayList<String>();
		availableRequests.add(OperationEnum.READ.toString().toUpperCase());
		availableRequests.add(OperationEnum.WRITE.toString().toUpperCase());
		Random rand = new Random();
		String file = cachedFiles.get(rand.nextInt(cachedFiles.size()));
		Logger.debug("File selected randomly: " + file);
		String operation = availableRequests.get(rand.nextInt(availableRequests.size()));
		Logger.debug("Operation selected randomly: " + operation);
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
		
		File fileWrite = new File(Host.getFilePath() + "operations.txt");
		try {
			FileUtils.writeStringToFile(fileWrite, result + "\n", MutexReferences.ENCODING, true);
		} catch (IOException e) {
			Logger.error("Error while writing operations to file: " + e);
		}
		return result;
	}
	

	/**
	 * Set/Update the requests map.
	 * @param file File to access.
	 * @param timestamp Current time.
	 */
	private static void setMyRequestsMap(String file, String operation, long timestamp) {
		Logger.debug("Updating my requests map for file: " + file + ", operation: " + operation);
		Logger.debug("Current status: " + myRequestsMap);
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
		Logger.debug("After update: " + myRequestsMap);
	}
	
	/**
	 * Set/Update the replies map on receiving replies from other clients.
	 * 
	 * @param response
	 */
	public static void setMyRepliesMap(String file, String opn, String host) {
		
		Logger.debug("Updating my replies map for file: " + file + ", operation: " + opn + ", from host: " + host);
		Logger.debug("Current status: " + myRepliesMap);
		
		Integer count = myRepliesMap.get(file, host);
		
		if(count == null){
			myRepliesMap.put(file, host, 1);
		}
		else{
			myRepliesMap.put(file, host, count + 1);
		}
		
		Logger.debug("After update: " + myRepliesMap);
	}

	
	/**
	 * Set/Update deferred map.
	 * @param request
	 */
	public static void setMyDeferredRepliesMap(String[] input, String host, int port) {		
		String operation = input[0];
		String file = input[1];
		Logger.debug("Deferring replies for: " + host + ", file: " + file + ", operation: " + operation);
		Logger.debug("Current status: " + myDeferredReplies);
		ArrayList<String> arrayList = myDeferredReplies.get(file, operation);
		if(arrayList == null) {
			ArrayList<String> temp = new ArrayList<String>();
			temp.add(host + MutexReferences.SEPARATOR_TEXT + port + MutexReferences.SEPARATOR_TEXT + String.join(MutexReferences.SEPARATOR_TEXT , input));
			myDeferredReplies.put(file, operation, new ArrayList<String>(temp));
		}
		else {
			arrayList.add(host + MutexReferences.SEPARATOR_TEXT + port + MutexReferences.SEPARATOR_TEXT + String.join(MutexReferences.SEPARATOR_TEXT, input));
			myDeferredReplies.put(file, operation, new ArrayList<String>(arrayList));
		}		
		Logger.debug("After: " + myDeferredReplies);
	}
	
	
	/**
	 * @param file
	 * @return no of replies for any file.
	 */
	public static boolean gotRequiredReplies(String file, String opn) {
		
		Logger.debug("Checking my replies map to evaluate for critical section entry: " + myRepliesMap);
		ApplicationConfig applicationConfig = MutexConfigHolder.getApplicationConfig();
		NodeDetails nodeDetails = applicationConfig.getNodeDetails();
		List<ClientDetails> clientDetails = nodeDetails.getClientDetails();
		int count = 0;
		Map<String, Integer> row = myRepliesMap.row(file);
		
		for(Map.Entry<String, Integer> hm : row.entrySet()) {
			Integer value = hm.getValue();
			if(value > 0) {
				count++;
			}
		}
		return count == clientDetails.size() - 1 ? true: false;
	}
	
	public static void updateRepliesMap(String file, String opn) {
		Logger.debug("Updating replies map. Current status: " + myRepliesMap);
		Map<String, Integer> replyMap = myRepliesMap.row(file);
		for(Map.Entry<String, Integer> status: replyMap.entrySet()) {
			String host = status.getKey();
			int val = status.getValue();
			if(val == 0) {
				Logger.error("A violation of mutual exclusion during : " + opn + ", for file: " + file);
				continue;
			}
			if(!inDeferredMap(file, host)) {
				cachedReply.put(file, host, true);
			}
			else {
				cachedReply.put(file, host, false);
			}
			myRepliesMap.put(file, host, val - 1);
		}
		Logger.debug("After updating: " + myRepliesMap);
	}
	
	
	private static boolean inDeferredMap(String file, String host) {
		Map<String, ArrayList<String>> row = myDeferredReplies.row(file);
		if(row == null || row.isEmpty()) {
			return false;
		}
		for(Map.Entry<String, ArrayList<String>> hm: row.entrySet()) {
			ArrayList<String> value = hm.getValue();
			for(String temp: value) {
				String hostName = temp.split(MutexReferences.SEPARATOR)[0];
				if(host.equalsIgnoreCase(hostName)) {
					return true;
				}
			}
		}
		return false;
	}


	public static boolean isMyTimeStampLarger(String file, String opn, long timestamp) {
		
		Map<String, ArrayList<Long>> row = myRequestsMap.row(file);
		Long minTimeStamp = Instant.MAX.getEpochSecond();
		
		if(row == null || row.isEmpty()) {
			return true;
		}		
		else {
			for(Map.Entry<String, ArrayList<Long>> hm: row.entrySet()) {
				if(hm.getValue() != null && !hm.getValue().isEmpty() && hm.getValue().get(0) < minTimeStamp) {
					minTimeStamp = hm.getValue().get(0);
				}
			}
		}			
		Logger.debug("Request's timestamp: " + timestamp + ", and my minimum Timestamp: " + minTimeStamp + ", hence returning: " + (minTimeStamp > timestamp));
		return minTimeStamp > timestamp ? true: false;
	}
	
	public static void enterCriticalSection(String file, String opn) {
		Logger.info("Entering critical section for file: " + file + ", and opeation: " + opn);
		inCriticalSection.put(file, true);
	}
	
	public static void exitCriticalSection(String file) {		
		inCriticalSection.put(file, false);
		Logger.info("Exiting critical section for file: " + file );
	}
	
	/**
	 * Clearing my requests map.
	 * @param file File on which operation was being performed.
	 * @param opn Operation being performed.
	 */
	public static void clearMyRequestsMap(String file, String opn) {
		Logger.debug("Clearing the request map. Current status: " + myRequestsMap);
		ArrayList<Long> temp = myRequestsMap.get(file, opn);		
		if(temp == null) {
			return;
		}
		else if(temp.isEmpty()) {
			myRequestsMap.remove(file, opn);
		}
		else {
			temp.remove(0);
			myRequestsMap.put(file, opn, new ArrayList<Long>(temp));
		}
		Logger.debug("After clearing: " + myRequestsMap);
	}
	
	public static Table<String, String, ArrayList<Long>> getMyReqMap() {
		return myRequestsMap;
	}
	
	public static Table<String, String, Integer> getMyRepliesMap() {
		return myRepliesMap;
	}
	
	public static Table<String, String, ArrayList<String>> getDeferredReplies() {
		return myDeferredReplies;
	}
	
	public static void clearMyRequestsMap(String file, String opn, String timestamp) {
		ArrayList<Long> temp = myRequestsMap.get(file, opn);		
		if(temp == null) {
			return;
		}
		else if(temp.isEmpty()) {
			myRequestsMap.remove(file, opn);
		}
		else {
			temp.remove(Long.parseLong(timestamp));
			myRequestsMap.put(file, opn, new ArrayList<Long>(temp));
		}		
	}
	
	public static void clearDeferredRepliesMap(String file, String opn) {
				
		ArrayList<String> temp = myDeferredReplies.get(file, opn);
		if(temp == null) {
			return;
		}
		else if(temp.isEmpty()) {
			myDeferredReplies.remove(file, opn);
		}
		else {
			temp.remove(0);
			myDeferredReplies.put(file, opn, new ArrayList<String>(temp));
		}
	}

	public static boolean inCriticalSectionStatus(String file, String opn) {
		if(inCriticalSection.get(file) == null) {
			return false;
		}else {
			return inCriticalSection.get(file);
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
		port = Host.getPortNumber(address);
		Logger.info("Sending READ REPLY to node: " + address + ", port: " + port + ", for file: " + file + ", and operation: " + operation);
		Socket socket = null; 
	    DataOutputStream out = null;
	    try {
			socket = new Socket(address, port);			
	        out = new DataOutputStream(socket.getOutputStream());
	        out.writeUTF(OperationEnum.REPLY.toString() + MutexReferences.SEPARATOR_TEXT + file + MutexReferences.SEPARATOR_TEXT + operation);
	        socket.close();
//	        clearDeferredRepliesMap(file, operation);
	    }catch(Exception e) {
	    	throw new MutexException("Error while sending a REPLY for READ message to the client. Error: " + e);
	    }
	}
	
	public static void sendWriteReply(String address, int port, String file, String operation, String content) throws MutexException {
		port = Host.getPortNumber(address);
		Logger.info("Sending WRITE REPLY to node: " + address + " for file: " + file + " and operation: " + operation);
		Socket socket = null; 
	    DataOutputStream out = null;
	    try {
			socket = new Socket(address, port);			
	        out = new DataOutputStream(socket.getOutputStream());
	        out.writeUTF(OperationEnum.REPLY.toString() + MutexReferences.SEPARATOR_TEXT + file + MutexReferences.SEPARATOR_TEXT + operation + MutexReferences.SEPARATOR_TEXT + content);
//	        clearDeferredRepliesMap(file, operation);
	        socket.close();
	    }catch(Exception e) {
	    	throw new MutexException("Error while sending a REPLY for WRITE message to the client. Error: " + e);
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
				Logger.info("Successfully updated all replicas of file: " + file);
			}
		}		
	}

	public static void sendDeferredReply(String file) {
		
		Logger.debug("My current deferred repiles map: " + myDeferredReplies);
		
		if(!myDeferredReplies.containsRow(file) || myDeferredReplies.row(file).isEmpty()) {
			Logger.debug("No deferred replies to send.");
			return;
		}		
		else {
			try {
				Logger.debug("Start sending all deferred replies for: " + file);
				Map<String, ArrayList<String>> row = myDeferredReplies.row(file);
				ArrayList<ArrayList<String>> deferredRepliesToDelete = new ArrayList<ArrayList<String>>();
				for(Map.Entry<String, ArrayList<String>> hm: row.entrySet()) {
					ArrayList<String> arrayList = hm.getValue();
					for(String pipedString: arrayList) {
						String[] input = pipedString.split(MutexReferences.SEPARATOR);
						String host = input[0];
						int port = (Integer.parseInt(input[1]));
						String opn = input[2];
						
						if(OperationEnum.READ.toString().equalsIgnoreCase(opn)) {					
							sendReadReply(host, port, file, opn);
							ArrayList<String> temp = new ArrayList<String>();
							temp.add(file);
							temp.add(opn);
							deferredRepliesToDelete.add(new ArrayList<String>(temp));
						}
						else {			
							String content = input[5];
							sendWriteReply(host, port, file, opn, content);
							ArrayList<String> temp = new ArrayList<String>();
							temp.add(file);
							temp.add(opn);
							deferredRepliesToDelete.add(new ArrayList<String>(temp));
						}
					}
				}
				
				for(ArrayList<String> deferredReplies: deferredRepliesToDelete) {
					String file2 = deferredReplies.get(0);
					String opn2 = deferredReplies.get(1);
					clearDeferredRepliesMap(file2, opn2);
				}
				deferredRepliesToDelete.clear();
				Logger.debug("After sending my deferred replies, map: " + myDeferredReplies);
			}catch(Exception e) {
				Logger.error("Error while sending deferred replies: " + e);
			}
		}
	}
}







