package org.edu_sharing.repository.update;

import org.alfresco.repo.policy.BehaviourFilter;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.apache.log4j.Logger;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.MCAlfrescoAPIClient;
import org.edu_sharing.repository.server.jobs.helper.NodeRunner;
import org.edu_sharing.service.nodeservice.RecurseMode;

import java.io.PrintWriter;
import java.io.Serializable;
import java.util.List;
import java.util.function.Consumer;

public class Release_5_0_Educontext_Default extends UpdateAbstract {

	public static final String ID = "Release_5_0_Educontext_Default";

	public static final String description = "add the default value \""+CCConstants.EDUCONTEXT_DEFAULT+"\" into the new field "+CCConstants.CCM_PROP_EDUCONTEXT_NAME;


	NodeService nodeService = serviceRegistry.getNodeService();

	MCAlfrescoAPIClient apiClient;

	public Release_5_0_Educontext_Default(PrintWriter out) {
		this.out = out;
		this.logger = Logger.getLogger(Release_5_0_Educontext_Default.class);
	}

	@Override
	public String getDescription() {
		return description;
	}

	@Override
	public String getId() {
		return ID;
	}

	@Override
	public void run() throws Throwable {
		doTask(false);
	}

	@Override
	public void test() {
		doTask(true);
	}

	private void doTask(boolean test) {
		NodeRunner runner=new NodeRunner();
		runner.setRunAsSystem(true);
		runner.setTypes(CCConstants.EDUCONTEXT_TYPES);
		runner.setThreaded(true);
		runner.setTransaction(NodeRunner.TransactionMode.LocalRetrying);
		runner.setKeepModifiedDate(true);
		runner.setRecurseMode(RecurseMode.All);
		int[] processed=new int[]{0};
		runner.setFilter((ref)->{
			Serializable prop = nodeService.getProperty(ref, QName.createQName(CCConstants.CCM_PROP_EDUCONTEXT_NAME));
			// we want to return true for all nodes which don't have the property set yet
			if(prop==null)
				return true;
			if(prop instanceof String) {
				 return ((String)prop).isEmpty();
			}
			else if(prop instanceof List){
				return ((List)prop).isEmpty();
			}
			else
				return true;
		});
		runner.setTask((ref)->{
			logDebug("add educontext to "+ref.getId());
			if(!test){
				nodeService.setProperty(ref,QName.createQName(CCConstants.CCM_PROP_EDUCONTEXT_NAME),CCConstants.EDUCONTEXT_DEFAULT);
			}
			processed[0]++;
		});
        runner.run();
		logInfo("Added educontext default value to a total of "+processed[0]+" nodes");
	}


}
