package com.enxime.entail.server;


public interface TailListener {
	public void init(TailWorker tail);
	public void fileNotFound();
	public void fileRotated();
	//public void prepareHandleLine();//
	public void handleLine(String line);
	//public void finishHandleLine(); // call after multiple handleLine calling
	public void handleLines(String[] lines);
	public void handleLines(String[] lines, int startIndex, int length);
	public void handleException(Exception ex);
	
}
