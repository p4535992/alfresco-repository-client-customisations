package com.surevine.alfresco.webscript.perishable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.alfresco.model.ContentModel;
import org.alfresco.model.ForumModel;
import org.alfresco.repo.jscript.Scopeable;
import org.alfresco.repo.jscript.ScriptNode;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.preference.PreferenceService;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchParameters;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.cmr.version.Version;
import org.alfresco.service.cmr.version.VersionHistory;
import org.alfresco.service.cmr.version.VersionService;
import org.alfresco.service.namespace.RegexQNamePattern;
import org.apache.log4j.Logger;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.DeclarativeWebScript;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

import com.surevine.alfresco.model.ManagedDeletionModel;
import com.surevine.alfresco.presence.PresenceService;
import com.surevine.alfresco.repo.delete.ManagedDeletionService;
import com.surevine.alfresco.repo.delete.NodeArchivalDetails;

public class PerishableWebscript extends DeclarativeWebScript implements
		Scopeable {
	private static final Logger _logger = Logger
			.getLogger(PerishableWebscript.class);

	private static final StoreRef STORE_REF = new StoreRef(
			StoreRef.PROTOCOL_WORKSPACE, "SpacesStore");

	private static final int MAX_RESULTS = 100;

	private ServiceRegistry _serviceRegistry;
	private SearchService _searchService;
	private PersonService _personService;
	private NodeService _nodeService;
	private PresenceService _presenceService;
	private PreferenceService _preferenceService;
	private VersionService _versionService;
	private ManagedDeletionService _managedDeletionService;
	private Scriptable _scope;

	public void setServiceRegistry(final ServiceRegistry serviceRegistry) {
		_serviceRegistry = serviceRegistry;
		_searchService = _serviceRegistry.getSearchService();
		_personService = _serviceRegistry.getPersonService();
		_nodeService = _serviceRegistry.getNodeService();
		_versionService = _serviceRegistry.getVersionService();
	}

	public void setSearchService(final SearchService searchService) {
		_searchService = searchService;
	}

	public void setPersonService(final PersonService personService) {
		_personService = personService;
	}

	public void setNodeService(final NodeService nodeService) {
		_nodeService = nodeService;
	}

	public void setPresenceService(final PresenceService presenceService) {
		_presenceService = presenceService;
	}

	public void setManagedDeletionService(
			final ManagedDeletionService managedDeletionService) {
		_managedDeletionService = managedDeletionService;
	}

	public void setPreferenceService(final PreferenceService preferenceService) {
		_logger.debug("Setting preference service to " + preferenceService);
		_preferenceService = preferenceService;
	}

	public void init() {
	}

	@Override
	protected Map<String, Object> executeImpl(WebScriptRequest req,
			Status status, Cache cache) {
		final Map<String, Object> model = new HashMap<String, Object>();

		ensureScopePresent();

		ResultSet rs = null;
		try {
			final SearchParameters parameters = new SearchParameters();
			parameters.addStore(STORE_REF);
			parameters.setLanguage(SearchService.LANGUAGE_LUCENE);
			parameters
					.setQuery("+PATH:\"/app:company_home//*\" +ASPECT:\"md:perishable\" -ASPECT:\"md:failedToDelete\"");
			parameters.addSort("@md:perishDue", true);

			rs = _searchService.query(parameters);

			final List<NodeRef> untrimmedNodeRefs = rs.getNodeRefs();
			final List<NodeRef> nodeRefs = untrimmedNodeRefs.subList(0,
					untrimmedNodeRefs.size() > MAX_RESULTS ? MAX_RESULTS
							: untrimmedNodeRefs.size());

			final ArrayList<Map<String, Object>> results = new ArrayList<Map<String, Object>>();

			final List<ItemApplicabilityVoter> voters = getVoters(_serviceRegistry
					.getAuthenticationService().getCurrentUserName());

			for (final NodeRef nodeRef : nodeRefs) {
				boolean isApplicable = false;

				ArrayList<String> explanations = new ArrayList<String>();

				for (ItemApplicabilityVoter voter : voters) {
					if (voter.isItemApplicable(nodeRef)) {
						isApplicable = true;

						explanations.add(voter.getApplicabilityString(nodeRef));
					}
				}

				if (!isApplicable) {
					continue;
				}

				final String perishRequestedBy = String.valueOf(_nodeService.getProperty(nodeRef,
						ManagedDeletionModel.PROP_PERISH_REQUESTED_BY));

				NodeArchivalDetails archiveDetails = _managedDeletionService
						.getArchivalDetails(nodeRef);

				final Map<String, Object> item = new HashMap<String, Object>();
				item.put("node", new ScriptNode(nodeRef, _serviceRegistry,
						_scope));
				item.put("perishRequestedByUser",
						_personService.getPerson(perishRequestedBy));
				item.put("perishRequestedByPresence",
						_presenceService.getUserPresence(perishRequestedBy, false));
				item.put("perishDue", _nodeService.getProperty(nodeRef,
						ManagedDeletionModel.PROP_PERISH_DUE));
				item.put("perishApplied", _nodeService.getProperty(nodeRef,
						ManagedDeletionModel.PROP_PERISHED_APPLIED));
				item.put("archiveDue", archiveDetails.archivalDue());
				item.put("explanations", explanations);

				// Warning: Slight Hack - for some reason node.children[] on the
				// ScriptNode isn't working, so we'll get the interesting child
				// node manually. I say some reason, it's actually because
				// Context.getContext() is returning null (for some reason!).
				if (_nodeService.getType(nodeRef).equals(ForumModel.TYPE_TOPIC)) {
					List<ChildAssociationRef> postAssocRefs = _nodeService
							.getChildAssocs(nodeRef,
									ContentModel.ASSOC_CONTAINS,
									RegexQNamePattern.MATCH_ALL);

					if (postAssocRefs.isEmpty()) {
						// This really shouldn't happen, but let's not get our
						// knickers in a twist if it does.
						_logger.warn("Topic node without any posts: " + nodeRef);
						continue;
					}

					item.put("postNode", new ScriptNode(postAssocRefs.get(0)
							.getChildRef(), _serviceRegistry, _scope));
				}

				results.add(item);
			}

			// Re-sort the results as the lucene results are sorted by perishDue
			// (which does not take into account marked for deletion)
			Collections.sort(results, new Comparator<Map<String, Object>>() {
				@Override
				public int compare(Map<String, Object> arg0,
						Map<String, Object> arg1) {
					Date archiveDue0 = (Date) arg0.get("archiveDue");
					Date archiveDue1 = (Date) arg1.get("archiveDue");

					return archiveDue0.compareTo(archiveDue1);
				}
			});

			model.put("results", results);
		} finally {
			if (rs != null)
				rs.close();
		}

		return model;
	}

	private List<ItemApplicabilityVoter> getVoters(final String userName) {
		List<ItemApplicabilityVoter> applicabilityVoters = new ArrayList<ItemApplicabilityVoter>(
				5);

		applicabilityVoters = new ArrayList<ItemApplicabilityVoter>();

		applicabilityVoters.add(new IsUserCreatorVoter(userName));
		applicabilityVoters.add(new IsUserEditorVoter(userName));
		applicabilityVoters.add(new IsUserMarkerVoter(userName));
		applicabilityVoters.add(new IsUserCommenterVoter(userName));
		applicabilityVoters.add(new IsUserDiscussionReplierVoter(userName));
		applicabilityVoters.add(new HasUserFavouritedVoter(userName));

		return applicabilityVoters;
	}

	@Override
	public void setScope(Scriptable scope) {
		_scope = scope;
	}

	private void ensureScopePresent() {
		if (_scope == null) {
			// Create a scope for the value conversion. This scope will be an
			// empty scope exposing basic Object and Function, sufficient for
			// value-conversion.
			// In case no context is active for the current thread, we can
			// safely enter end exit one to get hold of a scope
			Context ctx = Context.getCurrentContext();

			boolean closeContext = false;

			if (ctx == null) {
				ctx = Context.enter();
				closeContext = true;
			}

			_scope = ctx.initStandardObjects(null);
			_scope.setParentScope(null);

			if (closeContext) {
				// Only an exit call should be done when context didn't exist
				// before
				Context.exit();
			}
		}
	}

	private interface ItemApplicabilityVoter {
		boolean isItemApplicable(NodeRef nodeRef);

		String getApplicabilityString(NodeRef nodeRef);
	}

	private abstract class AbstractItemApplicabilityVoter implements
			ItemApplicabilityVoter {
		protected String userName;

		public AbstractItemApplicabilityVoter(final String userName) {
			this.userName = userName;
		}
	}

	/**
	 * Did the user create the document?
	 */
	private class IsUserCreatorVoter extends AbstractItemApplicabilityVoter {

		public IsUserCreatorVoter(final String userName) {
			super(userName);
		}

		@Override
		public boolean isItemApplicable(NodeRef nodeRef) {
			final Object creator = _nodeService.getProperty(nodeRef,
					ContentModel.PROP_CREATOR);

			return ((creator != null) && creator.equals(userName));
		}

		@Override
		public String getApplicabilityString(NodeRef nodeRef) {
			return "perish.explanantion.creator";
		}
	}

	/**
	 * Did the user create any previous version of the document?
	 */
	private class IsUserEditorVoter extends AbstractItemApplicabilityVoter {
		public IsUserEditorVoter(String userName) {
			super(userName);
		}

		@Override
		public boolean isItemApplicable(NodeRef nodeRef) {
			VersionHistory versionHistory = _versionService
					.getVersionHistory(nodeRef);

			if (versionHistory == null) {
				return false;
			}

			Collection<Version> versions = versionHistory.getAllVersions();

			if (versions == null) {
				return false;
			}

			for (Version version : versions) {
				if (version.getFrozenModifier().equals(userName)) {
					return true;
				}
			}

			return false;
		}

		@Override
		public String getApplicabilityString(NodeRef nodeRef) {
			return "perish.explanantion.editor";
		}
	}

	/**
	 * Did the user mark the document as perishable
	 */
	private class IsUserMarkerVoter extends AbstractItemApplicabilityVoter {
		public IsUserMarkerVoter(String userName) {
			super(userName);
		}

		@Override
		public boolean isItemApplicable(NodeRef nodeRef) {
			Object user = _nodeService.getProperty(nodeRef,
					ManagedDeletionModel.PROP_PERISH_REQUESTED_BY);

			return ((user != null) && user.equals(userName));
		}

		@Override
		public String getApplicabilityString(NodeRef nodeRef) {
			return "perish.explanantion.marker";
		}
	}

	/**
	 * Did the user comment on the document?
	 */
	private class IsUserCommenterVoter extends AbstractItemApplicabilityVoter {
		public IsUserCommenterVoter(String userName) {
			super(userName);
		}

		@Override
		public boolean isItemApplicable(NodeRef nodeRef) {
			if (!ContentModel.TYPE_CONTENT
					.equals(_nodeService.getType(nodeRef))) {
				return false;
			}

			List<ChildAssociationRef> forumAssocRefs = _nodeService
					.getChildAssocs(nodeRef, ForumModel.ASSOC_DISCUSSION,
							RegexQNamePattern.MATCH_ALL);

			if (forumAssocRefs.size() == 0) {
				return false;
			}

			ChildAssociationRef forumAssocRef = forumAssocRefs.get(0);

			List<ChildAssociationRef> topicNodeAssocRefs = _nodeService
					.getChildAssocs(forumAssocRef.getChildRef(),
							ContentModel.ASSOC_CONTAINS,
							RegexQNamePattern.MATCH_ALL);

			if (topicNodeAssocRefs.size() == 0) {
				return false;
			}

			ChildAssociationRef topicAssocRef = topicNodeAssocRefs.get(0);

			List<ChildAssociationRef> postAssocRefs = _nodeService
					.getChildAssocs(topicAssocRef.getChildRef(),
							ContentModel.ASSOC_CONTAINS,
							RegexQNamePattern.MATCH_ALL);

			if (postAssocRefs.size() == 0) {
				return false;
			}

			for (ChildAssociationRef postAssocRef : postAssocRefs) {
				Object creator = _nodeService.getProperty(
						postAssocRef.getChildRef(), ContentModel.PROP_CREATOR);

				if ((creator != null) && creator.equals(userName)) {
					return true;
				}
			}

			return false;
		}

		@Override
		public String getApplicabilityString(NodeRef nodeRef) {
			return "perish.explanantion.commenter";
		}
	}

	/**
	 * Did the user reply to a discussion?
	 */
	private class IsUserDiscussionReplierVoter extends
			AbstractItemApplicabilityVoter {
		public IsUserDiscussionReplierVoter(String userName) {
			super(userName);
		}

		@Override
		public boolean isItemApplicable(NodeRef nodeRef) {
			if (!ForumModel.TYPE_TOPIC.equals(_nodeService.getType(nodeRef))) {
				return false;
			}

			List<ChildAssociationRef> postAssocRefs = _nodeService
					.getChildAssocs(nodeRef, ContentModel.ASSOC_CONTAINS,
							RegexQNamePattern.MATCH_ALL);

			if (postAssocRefs.size() <= 1) { // Need more than the initial reply.
				return false;
			}
			
			final String topicName = _nodeService.getProperty(nodeRef,
					ContentModel.PROP_NAME).toString();

			for (ChildAssociationRef postAssocRef : postAssocRefs) {
				Object creator = _nodeService.getProperty(
						postAssocRef.getChildRef(), ContentModel.PROP_CREATOR);

				if ((creator != null) && creator.equals(userName)) {
					final String postName = _nodeService.getProperty(
							postAssocRef.getChildRef(), ContentModel.PROP_NAME
							).toString();
					
					if (!topicName.equals(postName)) { // Exclude initial reply.
						return true;
					}
				}
			}

			return false;
		}

		@Override
		public String getApplicabilityString(NodeRef nodeRef) {
			return "perish.explanantion.replier";
		}
	}

	/**
	 * Has the user favourited the document?
	 */
	private class HasUserFavouritedVoter extends AbstractItemApplicabilityVoter {
		private HashSet<String> favourites = null;

		public HasUserFavouritedVoter(String userName) {
			super(userName);

			Map<String, Serializable> prefs = _preferenceService
					.getPreferences(userName,
							"org.alfresco.share.documents.favourites");

			Object prefsFavourites = prefs
					.get("org.alfresco.share.documents.favourites");

			if (prefsFavourites == null) {
				return;
			}

			String favouritesString = prefsFavourites.toString();

			if (favouritesString == null) {
				return;
			}

			String[] favouritesArray = favouritesString.split(",");

			favourites = new HashSet<String>();

			for (int i = 0; i < favouritesArray.length; ++i) {
				favourites.add(favouritesArray[i]);
			}

		}

		@Override
		public boolean isItemApplicable(NodeRef nodeRef) {
			return ((favourites != null) && favourites.contains(nodeRef.toString()));
		}

		@Override
		public String getApplicabilityString(NodeRef nodeRef) {
			return "perish.explanantion.favourite";
		}
	}
}
