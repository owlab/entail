package com.enxime.entail.server;

import com.enxime.entail.share.EntailProto.ToServer;

public interface ServerProtocolListener {
	public void performCommand(ToServer fromClient);
}
