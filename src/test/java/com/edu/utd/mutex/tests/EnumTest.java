package com.edu.utd.mutex.tests;

public class EnumTest {

	public static void main(String[] args) {
		String val = "reds";
		EnumTests obj = null;
		obj = EnumTests.valueOf(val.toUpperCase());

		

			switch(obj) {
			case RED:
				System.out.println("This is a red color");
				break;
				
			case BLUE:
				System.out.println("This is a blue color");
				break;
				
			default:
				System.out.println("Color is obj" + obj.toString());
			}


}
}
