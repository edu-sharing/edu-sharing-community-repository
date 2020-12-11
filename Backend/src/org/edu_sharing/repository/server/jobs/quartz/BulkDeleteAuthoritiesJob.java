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
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.namespace.QName;
import org.apache.log4j.Logger;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.jobs.helper.NodeRunner;
import org.edu_sharing.repository.server.jobs.quartz.annotation.JobDescription;
import org.edu_sharing.repository.server.jobs.quartz.annotation.JobFieldDescription;
import org.edu_sharing.service.authority.AuthorityService;
import org.edu_sharing.service.authority.AuthorityServiceFactory;
import org.edu_sharing.service.nodeservice.NodeServiceFactory;
import org.edu_sharing.service.nodeservice.NodeServiceHelper;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.context.ApplicationContext;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@JobDescription(description = "Bulk delete authorities (users/groups)")
public class BulkDeleteAuthoritiesJob extends AbstractJob{
	protected Logger logger = Logger.getLogger(BulkDeleteAuthoritiesJob.class);
	@JobFieldDescription(file = true)
	private String data;

	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException {

		ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();

		ServiceRegistry serviceRegistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);

		AuthorityService authorityService = AuthorityServiceFactory.getLocalService();
		PersonService personService = serviceRegistry.getPersonService();
		NodeService nodeService = serviceRegistry.getNodeService();
		data = (String) context.getJobDetail().getJobDataMap().get(JobHandler.FILE_DATA);
		if (data == null){
			throw new IllegalArgumentException("Missing required file data");
		}
		Object deleteHomeFolder = context.getJobDetail().getJobDataMap().get("deleteHomeFolder");
		boolean delete;
		if (deleteHomeFolder == null){
			throw new IllegalArgumentException("Missing required 'deleteHomeFolder'");
		} else {
			delete = Boolean.parseBoolean(deleteHomeFolder.toString());
		}
		Object recycleHomeFolder = context.getJobDetail().getJobDataMap().get("recycleHomeFolder");
		boolean recycle;
		if (recycleHomeFolder != null){
			recycle = Boolean.parseBoolean(recycleHomeFolder.toString());
		} else {
			recycle = true;
		}
		String[] list = data.split("\n");
		AuthenticationUtil.runAsSystem(() -> {
			for (String entry : list) {
				entry = entry.trim();
				try {
					if(entry.startsWith(PermissionService.GROUP_PREFIX)) {
						// use alf authority service to remove admin groups
						serviceRegistry.getAuthorityService().deleteAuthority(entry);
					} else {
						NodeRef personRef = personService.getPersonOrNull(entry);
						if(personRef == null){
							logger.warn("Authority " + entry + " does not exist, skipping");
							continue;
						}
						if(delete) {
							NodeRef homeFolder = (NodeRef) nodeService.getProperty(personRef, QName.createQName(CCConstants.PROP_USER_HOMEFOLDER));
							if (homeFolder == null) {
								logger.warn("Authority " + entry + " has no home folder to delete");
							} else {
								NodeServiceFactory.getLocalService().removeNode(homeFolder.getId(), null, recycle);
								logger.info("Deleted home folder " + homeFolder.getId() +" of authority " + entry);
							}
						}

						personService.deletePerson(entry);
					}
					logger.info("Deleted authority " + entry);
				} catch (Throwable t) {
					logger.error("Could not delete authority " + entry, t);
				}
			}
			logger.info("Processed " + list.length + " entries");
			return null;
		});
	}
}
