package com.enxime.entail.client.ui;

import java.io.IOException;

import com.enxime.entail.client.TailClient;

public class Pingger implements Runnable {

    private TailClientController tailClientController;
    private volatile boolean run = true;
    
    public Pingger(TailClientController tailClientController) {
	this.tailClientController = tailClientController;
    }
    @Override
    public void run() {
	// TODO Auto-generated method stub
	
	while(run) {
	    try {
		Thread.sleep(1000);
	    } catch (InterruptedException e) {
		// TODO Auto-generated catch block
		// e.printStackTrace();
	    }
	    
	    try {
		tailClientController.ping();
	    } catch (IOException e) {
		// TODO Auto-generated catch block
		// e.printStackTrace();
	    }
	    
	}

    }
    
    public void stop() {
	this.run = false;
    }

}
