package com.enxime.entail.server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.enxime.entail.share.LogUtil;

public class TailServer implements Runnable {
	private static final Logger _logger = LogUtil.getLogger(TailServer.class
			.getName());

	private InetAddress hostAddress;
	private int port;

	// The channel on which we'll accept connections
	private ServerSocketChannel serverSocketChannel;

	// The selector we'll be monitoring
	private Selector selector;

	// The buffer into which we'll read data when it's available
	private ByteBuffer readBuffer = ByteBuffer.allocate(8192);

	private ServerDataHandler serverDataHandler;

	// A list of PendingChange instances
	private List<ServerChangeRequest> pendingChanges = new LinkedList<ServerChangeRequest>();

	// Maps a SocketChannel to a list of ByteBuffer instances
	private Map<AbstractClient, List<ByteBuffer>> pendingData = new HashMap<AbstractClient, List<ByteBuffer>>();

	// private Map<Socket, SSLSocket> sSLSocketMap = new HashMap<Socket,
	// SSLSocket>();
	public TailServer(InetAddress hostAddress, int port,
			ServerDataHandler serverDataHandler) throws IOException {
		_logger.info("called");
		this.hostAddress = hostAddress;
		this.port = port;
		this.selector = this.initSelector();
		this.serverDataHandler = serverDataHandler;
	}

	public ServerDataHandler getServerDataHandler() {
		return this.serverDataHandler;
	}

	private Selector initSelector() throws IOException {
		// Create a new selector
		Selector socketSelector = SelectorProvider.provider().openSelector();

		// Create a new non-blocking server socket channel
		this.serverSocketChannel = ServerSocketChannel.open();
		serverSocketChannel.configureBlocking(false);

		// Bind the server socket to the specified address and port
		InetSocketAddress inetSocketAddress = new InetSocketAddress(
				this.hostAddress, this.port);
		serverSocketChannel.socket().bind(inetSocketAddress);

		// Register the server socket channel, indicating an interest in
		// accepting new connections
		serverSocketChannel.register(socketSelector, SelectionKey.OP_ACCEPT);

		return socketSelector;
	}

	public void run() {
		_logger.info("listening port " + this.port);

		while (true) {
			try {
				// Process any pending changes
				synchronized (this.pendingChanges) {
					Iterator<ServerChangeRequest> changes = this.pendingChanges
							.iterator();
					while (changes.hasNext()) {
						ServerChangeRequest change = changes.next();
						SelectionKey key = null;
						switch (change.type) {
						case ServerChangeRequest.CHANGEOPS:
							key = change.socketChannel.keyFor(this.selector);
							// if(key != null) {
							key.interestOps(change.ops);
							// } else {
							// this.serverDataHandler.removeAbstractClient(change.abstractClient);
							// }
						}
					}
					this.pendingChanges.clear();
				}

				// Wait for an event one of the registered channels
				this.selector.select();

				// Iterate over the set of keys for which events are available
				Iterator<SelectionKey> selectedKeys = this.selector
						.selectedKeys().iterator();
				while (selectedKeys.hasNext()) {
					SelectionKey key = (SelectionKey) selectedKeys.next();
					selectedKeys.remove();

					if (!key.isValid()) {
						continue;
					}

					// Check what event is available and deal with it
					if (key.isAcceptable()) {
						this.accept(key);
					} else if (key.isReadable()) {
						this.read(key);
					} else if (key.isWritable()) {
						this.write(key);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private void accept(SelectionKey key) throws IOException {
		ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key
				.channel();

		SocketChannel socketChannel = serverSocketChannel.accept();
		socketChannel.configureBlocking(false);
		socketChannel.register(this.selector, SelectionKey.OP_READ);
	}

	private void read(SelectionKey key) throws IOException {
		/*
		 * When not using SSL
		 */
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

		// Hand the data off to our worker thread
		_logger.fine("SocketChannel: " + socketChannel.toString());
		this.serverDataHandler.processData(this, socketChannel,
				this.readBuffer.array(), numRead);
	}

	private void write(SelectionKey key) throws IOException {
		_logger.finer("called.");
		SocketChannel socketChannel = (SocketChannel) key.channel();

		synchronized (this.pendingData) {
			// TODO make below simpler
			// List<ByteBuffer> queue = this.pendingData.get(socketChannel);
			List<ByteBuffer> queue = null;
			for (AbstractClient aClient : this.pendingData.keySet()) {
				if (aClient.getSocketChannel().equals(socketChannel)) {
					queue = this.pendingData.get(aClient);
				}
			}

			// Write until there's not more data ...
			// _logger.info("write queue size = " + queue.size());

			while (!queue.isEmpty()) {
				ByteBuffer buf = (ByteBuffer) queue.get(0);
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

	public void sendToClient(AbstractClient abstractClient, byte[] data) {
		_logger.finer("called.");

		synchronized (this.pendingChanges) {
			// Indicate we want the interest ops set changed
			this.pendingChanges.add(new ServerChangeRequest(abstractClient,
					ServerChangeRequest.CHANGEOPS, SelectionKey.OP_WRITE));

			// And queue the data we want written
			synchronized (this.pendingData) {
				List<ByteBuffer> queue = (List<ByteBuffer>) this.pendingData
						.get(abstractClient);
				if (queue == null) {
					queue = new ArrayList<ByteBuffer>();
					this.pendingData.put(abstractClient, queue);
				}
				queue.add(ByteBuffer.wrap(data));
			}
		}

		// Finally, wake up our selecting thread so it can make the required
		// changes
		this.selector.wakeup();
	}

	public static void main(String[] args) {
		// set default log level
		LogUtil.setLevel(Level.INFO);

		int port = 7171;
		if (args.length == 2) {
			if ("-port".equalsIgnoreCase(args[0])) {
				try {
					port = Integer.parseInt(args[1]);
					System.out.println("Port " + port + " will listen.");
				} catch (NumberFormatException nfe) {
					_logger.warning("Given port number is not integer.");
				}
			}
		} else if (args.length == 1) {
			if ("-debug".equalsIgnoreCase(args[0])) {
				LogUtil.setLevel(Level.FINER);
			}
		} else {
			System.out.println("Default port " + port + " will listen.");
		}

		try {
			ServerDataHandler serverDataHandler = new ServerDataHandler();
			new Thread(serverDataHandler).start();

			new Thread(new TailServer(null, port, serverDataHandler)).start();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
