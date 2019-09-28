package edu.utd.aos.mutex.utils;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

import org.tinylog.Logger;

import edu.utd.aos.mutex.dto.ApplicationConfig;
import edu.utd.aos.mutex.references.MutexConfigHolder;
import edu.utd.aos.mutex.references.MutexReferences;
import edu.utd.aos.mutex.references.OperationEnum;

public class ClientResponder extends Thread {
    final Socket worker;
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
	    	String[] input = received.split(MutexReferences.SEPARATOR);
	    	OperationEnum operation = null;
	    	try {
	    		operation = OperationEnum.valueOf(input[0].toUpperCase());
	    	}catch(IllegalArgumentException e) {
	    		Logger.error("Operation not supported. Error: " + e);
	    	}
	    	
	    	switch(operation) {
		    	case REPLY:
		    		break;	    		
		    	case WRITE:
		    		break;	    		
		    	case READ:
		    		String opn = input[0];
		    		String file = input[1];
		    		long timestamp = Long.parseLong(input[2]);
		    		if(!Operation.getCriticalSectionStatus()) {
		    			
		    		}
		    		Logger.info("Got ENQUIRY request from client: " + worker.getInetAddress().getHostName());
		    		ApplicationConfig applicationConfig = MutexConfigHolder.getApplicationConfig();
		    		String listOfFiles = String.join(MutexReferences.SEPARATOR, applicationConfig.getListOfFiles());
		    		Logger.info("Responding client: " + worker.getInetAddress().getHostName() + ", with details: " + listOfFiles);
		    		dos.writeUTF(listOfFiles);
		    		break;
	    	}    	
    	}catch(IOException e) {
    		Logger.error("Error while reading/writing message from/to client." + e);
    	}
    }  

}
