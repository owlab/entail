package com.enxime.entail.experiment;

import java.util.logging.Logger;

import com.enxime.entail.share.EntailProto.ToClient;
import com.enxime.entail.share.LogUtil;
import com.lmax.disruptor.EventFactory;

public class ServerEvent {
	private static Logger logger = LogUtil.getLogger(ServerEvent.class);

	public static final EventFactory<ServerEvent> EVENT_FACTORY = new SimpleEventFactory();

	private volatile ToClient fromServer;
	
	private static volatile int instanceCount;

	private static final class SimpleEventFactory implements
			EventFactory<ServerEvent> {
		@Override
		public ServerEvent newInstance() {
			logger.info("Num. of instances = " + ++instanceCount);
			return new ServerEvent();
		}
	}
	
	public ToClient getServerEvent() {
		return this.fromServer;
	}
	
	public void setServerEvent(ToClient fromServer) {
		this.fromServer = fromServer;
	}


}
