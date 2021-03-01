package org.edu_sharing.repository.server.jobs.quartz;

import org.alfresco.service.ServiceRegistry;
import org.apache.log4j.Logger;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.jobs.helper.NodeRunner;
import org.edu_sharing.service.nodeservice.NodeServiceFactory;
import org.edu_sharing.service.nodeservice.NodeServiceHelper;
import org.springframework.context.ApplicationContext;
import org.edu_sharing.repository.server.jobs.helper.NodeRunner;

import java.util.Collections;

public class UpdateVHBContexts extends AbstractJob {
	Logger logger = Logger.getLogger(UpdateVHBContexts.class);
	ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
	ServiceRegistry serviceRegistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);

	public void execute(org.quartz.JobExecutionContext context) throws org.quartz.JobExecutionException {
		NodeRunner runner = new NodeRunner();
		runner.setFilter((ref)->
			NodeServiceHelper.getProperty(ref, CCConstants.CCM_PROP_IO_SEARCH_CONTEXT) == null
		);
		runner.setTask((ref)->{
			String license = NodeServiceHelper.getProperty(ref, CCConstants.CCM_PROP_IO_LICENSE);
			String value="smart";
			if(license == null){
				// do nothing
			} else if(	CCConstants.COMMON_LICENSE_CC_BY.equals(license) ||
						CCConstants.COMMON_LICENSE_CC_BY_SA.equals(license) ||
						CCConstants.COMMON_LICENSE_CC_ZERO.equals(license) ||
						CCConstants.COMMON_LICENSE_PDM.equals(license)
			){
				value = "oer";
			}
			NodeServiceFactory.getLocalService().setProperty(
					ref.getStoreRef().getProtocol(),
					ref.getStoreRef().getIdentifier(),
					ref.getId(),
					CCConstants.CCM_PROP_IO_SEARCH_CONTEXT,
					value);
			logger.info("Set property " + CCConstants.CCM_PROP_IO_SEARCH_CONTEXT + " for " + ref.getId()+ " to " + value);
		});
		runner.setTypes(Collections.singletonList(CCConstants.CCM_TYPE_IO));
		runner.setRunAsSystem(true);
		runner.setThreaded(false);
		runner.setKeepModifiedDate(true);
		runner.setTransaction(NodeRunner.TransactionMode.Local);
	}
	
	@Override
	public Class[] getJobClasses() {
		// TODO Auto-generated method stub
		super.addJobClass(UpdateVHBContexts.class);
		return super.allJobs;
	}
}
