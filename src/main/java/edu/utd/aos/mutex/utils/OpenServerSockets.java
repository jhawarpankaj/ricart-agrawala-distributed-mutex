package edu.utd.aos.mutex.utils;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import org.tinylog.Logger;

import edu.utd.aos.mutex.exception.MutexException;

public class OpenServerSockets extends Thread {
	
	@Override
	public void run() {
		ServerSocket serverSocket = null;
		try {
			serverSocket = new ServerSocket(Integer.parseInt(Host.getLocalPort()));
			while(true) {
				Socket clientSocket = null;
				clientSocket = serverSocket.accept();
				DataInputStream dis = new DataInputStream(clientSocket.getInputStream());
	            DataOutputStream dos = new DataOutputStream(clientSocket.getOutputStream());
	            Thread t = new ClientResponder(clientSocket, dis, dos);
	            t.start();
			}
		}catch(Exception e) {
			Logger.error("Error on client socket(acting as server): " + e);
		}
	}

}
