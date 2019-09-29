package edu.utd.aos.mutex.utils;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.tinylog.Logger;

import edu.utd.aos.mutex.dto.ApplicationConfig;
import edu.utd.aos.mutex.dto.NodeDetails;
import edu.utd.aos.mutex.dto.ServerDetails;
import edu.utd.aos.mutex.exception.MutexException;
import edu.utd.aos.mutex.references.MutexConfigHolder;

/**
 * Util class for replicating files across all servers.
 * 
 * @author pankaj
 */
public class Files {
	
	public static void createFileReplicaOnServer() throws MutexException {
		Logger.info("Creating replica of files on the server.");
		ApplicationConfig applicationConfig = MutexConfigHolder.getApplicationConfig();
		NodeDetails nodeDetails = applicationConfig.getNodeDetails();
		List<ServerDetails> serverDetails = nodeDetails.getServerDetails();
		List<String> filesList = applicationConfig.getListOfFiles();
		String localhost = Host.getLocalHost();
		Logger.info("Localhost: " + localhost);
		
		try {
			for(ServerDetails server: serverDetails) {
				
				if (localhost.equalsIgnoreCase(server.getName())) {
					Logger.info("Copying all files on the server.");
					String uniqueDestToCopyFile = server.getFilePath() + server.getId();
					Logger.info("Path to copy file for " + localhost + ": " + uniqueDestToCopyFile);
					File destDirectory = new File(uniqueDestToCopyFile);
					FileUtils.deleteDirectory(destDirectory);
					FileUtils.forceMkdir(destDirectory);
					for(String fileToCopy: filesList) {
						File sourceFileToCopy = new File(fileToCopy);					
						FileUtils.copyFileToDirectory(sourceFileToCopy, destDirectory);
					}
					Host.setListOfFiles();
					Logger.info("Copied all files on the server: " + localhost);
					break;
				}
			}
		}catch(IOException e) {			
			throw new MutexException("Error while creating file replicas on the server." + e);	
	}
}
	
	/**
	 * Constructor for utility class.
	 */
	private Files() {
		
	}
}
