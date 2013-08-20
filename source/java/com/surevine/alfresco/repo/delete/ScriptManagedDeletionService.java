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
package com.surevine.alfresco.repo.delete;

import java.util.ArrayList;
import java.util.List;

import org.alfresco.repo.jscript.ScriptNode;
import org.alfresco.repo.jscript.ScriptableHashMap;
import org.alfresco.repo.processor.BaseProcessorExtension;
import org.alfresco.service.cmr.repository.NodeRef;

/**
 * Class which wraps a {@link ManagedDeletionService} and makes it available to
 * the JS api.
 */
public class ScriptManagedDeletionService extends BaseProcessorExtension {

	private ManagedDeletionService _delegate;
	private PerishabilityLogic _perishabilityLogic;

	/**
	 * Sets the {@link ManagedDeletionService} which calls will be delegated to.
	 * 
	 * @param delegate
	 */
	public void setDelegate(final ManagedDeletionService delegate) {
		_delegate = delegate;
	}

	/**
	 * Sets the {@link PerishabilityLogic} which will be used to provide perish
	 * reasons.
	 * 
	 * @param perishabilityLogic
	 */
	public void setPerishabilityLogic(
			final PerishabilityLogic perishabilityLogic) {
		_perishabilityLogic = perishabilityLogic;
	}

	/**
	 * @see ManagedDeletionService#markForDelete(NodeRef)
	 */
	public void markForDelete(final ScriptNode node) {
		_delegate.markForDelete(node.getNodeRef());
	}

	/**
	 * @see ManagedDeletionService#removeDeletionMark(NodeRef)
	 */
	public void removeDeletionMark(final ScriptNode node) {
		_delegate.removeDeletionMark(node.getNodeRef());
	}

	/**
	 * @see ManagedDeletionService#delete(NodeRef)
	 */
	public NodeRef delete(final ScriptNode node)
			throws ManagedDeletionException {
		return _delegate.delete(node.getNodeRef());
	}

	/**
	 * @see ManagedDeletionService#undelete(NodeRef)
	 */
	public void undelete(final ScriptNode node) throws ManagedDeletionException {
		_delegate.undelete(node.getNodeRef());
	}

	/**
	 * @see ManagedDeletionService#setPerishable(NodeRef, String)
	 */
	public void setPerishable(final ScriptNode node, final String reason) {
		_delegate.setPerishable(node.getNodeRef(), ("".equals(reason) ? null
				: reason));
	}

	/**
	 * Gets the archival details about the node as follows:
	 * 
	 * <pre>
	 * {
	 *   "status": The current archival status of the node ("UNMARKED", "PERISHABLE", "MARKED_FOR_DELETE", or "DELETED"),
	 *   "archivalDue": The timestamp for the archival of the node,
	 *   "archivalUser": The user that requested this node be archived/perished,
	 *   "perishableReason": The reason that this item was made perishable
	 * }
	 * </pre>
	 * 
	 * @param node
	 *            the node
	 * @return
	 */
	public ScriptableHashMap<String, Object> getArchivalDetails(
			final ScriptNode node) {
		ScriptableHashMap<String, Object> result = new ScriptableHashMap<String, Object>();

		NodeArchivalDetails details = _delegate.getArchivalDetails(node
				.getNodeRef());

		result.put("status", details.getStatus().toString());

		switch (details.getStatus()) {
		case MARKED_FOR_DELETE:
			result.put("archivalDue", node.getProperties().get("md:archivalDue"));
			break;
		case PERISHABLE:
			result.put("archivalDue", node.getProperties().get("md:perishDue"));
			break;
		default:
			result.put("archivalDue", null);
			break;
		}

		result.put("archivalUser", details.archivalUser());

		String perishableReason = details.perishableReason();
		result.put("perishableReason", perishableReason);

		return result;
	}

	/**
	 * Gets the list of perish reason as follows:
	 * 
	 * <pre>
	 * [
	 *   {
	 *     code: "reason1",
	 *     perishDays: 28
	 *   },
	 *   {
	 *     code: "reason2",
	 *     perishDays: 28
	 *   }
	 * ]
	 * </pre>
	 * 
	 * @param site
	 *            the site name
	 * @return
	 */
	public List<ScriptableHashMap<String, Object>> getPerishReasons(
			final String site) {
		List<PerishReason> reasons = _perishabilityLogic.getPerishReasons(site);

		List<ScriptableHashMap<String, Object>> scriptReasons = new ArrayList<ScriptableHashMap<String, Object>>(
				reasons.size());

		for (PerishReason reason : reasons) {
			ScriptableHashMap<String, Object> scriptReason = new ScriptableHashMap<String, Object>();

			scriptReason.put("code", reason.getCode());
			scriptReason.put("perishDays", reason.getPerishDays());
			scriptReason.put("title", reason.getTitle());
			scriptReason.put("description", reason.getDescription());

			scriptReasons.add(scriptReason);
		}

		return scriptReasons;
	}

	/**
	 * Gets a perish reason by the reason code.
	 * 
	 * @param reasonCode
	 * @return
	 */
	public ScriptableHashMap<String, Object> getPerishReason(
			final String reasonCode) {
		PerishReason reason = _perishabilityLogic.getPerishReason(reasonCode);

		if (reason == null) {
			return null;
		}

		ScriptableHashMap<String, Object> scriptReason = new ScriptableHashMap<String, Object>();

		scriptReason.put("code", reason.getCode());
		scriptReason.put("perishDays", reason.getPerishDays());
		scriptReason.put("title", reason.getTitle());
		scriptReason.put("description", reason.getDescription());

		return scriptReason;
	}
}
