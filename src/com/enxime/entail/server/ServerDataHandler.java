package com.enxime.entail.server;

import java.nio.channels.SocketChannel;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import com.enxime.entail.experiment.CountUtil;
import com.enxime.entail.share.LogUtil;

public class ServerDataHandler implements Runnable {
	private static final Logger _logger = LogUtil
			.getLogger(ServerDataHandler.class);

	private List<ServerDataEvent> queue = new LinkedList<ServerDataEvent>();

	private ConcurrentHashMap<SocketChannel, AbstractClient> abstractClientMap = new ConcurrentHashMap<SocketChannel, AbstractClient>();

	//

	public ServerDataHandler() {

	}

	public void processData(TailServer server, SocketChannel socket,
			final byte[] data, int count) {
		_logger.fine("called.");

		// if(this.server == null)
		// this.server = server;

		byte[] dataCopy = new byte[count];
		System.arraycopy(data, 0, dataCopy, 0, count);
		synchronized (queue) {
			queue.add(new ServerDataEvent(server, socket, dataCopy));
			queue.notify();
		}
	}

	@Override
	public void run() {
		_logger.fine("called.");
		// to know performance
		CountUtil.start("ServerDataHandler");

		ServerDataEvent dataEvent = null;

		while (true) {
			// Wait for data to become available
			synchronized (queue) {
				while (queue.isEmpty()) {
					try {
						queue.wait();
					} catch (InterruptedException e) {
					}
				}
				dataEvent = queue.remove(0);
			}

			// this.handleDataEvent(dataEvent);
			this.handleServerDataEvent(dataEvent);

			// Return to sender
			// dataEvent.server.send(dataEvent.socket, dataEvent.data);
			// TODO change to send tail lines
			// this.server.send(dataEvent.socket, dataEvent.data);
			CountUtil.incrementUntil(1000);
		}
	}

	public void handleServerDataEvent(ServerDataEvent dataEvent) {
		_logger.fine("called.");
		// AbstractClient abstractClient =
		// this.abstractClientMap.putIfAbsent(dataEvent.socket, new
		// AbstractClient(dataEvent.server, dataEvent.socket));
		AbstractClient abstractClient = this.abstractClientMap
				.get(dataEvent.socketChannel);

		if (abstractClient == null) {
			_logger.fine("abstractClient was null");
			// abstractClient = this.abstractClientMap.get(dataEvent.socket);
			abstractClient = this.createAbstractClientAndPutToMap(
					dataEvent.tailServer, dataEvent.socketChannel);
		}

		_logger.fine("abstractClientMap.size: " + this.abstractClientMap.size());
		abstractClient.handleDataEvent(dataEvent);
	}

	private synchronized AbstractClient createAbstractClientAndPutToMap(
			TailServer tailServer, SocketChannel socketChannel) {
		_logger.fine("called.");
		if (this.abstractClientMap.get(socketChannel) == null) {

			this.abstractClientMap.put(socketChannel, new AbstractClient(
					tailServer, socketChannel));
			_logger.fine("New AbstractClient created.");
		}
		return this.abstractClientMap.get(socketChannel);
	}

	public void removeAbstractClient(SocketChannel socketChannel) {
		_logger.fine("called.");
		this.abstractClientMap.remove(socketChannel);
	}

}