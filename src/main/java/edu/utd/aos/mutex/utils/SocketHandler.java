package edu.utd.aos.mutex.utils;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.tinylog.Logger;

import edu.utd.aos.mutex.dto.ApplicationConfig;
import edu.utd.aos.mutex.dto.NodeDetails;
import edu.utd.aos.mutex.dto.ServerDetails;
import edu.utd.aos.mutex.exception.MutexException;
import edu.utd.aos.mutex.references.MutexConfigHolder;
import edu.utd.aos.mutex.references.MutexReferences;
import edu.utd.aos.mutex.references.OperationEnum; 

/**
 * All socket related operations.
 * 
 * @author pankaj
 * 
 */
public class SocketHandler {
	
	/**
	 * Setting up client or server sockets depending on type on node.
	 * @throws MutexException
	 */
	public static void setup() throws MutexException{
		if(Host.isServer()) {
			try {
				serverSocketsetup();
			} catch (IOException e) {
				throw new MutexException("Error while closing socket on server" + e);
			}
		}
		else {
			clientSocketSetup();
		}
	}

	/**
	 * Create all client sockets.
	 * @throws MutexException 
	 */
	private static void clientSocketSetup() throws MutexException {
		Logger.info("Setting up for client sockets.");
		cacheServerDetails();
		Thread t1 = new OpenServerSockets();
		t1.start();
		Thread t2 = new ClientOperationGenerator();
		t2.start();
	}

	/**
	 * To cache all file details.
	 * 
	 * @throws MutexException
	 */
	private static void cacheServerDetails() throws MutexException {
		Logger.info("Caching up file details from server for future.");
		ArrayList<String> aRandomServer = Host.getARandomServer();
		String address = aRandomServer.get(0);
		int port = Integer.parseInt(aRandomServer.get(1));
		
		Socket socket = null; 
	    DataInputStream input = null; 
	    DataOutputStream out = null;
	    
	    try {
			socket = new Socket(address, port);
			Logger.info("Connected to server: " + address + " to cache the details of all available files. Client: " + Host.getLocalHost());
			input = new DataInputStream(socket.getInputStream());
	        out = new DataOutputStream(socket.getOutputStream());
	        out.writeUTF(OperationEnum.ENQUIRY.toString());
	        String listOfFiles = input.readUTF();
	        Logger.info("Got response from server: " + Arrays.asList(listOfFiles.split(MutexReferences.SEPARATOR)));
	        Host.setCachedFiles(listOfFiles);
	        socket.close();
	    }catch(Exception e) {
	    	throw new MutexException("Error while getting file details from the server. Error" + e);
	    }
	    Logger.info("Cached all files.");
	}

	/**
	 * Create all server sockets.
	 * @throws MutexException 
	 * @throws IOException 
	 */
	private static void serverSocketsetup() throws MutexException, IOException {
		Logger.info("Setting up for server sockets.");
		ServerSocket serverSocket = null;
		try {
			serverSocket = new ServerSocket(Integer.parseInt(Host.getLocalPort()));
			while(true) {
				Socket clientSocket = null;
				clientSocket = serverSocket.accept();
				DataInputStream dis = new DataInputStream(clientSocket.getInputStream());
	            DataOutputStream dos = new DataOutputStream(clientSocket.getOutputStream());
	            Thread t = new ClientHandler(clientSocket, dis, dos);
	            t.start();
			}
		}catch(Exception e) {
			serverSocket.close();
			throw new MutexException("Error in server socket." + e);
		}
	}
}
