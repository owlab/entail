package com.enxime.entail.server;

import java.nio.ByteBuffer;

public class ServerProtocolSizeParser implements ServerProtocolParser {
	private ServerProtocolListener protocolListener;
	private ByteBuffer headerBuffer = ByteBuffer.allocate(4);
	private int remaining;
	
	public ServerProtocolSizeParser(ServerProtocolListener protocolListener) 
	{
		this.protocolListener = protocolListener;
		this.reset(4);
	}

	public void reset(int size) {
		//this.headerBuffer = ByteBuffer.allocate(size);
		this.headerBuffer.clear();
		this.remaining = this.headerBuffer.remaining();
	}
	
	/* (non-Javadoc)
	 * @see com.enxime.entail.experiment.State#handleData(com.enxime.entail.experiment.StateContext, java.nio.ByteBuffer)
	 */
	@Override
	public void handleData(ServerProtocolParserContext context, ByteBuffer byteBuffer) 
	{
		// If assuming the byteBuffer was used for writing
		
		
		if(this.headerBuffer.remaining() > byteBuffer.remaining())
		{
			this.headerBuffer.put(byteBuffer);
			this.remaining = this.headerBuffer.remaining();
		}
		else
		{
			for(int i= 0; i < this.remaining; i++)
				this.headerBuffer.put(byteBuffer.get()); // so, byteBuffer has remaining data
			
			this.headerBuffer.flip();
			int objectSize = this.headerBuffer.getInt();
			
			// Change state to ObjectRead
			context.changeParser().reset(objectSize);
			
			context.handleData(byteBuffer);
		}
		

	}
}
