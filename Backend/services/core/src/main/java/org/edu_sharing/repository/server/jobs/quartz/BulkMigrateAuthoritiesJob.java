package org.edu_sharing.repository.server.jobs.quartz;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.person.PersonServiceImpl;
import org.alfresco.repo.transaction.AlfrescoTransactionSupport;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.cmr.security.PersonService;
import org.apache.log4j.Logger;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.server.jobs.quartz.annotation.JobDescription;
import org.edu_sharing.repository.server.jobs.quartz.annotation.JobFieldDescription;
import org.edu_sharing.service.authority.AuthorityService;
import org.edu_sharing.service.authority.AuthorityServiceFactory;
import org.edu_sharing.service.util.CSVTool;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.context.ApplicationContext;

import java.util.Map;

@JobDescription(description = "Bulk migrate authorities (mapping old group authority names to new group names)")
public class BulkMigrateAuthoritiesJob extends AbstractJob{
	protected Logger logger = Logger.getLogger(BulkMigrateAuthoritiesJob.class);
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
		CSVTool.CSVResult csv = BulkEditNodesJob.readCSVMapping(data);

		AuthenticationUtil.runAsSystem(() -> {
			for (Map<String, String> line : csv.getLines()) {

				String oldValue = line.get("oldValue");
				String newValue = line.get("newValue");
				if(!oldValue.startsWith(PermissionService.GROUP_PREFIX)){
					logger.error("Only authorities of type GROUP are supported. Identifer must start with " + PermissionService.GROUP_PREFIX+ ", got " + oldValue);
					continue;
				}
				serviceRegistry.getTransactionService().getRetryingTransactionHelper()
						.doInTransaction(new RetryingTransactionHelper.RetryingTransactionCallback<Void>() {
							@Override
							public Void execute() throws Throwable {

								AlfrescoTransactionSupport.bindResource(
										PersonServiceImpl.KEY_ALLOW_UID_UPDATE, Boolean.TRUE);
								NodeRef oldGroupRef = authorityService.getAuthorityNodeRef(oldValue);
								if(oldGroupRef == null){
									logger.warn("Authority " + oldValue + " was not found, skipping");
									return null;
								}
								nodeService.setProperty(oldGroupRef,
										ContentModel.PROP_AUTHORITY_NAME, newValue);
								logger.info("Migrated " + oldValue + " to " + newValue);
								return null;
							}
						});

			}
			logger.info("Processed " + csv.getLines().size() + " entries");
			return null;
		});
	}
}
