package edu.utd.aos.mutex.utils;

import java.io.DataOutputStream;
import java.net.Socket;
import java.util.ArrayList;

import org.tinylog.Logger;

import edu.utd.aos.mutex.references.MutexReferences;

public class ClientRequestSender extends Thread {
	
	String operation;
	
	public ClientRequestSender(String operation) {
		this.operation = operation;
	}
	
	@Override
	public void run() {
		ArrayList<ArrayList<String>> allOtherClients = Host.getAllOtherClients();
		for(ArrayList<String> client: allOtherClients) {
			String address = client.get(0);
			int port = Integer.parseInt(client.get(1));
			Logger.debug("Sending request to client: " + address + ", on port: " + port);
			Socket socket = null;
		    DataOutputStream out = null;
		    try {
				socket = new Socket(address, port);
				Logger.info("Requesting client: " + address + " to perform operation: " + operation);
		        out = new DataOutputStream(socket.getOutputStream());
		        out.writeUTF(operation);
		        socket.close();
		    }catch(Exception e) {

		    	String[] split = operation.split(MutexReferences.SEPARATOR);
		    	String opn = split[0];
		    	String file = split[1];
		    	String timestamp = split[2];
		    	Operation.clearMyRequestsMap(file, opn, timestamp);
		    	Logger.error("Error while sending request to client: " + address + ". Error: " + e);
		    }		        
		}
	}
}
