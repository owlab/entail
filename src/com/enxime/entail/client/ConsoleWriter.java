package com.enxime.entail.client;

import java.util.logging.Logger;

import com.enxime.entail.share.EntailProto.ToClient;
import com.enxime.entail.share.LogUtil;
import com.enxime.entail.share.ResultCode;

public class ConsoleWriter implements ClientProtocolListener {
    private static final Logger _logger = LogUtil.getLogger(ConsoleWriter.class.getName());
	
	@Override
	public void handleServerResponse(ToClient fromServer) {
		int resultCode = fromServer.getResultCode();
		
		if(resultCode == ResultCode.OK) {
			for(String line : fromServer.getTailLineList())
				System.out.println(line);
		} else if(resultCode == ResultCode.FILE_ROTATED) {
			System.out.println("File rotated.");
		} else if(resultCode == ResultCode.NOT_FOUND) {
			System.out.println("File not found.");
		} else if(resultCode == ResultCode.EXCEPTION) {
			System.out.println("Exception occurred: " + fromServer.getResultMessage());
		} else {
			_logger.severe("Not valid result code: " + resultCode);
		}
	}
}
