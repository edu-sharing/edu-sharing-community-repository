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
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.namespace.QName;
import org.apache.log4j.Logger;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.jobs.quartz.annotation.JobDescription;
import org.edu_sharing.repository.server.jobs.quartz.annotation.JobFieldDescription;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;

@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
@JobDescription(description = "Bulk delete authorities (users/groups)")
public class BulkDeleteAuthoritiesJob extends AbstractJob{
	protected Logger logger = Logger.getLogger(BulkDeleteAuthoritiesJob.class);
	@JobFieldDescription(file = true)
	private String data;

    @Autowired
    private AuthorityService authorityService;

    @Autowired
    private NodeService nodeService;

    @Autowired
    private PersonService personService;

    @Autowired
    private org.edu_sharing.service.nodeservice.NodeService eduNodeService;


	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException {

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
                        authorityService.deleteAuthority(entry);
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
								eduNodeService.removeNode(homeFolder.getId(), null, recycle);
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
