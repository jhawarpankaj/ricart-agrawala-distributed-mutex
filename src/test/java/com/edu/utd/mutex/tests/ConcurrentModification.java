package com.edu.utd.mutex.tests;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ConcurrentModification {

	public static void main(String[] args) throws InterruptedException {
		whilstRemovingDuringIteration_shouldThrowException();

	}
	
	public static void whilstRemovingDuringIteration_shouldThrowException() throws InterruptedException {
		 
	    List<Integer> integers = new ArrayList(Arrays.asList(1, 2, 3));
	 
	    for (Integer integer : integers) {
	        integers.remove(1);
	    }
	}
}
