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
package com.surevine.alfresco.repo.action;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.content.ContentServicePolicies;
import org.alfresco.repo.policy.Behaviour.NotificationFrequency;
import org.alfresco.repo.policy.JavaBehaviour;
import org.alfresco.repo.policy.PolicyComponent;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.surevine.alfresco.model.ManagedDeletionModel;

/**
 * Behavior to provide validation for Perishable Reasons.
 * 
 * @author richardm
 */
public class PerishableReasonsValidation implements ContentServicePolicies.OnContentUpdatePolicy {

	private static final Log LOGGER = LogFactory.getLog(PerishableReasonsValidation.class);

	private PolicyComponent _policyComponent;
	private NodeService _nodeService;
	private ContentService _contentService;

	/**
	 * Injected
	 * 
	 * @param policyComponent
	 */
	public void setPolicyComponent(final PolicyComponent policyComponent) {
		_policyComponent = policyComponent;
	}

	/**
	 * Injected
	 * 
	 * @param nodeService
	 */
	public void setNodeService(final NodeService nodeService) {
		_nodeService = nodeService;
	}

	/**
	 * Injected
	 */
	public void setContentService(final ContentService contentService) {
		_contentService = contentService;
	}

	/**
	 * Register this behaviour with the policy component.
	 */
	public void init() {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Initialising Perishable Reasons Validation bean");
		}

		/**
		 * Fire the policy when content is updated, at the end of the
		 * transaction, for content and discussions
		 */
		_policyComponent.bindClassBehaviour(
				ContentServicePolicies.OnContentUpdatePolicy.QNAME,
				ManagedDeletionModel.ASPECT_VALIDATE_PERISHABLE_REASONS,
				new JavaBehaviour(this, "onContentUpdate", NotificationFrequency.TRANSACTION_COMMIT));

		if (LOGGER.isInfoEnabled()) {
			LOGGER.info("Perishable Reasons Validation bean initialised");
		}
	}

	@Override
	public void onContentUpdate(NodeRef nodeRef, boolean newContent) {
		// Because we're firing post-transaction, it's possible for a node (say,
		// a working copy) to no longer exist by the time we are fired
		if (_nodeService.exists(nodeRef)) {
			final String content = _contentService.getReader(nodeRef,
					ContentModel.PROP_CONTENT).getContentString();
			
			if (LOGGER.isInfoEnabled()) {
				LOGGER.info("New Perishable reasons: " + content);
			}
			
			validatePerishableReasons(content);
		}
	}
	
	public void validatePerishableReasons(final String json) {
		try {
			final JSONObject root = new JSONObject(json);
			
			assertHas(root, "perishableReasons");
			
			final JSONArray reasons = root.getJSONArray("perishableReasons");
			
			for (int i = 0; i<reasons.length(); i++) {
				final JSONObject reason = reasons.getJSONObject(i);

				assertHas(reason, "code");
				assertHas(reason, "perishDays");
				assertHas(reason, "title");
				assertHas(reason, "description");
				assertHas(reason, "sites");
			}
		} catch (JSONException e) {
			throw new RuntimeException("Failed to parse perishableReasons as a JSON object.", e);
		}
	}
	
	public void assertHas(final JSONObject object, final String key) {
		if (!object.has(key)) {
			throw new RuntimeException(String.format(
					"Unable to find a %s key on the object %s",
					key, object.toString()));
		}
	}
}
