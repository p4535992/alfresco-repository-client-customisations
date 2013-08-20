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
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.transaction.TransactionService;
import org.apache.commons.lang.time.DateUtils;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * An implementation which loads the perish definitions from a file in the
 * repository. The file should be: Workspace://SpacesStore/Company Home/Data
 * Dictionary/perishableReasons.json. The format of the file is:
 * <pre>
 * {
 *   perishableReasons:[
 *     {
 *       code: "reason1",
 *       perishDays: 14,
 *       title: "Reason 1",
 *       description: "This is Reason 1",
 *       sites: ["site1"]
 *     },{
 *       code: "reason2",
 *       perishDays: 28,
 *       title: "Reason 2",
 *       description: "This is Reason 2",
 *       sites: ["site1","site2"]
 *     }
 *   ]
 * }
 * </pre>
 */
public class PerishabilityLogicImpl implements PerishabilityLogic {

	private final static Logger _logger = Logger
			.getLogger(PerishabilityLogicImpl.class);

	private SearchService _searchService;

	public void setSearchService(final SearchService searchService) {
		_searchService = searchService;
	}

	private ContentService _contentService;

	public void setContentService(final ContentService contentService) {
		_contentService = contentService;
	}

	private TransactionService _transactionService;

	public void setTransactionService(
			final TransactionService transactionService) {
		_transactionService = transactionService;
	}

	private List<PerishReason> perishReasons;
	private Map<String, PerishReason> perishReasonsByCode;
	private Map<String, List<PerishReason>> perishReasonsBySite;

	/**
	 * Initialise the bean by loading the perish reasons from the repository.
	 */
	public void init() {
		try {
			loadPerishReasons();
		} catch(Exception e) {
			// We want to prevent this failing at all costs as we don't want to prevent startup of the application
			_logger.error("Exception caught while initialising perish reasons", e);
		}
	}

	@Override
	public Date calculatePerishDue(final String reasonCode, final Date currentDate) {
		PerishReason reason = perishReasonsByCode.get(reasonCode);

		if (reason == null) {
			throw new IllegalArgumentException(reasonCode
					+ " is not a valid reason code");
		}

		Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
		cal.setTime(currentDate);
		cal.add(Calendar.DAY_OF_MONTH, reason.getPerishDays() + 1);
		cal = DateUtils.truncate(cal, Calendar.DAY_OF_MONTH);
		return cal.getTime();
	}

	@Override
	public List<PerishReason> getPerishReasons(final String site) {
		List<PerishReason> reasons = perishReasonsBySite.get(site);

		if (reasons == null) {
			return Collections.emptyList();
		}

		return reasons;
	}

	private synchronized void loadPerishReasons() throws JSONException,
			SecurityException, IllegalStateException, RollbackException,
			HeuristicMixedException, HeuristicRollbackException,
			SystemException, NotSupportedException {
		UserTransaction transaction = null;
		ResultSet rs = null;

		try {
			StoreRef storeRef = new StoreRef(StoreRef.PROTOCOL_WORKSPACE,
					"SpacesStore");
			rs = _searchService
					.query(storeRef, SearchService.LANGUAGE_LUCENE,
							"PATH:\"/app:company_home/app:dictionary/cm:perishableReasons.json\"");
			NodeRef nodeRef = null;
			transaction = _transactionService.getUserTransaction(true);

			transaction.begin();
			
			if (rs.length() == 0) {
				_logger.error("Unable to load perishable reasons: Didn't find perishableReasons.json in the Data Dictionary.");
				perishReasons = Collections.emptyList();
				perishReasonsByCode = Collections.emptyMap();
				perishReasonsBySite = Collections.emptyMap();
				return;
			}
			nodeRef = rs.getNodeRef(0);

			ContentReader reader = _contentService.getReader(nodeRef,
					ContentModel.PROP_CONTENT);

			JSONObject obj = new JSONObject(reader.getContentString());

			JSONArray perishableReasons = obj.getJSONArray("perishableReasons");

			perishReasons = new ArrayList<PerishReason>(
					perishableReasons.length());
			perishReasonsByCode = new HashMap<String, PerishReason>();
			perishReasonsBySite = new HashMap<String, List<PerishReason>>();

			for (int i = 0; i < perishableReasons.length(); ++i) {
				PerishReason reason = PerishReason.fromJSON(perishableReasons
						.getJSONObject(i));
				perishReasons.add(reason);
				perishReasonsByCode.put(reason.getCode(), reason);
				addPerishReasonBySite(reason);
			}

			transaction.commit();
		} finally {
			if (rs != null) {
				rs.close();
			}

			if ((transaction != null)
					&& (transaction.getStatus() == Status.STATUS_ACTIVE)) {
				transaction.rollback();
			}
		}
	}

	private void addPerishReasonBySite(final PerishReason reason) {
		for (String site : reason.getSites()) {
			List<PerishReason> reasons = perishReasonsBySite.get(site);
			if (reasons == null) {
				reasons = new ArrayList<PerishReason>();
				perishReasonsBySite.put(site, reasons);
			}

			reasons.add(reason);
		}
	}

	@Override
	public PerishReason getPerishReason(String reasonCode) {
		return perishReasonsByCode.get(reasonCode);
	}
}
