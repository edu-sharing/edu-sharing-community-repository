package org.edu_sharing.repository.server.jobs.quartz;

import org.alfresco.model.ContentModel;
import org.alfresco.query.PagingRequest;
import org.alfresco.repo.model.Repository;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.OwnableService;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.namespace.RegexQNamePattern;
import org.apache.log4j.Logger;
import org.edu_sharing.alfresco.service.OrganisationService;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.jobs.helper.NodeRunner;
import org.edu_sharing.repository.server.jobs.quartz.annotation.JobFieldDescription;
import org.edu_sharing.service.authentication.ScopeUserHomeServiceImpl;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.context.ApplicationContext;

import java.util.Arrays;
import java.util.List;


public class FixOwnerSafeScopeJob extends AbstractJob{

    Logger logger = Logger.getLogger(FixOwnerSafeScopeJob.class);
    ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
    ServiceRegistry serviceRegistry = applicationContext.getBean(ServiceRegistry.class);
    //using alfrescoDefaultDbNodeService to run through shared folders only once (not every userhome)
    NodeService nodeService = (NodeService)applicationContext.getBean("alfrescoDefaultDbNodeService");


    OrganisationService eduOrganisationService = (OrganisationService)applicationContext.getBean("eduOrganisationService");

    OwnableService ownableService = serviceRegistry.getOwnableService();

    @JobFieldDescription(description = "run job in persist mode false/true", sampleValue = "false")
    boolean persist = false;

    @Override
    public void execute(final JobExecutionContext jobExecutionContext) throws JobExecutionException {

        String persistParam = (String)jobExecutionContext.getJobDetail().getJobDataMap().get("persist");
        persist = new Boolean(persistParam);


        logger.info("starting getAllPeople");
        List<PersonService.PersonInfo> allPeople = getAllPeople();
        logger.info("finished getAllPeople");
        for(PersonService.PersonInfo personInfo : allPeople){
            NodeRef personNodeRef = personInfo.getNodeRef();
            List<ChildAssociationRef> personScopes = nodeService.getChildAssocs(personNodeRef, ScopeUserHomeServiceImpl.CHILD_ASSOC_PERSON_SCOPES, RegexQNamePattern.MATCH_ALL);
            if(personScopes == null || personScopes.size() == 0){
                logger.info("no person scope found for person " + personInfo.getUserName());
                continue;
            }

            for(ChildAssociationRef personScope : personScopes){
                if(!ScopeUserHomeServiceImpl.TYPE_PERSON_SCOPE.equals(nodeService.getType(personScope.getChildRef()))) {
                    logger.error(personScope.getChildRef() + " nis no person scope");
                    continue;
                }

                String scope = (String)nodeService.getProperty(personScope.getChildRef(),ScopeUserHomeServiceImpl.PROP_PERSON_SCOPE_NAME);
                if(!scope.equals(CCConstants.CCM_VALUE_SCOPE_SAFE)){
                    logger.error("unkown scope found for "+personScope.getChildRef());
                    continue;
                }

                NodeRef safeUserhome = (NodeRef)nodeService.getProperty(personScope.getChildRef(),ScopeUserHomeServiceImpl.PROP_PERSON_SCOPE_HOMEFOLDER);
                if(safeUserhome == null){
                    logger.error("no userhome found for personScope node ");
                    continue;
                }

                NodeRunner nr = getBasicNodeRunner();
                nr.setTask((ref) -> {
                    checkAndFixOwner(ref);
                });
                nr.setNodesList(Arrays.asList(new NodeRef[]{safeUserhome}));
                nr.run();
            }
        }
    }

    private List<PersonService.PersonInfo> getAllPeople() {
        return AuthenticationUtil.runAsSystem(() -> {
            PersonService personService = serviceRegistry.getPersonService();
            return personService.getPeople(null, null, null, new PagingRequest(Integer.MAX_VALUE, null)).getPage();
        });
    }

    private NodeRunner getBasicNodeRunner(){
        NodeRunner nr = new NodeRunner();
        nr.setRunAsSystem(true);
        nr.setTransaction(NodeRunner.TransactionMode.Local);
        nr.setKeepModifiedDate(true);
        nr.setTypes(Arrays.asList(new String[] { CCConstants.CCM_TYPE_IO, CCConstants.CCM_TYPE_MAP }));
        return nr;
    }

    void checkAndFixOwner(NodeRef ref){
        String path = nodeService.getPath(ref).toDisplayPath(nodeService,serviceRegistry.getPermissionService());

        String pathIncludingNode = path + "/" +nodeService.getProperty(ref, ContentModel.PROP_NAME);
        logger.info(pathIncludingNode);

        String owner = ownableService.getOwner(ref);
        if(owner == null){
            logger.error("now owner found for "+ref);
            return;
        }

        logger.info("adding ALL permission to user and OWNER_AUTHORITY for "+pathIncludingNode +" "+ref +" user:" + owner);
        if(persist){

            //set ROLE_OWNER authority to ALL like alfresco is doing
            // (watch out for alfresco bean "personServicePermissionsManager"
            //  and class org.alfresco.repo.security.person.PermissionsManagerImpl)
            serviceRegistry.getPermissionService().setPermission(ref, PermissionService.OWNER_AUTHORITY, PermissionService.ALL_PERMISSIONS, true);
            serviceRegistry.getPermissionService().setPermission(ref, owner, PermissionService.ALL_PERMISSIONS, true);

        }
    }
}
