package com.enxime.entail.server;

import java.nio.ByteBuffer;

import com.enxime.entail.share.EntailProto.ToServer;
import com.google.protobuf.InvalidProtocolBufferException;



public class ServerProtocolObjectParser implements ServerProtocolParser {
	//private static ServerProtocolObjectParser THIS = new ServerProtocolObjectParser();
	//private Builder protoObjectBuilder = ToServer.newBuilder();
	private ServerProtocolListener protocolListener;
	int objectSize;
	ByteBuffer objectBuffer;
	int remaining = 0;
	
	public ServerProtocolObjectParser(ServerProtocolListener protocolListener)
	{
		this.protocolListener = protocolListener;
	}
	
	public void reset(int objectSize) {
		this.objectBuffer = ByteBuffer.allocate(objectSize);
		this.remaining = objectSize;
	}

	@Override
	public void handleData(ServerProtocolParserContext context, final ByteBuffer byteBuffer) {
		if(this.objectBuffer.remaining() > byteBuffer.remaining())
		{
			this.objectBuffer.put(byteBuffer);
			this.remaining = this.objectBuffer.remaining();
		}
		else
		{
			for(int i = 0; i < this.remaining; i++)
				this.objectBuffer.put(byteBuffer.get());
			// to something from this code block
			ToServer objectFromClient = null;
			try {
				objectFromClient = ToServer.parseFrom(this.objectBuffer.array());
			} catch (InvalidProtocolBufferException ipbe) {
				ipbe.getStackTrace();
			}

			this.protocolListener.performCommand(objectFromClient);
			
			context.changeParser().reset(4);
			
			context.handleData(byteBuffer);
		}
	}
}
