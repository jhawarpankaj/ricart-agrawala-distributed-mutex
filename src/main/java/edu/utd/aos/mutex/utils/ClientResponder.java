package edu.utd.aos.mutex.utils;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

import org.tinylog.Logger;

import edu.utd.aos.mutex.exception.MutexException;
import edu.utd.aos.mutex.references.MutexReferences;
import edu.utd.aos.mutex.references.OperationEnum;

/**
 * To respond to fellow clients requests.
 * @author pankaj
 *
 */
public class ClientResponder extends Thread {
    final Socket worker; //request came from this worker.
	final DataInputStream dis;
    final DataOutputStream dos;
    
    public ClientResponder(Socket worker, DataInputStream dis, DataOutputStream dos) {
    	this.worker = worker;
    	this.dis = dis;
    	this.dos = dos;
    }
    
    @Override
    public void run(){
    	try {
    		
	    	String received = dis.readUTF();
	    	Logger.debug("Received input raw string: " + received);
	    	String[] input = received.split(MutexReferences.SEPARATOR);
	    	Logger.debug("Received input array: " + input);
	    	OperationEnum operation = null;
	    	try {
	    		operation = OperationEnum.valueOf(input[0].toUpperCase());
	    	}catch(IllegalArgumentException e) {
	    		Logger.error("Operation not supported. Error: " + e);
	    	}
	    	
	    	switch(operation) {
		    	case REPLY:
		    		String host = worker.getInetAddress().getHostName();		    		
		    		String file = input[1];
		    		String readWriteOpn = input[2];
		    		Logger.info("Got REPLY from client: " + host + ", for file: " + file + ", and operation: " + readWriteOpn);
		    		synchronized(ClientResponder.class) {
			    		Operation.setMyRepliesMap(file, readWriteOpn, host);
			    		if(Operation.gotRequiredReplies(file, readWriteOpn)) {
			    			Logger.info("Got all required REPLIES to enter critical section for file: " + file + ", and operation: " + readWriteOpn);
			    			Operation.enterCriticalSection(file, readWriteOpn);
			    			Operation.executeCriticalSection(input);
			    			Operation.clearMyRequestsMap(file, readWriteOpn);
			    			Operation.updateRepliesMap(file, readWriteOpn);
			    			Operation.exitCriticalSection(file);		    			
			    			Operation.sendDeferredReply(file);
			    		}
		    		}
		    		break;
		    	case WRITE:
		    	case READ:
		    		synchronized(ClientResponder.class) {
		    			performReadWriteOperation(input);
		    		}		    		
		    		break;
		    	default:
		    		Logger.error("Illegal operation, not known to client.");
		    		break;
	    	}    	
    	}catch(IOException e) {
    		Logger.error("Error while reading/writing message from/to client." + e);
    	}
    }

	/**
	 *  To perform RAD/WRITE requests of fellow clients.
	 * @param input Input string from fellow client.
	 */
	private void performReadWriteOperation(String[] input) {
		String host = worker.getInetAddress().getHostName();
		int port = Host.getPortNumber(host);
		String operation = input[0].toString();
		String file = input[1];
		String content = null;
		long timestamp = Long.parseLong(input[2]);
		
		Logger.info("Got " + operation + " request from client: " + host + ", for file: " + file + ", and timestamp: " + timestamp);
		Boolean isCached = Operation.cachedReply.get(file, host);
		Boolean alreadyGeneratedReq = Operation.alreadyGeneratedRequest.get(file, host);
		boolean temp = false;
		if(isCached == null || alreadyGeneratedReq == null) {
			temp = false;
		}
		else {
			temp = isCached && alreadyGeneratedReq;
		}
		Operation.cachedReply.put(file, host, false);
		if(!Operation.inCriticalSectionStatus(file, operation) && Operation.isMyTimeStampLarger(file, operation, timestamp) && !temp) {
			try {
				if(OperationEnum.READ.toString().equalsIgnoreCase(operation)) {
					Operation.sendReadReply(host, port, file, operation);
				}
				else {
					content = input[3];
					Operation.sendWriteReply(host, port, file, operation, content);
				}
				
			} catch (MutexException e) {
				Logger.error("Error while sending reply message to node: " + worker.getInetAddress().getHostName() + ", for file:" + file + ".Error: " + e);							
			}
		}
		
		else {
			Logger.info("Deferring REPLY for: " + operation + ", for file: " + file + ", and host: " + host);
			Logger.debug("My Request map: " + Operation.getMyReqMap());
			Logger.debug("My Replies map: " + Operation.getMyRepliesMap());
			Operation.setMyDeferredRepliesMap(input, host, port);			
		}
		
	}  

}
