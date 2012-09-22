package com.enxime.entail.client.ui;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import com.enxime.entail.share.EntailProto.ToClient;
import com.enxime.entail.share.LogUtil;
import com.enxime.entail.share.ResultCode;

public class TailClientControllerManager {
	private static final Logger _logger = LogUtil
			.getLogger(TailClientControllerManager.class.getName());

	private static TailClientControllerManager tailClientControllerManager = new TailClientControllerManager();

	private static ConcurrentHashMap<String, TailClientController> tailClientControllers = new ConcurrentHashMap<String, TailClientController>();

	private TailClientControllerManager() {

	}

	public static TailClientControllerManager getInstance() {
		return tailClientControllerManager;
	}

	// Create AbstractTailServer, if not exist, and start tailing
	public boolean addTailURL(TailURL tailUrl) {
		_logger.fine("called.");
		String hostAndPort = tailUrl.getHostAndPort();

		if (!tailClientControllers.containsKey(hostAndPort)) {
			try {
				tailClientControllers.put(
						hostAndPort,
						new TailClientController(tailUrl.getHost(), tailUrl
								.getPort()));
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return false;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return false;
			}
		}

		TailClientController tailClientController = tailClientControllers
				.get(hostAndPort);
		try {
			tailClientController.addTailUrl(tailUrl);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}

		return true;
	}

	// stop tailing a file
	public void removeTailURL(TailURL tailUrl) {
		_logger.fine("called.");
		String hostAndPort = tailUrl.getHostAndPort();

		TailClientController tailClientController = tailClientControllers
				.get(hostAndPort);
		if (tailClientController != null) {
			try {
				tailClientController.removeTailUrl(tailUrl);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		if (tailClientController != null
				&& tailClientController.getTailUrls().size() == 0) {
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			tailClientController.stopTailClientAndDataHandlerThreads();
			tailClientControllers.remove(hostAndPort);
		}
	}

	// stop tailing a file
	public void suspendTailURL(TailURL tailUrl) {
		_logger.fine("called.");
		String hostAndPort = tailUrl.getHostAndPort();

		TailClientController tailClientController = tailClientControllers
				.get(hostAndPort);
		if (tailClientController != null) {
			try {
				tailClientController.suspendTailUrl(tailUrl);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public void restartTailURL(TailURL tailUrl) {
		_logger.fine("called.");
		String hostAndPort = tailUrl.getHostAndPort();

		TailClientController tailClientController = tailClientControllers
				.get(hostAndPort);
		if (tailClientController != null) {
			try {
				tailClientController.restartTailUrl(tailUrl);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public void handleServerResponse(TailURL tailUrl, ToClient fromServer) {

		switch (fromServer.getResultCode()) {
		case ResultCode.OK:
			// clientController.appendText(fromServer.getTailLineList());
			CTabItemWrapperManager.getInstance()
					.appendLinesToCTabItemBodyFromOtherThread(tailUrl,
							fromServer.getTailLineList());
			break;
		case ResultCode.NOT_FOUND:
			// clientController.setMessage("file not found.");
			CTabItemWrapperManager.getInstance()
					.setMessageToCTabItemMessageFromOtherThread(tailUrl,
							"file not found");
			break;
		case ResultCode.EXCEPTION:
			CTabItemWrapperManager.getInstance()
					.setMessageToCTabItemMessageFromOtherThread(tailUrl,
							fromServer.getResultMessage());
			break;
		case ResultCode.FILE_ROTATED:
			CTabItemWrapperManager.getInstance()
					.setMessageToCTabItemMessageFromOtherThread(tailUrl,
							"file rotated");
			break;
		case ResultCode.ACK:
			// TODO something meaningful...
			break;
		}
	}

	public void handleTailClientException(
			TailClientController tailClientController, Thread thread,
			Throwable exception) {
		tailClientController.stopTailClientAndDataHandlerThreads();
		tailClientControllers.remove(tailClientController.getHostAndPort());

		List<TailURL> tailUrls = tailClientController.getTailUrls();
		for (TailURL tailUrl : tailUrls) {

			// CTabItemWrapperManager.getInstance().disposeCTabItemWrapper(tailUrl);
			CTabItemWrapperManager.getInstance()
					.setMessageToCTabItemMessageFromOtherThread(tailUrl,
							exception.getMessage());
		}
		// CTabItemWrapperManager.getInstance().removeCTabItemWrapper(tailUrl);
	}
}
