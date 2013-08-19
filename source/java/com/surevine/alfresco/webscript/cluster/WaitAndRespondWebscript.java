package com.surevine.alfresco.webscript.cluster;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.extensions.webscripts.AbstractWebScript;
import org.springframework.extensions.webscripts.WebScriptRequest;
import org.springframework.extensions.webscripts.WebScriptResponse;


public class WaitAndRespondWebscript  extends AbstractWebScript {
	
	private static final Log _logger = LogFactory.getLog(WaitAndRespondWebscript.class);

	private volatile static Integer NUMBER_OF_CONCURRENT_REQUESTS=0;
	
	private long _waitSeconds=60l;
	
	public void setWaitSeconds(long ws) {
		_waitSeconds=ws;
	}

	private synchronized void incrementCurrentRequestCount() {
		synchronized (NUMBER_OF_CONCURRENT_REQUESTS) {
			NUMBER_OF_CONCURRENT_REQUESTS++;
		}
	}
	
	private synchronized void decrementCurrentRequestCount() {
		synchronized (NUMBER_OF_CONCURRENT_REQUESTS) {
			NUMBER_OF_CONCURRENT_REQUESTS--;
		}
	}
	
	public int getNumberOfConcurrentRequests() {
		return NUMBER_OF_CONCURRENT_REQUESTS;
	}

	@Override
	public void execute(WebScriptRequest request, WebScriptResponse response) throws IOException {
		try {
			incrementCurrentRequestCount();
			_logger.info("Starting "+WaitAndRespondWebscript.class+". "+getNumberOfConcurrentRequests()+" now active");
			try {
				Thread.sleep(_waitSeconds*1000l);
			} catch (InterruptedException e) {
				_logger.warn(WaitAndRespondWebscript.class+" interrupted!");
			}
			response.setStatus(200);
			response.setContentType("text/html");
			response.getWriter().write("<html><head><title>Alfresco</title></head><body>OK</body></html>");
			response.getWriter().flush();
		}
		finally {
			decrementCurrentRequestCount();
			_logger.info("Finishing "+WaitAndRespondWebscript.class+". "+getNumberOfConcurrentRequests()+" now active");
		}
	}
}
