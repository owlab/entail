package com.enxime.entail.server;

import java.nio.ByteBuffer;
import java.util.logging.Logger;

import com.enxime.entail.share.LogUtil;

public class ServerProtocolParserContext {
    private static final Logger _logger = LogUtil.getLogger(ServerProtocolParserContext.class.getName());
    	
	private volatile ServerProtocolParser parser;
	private ServerProtocolSizeParser sizeParser;
	private ServerProtocolObjectParser objectParser;
	
	//private static ConcurrentHashMap<SocketChannel, ServerProtocolParserContext> protocolParserMap = new ConcurrentHashMap<SocketChannel, ServerProtocolParserContext>();
	
	public ServerProtocolParserContext(ServerProtocolListener protocolListener) {
		this.sizeParser = new ServerProtocolSizeParser(protocolListener);
		this.objectParser = new ServerProtocolObjectParser(protocolListener);
		
		// initialize state to StateHeaderRead
		this.parser = this.sizeParser;
		

	}
	
	public ServerProtocolParser changeParser() {
		if(this.parser instanceof ServerProtocolSizeParser) {
			this.parser = this.objectParser;
		} else if(this.parser instanceof ServerProtocolObjectParser) {
			this.parser = this.sizeParser;
		}
		return this.parser;
	}
	
	public ServerProtocolParser getParser() {
		return this.parser;
	}
	/**
	 * @param newState
	 */
//	public void setState(ServerProtocolParser newState) {
//		this.state = newState;
//	}
	
	/**
	 * @param dataBuffer 
	 */
	public void handleData(final ByteBuffer dataBuffer) {
	    _logger.fine("byteBuffer capacity: " + dataBuffer.capacity());
	    _logger.fine("byteBuffer limit: " + dataBuffer.limit());
	    _logger.fine("byteBuffer position: " + dataBuffer.position());
	    _logger.fine("byteBuffer remaining: " + dataBuffer.remaining());
	    
		if(dataBuffer.hasRemaining()) {
		    _logger.fine("dataBuffer has remaining.");
		    this.parser.handleData(this, dataBuffer);
		}
	}
}
