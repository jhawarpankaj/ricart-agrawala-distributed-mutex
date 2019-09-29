package com.edu.utd.mutex.tests;

public class SwitchTest {
	public static void main(String[] args) {
		String q = "b1";
		switch(q) {
		case "b1":
			System.out.println("B1");
		case "c1":
		case "a1":
			System.out.println("A1");
		}
	}
}
