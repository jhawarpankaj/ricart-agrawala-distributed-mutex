package edu.utd.aos.mutex.utils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import edu.utd.aos.mutex.references.MutexReferences;
import edu.utd.aos.mutex.references.OperationEnum;

public class Operation {
	
	/**
	 * key is Filename and Value is list of requests timestamp.
	 */
	private static Map<String, ArrayList<Long>> myRequestsMap = new HashMap<String, ArrayList<Long>>();
	
	/**
	 * Key is file name and values is count of the replies.
	 */
	private static Map<String, Integer> myRepliesMap = new HashMap<String, Integer>();
	
	/**
	 * Key is hostname and value is list of files.
	 */
	private static Map<String, ArrayList<String>> myDeferredReplies = new HashMap<String, ArrayList<String>>();
	
	/**
	 * To check if currently in critical section.
	 */
	private static boolean inCriticalSection = false;
	
	/**
	 * @return formatted string for requests.
	 */
	public static String generateRequest() {
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
			result = result + operation + MutexReferences.SEPARATOR + file + MutexReferences.SEPARATOR + now;
		}
		else {			
			String content = "<" + Host.getId() + ", " + now + ">"; 			
			result = result + operation + MutexReferences.SEPARATOR + file + MutexReferences.SEPARATOR + now + MutexReferences.SEPARATOR + content;
		}
		setMyRequestsMap(file, now);
		return result;
	}

	/**
	 * Set/Update the requests map.
	 * @param file File to access.
	 * @param timestamp Current time.
	 */
	private static void setMyRequestsMap(String file, long timestamp) {		
		if(myRequestsMap.containsKey(file)) {
			ArrayList<Long> temp = myRequestsMap.get(file);
			temp.add(timestamp);
			myRequestsMap.put(file, new ArrayList<Long>(temp));
		}
		else {
			ArrayList<Long> temp = new ArrayList<Long>();
			temp.add(timestamp);
			myRequestsMap.put(file, new ArrayList<Long>(temp));
		}		
	}
	
	/**
	 * Set/Update the replies map on receiving replies from other clients.
	 * 
	 * @param response
	 */
	public static void setMyRepliesMap(String response) {
		String[] temp = response.split(MutexReferences.SEPARATOR);
		int val = myRepliesMap.getOrDefault(temp[0], 0);
		myRepliesMap.put(temp[0], val + 1);
	}
	
	/**
	 * Set/Update deferred map.
	 * @param request
	 */
	public static void setMyDeferredRepliesMap(String request) {
		String[] req = request.split(MutexReferences.SEPARATOR);
		String hostname = req[0];
		String file = req[1];
		ArrayList<String> listOfDeferred = myDeferredReplies.getOrDefault(hostname, new ArrayList<String>());
		listOfDeferred.add(file);
		myDeferredReplies.put(hostname, new ArrayList<String>(listOfDeferred));		
	}
	
	/**
	 * @return replies Map.
	 */
	public static Map<String, Integer> getRepliesMap() {
		return myRepliesMap;
	}
	
	/**
	 * @param file
	 * @return no of replies for any file.
	 */
	public static int getRepliesCount(String file) {
		return myRepliesMap.getOrDefault(file, 0);
	}
	
	/**
	 * @return the requests map.
	 */
	public static Map<String, ArrayList<Long>> getMyRequestsMap(){
		return myRequestsMap;
	}
	
	public static boolean isMyTimeStampLarger(String file, long timestamp) {
		Map<String, ArrayList<Long>> myReqMap = getMyRequestsMap();
		if(!myReqMap.containsKey(file)) {
			return true;
		}
		else {
			ArrayList<Long> temp = myReqMap.get(file);
			if(temp.isEmpty()) {
				return true;
			}
			else {
				Long myTimestamp = temp.get(0);
				return myTimestamp > timestamp ? true: false;
			}
		}
	}
	
	/**
	 * @return My Deferred Map.
	 */
	public static Map<String, ArrayList<String>> getMyDeferredMap(){
		return myDeferredReplies;
	}
	
	public static void enterCriticalSection() {
		inCriticalSection = true;
	}
	
	public static void exitCriticalSection() {
		inCriticalSection = false;
	}
	
	public static boolean getCriticalSectionStatus() {
		return inCriticalSection;
	}
}
