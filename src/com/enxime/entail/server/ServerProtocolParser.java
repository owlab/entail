package com.enxime.entail.server;

import java.nio.ByteBuffer;

public interface ServerProtocolParser {
	/**
	 * @param context
	 * @param byteBuffer Already prepared for Read access
	 * @return
	 */
	void handleData(ServerProtocolParserContext context, ByteBuffer byteBuffer);
	
	void reset(int size);
}
