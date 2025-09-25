package org.edu_sharing.repository.server.jobs.quartz;

import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
import lombok.extern.slf4j.Slf4j;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.security.AccessPermission;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.security.AuthorityType;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.namespace.QName;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.AuthenticationTool;
import org.edu_sharing.repository.server.RepoFactory;
import org.edu_sharing.repository.server.SearchResultNodeRef;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.service.search.SearchService;
import org.edu_sharing.service.search.model.SearchToken;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;

@Slf4j
@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
public class AssignedLicenseManagerJob extends AbstractJob {


    @Autowired
    private NodeService nodeService;
    @Autowired
    private AuthorityService authorityService;
    @Autowired
    private PermissionService permissionService;

    @Autowired
    private SearchService searchService;


    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {

        try {
            ApplicationInfo homeRep = ApplicationInfoList.getHomeRepository();
            AuthenticationTool authTool = RepoFactory.getAuthenticationToolInstance(homeRep.getAppId());
            authTool.createNewSession(homeRep.getUsername(), homeRep.getPassword());
            manageExpiryDate(fetch());

        } catch (Throwable e) {
            log.error(e.getMessage(), e);
        }
    }

    void manageExpiryDate(SearchResultNodeRef rs) {
        rs.getData().forEach(n -> {
            NodeRef nodeRef = new NodeRef(new StoreRef(n.getStoreProtocol(), n.getStoreId()), n.getNodeId());
            Date date = (Date) nodeService.getProperty(nodeRef, QName.createQName(CCConstants.CCM_PROP_ASSIGNED_LICENSE_ASSIGNEDLICENSE_EXPIRY));
            // only handle assignedLicneses that got an expiry date
            if (date == null) {
                return;
            }


            String authority = (String) nodeService.getProperty(nodeRef, QName.createQName(CCConstants.CCM_PROP_ASSIGNED_LICENSE_AUTHORITY));
            ChildAssociationRef parentAssocRef = nodeService.getPrimaryParent(nodeRef);
            Set<AccessPermission> accessPermissions = permissionService.getAllSetPermissions(parentAssocRef.getParentRef());

            // create authorities
            if (!authorityService.authorityExists(authority)) {
                authorityService.createAuthority(AuthorityType.GROUP, authority.replace(AuthorityType.GROUP.getPrefixString(), ""));
            }

            // set permissions for authority if no access
            boolean authorityCanRead = false;
            for (AccessPermission accessPermission : accessPermissions) {
                if (accessPermission.getAuthority().equals(authority)) {
                    authorityCanRead = true;
                }
            }
            if (!authorityCanRead && date.getTime() > System.currentTimeMillis()) {
                permissionService.setPermission(parentAssocRef.getParentRef(), authority, PermissionService.READ, true);
                permissionService.setPermission(parentAssocRef.getParentRef(), authority, CCConstants.PERMISSION_CC_PUBLISH, true);
            }

            // remove expired
            if (date.getTime() < System.currentTimeMillis()) {


                for (AccessPermission perm : accessPermissions) {

                    //logger.info("authority:" + authority + " perm:"+perm.getAuthority() +"" + perm.getPermission());

                    if (perm.isInherited()) continue;
                    if (perm.getAuthority().equals(authority)) {
                        log.info("delete permission for " + parentAssocRef.getParentRef() + " authority:" + perm.getAuthority() + " " + perm.getPermission());
                        permissionService.deletePermission(parentAssocRef.getParentRef(), perm.getAuthority(), perm.getPermission());
                    }
                }
                List<String> mediaCentres = (List<String>) nodeService.getProperty(parentAssocRef.getParentRef(), QName.createQName(CCConstants.CCM_PROP_IO_MEDIACENTER));
                ArrayList<String> newMediacentres = new ArrayList<>();
                if (mediaCentres != null) {
                    for (String mediaCentre : mediaCentres) {
                        if (!authority.replace(AuthorityType.GROUP.getPrefixString(), "").equals(mediaCentre)) {
                            newMediacentres.add(mediaCentre);
                        }
                    }
                }
                nodeService.setProperty(parentAssocRef.getParentRef(), QName.createQName(CCConstants.CCM_PROP_IO_MEDIACENTER), newMediacentres);
            }
        });
    }

    SearchResultNodeRef fetch() {
        SearchToken searchToken = new SearchToken();
        searchToken.setFrom(0);
        searchToken.setMaxResult(Integer.MAX_VALUE);
        searchToken.setElasticQuery(QueryBuilders.term()
                .field("type")
                .value(CCConstants.getValidLocalName(CCConstants.CCM_TYPE_ASSIGNED_LICENSE))
                .build());

        return searchService.search(searchToken);
    }

    @Override
    public Class<?>[] getJobClasses() {
        // TODO Auto-generated method stub
        List<Class<?>> allJobs = new ArrayList<>(Arrays.asList(super.allJobs));
        allJobs.add(AssignedLicenseManagerJob.class);
        return allJobs.toArray(new Class[0]);
    }
}
