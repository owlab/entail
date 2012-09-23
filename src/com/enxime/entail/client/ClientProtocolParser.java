package com.enxime.entail.client;

import java.nio.ByteBuffer;

public interface ClientProtocolParser {
	/**
	 * @param context
	 * @param byteBuffer Already prepared for Read access
	 * @return
	 */
	void reset(int size);
	void handleData(ClientProtocolParserContext context, ByteBuffer byteBuffer) throws InvalidDataException;
}
