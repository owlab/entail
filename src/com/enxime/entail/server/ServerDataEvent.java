package com.enxime.entail.server;

import java.nio.channels.SocketChannel;

class ServerDataEvent {
	public TailServer tailServer;
	public SocketChannel socketChannel;
	public byte[] data;
	
	public ServerDataEvent(TailServer tailServer, SocketChannel socketChannel, final byte[] data) {
		this.tailServer = tailServer;
		this.socketChannel = socketChannel;
		this.data = data;
	}
}
