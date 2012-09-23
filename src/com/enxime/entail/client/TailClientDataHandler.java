package com.enxime.entail.client;


import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

import com.enxime.entail.share.LogUtil;

public class TailClientDataHandler implements Runnable {
	private static final Logger _logger = LogUtil.getLogger(TailClientDataHandler.class.getName());
	
	/*
	 *  For JVM 1.4
	 */
	private List<ClientDataEvent> queue = new LinkedList<ClientDataEvent>();
	
	/*
	 * For Java 1.5 and above
	 */
	//private ConcurrentLinkedQueue<ClientDataEvent> queue = new ConcurrentLinkedQueue<ClientDataEvent>(); 
	
	
	private TailClient client;
	private ClientProtocolParserContext readStateContext;
	
	private volatile boolean stop = false;
	
	// this is for performance measure
	private volatile int count = 0;
	private volatile long startTime = 0;
	
	public TailClientDataHandler(ClientProtocolListener protocolListener) {
		this.readStateContext = new ClientProtocolParserContext(protocolListener);
	}

	public void processData(TailClient client, SocketChannel socket, final byte[] data, int count) {
		_logger.finer("called.");
		if(this.count == 0) {
		    this.startTime = System.currentTimeMillis();
		}
		
		if(this.client == null)
			this.client = client;
		
		byte[]  dataCopy = new byte[count];
		System.arraycopy(data, 0, dataCopy, 0, count);
		/*
		 * For JVM 1.4
		 */
		synchronized(queue) {
			queue.add(new ClientDataEvent(client, socket, dataCopy));
			queue.notify();
		
		/*  Java 1.5 or above
		 *  this.queue.offer(new ClientDataEvent(client, socket, dataCopy));
		 */
//		this.count++;
//		if(this.count == 1000) {
//		   _logger.fine("time for " + this.count + " processing: " + (System.currentTimeMillis() - this.startTime) + "ms");
//		    this.count = 0;
		}
	}
	@Override
	public void run() {
		_logger.fine("called.");
		
		ClientDataEvent clientDataEvent = null;
		boolean isInterrupted = false;
		while(!stop) {
			
			 /* For JVM 1.4
			 * 
			 */
		    synchronized(queue) {
				while(queue.isEmpty()) {
					try {
						queue.wait(1000);
					} catch(InterruptedException e) {
						_logger.fine("Interrupted.");
						Thread.currentThread().interrupt();
						isInterrupted = true;
						break;
					}
				}
				if(queue.size() > 0) {
				    //_logger.fine("queue size=" + queue.size());
					clientDataEvent = queue.remove(0);
				}
			}
		   /* Java 1.5 and above
		    * clientDataEvent = this.queue.poll();
		    */
		   if(clientDataEvent != null) {
				try {
					readStateContext.handleData(ByteBuffer.wrap(clientDataEvent.data));
				} catch (InvalidDataException e) {
					// TODO Auto-generated catch block
					//e.printStackTrace();
					_logger.severe(e.getMessage());
					break;
				}
		   }
		   if(isInterrupted)
				break;
		}
		
		_logger.fine("Thread stopped.");
	}
	
	public void stop() {
		_logger.fine("called.");
		
		this.stop = true;
	}

}
