package com.enxime.entail.client;


import java.io.IOException;
import java.lang.Thread.UncaughtExceptionHandler;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import com.enxime.entail.server.ServerChangeRequest;
import com.enxime.entail.share.EntailProto.ToServer;
import com.enxime.entail.share.EntailProto.ToServer.Builder;
import com.enxime.entail.share.LogUtil;


public class TailClient implements Runnable {
    private static final Logger _logger = LogUtil.getLogger(TailClient.class.getName());
    
//  private TailClientExceptionListener tailClientExceptionListener;
    // The host:port combination to connect to
    private InetAddress hostAddress;
    private int port;
    private volatile boolean stop = false;

    private TailClientDataHandler clientDataHandler;
//    private Thread clientDataHandlerThread;

    // The selector we'll be monitoring
    private Selector selector;

    // Client socket dedicated to a server
    private SocketChannel socket;
    // The buffer into which we'll read data when it's available
    private ByteBuffer readBuffer = ByteBuffer.allocate(8192);

    // A list of PendingChange instances
    private List<ClientChangeRequest> pendingChanges = new LinkedList<ClientChangeRequest>();

    // Maps a SocketChannel to a list of ByteBuffer instances
    private Map<SocketChannel, List<ByteBuffer>> pendingData = new HashMap<SocketChannel, List<ByteBuffer>>();

    //	private Map<Socket, SSLSocket>	sSLSocketMap = new HashMap<Socket, SSLSocket>();

    public TailClient(/*TailClientExceptionListener tailClientExceptionListener, */InetAddress hostAddress, int port, TailClientDataHandler clientDataHandler) throws IOException  {
//	this.tailClientExceptionListener = tailClientExceptionListener;
	this.hostAddress = hostAddress;
	this.port = port;
	this.clientDataHandler = clientDataHandler;
	this.selector = this.initSelector();
    }

    public void stop() {
	_logger.fine("called");

	this.stop = true;
    }

    public void run() {
	_logger.fine("called.");
	while (!this.stop) {
//	    _logger.fine("position-run.while");
	    try {
		// Process any pending changes
		synchronized (this.pendingChanges) {
		    Iterator<ClientChangeRequest> changes = this.pendingChanges.iterator();
		    while (changes.hasNext()) {
			ClientChangeRequest change = changes.next();
			switch (change.type) {
			    case ClientChangeRequest.CHANGEOPS:
				SelectionKey key = change.socket.keyFor(this.selector);
				key.interestOps(change.ops);
				break;
			    case ClientChangeRequest.REGISTER:
				change.socket.register(this.selector, change.ops);
				break;
			}
		    }
		    this.pendingChanges.clear();
		}

		// Wait for an event one of the registered channels
//		_logger.fine("position-before selection");
		this.selector.select(1000);

		// Iterate over the set of keys for which events are available
		Iterator<SelectionKey> selectedKeys = this.selector.selectedKeys().iterator();
		while (selectedKeys.hasNext()) {
//		    _logger.fine("position-run.while.while");
		    SelectionKey key = selectedKeys.next();
		    selectedKeys.remove();

		    if (!key.isValid()) {
			continue;
		    }
		    // Check what event is available and deal with it

		    if (key.isConnectable()) {
			this.finishConnection(key);
		    } else if (key.isReadable()) {
			this.read(key);
		    } else if (key.isWritable()) {
			this.write(key);
		    }
		}
	    } catch (ClosedChannelException e) {
		//e.printStackTrace();
		throw new RuntimeException(e.getMessage());
		//this.tailClientExceptionListener.handleTailClientException(e);
	    } catch (IOException e) {
		//e.printStackTrace();
		throw new RuntimeException(e.getMessage());
		//this.tailClientExceptionListener.handleTailClientException(e);
	    } // TODO add finally to stop in case of exceptions
	}
	
	_logger.fine("Thread stopped.");
    }

    private void read(SelectionKey key) throws IOException {
	SocketChannel socketChannel = (SocketChannel) key.channel();

	// Clear out our read buffer so it's ready for new data
	this.readBuffer.clear();

	// Attempt to read off the channel
	int numRead;
	try {
	    numRead = socketChannel.read(this.readBuffer);
	} catch (IOException e) {
	    // The remote forcibly closed the connection, cancel
	    // the selection key and close the channel.
	    key.cancel();
	    socketChannel.close();
	    return;
	}

	if (numRead == -1) {
	    // Remote entity shut the socket down cleanly. Do the
	    // same from our end and cancel the channel.
	    key.channel().close();
	    key.cancel();
	    return;
	}

	// Handle the response
	//this.handleResponse(socketChannel, this.readBuffer.array(), numRead);
	this.clientDataHandler.processData(this, socketChannel, this.readBuffer.array(), numRead);
    }

    /*
	private void handleResponse(SocketChannel socketChannel, byte[] data, int numRead) throws IOException {
		// Make a correctly sized copy of the data before handing it
		// to the client
		byte[] rspData = new byte[numRead];
		System.arraycopy(data, 0, rspData, 0, numRead);

		// Look up the handler for this channel
		ResponseHandler handler = (ResponseHandler) this.rspHandlers.get(socketChannel);

		// And pass the response to it
		if (handler.handleResponse(rspData)) {
			// The handler has seen enough, close the connection
			socketChannel.close();
			socketChannel.keyFor(this.selector).cancel();
		}
	}
     */

