package com.enxime.entail.client;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.List;
import java.util.logging.Logger;

import com.enxime.entail.share.EntailProto.ToClient;
import com.enxime.entail.share.LogUtil;
import com.enxime.entail.share.ResultCode;
import com.google.protobuf.InvalidProtocolBufferException;

public class ClientProtocolHandler {
    private static final Logger _logger = LogUtil.getLogger(ClientProtocolHandler.class.getName());
	
	private SocketChannel socket;
	private TailClient client;
	
	private int accuReadCount;
	private ClientReadState readState;
	private ByteBuffer headerBuffer = ByteBuffer.allocate(4);
	private int bodySize;
	private ByteBuffer bodyBuffer;
	
//	private String filePath;

	
	public ClientProtocolHandler(SocketChannel socket, TailClient client) {
		_logger.fine("called.");
		this.socket = socket;
		this.client = client;
		this.init();
	}
	
	public void init() {
		_logger.fine("called.");
		
		this.readState = ClientReadState.INIT;
		this.headerBuffer.clear();
		this.accuReadCount = 0;
		this.bodySize = 0;
	}

	public void parseData(SocketChannel socket, byte[] data) {
		_logger.fine("called.");
		
		this.accuReadCount += data.length;
		int dataOffset = 0;
		
		
		if(this.readState == ClientReadState.INIT) { // Not very meaningful but for clarity
			this.readState = ClientReadState.HEADER;
		}
		
		if(this.readState == ClientReadState.HEADER) {
			if(this.accuReadCount < 4) {
				_logger.fine("accReadCount = " + this.accuReadCount);
				//then more data needed
				//System.arraycopy(data, 0, this.headerBytes, 0, data.length);
				this.headerBuffer.put(data);
				
			} else { // this.accuReadSize >= 4
				_logger.fine("accReadCount = " + this.accuReadCount);
				
				int position = this.headerBuffer.position();
				this.headerBuffer.put(data, 0, (4 - position));
				this.headerBuffer.flip();
				this.bodySize = headerBuffer.getInt();
				_logger.fine("ProtoBuffer Size = " + this.bodySize);
				this.bodyBuffer = ByteBuffer.allocate(this.bodySize);
				
				this.readState = ClientReadState.BODY;
				//dataOffset = data.length - (4-position);
				dataOffset = (4 - position);
			}
		}
		
		if(this.readState == ClientReadState.BODY) {
			if(dataOffset > 0) { // Overflow from previous IF clause
				if(this.accuReadCount <= (4 + this.bodySize)) {
					_logger.fine("accuReadAccount <= bodySize");
					
					this.bodyBuffer.put(data, dataOffset, (data.length - dataOffset));
					
					if(this.accuReadCount == (4 + this.bodySize)) {
						//
						this.parsePBObjectFromServer(socket, this.bodyBuffer.array());
						this.init();
					}
				} else { // accuReadCount > (4 + this.bodySize)
					this.bodyBuffer.put(data, dataOffset, this.bodySize);
					//
					this.parsePBObjectFromServer(socket, this.bodyBuffer.array());
					
					// treat remaining bytes
					int remainSize = data.length - (dataOffset + this.bodySize);
					byte[] subData = new byte[remainSize];
					System.arraycopy(data, (dataOffset + this.bodySize), subData, 0, subData.length);
					
					this.init();
					
					// recursive call for remaining bytes
					this.parseData(socket, subData);
				}
				dataOffset = 0;
			} else {
				if(this.accuReadCount < (4 + this.bodySize)){
					this.bodyBuffer.put(data);
				} else { // this.accuReadcount >= (4 + this.bodySize)
					_logger.finer("this.bodyBuffer size = " + this.bodyBuffer.capacity());
					_logger.finer("this.bodyBuffer posi = " + this.bodyBuffer.position());
					_logger.finer("this.bodySize length = " + this.bodySize);
					
					//this.bodyBuffer.put(data, 0, this.bodySize);
					int position = this.bodyBuffer.position();
					this.bodyBuffer.put(data, 0, this.bodySize - position);
					
					this.parsePBObjectFromServer(socket, this.bodyBuffer.array());
					
					// treat remaining bytes
					int remainSize = data.length - (this.bodySize - position);
					byte[] subData = new byte[remainSize];
					System.arraycopy(data, (this.bodySize - position), subData, 0, subData.length);
					
					this.init();
					// recursive call for remaining bytes
					this.parseData(socket, subData);	
				}
			}

		}
	}
	
	private void parsePBObjectFromServer(SocketChannel socket, final byte[] toClientBytes) { 
		// TODO 
		_logger.fine("called.");
		_logger.fine("commandBytes size = " + toClientBytes.length);
		try {
			ToClient fromServer = ToClient.parseFrom(toClientBytes);
			
			//this.filePath = fromServer.getFilePath();
			int resultCode = fromServer.getResultCode();
			
			if(resultCode == ResultCode.OK) {
				for(String line : fromServer.getTailLineList())
					System.out.println(line);
			} else if(resultCode == ResultCode.FILE_ROTATED) {
				System.out.println("File rotated.");
			} else if(resultCode == ResultCode.NOT_FOUND) {
				System.out.println("File not found.");
			} else if(resultCode == ResultCode.EXCEPTION) {
				System.out.println("Exception occurred: " + fromServer.getResultMessage());
			} else {
				_logger.severe("Not valid result code: " + resultCode);
			}
			String resultMessage = fromServer.getResultMessage();
			List<String> tailLineList = fromServer.getTailLineList();
			
			
			
		} catch (InvalidProtocolBufferException ipbe) {
			// TODO Auto-generated catch block
			ipbe.printStackTrace();
		}
		
	}
	
//	public void stopTailer() {
//		if(this.tailer != null) {
//			this.tailer.stop();
//		} else
//			_logger.warning("this.tailer already stopped.");
//	}

}
