package com.enxime.entail.server;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.logging.Logger;

import com.enxime.entail.share.EntailProto.ToClient;
import com.enxime.entail.share.EntailProto.ToClient.Builder;
import com.enxime.entail.share.LogUtil;
import com.enxime.entail.share.ResultCode;

public class TailListenerAdapter implements TailListener {
    private static final Logger _logger = LogUtil.getLogger(TailListenerAdapter.class.getName());
	
	private AbstractClient client;
	private TailWorker tailer;
	
	public TailListenerAdapter(AbstractClient client) {
		this.client = client;
	}
	
	@Override
	public void init(TailWorker tailer) {
		// TODO Auto-generated method stub
		_logger.fine("called");
		this.tailer = tailer;

	}

	@Override
	public void fileNotFound() {
		_logger.fine("called");
		// TODO Auto-generated method stub
		
		ToClient toClient = ToClient.newBuilder()
				.setFilePath(this.tailer.getFilePath())
				.setResultCode(ResultCode.NOT_FOUND)
				.setResultMessage("File not found")
				.build();
		this.client.send(makeBytesToClient(toClient));
	}

	@Override
	public void fileRotated() {
		_logger.fine("called");
		// TODO Auto-generated method stub
		ToClient toClient = ToClient.newBuilder()
				.setFilePath(this.tailer.getFilePath())
				.setResultCode(ResultCode.FILE_ROTATED)
				.setResultMessage("file rotated.")
				.build();
		this.client.send(makeBytesToClient(toClient));
	}

	
	@Override
	public void handleLine(String line) {
		_logger.fine("called");
		_logger.finer("line: " + line);
		// TODO Auto-generated method stub
		ToClient toClient = ToClient.newBuilder()
				.setFilePath(this.tailer.getFilePath())
				.setResultCode(ResultCode.OK)
				.addTailLine(line)
				.build();
		this.client.send(makeBytesToClient(toClient));
	}
	
	@Override
	public void handleLines(String[] lines) {
		_logger.fine("called");
		//_logger.finer("line: " + line);
		handleLines(lines, 0, lines.length);
	}
	
	@Override
	public void handleLines(String[] lines, int startIndex, int length) {
		_logger.fine("called");
		//_logger.finer("line: " + line);
		Builder builder = ToClient.newBuilder()
				.setFilePath(this.tailer.getFilePath())
				.setResultCode(ResultCode.OK);
		for(int i = startIndex; i < length; i++) {
			builder.addTailLine(lines[i]);
		}
		
		ToClient toClient = builder.build();
		this.client.send(makeBytesToClient(toClient));
	}

	@Override
	public void handleException(Exception ex) {
		_logger.fine("called");
		// TODO Auto-generated method stub
		ToClient toClient = ToClient.newBuilder()
				.setFilePath(this.tailer.getFilePath())
				.setResultCode(ResultCode.EXCEPTION)
				.setResultMessage(ex.getMessage())
				.build();
		this.client.send(makeBytesToClient(toClient));
	}
	
	private byte[] makeBytesToClient(ToClient toClient) {
		_logger.fine("called.");
		byte[] toClientBytes =toClient.toByteArray(); 
		int objSize = toClientBytes.length;
		ByteBuffer b = ByteBuffer.allocate(4 + objSize);
		b.putInt(objSize);
		b.put(toClientBytes);
		return b.array();
	}

}
