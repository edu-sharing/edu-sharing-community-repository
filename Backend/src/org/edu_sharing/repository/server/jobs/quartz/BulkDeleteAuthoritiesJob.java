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

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.security.PermissionService;
import org.apache.log4j.Logger;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.jobs.helper.NodeRunner;
import org.edu_sharing.service.authority.AuthorityService;
import org.edu_sharing.service.authority.AuthorityServiceFactory;
import org.edu_sharing.service.nodeservice.NodeService;
import org.edu_sharing.service.nodeservice.NodeServiceFactory;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.context.ApplicationContext;

import java.io.InputStream;
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
public class BulkDeleteAuthoritiesJob extends AbstractJob{
	protected Logger logger = Logger.getLogger(BulkDeleteAuthoritiesJob.class);

	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException {

		ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();

		ServiceRegistry serviceRegistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);

		AuthorityService authorityService = AuthorityServiceFactory.getLocalService();
		String data = (String) context.getJobDetail().getJobDataMap().get(JobHandler.FILE_DATA);
		if(data == null){
			throw new IllegalArgumentException("Missing required file data");
		}
		String[] list = data.split("\n");
		AuthenticationUtil.runAsSystem(() -> {
			for (String entry : list) {
				entry = entry.trim();
				try {
					if(entry.startsWith(PermissionService.GROUP_PREFIX)) {
						authorityService.deleteAuthority(entry);
						logger.info("Deleted authority " + entry);
					} else {
						logger.warn("This job currently only supports group, but prefix was missing. Skipping entry " + entry);
					}
				} catch (Throwable t) {
					logger.error("Could not delete authority " + entry, t);
				}
			}
			logger.info("Processed " + list.length + " entries");
			return null;
		});
	}
}
