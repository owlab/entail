package com.enxime.entail.client;

import java.nio.ByteBuffer;
import java.util.logging.Logger;

import com.enxime.entail.share.LogUtil;

public class ClientProtocolSizeParser implements ClientProtocolParser {
    private static final Logger _logger = LogUtil.getLogger(ClientProtocolSizeParser.class.getName());
	
	private ByteBuffer headerBuffer = ByteBuffer.allocate(4);
	private int remaining;
	
	public  ClientProtocolSizeParser() 
	{
		this.reset(4);
	}
	
	public void reset(int size) {
		this.headerBuffer.clear();
		this.remaining = this.headerBuffer.capacity();
	}
	
	/* (non-Javadoc)
	 * @see com.enxime.entail.experiment.State#handleData(com.enxime.entail.experiment.StateContext, java.nio.ByteBuffer)
	 */
	@Override
	public void handleData(ClientProtocolParserContext context, ByteBuffer byteBuffer) throws InvalidDataException 
	{
		// If assuming the byteBuffer was used for writing
		
		
		if(this.headerBuffer.remaining() > byteBuffer.remaining())
		{
			_logger.finer("headerBuffer.remaining > byteBuffer.remaining");
			
			this.headerBuffer.put(byteBuffer);
			this.remaining = this.headerBuffer.remaining();
		}
		else
		{
			_logger.finer("headerBuffer.remaining <= byteBuffer.remaining");
			for(int i= 0; i < this.remaining; i++)
				this.headerBuffer.put(byteBuffer.get()); // so, byteBuffer has remaining data
			
			this.headerBuffer.flip();
			int objectSize = this.headerBuffer.getInt();
			if(objectSize >= 1213486160) {
				// probably invalid size
				// must stop communication
				throw new InvalidDataException("Reported object size is abnormal: " + objectSize);
			}
			// Change state to ObjectRead
			context.changeParser().reset(objectSize);
			context.handleData(byteBuffer);
		}
		

	}
}
