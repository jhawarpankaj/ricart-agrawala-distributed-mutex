package edu.utd.aos.mutex.references;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Constants and references used across project.
 * 
 * @author pankaj
 *
 */
public class MutexReferences {

	/**
	 * System Property that provides URI for the config.
	 */
	public static final String KEY_MUTEX_CONFIG = "mutex.config";
	
	/**
	 * Exit code for errors and exception.
	 */
	public static final int CONST_CODE_ERROR = 1;
	
	/**
	 * Separator used across message transfer between nodes. Backslash for Regex Expression.
	 */
	public static final String SEPARATOR = "\\|\\|";
	
	/**
	 * Separator.
	 */
	public static final String SEPARATOR_TEXT = "||";
	
	/**
	 * Constant encoding to be used.
	 */
	public static final Charset ENCODING = StandardCharsets.UTF_8;
	
	/**
	 * To wait until all client start.
	 */
	public static boolean firstRequest = true;
	
	/**
	 * Private constructor for utility class.
	 */
	private MutexReferences() {
		
	}
}
