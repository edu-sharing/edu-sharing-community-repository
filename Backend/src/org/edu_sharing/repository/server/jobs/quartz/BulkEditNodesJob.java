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

import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.namespace.QName;
import org.apache.log4j.Logger;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.jobs.helper.NodeRunner;
import org.edu_sharing.repository.server.jobs.quartz.annotation.JobDescription;
import org.edu_sharing.repository.server.jobs.quartz.annotation.JobFieldDescription;
import org.edu_sharing.service.nodeservice.RecurseMode;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.context.ApplicationContext;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Batch edit property for multiple nodes
 * Required parameters:
 * property: The property name to replace
 * value: the target value to set
 * OR copy: the source property to copy the value of
 * startFolder: The id of the folder to start (recursively processing all children)
 * mode: The mode, see enum
 * types: the types of nodes to process, e.g. ccm:io (comma seperated string)
 *
 */
@JobDescription(description = "Bulk change metadata of nodes")
public class BulkEditNodesJob extends AbstractJob{
	protected Logger logger = Logger.getLogger(BulkEditNodesJob.class);
	private org.alfresco.service.cmr.repository.NodeService nodeService;
	@JobFieldDescription(description = "folder id to start from")
	private String startFolder;
	@JobFieldDescription(description = "property to modify, e.g. cm:name")
	private String property;
	@JobFieldDescription(description = "Value to replace target property with")
	private Serializable value;
	@JobFieldDescription(description = "property to copy value from, if mode == copy")
	private String copy;
	@JobFieldDescription(description = "token to replace, if mode == replaceToken")
	private String searchToken;
	@JobFieldDescription(description = "Token to replace with, if mode == replaceToken")
	private String replaceToken;
	@JobFieldDescription(description = "Mode to use")
	private Mode mode;
	@JobFieldDescription(description = "Element types to modify (comma seperated list), e.g. ccm:map,ccm:io")
	private List<String> types;
	@JobFieldDescription(description = "RecurseMode to use")
	private RecurseMode recurseMode;

	private enum Mode{
		Replace,
		ReplaceToken,
		Append,
		Remove
	};

	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException {

		ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();

		ServiceRegistry serviceRegistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);

		nodeService = serviceRegistry.getNodeService();

		try {
			mode = Mode.valueOf((String) context.getJobDetail().getJobDataMap().get("mode"));
		}catch(Throwable t){
			throw new IllegalArgumentException("Missing or invalid value for required parameter 'mode'",t);
		}

		property = prepareParam(context, "property", true);
		property = CCConstants.getValidGlobalName(property);

		copy = prepareParam(context, "copy", false);
		if(copy!=null){
			copy=CCConstants.getValidGlobalName(copy);
		}
		value = prepareParam(context, "value", false);
		if(mode.equals(Mode.Replace)) {
			if (copy == null && value == null) {
				throwMissingParam("'value' or 'copy'");
			}
			if (copy != null && value != null) {
				throw new IllegalArgumentException("Only one of parameters 'value' and 'copy' may be set");
			}
		}
		if(mode.equals(Mode.ReplaceToken)){
			searchToken = prepareParam(context, "searchToken", true);
			replaceToken = prepareParam(context, "replaceToken", true);
		}

		startFolder =prepareParam(context, "startFolder", true);
		try {
			types = Arrays.stream(((String) context.getJobDetail().getJobDataMap().get("types")).
					split(",")).map(String::trim).map(CCConstants::getValidGlobalName).
					collect(Collectors.toList());
		}catch(Throwable t){}
		if(types==null || types.isEmpty()) {
			throwMissingParam("types");
		}
		recurseMode = RecurseMode.Folders;
		try {
			if(context.getJobDetail().getJobDataMap().get("recurseMode") != null) {
				recurseMode = RecurseMode.valueOf((String) context.getJobDetail().getJobDataMap().get("recurseMode"));
			}
		}catch(Throwable t){
			throw new IllegalArgumentException("Missing or invalid value for parameter 'recurseMode'",t);
		}
		NodeRunner runner = new NodeRunner();
		runner.setTask((ref)->{
			org.alfresco.service.cmr.repository.NodeRef nodeRef = new org.alfresco.service.cmr.repository.NodeRef(ref.getStoreRef(), ref.getId());
			logger.info("Bulk edit metadata for node "+ref.getId());
			if(copy!=null){
				value=nodeService.getProperty(nodeRef,QName.createQName(copy));
			}
			if(mode.equals(Mode.Replace)){
				nodeService.setProperty(nodeRef,QName.createQName(property),value);
			}
			else if(mode.equals(Mode.Remove)){
				nodeService.removeProperty(nodeRef,QName.createQName(property));
			} else if(mode.equals(Mode.ReplaceToken)){
				Serializable current=nodeService.getProperty(nodeRef, QName.createQName(property));
				if(current!=null) {
					if (current instanceof String) {
						nodeService.setProperty(nodeRef, QName.createQName(property), ((String) current).replace(searchToken, replaceToken));
					} else if (current instanceof List) {
						nodeService.setProperty(nodeRef, QName.createQName(property), (Serializable) ((List<String>) current).stream().map((s) -> s.replace(searchToken, replaceToken)).collect(Collectors.toList()));
					} else {
						logger.info("Can not replace property " + property + "for node " + nodeRef + ": current data is not of type String/List");
					}
				}
			}
			else {
				throw new IllegalArgumentException("Mode " + mode + " is currently not supported");
			}
		});
		runner.setTypes(types);
		runner.setRunAsSystem(true);
		runner.setThreaded(false);
		runner.setRecurseMode(recurseMode);
		runner.setStartFolder(startFolder);
		runner.setKeepModifiedDate(true);
		runner.setTransaction(NodeRunner.TransactionMode.Local);
		int count=runner.run();
		logger.info("Processed "+count+" nodes");
	}

	private String prepareParam(JobExecutionContext context, String param, boolean required) {
		String value = (String) context.getJobDetail().getJobDataMap().get(param);
		if(value==null && required) {
			throwMissingParam(param);
		}
		return value;

	}

	private void throwMissingParam(String param) {
		throw new IllegalArgumentException("Missing required parameter(s) '" + param + "'");
	}

	public void run() {

	}
	
	@Override
	public Class[] getJobClasses() {
		// TODO Auto-generated method stub
		return allJobs;
	}
}
