package com.edu.utd.mutex.tests;

import java.util.ArrayList;
import java.util.Random;

public class RandomTest {
	public static void main(String[] args) throws InterruptedException {
		Random rand = new Random();
		ArrayList<String> arr = new ArrayList<String>();
		arr.add("1");
		arr.add("2");
		arr.add("3");
		while(true){
			System.out.println(arr.get(rand.nextInt(arr.size())));
			Thread.sleep(1000);
		}
		
	}
}
