package edu.utd.aos.mutex.utils;

import org.tinylog.Logger;

public class ClientOperationGenerator extends Thread {
	
	
	@Override
	public void run() {
		while(true) {
			String operation = Operation.generateRequest();
			Thread t = new ClientRequestSender(operation);
			t.start();
			try {
				Thread.sleep(10000);
			} catch (InterruptedException e) {
				Logger.error("Error while thread sleep: " + e);
			}
		}
	}
}
