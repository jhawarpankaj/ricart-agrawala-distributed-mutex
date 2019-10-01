package edu.utd.aos.mutex.utils;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.Charsets;
import org.apache.commons.io.FileUtils;
import org.tinylog.Logger;

import edu.utd.aos.mutex.dto.ApplicationConfig;
import edu.utd.aos.mutex.references.MutexConfigHolder;
import edu.utd.aos.mutex.references.MutexReferences;
import edu.utd.aos.mutex.references.OperationEnum;
import org.apache.commons.io.input.*;

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
		    		String fileName = input[1];
		    		File file = new File(Host.getFilePath() + fileName);
		    		ReversedLinesFileReader fileReader = new ReversedLinesFileReader(file, MutexReferences.ENCODING);
		    		String lastLine = fileReader.readLine();
		    		dos.writeUTF(lastLine);
		    		break;
		    	case WRITE:
		    		String fileToUpdate = input[1];
		    		String data = "\n" + input[2] + "\n";
		    		File fileWrite = new File(Host.getFilePath() + fileToUpdate);
		    		FileUtils.writeStringToFile(fileWrite, data, MutexReferences.ENCODING, true);
		    		dos.writeUTF(Host.getLocalHost());
		    		break;
		    	case ENQUIRY:
		    		Logger.info("Got ENQUIRY request from client: " + worker.getInetAddress().getHostName());
		    		String listOfFiles = String.join(MutexReferences.SEPARATOR_TEXT, Host.getListOfFiles());
		    		Logger.info("Responding client: " + worker.getInetAddress().getHostName() + ", with details: " + listOfFiles);
		    		dos.writeUTF(listOfFiles);
		    		break;
				default:
					Logger.error("Illegal Operation type.");
					break;
	    	}
    	}catch(IOException e) {
    		Logger.error("Error while reading/writing message from/to client." + e);
    	}
    }    	
}
