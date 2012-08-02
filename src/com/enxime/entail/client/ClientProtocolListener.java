package com.enxime.entail.client;

import com.enxime.entail.share.EntailProto.ToClient;

public interface ClientProtocolListener {
	public void handleServerResponse(ToClient fromServer);
}
