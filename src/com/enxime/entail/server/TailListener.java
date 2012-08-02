package com.enxime.entail.server;


public interface TailListener {
	public void init(TailWorker tail);
	public void fileNotFound();
	public void fileRotated();
	public void handleLine(String line);
	public void handleLines(String[] lines);
	public void handleException(Exception ex);

}
