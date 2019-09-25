package org.edu_sharing.repository.update;

import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.policy.BehaviourFilter;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchParameters;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.namespace.RegexQNamePattern;
import org.apache.log4j.Logger;
import org.edu_sharing.repository.client.rpc.ACE;
import org.edu_sharing.repository.client.rpc.ACL;
import org.edu_sharing.repository.client.rpc.Notify;
import org.edu_sharing.repository.client.rpc.User;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.MCAlfrescoAPIClient;
import org.edu_sharing.service.Constants;

import com.google.gson.Gson;
import org.edu_sharing.service.permission.PermissionServiceHelper;

public class Release_5_0_NotifyRefactoring extends UpdateAbstract {

	public static final String ID = "Release_5_0_NotifyRefactoring";

	public static final String description = "remove notify objects, use permission_history aspect";

	int maxItems = 100;

	SearchService searchService = serviceRegistry.getSearchService();
	NodeService nodeService = serviceRegistry.getNodeService();

	BehaviourFilter policyBehaviourFilter = (BehaviourFilter) applicationContext.getBean("policyBehaviourFilter");

	MCAlfrescoAPIClient apiClient;

	public Release_5_0_NotifyRefactoring(PrintWriter out) {
		this.out = out;
		this.logger = Logger.getLogger(Release_5_0_NotifyRefactoring.class);
	}

	@Override
	public void execute() {
		this.executeWithProtocolEntry();
	}

	@Override
	public String getDescription() {
		// TODO Auto-generated method stub
		return description;
	}

	@Override
	public String getId() {
		// TODO Auto-generated method stub
		return ID;
	}

	@Override
	public void run() throws Throwable {
		apiClient = new MCAlfrescoAPIClient();

		SearchParameters sp = new SearchParameters();
		sp.addStore(Constants.storeRef);
		sp.setLanguage(SearchService.LANGUAGE_LUCENE);
		sp.setMaxItems(maxItems);
		sp.setQuery("TYPE:\"ccm:io\"");
		sp.setSkipCount(0);

		doIt(sp, null);

	}

