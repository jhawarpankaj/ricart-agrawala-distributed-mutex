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

public class ClientHandler extends Thread {
    final Socket worker;
	final DataInputStream dis;
    final DataOutputStream dos;
    
    public ClientHandler(Socket worker, DataInputStream dis, DataOutputStream dos) {
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
	    		Logger.error("Operation not supported by server." + e);
	    	}
	    	
	    	switch(operation) {
		    	case READ:
		    		break;	    		
		    	case WRITE:
		    		break;	    		
		    	case ENQUIRY:
		    		ApplicationConfig applicationConfig = MutexConfigHolder.getApplicationConfig();
		    		String listOfFiles = String.join(MutexReferences.SEPARATOR, applicationConfig.getListOfFiles());
		    		dos.writeUTF(listOfFiles);
		    		break;
	    	}    	
    	}catch(IOException e) {
    	Logger.error("Error while reading/writing message from/to client." + e);
    	}
    }
    	
}
