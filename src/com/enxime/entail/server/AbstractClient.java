package com.enxime.entail.server;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import com.enxime.entail.share.EntailProto.ToClient;
import com.enxime.entail.share.EntailProto.ToServer;
import com.enxime.entail.share.LogUtil;
import com.enxime.entail.share.ResultCode;

public class AbstractClient implements ServerProtocolListener {
    private static final Logger _logger = LogUtil.getLogger(AbstractClient.class.getName());
	
    	private static byte[] pongBytes = ToClient.newBuilder().setFilePath("/dummy").setResultCode(ResultCode.ACK).build().toByteArray();
	private TailServer tailServer;
	private SocketChannel socketChannel;
	private ServerProtocolParserContext protocolContext;
	private ConcurrentHashMap<String, TailWorker> tailWorkerMap = new ConcurrentHashMap<String, TailWorker>();

	public AbstractClient(TailServer tailServer, SocketChannel socketChannel) {
		_logger.fine("called.");
		
		this.tailServer = tailServer;
		this.socketChannel = socketChannel;
		this.protocolContext = new ServerProtocolParserContext(this);
	}
	
	public SocketChannel getSocketChannel() {
		return this.socketChannel;
	}

	public void handleDataEvent(ServerDataEvent dataEvent) {
		protocolContext.handleData(ByteBuffer.wrap(dataEvent.data));
	}

	@Override
	public void performCommand(ToServer fromClient) {
		_logger.fine("called.");
		_logger.fine("This object is " + this.toString());
		_logger.fine("This object's tailWorkerMap is " + this.tailWorkerMap);

		

		String filePath = fromClient.getFilePath();
		String command = fromClient.getCommand();
		_logger.info("command: " + command);
		_logger.info("filePath: " + filePath);
		// commands are "start" or "stop" tailing of a file
		if("start".equalsIgnoreCase(command)) {
			synchronized (tailWorkerMap) {
				if(!tailWorkerMap.containsKey(filePath)) {
					TailListener tailListener = new TailListenerAdapter(this);
					TailWorker tailWorker = TailWorker.tail(filePath, tailListener);
					tailWorkerMap.put(filePath, tailWorker);
				} else {
					_logger.warning("Already tailing file=" + filePath);
				}
			}
		} else if("stop".equalsIgnoreCase(command)) {
			synchronized (tailWorkerMap) {
				_logger.fine(tailWorkerMap.toString());
				
				if(tailWorkerMap.containsKey(filePath)) {
					TailWorker tailWorker = tailWorkerMap.remove(filePath);
					tailWorker.stop();
					_logger.info("A tail worker stopped: " + filePath);
				} else {
					_logger.warning("No such file tailed.");
				}
			}
			if(tailWorkerMap.isEmpty()) {
			    _logger.fine("tailWorkerMap is empty.");
			    this.tailServer.getServerDataHandler().removeAbstractClient(this.socketChannel);
			}
			
		} else if("pause".equalsIgnoreCase(command)) {
			synchronized (tailWorkerMap) {
				_logger.fine(tailWorkerMap.toString());
				
				if(tailWorkerMap.containsKey(filePath)) {
					TailWorker tailWorker = tailWorkerMap.get(filePath);
					tailWorker.pause();
					_logger.info("A tail worker paused: " + filePath);
				} else {
					_logger.warning("No such file tailed.");
				}
			}
			if(tailWorkerMap.isEmpty()) {
			    _logger.fine("tailWorkerMap is empty.");
			    this.tailServer.getServerDataHandler().removeAbstractClient(this.socketChannel);
			}
			
		} else if("restart".equalsIgnoreCase(command)) {
			synchronized (tailWorkerMap) {
				_logger.fine(tailWorkerMap.toString());
				
				if(tailWorkerMap.containsKey(filePath)) {
				    TailWorker tailWorker = tailWorkerMap.get(filePath);
					tailWorker.restart();
					_logger.info("A tail worker restarted: " + filePath);
				} else {
					_logger.warning("No such file tailed.");
				}
			}
			if(tailWorkerMap.isEmpty()) {
			    _logger.fine("tailWorkerMap is empty.");
			    this.tailServer.getServerDataHandler().removeAbstractClient(this.socketChannel);
			}
			
		} else if("ping".equalsIgnoreCase(command)){
		    
		    this.send(pongBytes);
		    
		} else {
			_logger.severe("Command not defined: " + command + " (start or stop possible)");
		}


	}
	
	public void send(byte[] data) {
		this.tailServer.sendToClient(this, data);
	}
	
	public void stopAll() {
		_logger.fine("called.");
		Collection<TailWorker> tailWorkers = tailWorkerMap.values();
		Iterator<TailWorker> it = tailWorkers.iterator();
		while(it.hasNext()) {
			it.next().stop();
		}
	}

}
