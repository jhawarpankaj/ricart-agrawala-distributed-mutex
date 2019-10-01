package com.edu.utd.mutex.tests;

import java.util.ArrayList;

public class StringJoin {

	public static void main(String[] args) {
		ArrayList<String> a = new ArrayList<String>();
		a.add("A");
		a.add("B");
		String c = String.join("||", a);
		System.out.println(c);

	}

}
