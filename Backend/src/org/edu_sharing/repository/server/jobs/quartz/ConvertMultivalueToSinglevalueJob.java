/**
 *
 *  
 * 
 * 
 *	
 *
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 *
 */
package org.edu_sharing.repository.server.jobs.quartz;

import org.alfresco.repo.model.Repository;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.namespace.QName;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.SearchResultNodeRef;
import org.edu_sharing.repository.server.jobs.helper.NodeRunner;
import org.edu_sharing.service.model.NodeRef;
import org.edu_sharing.service.nodeservice.NodeService;
import org.edu_sharing.service.nodeservice.NodeServiceFactory;
import org.edu_sharing.service.search.SearchService;
import org.edu_sharing.service.search.SearchServiceFactory;
import org.edu_sharing.service.search.model.SearchToken;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.context.ApplicationContext;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * Convert a multivalue field to a singlevalue
 * If there are still multivalues found, they will combined into a string using the "seperator"
 */
public class ConvertMultivalueToSinglevalueJob extends AbstractJob{

	private String property,seperator;

	protected Logger logger = Logger.getLogger(ConvertMultivalueToSinglevalueJob.class);
	private org.alfresco.service.cmr.repository.NodeService nodeService;

	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException {

		ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();

		ServiceRegistry serviceRegistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);

		nodeService = serviceRegistry.getNodeService();
		
		property = (String) context.getJobDetail().getJobDataMap().get("property");
		seperator = (String) context.getJobDetail().getJobDataMap().get("seperator");
		if(property==null){
			throw new IllegalArgumentException("Missing requied parameter 'property'");
		}
		property = CCConstants.getValidGlobalName(property);
		if(seperator==null){
			seperator=CCConstants.MULTIVALUE_SEPARATOR;
		}

		NodeRunner runner = new NodeRunner();
		runner.setTask((ref)->{
			org.alfresco.service.cmr.repository.NodeRef nodeRef = new org.alfresco.service.cmr.repository.NodeRef(ref.getStoreRef(), ref.getId());
			Serializable value = nodeService.getProperty(nodeRef, QName.createQName(property));
			//logger.info("ref: "+ref.getId()+ " value: "+value);
			if(value!=null) {
				if(value instanceof List){
					List list = (List) value;
					value=StringUtils.join(list,seperator);
					logger.info("Value for ref " + ref.getId() + " count: " +list.size() + " converted: " + value);
					try {
						nodeService.setProperty(nodeRef, QName.createQName(property), value);
					}catch(Throwable t){
						logger.error("can't set value "+value+" for ref: "+ref.getId(),t);
					}
				}
				else if(value instanceof String){
					logger.info("Value for ref " + ref.getId() + " is already singlevalue/string: "+value);
				}
				else{
					logger.info("Value for ref " + ref.getId() + " is of type "+value.getClass().getSimpleName()+", use toString(): " + value);
					try {
						nodeService.setProperty(nodeRef, QName.createQName(property), value.toString());
					}catch(Throwable t){
						logger.error("can't set value "+value+" for ref: "+ref.getId(),t);
					}
				}
			}
		});
		runner.setTypes(Collections.singletonList(CCConstants.CCM_TYPE_IO));
		runner.setRunAsSystem(true);
		runner.setThreaded(true);
		int count=runner.run();
		logger.info("Processed "+count+" nodes");
	}
	
	public void run() {

	}
	
	@Override
	public Class[] getJobClasses() {
		// TODO Auto-generated method stub
		return allJobs;
	}
}
