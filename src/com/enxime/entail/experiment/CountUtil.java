package com.enxime.entail.experiment;

public class CountUtil {
	public static String countName;
	public static volatile long count;
	public static long startTime;
	public static long duration;

	public static void start(String name) {
		countName = name;
		count = 0;
		startTime = System.currentTimeMillis(); 
	}
	
	public static void increment() {
		count++;
	}
	
	public static void incrementUntil(long limit) {
		if(count < limit) {
			count++;
		} else {
			stop();
		}
	}
	
	public static void stop() {
		duration = System.currentTimeMillis() - startTime;
		System.out.println("[Count Result] name = " + countName + ", count = " + count + ", duration = " + duration + " ms.");
	}
}
