/*
 * Copyright (C) 2008-2010 Surevine Limited.
 *   
 * Although intended for deployment and use alongside Alfresco this module should
 * be considered 'Not a Contribution' as defined in Alfresco'sstandard contribution agreement, see
 * http://www.alfresco.org/resource/AlfrescoContributionAgreementv2.pdf
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
*/
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
