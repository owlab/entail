package com.enxime.entail.client;

import java.nio.ByteBuffer;
import java.util.logging.Logger;

import com.enxime.entail.share.LogUtil;

public class ClientProtocolParserContext {
    private static final Logger  _logger = LogUtil.getLogger(ClientProtocolParserContext.class.getName());
	private ClientProtocolParser sizeParser;
	private ClientProtocolParser objectParser;
	
	private volatile ClientProtocolParser state;
	
	private volatile int count = 0;
	private volatile long startTime = 0;
	
	public ClientProtocolParserContext(ClientProtocolListener protocolListener) {
		// initialize state to StateHeaderRead
		this.sizeParser = new ClientProtocolSizeParser();
		this.objectParser = new ClientProtocolObjectParser(protocolListener);
		this.state = this.sizeParser;
	}
	
	public ClientProtocolParser changeParser() {
		if(this.state instanceof ClientProtocolSizeParser)
			this.state = this.objectParser;
		else
			this.state = this.sizeParser;
		
		return this.state;
	}
	
	/**
	 * @param newState
	 */
//	public void setState(ClientProtocolParser newState) {
//		this.state = newState;
//	}
	
	/**
	 * @param dataBuffer 
	 * @throws InvalidDataException 
	 */
	public void handleData(ByteBuffer dataBuffer) throws InvalidDataException {
	    if(this.count == 0) {
		this.startTime = System.currentTimeMillis();
	    }
		if(dataBuffer.remaining() > 0)
			this.state.handleData(this, dataBuffer);
		
		this.count++;
		if(this.count == 1000) {
		    _logger.finer("time for " + this.count + " processing: " + (System.currentTimeMillis() - this.startTime) + "ms");
		    this.count = 0;
		}
	}
}
