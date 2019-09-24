package edu.utd.aos.mutex.core;
import java.util.List;

import org.tinylog.Logger;

import edu.utd.aos.mutex.dto.ApplicationConfig;
import edu.utd.aos.mutex.dto.NodeDetails;
import edu.utd.aos.mutex.dto.ServerDetails;
import edu.utd.aos.mutex.exception.MutexException;
import edu.utd.aos.mutex.references.MutexConfigHolder;
import edu.utd.aos.mutex.references.MutexReferences;
import edu.utd.aos.mutex.utils.Host;
import edu.utd.aos.mutex.utils.Files;

public class Main {

	public static void main(final String[] args) {
		
		try {
			initialize();
			setup();
		}catch(final Exception e) {
			Logger.error("Error: " + e);
			System.exit(MutexReferences.CONST_CODE_ERROR);
		}
	}

	private static void initialize() throws MutexException {
		MutexConfigHolder.initialize();
		Host.initialize();
	}

	private static void setup() throws MutexException {		
		Files.createFileReplicaOnServer();
	}
}