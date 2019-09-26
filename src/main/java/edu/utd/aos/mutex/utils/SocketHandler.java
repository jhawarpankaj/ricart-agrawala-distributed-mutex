package edu.utd.aos.mutex.utils;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.*;

import edu.utd.aos.mutex.exception.MutexException; 

/**
 * All socket related operations.
 * 
 * @author pankaj
 * 
 */
public class SocketHandler {
	
	public static void setup() throws MutexException{
		if(Host.isServer()) {
			serverSocketsetup();
		}
		else {
			clientSocketSetup();
		}
	}

	/**
	 * Create all client sockets.
	 */
	private static void clientSocketSetup() {
		
		
	}

	/**
	 * Create all server sockets.
	 * @throws MutexException 
	 */
	private static void serverSocketsetup() throws MutexException {
		try {
			ServerSocket serverSocket = new ServerSocket(Integer.parseInt(Host.getLocalPort()));
			while(true) {
				Socket clientSocket = null;
				clientSocket = serverSocket.accept();
				DataInputStream dis = new DataInputStream(clientSocket.getInputStream()); 
	            DataOutputStream dos = new DataOutputStream(clientSocket.getOutputStream());
	            Thread t = new ClientHandler(clientSocket, dis, dos);
	            t.start();
			}
		}catch(Exception e) {
			throw new MutexException("Error in server socket." + e);
		}

	}
}
