package com.enxime.entail.client;

import java.nio.channels.SocketChannel;

class ClientDataEvent {
	public TailClient client;
	public SocketChannel socket;
	public byte[] data;
	
	public ClientDataEvent(TailClient client, SocketChannel socket, byte[] data) {
		this.client = client;
		this.socket = socket;
		this.data = data;
	}
}
