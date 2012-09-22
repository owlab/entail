package com.enxime.entail.experiment;

public class CountUtil {
	public String countName;
	public volatile long count;
	public long startTime;
	public long duration;

	public void start(String name) {
		countName = name;
		count = 0;
		startTime = System.currentTimeMillis(); 
	}
	
	public  void increment() {
		count++;
	}
	
	public void incrementUntil(long limit) {
		if(count < limit) {
			count++;
		} else {
			stop();
		}
	}
	
	public void stop() {
		duration = System.currentTimeMillis() - startTime;
		System.out.println("[Count Result] name = " + countName + ", count = " + count + ", duration = " + duration + " ms.");
	}
}