	private void doIt(SearchParameters sp, ResultSet rs) throws Throwable{
		if (rs != null) {
			sp.setSkipCount(rs.getStart() + maxItems);
		}
		rs = searchService.query(sp);

		for (NodeRef nodeRef : rs.getNodeRefs()) {

			logger.info("migrating " + nodeService.getProperty(nodeRef, ContentModel.PROP_NAME));

			List<ChildAssociationRef> notifyParentAssocs = nodeService.getParentAssocs(nodeRef,
					QName.createQName(CCConstants.CCM_TYPE_NOTIFY + "_nodes"), RegexQNamePattern.MATCH_ALL);

			Map<NodeRef, Map<QName, Serializable>> notifyProps = new HashMap<NodeRef, Map<QName, Serializable>>();

			for (ChildAssociationRef childRef : notifyParentAssocs) {
				NodeRef notifyRef = childRef.getParentRef();
				Map<QName, Serializable> properties = nodeService.getProperties(notifyRef);
				notifyProps.put(notifyRef, properties);
			}

			List<Map.Entry<NodeRef, Map<QName, Serializable>>> toSort = new ArrayList<Map.Entry<NodeRef, Map<QName, Serializable>>>();

			for (Map.Entry<NodeRef, Map<QName, Serializable>> entry : notifyProps.entrySet()) {
				toSort.add(entry);
			}

			Collections.sort(toSort, new Comparator<Map.Entry<NodeRef, Map<QName, Serializable>>>() {
				@Override
				public int compare(Entry<NodeRef, Map<QName, Serializable>> o1,
						Entry<NodeRef, Map<QName, Serializable>> o2) {
					Date o1Created = (Date) o1.getValue().get(ContentModel.PROP_CREATED);
					Date o2Created = (Date) o2.getValue().get(ContentModel.PROP_CREATED);
					return o1Created.compareTo(o2Created);
				}
			});

			/**
			 * transform notify history
			 */
			int i = 0;

			try {
				// prevent modified date change
				policyBehaviourFilter.disableBehaviour(nodeRef);
				for (Map.Entry<NodeRef, Map<QName, Serializable>> entry : toSort) {
					logger.info(" transforming notify from: " + entry.getValue().get(ContentModel.PROP_CREATED));

					Gson gson = new Gson();
					Notify n = new Notify();

					/**
					 *  get acl from notify cause its the same as io
					 *  does not really work for notifys cause there is one notify also with the inherited permissions
					 */
					ACL acl = apiClient.getPermissions(entry.getKey().getId());
					List<ACE> directlySetAces = new ArrayList<ACE>();
					for (ACE ace : acl.getAces()) {
						if (!ace.isInherited()) {
							directlySetAces.add(ace);
						}
					}
					// set of all authority names that are not inherited, but explicitly set
					nodeService.setProperty(nodeRef, QName.createQName(CCConstants.CCM_PROP_PH_INVITED), PermissionServiceHelper.getExplicitAuthoritiesFromACL(acl));

					Date created = (Date) nodeService.getProperty(entry.getKey(), ContentModel.PROP_CREATED);
					String action = (String) nodeService.getProperty(entry.getKey(),
							QName.createQName(CCConstants.CCM_PROP_NOTIFY_ACTION));
					String user = (String) nodeService.getProperty(entry.getKey(),
							QName.createQName(CCConstants.CCM_PROP_NOTIFY_USER));
					acl.setAces(directlySetAces.toArray(new ACE[] {}));
					n.setAcl(acl);
					n.setCreated(created);
					n.setNotifyAction(action);
					n.setNotifyUser(user);
					User u = new User();
					u.setAuthorityName(user);
					u.setUsername(user);
					n.setUser(u);

					if (!nodeService.hasAspect(nodeRef, QName.createQName(CCConstants.CCM_ASPECT_PERMISSION_HISTORY))) {
						nodeService.addAspect(nodeRef, QName.createQName(CCConstants.CCM_ASPECT_PERMISSION_HISTORY),
								null);
					}
					String jsonStringACL = gson.toJson(n);
					List<String> history = (List<String>) nodeService.getProperty(nodeRef,
							QName.createQName(CCConstants.CCM_PROP_PH_HISTORY));
					history = (history == null) ? new ArrayList<String>() : history;
					history.add(jsonStringACL);
					nodeService.setProperty(nodeRef, QName.createQName(CCConstants.CCM_PROP_PH_HISTORY),
							new ArrayList(history));

					/**
					 * last one will be current
					 */
					if (i == (toSort.size() - 1)) {
						nodeService.setProperty(nodeRef, QName.createQName(CCConstants.CCM_PROP_PH_ACTION), action);
						
						ArrayList<String> phUsers = (ArrayList<String>)nodeService.getProperty(nodeRef, QName.createQName(CCConstants.CCM_PROP_PH_USERS));
						if(phUsers == null) phUsers = new ArrayList<String>();
						if(!phUsers.contains(user)) phUsers.add(user);
						nodeService.setProperty(nodeRef, QName.createQName(CCConstants.CCM_PROP_PH_USERS), phUsers);
						nodeService.setProperty(nodeRef, QName.createQName(CCConstants.CCM_PROP_PH_MODIFIED), created);
					}
					nodeService.addAspect(entry.getKey(), ContentModel.ASPECT_TEMPORARY, null);
					nodeService.deleteNode(entry.getKey());

					i++;

				}
			} catch (Throwable e1) {
				logger.error(e1.getMessage(), e1);
				throw e1;
			}finally {
				policyBehaviourFilter.enableBehaviour(nodeRef);
			}

		}
	}

	@Override
	public void test() {
		logInfo("not implemented");

	}

}
