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

import lombok.extern.slf4j.Slf4j;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.person.PersonServiceImpl;
import org.alfresco.repo.transaction.AlfrescoTransactionSupport;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.security.PermissionService;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.jobs.quartz.annotation.JobDescription;
import org.edu_sharing.repository.server.jobs.quartz.annotation.JobFieldDescription;
import org.edu_sharing.service.authority.AuthorityService;
import org.edu_sharing.service.search.SearchService;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import java.util.Arrays;

@Slf4j
@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
@JobDescription(description = "Repair the internal id of admin groups of orgs or media_centers")
public class RepairAdminGroupsAuthorityName extends AbstractJob {
    @JobFieldDescription(description = "group types to fix, orgs or media center")
    private Type type;
    @JobFieldDescription(description = "test/log only")
    private boolean testMode;


    @Autowired
    private SearchService searchService;

    @Autowired
    private AuthorityService authorityService;

    @Autowired
    private RetryingTransactionHelper retryingTransactionHelper;

    ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
    ServiceRegistry serviceRegistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        type = Type.valueOf(context.getJobDetail().getJobDataMap().getString("type"));
        testMode = context.getJobDetail().getJobDataMap().getBoolean("testMode");
        AuthenticationUtil.runAsSystem(() -> {
            int[] counts = new int[]{0, 0};
            if (type.equals(Type.ORGANIZATION)) {
                throw new RuntimeException("organization mode not implemented yet");
            } else if (type.equals(Type.MEDIA_CENTER)) {
                try {
                    searchService.getAllMediacenters().forEach(mz -> {
                        log.info("Processing media center {}", mz);
                        String[] memberships = authorityService.getMembershipsOfGroup(mz);
                        Arrays.stream(memberships).forEach(subgroup -> {
                            String grouptype = (String) authorityService.getAuthorityProperty(subgroup, CCConstants.CCM_PROP_GROUPEXTENSION_GROUPTYPE);
                            if (org.edu_sharing.alfresco.service.AuthorityService.MEDIACENTER_ADMINISTRATORS_GROUP.equals(grouptype)) {
                                log.info("Checking subgroup " + subgroup + " of parent " + mz);
                                String validName = PermissionService.GROUP_PREFIX + AuthorityService.getGroupName(org.edu_sharing.alfresco.service.AuthorityService.MEDIACENTER_ADMINISTRATORS_GROUP, mz);
                                counts[0]++;
                                if (validName.equals(subgroup)) {
                                    log.info("Subgroup " + validName + " has proper id. Doing nothing.");
                                } else {
                                    log.info("Subgroup " + validName + " has invalid id (" + subgroup + ").");
                                    if (!testMode) {
                                        log.info("Repairing " + validName);
                                        NodeRef nodeRef = authorityService.getAuthorityNodeRef(subgroup);
                                        renameAuthority(nodeRef, validName);

                                    }
                                    counts[1]++;
                                }
                            }
                        });
                    });
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
            log.info("Process finished. Total of {} objects processed. {} objects had to be repaired", counts[0], counts[1]);
            ;
            return null;
        });
    }

    private void renameAuthority(NodeRef nodeRef, String newAuthorityName) {
        retryingTransactionHelper.doInTransaction((RetryingTransactionHelper.RetryingTransactionCallback<Void>) () -> {
            AlfrescoTransactionSupport.bindResource(
                    PersonServiceImpl.KEY_ALLOW_UID_UPDATE, Boolean.TRUE);
            serviceRegistry.getNodeService().setProperty(nodeRef,
                    ContentModel.PROP_AUTHORITY_NAME,
                    newAuthorityName
            );
            return null;
        });
    }

    public enum Type {
        ORGANIZATION,
        MEDIA_CENTER
    }
}
