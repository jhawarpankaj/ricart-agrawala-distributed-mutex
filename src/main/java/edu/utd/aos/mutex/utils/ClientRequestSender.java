package edu.utd.aos.mutex.utils;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.util.ArrayList;

import org.tinylog.Logger;

public class ClientRequestSender extends Thread {
	
	String operation;
	
	public ClientRequestSender(String operation) {
		this.operation = operation;
	}
	
	@Override
	public void run() {
		ArrayList<String[]> allOtherClients = Host.getAllOtherClients();
		for(String[] client: allOtherClients) {
			String address = client[0];
			int port = Integer.parseInt(client[1]);
			Socket socket = null; 
		    DataInputStream input = null; 
		    DataOutputStream out = null;
		    try {
				socket = new Socket(address, port);
				Logger.info("Requesting client: " + address + " to perform operation: " + operation);
				input = new DataInputStream(socket.getInputStream());
		        out = new DataOutputStream(socket.getOutputStream());
		        out.writeUTF(operation);
		        socket.close();
		    }catch(Exception e) {
		    	Logger.error("Error while sending request to client: " + address + ". Error: " + e);
		    }		        
		}
	}
}
