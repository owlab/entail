package com.enxime.entail.client;

import java.nio.ByteBuffer;
import java.util.logging.Logger;

import com.enxime.entail.share.EntailProto.ToClient;
import com.enxime.entail.share.LogUtil;
import com.google.protobuf.InvalidProtocolBufferException;

public class ClientProtocolObjectParser implements ClientProtocolParser {
    private static final Logger _logger = LogUtil.getLogger(ClientProtocolObjectParser.class.getName());
    
    // private static ClientProtocolObjectParser THIS = new
    // ClientProtocolObjectParser();
    // private Builder protoObjectBuilder = ToServer.newBuilder();
    // private int objectSize;

    private ByteBuffer objectBuffer;
    private int remaining = 0;
    // private boolean isInitialized = false;

    ClientProtocolListener protocolListener;

    public ClientProtocolObjectParser(ClientProtocolListener protocolListener) {
	this.protocolListener = protocolListener;

    }

    public void reset(int objectSize) {
	_logger.finer("object size = " + objectSize);
	this.objectBuffer = ByteBuffer.allocate(objectSize);
	this.remaining = objectSize;
    }

    @Override
    public void handleData(ClientProtocolParserContext context,
	    ByteBuffer byteBuffer) {
	if (this.objectBuffer.remaining() > byteBuffer.remaining()) {
	    this.objectBuffer.put(byteBuffer);
	    this.remaining = this.objectBuffer.remaining();
	} else {
	    for (int i = 0; i < this.remaining; i++)
		this.objectBuffer.put(byteBuffer.get());
	    // to something from this code block
	    ToClient objectFromServer = null;
	    try {
		objectFromServer = ToClient
			.parseFrom(this.objectBuffer.array());
	    } catch (InvalidProtocolBufferException ipbe) {
		ipbe.getStackTrace();
	    }

	    // following object and method might be better to be independent
	    // thread
	    // ResponseHandler.handleResponse(objectFromServer);
	    this.protocolListener.handleServerResponse(objectFromServer);

	    context.changeParser().reset(4);
	    context.handleData(byteBuffer);
	}
    }
}
