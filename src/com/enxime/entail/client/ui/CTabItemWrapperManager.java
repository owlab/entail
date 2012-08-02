package com.enxime.entail.client.ui;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;

import com.enxime.entail.share.LogUtil;

public class CTabItemWrapperManager {
    private static final Logger _logger = LogUtil.getLogger(CTabItemWrapperManager.class.getName());
    
    //private static volatile int count = 0;
    
    private static CTabItemWrapperManager cTabItemWrapperManager = new CTabItemWrapperManager();
    
    private CTabItemWrapperManager() {
	
    }
    
    public static CTabItemWrapperManager getInstance() {
	return cTabItemWrapperManager;
    }
    
    private ConcurrentHashMap<TailURL, CTabItemWrapper> cTabItemWrapperMap = new ConcurrentHashMap<TailURL, CTabItemWrapper>();
    
    public boolean addCTabItemWrapper(CTabFolder folder, TailURL tailUrl) {
	//count++;
	_logger.fine("called.");
	if(cTabItemWrapperMap.containsKey(tailUrl)) {
	    return false;
	} else {
	    CTabItemWrapper cTabItemWrapper = new CTabItemWrapper(folder, tailUrl);
	    cTabItemWrapperMap.put(tailUrl,cTabItemWrapper );
	    cTabItemWrapper.setSelection();
	    return true;
	}
    }
    public boolean isContained(TailURL tailUrl) {
	return cTabItemWrapperMap.containsKey(tailUrl);
    }
    public void appendLinesToCTabItemBody(TailURL tailUrl, List<String> textList) {
	cTabItemWrapperMap.get(tailUrl).appendText(textList);
    }
    
    public void appendLinesToCTabItemBodyFromOtherThread(TailURL tailUrl, List<String> textList) {
	CTabItemWrapper cTabItemWrapper = cTabItemWrapperMap.get(tailUrl);
		if(cTabItemWrapper != null)
		    cTabItemWrapper.appendTextFromOtherThread(textList);
    }
    
    public void setMessageToCTabItemMessage(TailURL tailUrl, String message) {
	cTabItemWrapperMap.get(tailUrl).setMessage(message);
    }
    
    public void setMessageToCTabItemMessageFromOtherThread(TailURL tailUrl, String message) {
	cTabItemWrapperMap.get(tailUrl).setMessageFromOtherThread(message);
    }
    
    public void removeCTabItemWrapper(TailURL tailUrl) {
	_logger.fine("called.");
	CTabItemWrapper cTabItemWrapper = cTabItemWrapperMap.remove(tailUrl);
    }
    
    public int numberOfCTabItemWrapper() {
	return cTabItemWrapperMap.size();
    }
    
    public CTabItemWrapper getCTabItemWrapper(CTabItem cTabItem) {
	Collection<CTabItemWrapper> cTabItemWrappers = cTabItemWrapperMap.values();
	for(CTabItemWrapper cTabItemWrapper: cTabItemWrappers) {
	    if(cTabItemWrapper.getCTabItem().equals(cTabItem)) {
		return cTabItemWrapper;
	    }
	}
	return null;
    }
 
}
