package org.edu_sharing.repository.update;

import java.io.PrintWriter;
import java.io.Serializable;
import java.util.*;
import java.util.Map.Entry;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.policy.BehaviourFilter;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchParameters;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.namespace.RegexQNamePattern;
import org.apache.log4j.Logger;
import org.edu_sharing.alfresco.workspace_administration.NodeServiceInterceptor;
import org.edu_sharing.repository.client.rpc.ACE;
import org.edu_sharing.repository.client.rpc.ACL;
import org.edu_sharing.repository.client.rpc.Notify;
import org.edu_sharing.repository.client.rpc.User;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.MCAlfrescoAPIClient;
import org.edu_sharing.repository.server.jobs.helper.NodeRunner;
import org.edu_sharing.repository.server.tools.UserEnvironmentTool;


import com.google.gson.Gson;
import org.edu_sharing.service.nodeservice.NodeServiceFactory;
import org.edu_sharing.service.nodeservice.NodeServiceHelper;
import org.edu_sharing.service.permission.PermissionServiceHelper;

public class Release_5_0_NotifyRefactoring extends UpdateAbstract {

	public static final String ID = "Release_5_0_NotifyRefactoring";

	public static final String description = "remove notify objects, use permission_history aspect";

	NodeService nodeService = serviceRegistry.getNodeService();
	MCAlfrescoAPIClient apiClient=new MCAlfrescoAPIClient();

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
		doTask();

	}
	public void doTask(){
		NodeRunner runner=new NodeRunner();
		runner.setRunAsSystem(true);
		runner.setTypes(Arrays.asList(CCConstants.CCM_TYPE_IO,CCConstants.CCM_TYPE_MAP));
		runner.setThreaded(false);
		runner.setKeepModifiedDate(true);
		runner.setTransaction(NodeRunner.TransactionMode.LocalRetrying);
		int[] processed=new int[]{0};
		runner.setTask((ref)->{
			migrate(ref);
			processed[0]++;
		});
		runner.run();
		logInfo("Converted a total of "+processed[0]+" nodes");
		try {
            String notify = new UserEnvironmentTool().getEdu_SharingNotifyFolder();
            NodeRef ref = new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, notify);
            nodeService.addAspect(ref, ContentModel.ASPECT_TEMPORARY, null);
            nodeService.deleteNode(ref);
            logInfo("removed the notify folder");
        }catch(Throwable t) {
            logger.error(t.getMessage(),t);
        }
		try{
            NodeServiceInterceptor.setEduSharingScope(CCConstants.CCM_VALUE_SCOPE_SAFE);
			String notifySafe = new UserEnvironmentTool().getEdu_SharingNotifyFolder();
            NodeRef ref = new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, notifySafe);
			nodeService.addAspect(ref, ContentModel.ASPECT_TEMPORARY, null);
			nodeService.deleteNode(ref);
            logInfo("removed the notify safe folder");
		} catch (Throwable t) {
			logger.error(t.getMessage(),t);
		}
		finally {
            NodeServiceInterceptor.setEduSharingScope(null);;
        }


	}

	private void migrate(NodeRef nodeRef) {
		List<ChildAssociationRef> notifyParentAssocs = nodeService.getParentAssocs(nodeRef,
				QName.createQName(CCConstants.CCM_ASSOC_NOTIFY_NODES), RegexQNamePattern.MATCH_ALL);

		Map<NodeRef, Map<QName, Serializable>> notifyProps = new HashMap<>();

		for (ChildAssociationRef childRef : notifyParentAssocs) {
			NodeRef notifyRef = childRef.getParentRef();
			Map<QName, Serializable> properties = nodeService.getProperties(notifyRef);
			notifyProps.put(notifyRef, properties);
		}

		List<Entry<NodeRef, Map<QName, Serializable>>> toSort = new ArrayList<>(notifyProps.entrySet());
		Collections.sort(toSort, (o1, o2) -> {
			Date o1Created = (Date) o1.getValue().get(ContentModel.PROP_CREATED);
			Date o2Created = (Date) o2.getValue().get(ContentModel.PROP_CREATED);
			return o1Created.compareTo(o2Created);
		});

		/**
		 * transform notify history
		 */
		int i=0;
		for (Map.Entry<NodeRef, Map<QName, Serializable>> entry : toSort) {
			logger.info("transforming notify from: " +entry.getKey()+" "+entry.getValue().get(ContentModel.PROP_CREATED));

			Gson gson = new Gson();
			Notify n = new Notify();

			/**
			 *  get acl from notify cause its the same as io
			 *  does not really work for notifys cause there is one notify also with the inherited permissions
			 */
			ACL acl = null;
			try {
				acl = apiClient.getPermissions(entry.getKey().getId());
			} catch (Exception e) {
				logger.warn("getPermissions() failed: "+e.getMessage());
				throw new RuntimeException(e);
			}
			List<ACE> directlySetAces = new ArrayList<ACE>();
			for (ACE ace : acl.getAces()) {
				if (!ace.isInherited()) {
					directlySetAces.add(ace);
				}
			}
			Date created = (Date) nodeService.getProperty(entry.getKey(), ContentModel.PROP_CREATED);
			String action = (String) nodeService.getProperty(entry.getKey(),
					QName.createQName(CCConstants.CCM_PROP_NOTIFY_ACTION));
			String user = (String) nodeService.getProperty(entry.getKey(),
					QName.createQName(CCConstants.CCM_PROP_NOTIFY_USER));
			acl.setAces(directlySetAces.toArray(new ACE[]{}));
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
			// set of all authority names that are not inherited, but explicitly set
			nodeService.setProperty(nodeRef, QName.createQName(CCConstants.CCM_PROP_PH_INVITED), PermissionServiceHelper.getExplicitAuthoritiesFromACL(acl));

			String jsonStringACL = gson.toJson(n);
			List<String> history = (List<String>) nodeService.getProperty(nodeRef,
					QName.createQName(CCConstants.CCM_PROP_PH_HISTORY));
			history = (history == null) ? new ArrayList<>() : history;
			history.add(jsonStringACL);
			nodeService.setProperty(nodeRef, QName.createQName(CCConstants.CCM_PROP_PH_HISTORY),
					new ArrayList<>(history));

			/**
			 * last one will be current
			 */
			if (i == (toSort.size() - 1)) {
				nodeService.setProperty(nodeRef, QName.createQName(CCConstants.CCM_PROP_PH_ACTION), action);

				ArrayList<String> phUsers = (ArrayList<String>) nodeService.getProperty(nodeRef, QName.createQName(CCConstants.CCM_PROP_PH_USERS));
				if (phUsers == null) phUsers = new ArrayList<>();
				if (!phUsers.contains(user)) phUsers.add(user);
				nodeService.setProperty(nodeRef, QName.createQName(CCConstants.CCM_PROP_PH_USERS), phUsers);
				nodeService.setProperty(nodeRef, QName.createQName(CCConstants.CCM_PROP_PH_MODIFIED), created);
			}
			nodeService.addAspect(entry.getKey(), ContentModel.ASPECT_TEMPORARY, null);
			nodeService.deleteNode(entry.getKey());
			i++;
		}

	}

	@Override
	public void test() {
		logInfo("not implemented");

	}

}
