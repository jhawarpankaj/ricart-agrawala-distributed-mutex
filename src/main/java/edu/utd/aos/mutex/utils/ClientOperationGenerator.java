package edu.utd.aos.mutex.utils;

import org.tinylog.Logger;

import edu.utd.aos.mutex.references.MutexReferences;

public class ClientOperationGenerator extends Thread {
	
	
	@Override
	public void run() {
		while(true) {
			String operation = Operation.generateClientRequest();
			Thread t = new ClientRequestSender(operation);			
			try {
				if(MutexReferences.firstRequest) {
					MutexReferences.firstRequest = false;
					Thread.sleep(60000);
				}
				else {
					Thread.sleep(10000);
				}				
				t.start();
			} catch (InterruptedException e) {
				Logger.error("Error while thread sleep: " + e);
			}
		}
	}
}
