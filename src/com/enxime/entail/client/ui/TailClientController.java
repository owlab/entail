package com.enxime.entail.client.ui;

import java.io.IOException;
import java.lang.Thread.UncaughtExceptionHandler;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import com.enxime.entail.client.ClientProtocolListener;
import com.enxime.entail.client.ConsoleWriter;
import com.enxime.entail.client.TailClient;
import com.enxime.entail.client.TailClientDataHandler;
import com.enxime.entail.share.EntailProto.ToClient;
import com.enxime.entail.share.EntailProto.ToServer;
import com.enxime.entail.share.EntailProto.ToServer.Builder;
import com.enxime.entail.share.LogUtil;
import com.enxime.entail.share.ResultCode;

public class TailClientController implements ClientProtocolListener,
		UncaughtExceptionHandler /* , TailClientExceptionListener */{
	private static final Logger _logger = LogUtil
			.getLogger(TailClientController.class.getName());

	private String host;
	private int port;
	private TailClientDataHandler tailClientDataHandler;
	private TailClient tailClient;
	private Pingger pingger;
	private Thread tailClientDataHandlerThread;
	private Thread tailClientThread;
	private Thread pinggerThread;

	private List<TailURL> tailUrls = new ArrayList<TailURL>();

	public TailClientController(String host, int port)
			throws UnknownHostException, IOException {
		this.host = host;
		this.port = port;

		this.tailClientDataHandler = new TailClientDataHandler(this);
		this.tailClientDataHandlerThread = new Thread(
				this.tailClientDataHandler);
		this.tailClientDataHandlerThread
				.setName("TailClientDataHandler thread: "
						+ this.tailClientDataHandlerThread.getName());

		this.tailClientDataHandlerThread.start();

		_logger.fine(this.tailClientDataHandlerThread.getName() + " started.");

		this.tailClient = new TailClient(InetAddress.getByName(host), port,
				this.tailClientDataHandler);

		this.tailClientThread = new Thread(this.tailClient);
		this.tailClientThread.setName("TailClient thread: "
				+ this.tailClientThread.getName());
		this.tailClientThread.setUncaughtExceptionHandler(this);

		this.tailClientThread.start();

		// this.pingger = new Pingger(this);
		// this.pinggerThread = new Thread(this.pingger);
		// this.pinggerThread.start();

		_logger.fine(this.tailClientThread.getName() + " started.");

		_logger.fine("Threads started for server: " + host + ":" + port);

	}

	public String getHostAndPort() {
		return host + ":" + port;
	}

	public List<TailURL> getTailUrls() {
		return tailUrls;
	}

	public void addTailUrl(TailURL tailUrl) throws IOException {
		try {
			this.sendCommandToServer(tailUrl.getFilePath(), "start", null);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			throw e;
		}
		this.tailUrls.add(tailUrl);
	}

	public void suspendTailUrl(TailURL tailUrl) throws IOException {
		try {
			this.sendCommandToServer(tailUrl.getFilePath(), "pause", null);
		} catch (IOException e) {
			throw e;
		}
		// TODO something to manage file tailing status
	}

	public void restartTailUrl(TailURL tailUrl) throws IOException {
		try {
			this.sendCommandToServer(tailUrl.getFilePath(), "restart", null);
		} catch (IOException e) {
			throw e;
		}
		// TODO something to manage file tailing status
	}

	public void removeTailUrl(TailURL tailUrl) throws IOException {
		_logger.fine("called.");
		this.tailUrls.remove(tailUrl);

		try {
			this.sendCommandToServer(tailUrl.getFilePath(), "stop", null);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			throw e;
		}

	}

	public void stopTailClientAndDataHandlerThreads() {
		_logger.info("called.");
		// this.pingger.stop();
		this.tailClient.stop();
		this.tailClientDataHandlerThread.interrupt();
		// this.tailClientDataHandler.stop();
	}

	public void ping() throws IOException {
		this.sendCommandToServer("/dummy", "ping", null);
	}

	private void sendCommandToServer(String tailFilePath, String command,
			String parameter) throws IOException {
		_logger.fine("Command sending: " + tailFilePath + ", " + command + ", "
				+ null);
		Builder builder = ToServer.newBuilder();
		builder.setFilePath(tailFilePath).setCommand(command);
		if (parameter != null)
			builder.addParameter(parameter);
		ToServer toServerObject = builder.build();

		byte[] toServerObjectArray = toServerObject.toByteArray();
		ByteBuffer toServerByteBuffer = ByteBuffer
				.allocate(4 + toServerObjectArray.length);
		toServerByteBuffer.putInt(toServerObjectArray.length);
		toServerByteBuffer.put(toServerObjectArray);

		this.tailClient.sendToServer(toServerByteBuffer.array());
	}

	@Override
	public void handleServerResponse(ToClient fromServer) {
		TailURL tailUrl = null;
		String filePath = fromServer.getFilePath();

		try {
			if (filePath.contains(":"))
				tailUrl = new TailURL("tail://" + host + ":" + port + "/"
						+ filePath);
			else
				tailUrl = new TailURL("tail://" + host + ":" + port + filePath);
		} catch (InvalidTailURLException itue) {
			itue.printStackTrace();
			return;
		}

		// Too bad presentation performance
		TailClientControllerManager.getInstance().handleServerResponse(tailUrl,
				fromServer);
		// Below is far fast than the above
		// ConsoleWriter cw = new ConsoleWriter();
		// cw.handleServerResponse(fromServer);
		
		// To enable disruptor framework
		
	}

	@Override
	public void uncaughtException(Thread thread, Throwable exception) {

		_logger.severe("Exception on thread: " + thread);
		_logger.severe(exception.getMessage());

		TailClientControllerManager.getInstance().handleTailClientException(
				this, thread, exception);

	}
}
