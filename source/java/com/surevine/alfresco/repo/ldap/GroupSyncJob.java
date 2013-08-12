/**
 * 
 */
package com.surevine.alfresco.repo.ldap;

import org.apache.log4j.Logger;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.http.HttpStatus;

import com.surevine.alfresco.AlfrescoException;
import com.surevine.alfresco.CasAuthenticator;
import com.surevine.alfresco.PropertyException;
import com.surevine.alfresco.PropertyWrapper;
import com.surevine.alfresco.connector.SecurityModelConnector;
import com.surevine.alfresco.connector.model.AlfrescoHttpResponse;

/**
 * A quartz job which calls the SV security-model web service /alfresco/wcs/surevine/security-model.
 * 
 * @author richardm
 */
public class GroupSyncJob implements Job {
	
	private final Logger LOG = Logger.getLogger(GroupSyncJob.class);
	
	/**
	 * Location of the properties required by the connector.
	 */
	private static final String SYNC_PROPERTIES = 
			"alfresco/module/com_surevine_alfresco_SvThemeRepoModule/com/surevine/alfresco/repo/ldap/ldapgroupsync";

	/**
	 * Execution frequency defined in scheduled-jobs-context.xml
	 */
	@Override
	public void execute(final JobExecutionContext ctx) throws JobExecutionException {
		PropertyWrapper wrapper;
		CasAuthenticator authenticator;
		try {
			wrapper = new PropertyWrapper(SYNC_PROPERTIES);
			authenticator = new CasAuthenticator(wrapper);
		} catch (final PropertyException e) {
			LOG.error(e);
			throw new JobExecutionException(
					"Configuration error when checking for security model updates.", e);
		}
		
		SecurityModelConnector connector;
		AlfrescoHttpResponse securityModel;
		try {
			connector = new SecurityModelConnector(wrapper, authenticator);
			securityModel = connector.getSecurityModel();
		} catch (final AlfrescoException e) {
			LOG.error(e);
			throw new JobExecutionException(
					"Failure while checking for updates to the security model.", e);
		}
		
		if (securityModel.getStatusCode() != HttpStatus.NOT_MODIFIED.value()) {
			LOG.info("The security model has been modified in LDAP. Attempting to update...");
			
			try {
				final String securityModelXML = securityModel.asString();
				
				if (LOG.isDebugEnabled()) {
					LOG.debug(securityModelXML);
				}
				
				connector.setSecurityModel(securityModelXML);
			} catch (final AlfrescoException e) {
				LOG.error("SEVERE ERROR UPDATING SECURITY MODEL", e);
				throw new JobExecutionException(
						"The model has been updated in LDAP but failed updating the model in Alfresco.", e);
			}
			
			LOG.info("...security model has been updated successfully.");
		} else {
			LOG.info("The security model has not been modified in LDAP. Skipping update to Alfresco.");
		}
	}
}
