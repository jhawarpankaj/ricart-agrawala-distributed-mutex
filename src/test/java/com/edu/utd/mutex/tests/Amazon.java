package com.edu.utd.mutex.tests;
import java.util.*;
public class Amazon {

	public static void main(String[] args) {
		String s = "This is a sample input which you could not solve for Chitni's exam. He had a cheese cake. Cheese cake's was good.";
		ArrayList<String> excludedList = new ArrayList<String>(Arrays.asList("is", "a", "had", "was"));		
		String replaced = s.replaceAll("[.']", " ");
		String removedSpaces = replaced.replaceAll("\\s+", " ");
		String[] s1 = removedSpaces.split(" ");
		ArrayList<String> spaces = new ArrayList<String>(Arrays.asList(s1));
		spaces.removeAll(excludedList);
		List<String> res = new ArrayList<String>();
		HashMap<String, Integer> hm = new HashMap<String, Integer>();
		int max = -1;		
		for(String aa: spaces) {
			int val = hm.getOrDefault(aa.toLowerCase(), 0);
			hm.put(aa.toLowerCase(), val + 1);
			if(max < val + 1) {
				max = val + 1;
			}
		}
		for(Map.Entry<String, Integer> a: hm.entrySet()) {
			if(a.getValue() == max) {
				res.add(a.getKey());
			}
		}
		System.out.println(res);
	}
}