    private void write(SelectionKey key) throws IOException {
	SocketChannel socketChannel = (SocketChannel) key.channel();

	synchronized (this.pendingData) {
	    List<ByteBuffer> queue = this.pendingData.get(socketChannel);

	    // Write until there's not more data ...
	    while (!queue.isEmpty()) {
		ByteBuffer buf = queue.get(0);
		socketChannel.write(buf);
		if (buf.remaining() > 0) {
		    // ... or the socket's buffer fills up
		    break;
		}
		queue.remove(0);
	    }

	    if (queue.isEmpty()) {
		// We wrote away all data, so we're no longer interested
		// in writing on this socket. Switch back to waiting for
		// data.
		key.interestOps(SelectionKey.OP_READ);
	    }
	}
    }

    private SocketChannel initiateConnection() throws IOException {
	// Create a non-blocking socket channel
	SocketChannel socketChannel = SocketChannel.open();
	socketChannel.configureBlocking(false);

	// Kick off connection establishment
	socketChannel.connect(new InetSocketAddress(this.hostAddress, this.port));

	// Queue a channel registration since the caller is not the 
	// selecting thread. As part of the registration we'll register
	// an interest in connection events. These are raised when a channel
	// is ready to complete connection establishment.
	synchronized(this.pendingChanges) {
	    this.pendingChanges.add(new ClientChangeRequest(socketChannel, ServerChangeRequest.REGISTER, SelectionKey.OP_CONNECT));
	}

	return socketChannel;
    }

    public void sendToServer(byte[] data /*, ResponseHandler handler */) throws IOException  {
	// Start a new connection if it didn't
	// SocketChannel socket = this.initiateConnection();
	if(this.socket == null) {
	    _logger.fine("Initial connection");
	    this.socket = this.initiateConnection();

	    synchronized (this.pendingData) {
		List<ByteBuffer> queue = this.pendingData.get(this.socket);
		if (queue == null) {
		    queue = new ArrayList<ByteBuffer>();
		    this.pendingData.put(this.socket, queue);
		}
		queue.add(ByteBuffer.wrap(data));
	    }

	} else {
	    synchronized (this.pendingChanges) {
		// Indicate we want the interest ops set changed
		this.pendingChanges.add(new ClientChangeRequest(this.socket,
			ServerChangeRequest.CHANGEOPS, SelectionKey.OP_WRITE));
		// And queue the data we want written
		synchronized (this.pendingData) {
		    List<ByteBuffer> queue = this.pendingData.get(this.socket);
		    if (queue == null) {
			queue = new ArrayList<ByteBuffer>();
			this.pendingData.put(this.socket, queue);
		    }
		    queue.add(ByteBuffer.wrap(data));
		}
	    }
	}

	// Finally, wake up our selecting thread so it can make the required changes
	this.selector.wakeup();
    }

//    public void sendCommandToServer(String filePath, String command, String parameter) throws IOException {
//	_logger.fine("Command sending: " + filePath + ", " + command + ", " + null);
//	Builder builder = ToServer.newBuilder();
//	builder.setFilePath(filePath).setCommand(command);
//	if(parameter != null)
//	    builder.addParameter(parameter);
//	ToServer toServerObject = builder.build();
//
//	byte[] toServerObjectArray = toServerObject.toByteArray();
//	ByteBuffer toServerByteBuffer = ByteBuffer.allocate(4 + toServerObjectArray.length);
//	toServerByteBuffer.putInt(toServerObjectArray.length);
//	toServerByteBuffer.put(toServerObjectArray);
//	//		
//	//		try {
//	this.sendToServer(toServerByteBuffer.array());
//	//		} catch (IOException e) {
//	//			// TODO Auto-generated catch block
//	//			e.printStackTrace();
//	//		}
//    }



    private void finishConnection(SelectionKey key) throws IOException {
	SocketChannel socketChannel = (SocketChannel) key.channel();

	// Finish the connection. If the connection operation failed
	// this will raise an IOException.
	try {
	    socketChannel.finishConnect();
	} catch (IOException e) {
	    // Cancel the channel's registration with our selector
	    //e.printStackTrace();
	    key.cancel();
	    //return;
	    throw e;
	}

	//	this.registerSocket(socketChannel.socket(), this.hostAddress, this.port, false);

	// Register an interest in writing on this channel
	key.interestOps(SelectionKey.OP_WRITE);
    }

    //	protected void registerSocket(Socket socket, InetAddress host, int port, boolean isClient) throws IOException {
    //		SSLSocketFactory sSLSocketFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
    //		SSLSocket sSLSocket = (SSLSocket) sSLSocketFactory.createSocket(socket, host.getHostAddress(), port, true); 
    //		sSLSocket.setUseClientMode(isClient);
    //		this.sSLSocketMap.put(socket, sSLSocket);
    //	}
    //	
    //	protected void deRegisterSocket(Socket socket) {
    //		this.sSLSocketMap.remove(socket);
    //		this.sSLSessionMap.remove(socket);
    //	}
    private Selector initSelector() throws IOException {
	// Create a new selector
	return SelectorProvider.provider().openSelector();
    }


}

