package com.enxime.entail.server;

import java.nio.channels.SocketChannel;


public class ServerChangeRequest {
	public static final int REGISTER = 1;
	public static final int CHANGEOPS = 2;
	
	public AbstractClient abstractClient;
	public SocketChannel socketChannel;
	public int type;
	public int ops;
	
	public ServerChangeRequest(AbstractClient abstractClient, int type, int ops) {
		this.abstractClient = abstractClient;
		this.socketChannel = abstractClient.getSocketChannel();
		this.type = type;
		this.ops = ops;
	}
}