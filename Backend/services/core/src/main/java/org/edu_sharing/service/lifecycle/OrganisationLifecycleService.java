package org.edu_sharing.service.lifecycle;

import org.alfresco.model.ContentModel;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.security.AuthorityType;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.namespace.QName;
import org.apache.log4j.Logger;
import org.edu_sharing.alfresco.service.OrganisationService;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.ApplicationContext;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Orgs:
 * - SafeOrg
 * Subgroups
 * OrgFolder
 * Collections mit safescope?
 * - DefaultOrg
 * Subgroups
 * OrgFolder
 * Persons (mit PersonLifecycle ohne Organisationsordner)
 * <p>
 * <p>
 * bei PersonLifeCycleService müsste personstatus auf todelete gesetzt werren
 */
public class OrganisationLifecycleService {

    Logger logger = Logger.getLogger(OrganisationLifecycleService.class);

    PersonLifecycleService personLifecycleService = new PersonLifecycleService();

    ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
    OrganisationService organisationService = (OrganisationService) applicationContext.getBean("eduOrganisationService");

    ServiceRegistry serviceRegistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);
    AuthorityService authorityService = serviceRegistry.getAuthorityService();
    PersonService personService = serviceRegistry.getPersonService();
    NodeService nodeService = serviceRegistry.getNodeService();

    OrganisationDeleteProtocolService protocolService = null;

    String organisation;

    public OrganisationLifecycleService(String organisation){
        this.organisation = organisation;
        protocolService = new OrganisationDeleteProtocolServiceCSV(organisation);
    }


    public void deleteOrganisation() {
        String authorityName = organisationService.getAuthorityName(this.organisation );

        logger.info("starting for organisation:" + authorityName);
        try {
            if(!authorityService.authorityExists(authorityName)){
                logger.error("authority does not exists " + authorityName);
                throw new Exception("authority does not exists " + authorityName);
            }

            deleteOrganisationWithContext(this.organisation , authorityName);

        }catch(Throwable e){
            logger.error(e.getMessage(),e);
            protocolService.protocolError(this.organisation, this.organisation, e.getMessage() );
        }finally {
            protocolService.writeProtocolToAlfrescoNode(OrganisationDeleteProtocolServiceCSV.MIMETYPE);
            protocolService.cleanUp();
        }
    }

    private void deleteOrganisationWithContext(String orga, String authorityName) {
        Set<String> users = authorityService.getContainedAuthorities(AuthorityType.USER, authorityName, true);

        for (String user : users) {
            try {
                PersonDeleteResult personDeleteResult = deleteUser(authorityName, user);
                protocolService.protocolPersons(orga, personDeleteResult);
            } catch (RuntimeException e) {
                logger.error(e.getMessage(),e);
                protocolService.protocolError(orga, user, e.getMessage());
                //skip for fixing problem by hand
                return;
            }catch(Throwable e){
                logger.error(e.getMessage(),e);
                return;
            }
        }

        //delete safe org and subgroups
        String authorityNameSafe = authorityName + "_safe";
        deleteOrganisationGroup(orga, authorityNameSafe);


        //delete org and subgroups
        deleteOrganisationGroup(orga, authorityName);
    }

    protected void deleteOrganisationGroup(String orga, String authorityName){
        if(authorityService.authorityExists(authorityName)) {
            //delete subgroups
            deleteSubGroups(orga, authorityName);

            //delete org folders @TDOD: check why personlifecycle service does not remove folders, can be a perfomance issue for large folder structures
            Map<QName, Serializable> orgProps = organisationService.getOrganisation(organisationService.getCleanName(authorityName));

            NodeRef orgFolder = (NodeRef) orgProps.get(QName.createQName(OrganisationService.CCM_PROP_EDUGROUP_EDU_HOMEDIR));
            if (nodeService.exists(orgFolder)) {
                nodeService.addAspect(orgFolder, ContentModel.ASPECT_TEMPORARY, null);
                nodeService.deleteNode(orgFolder);
            } else {
                logger.warn("no organisation folder found for org:" + authorityName + " ref:" + orgFolder);
            }

            //delete organisation group
            authorityService.deleteAuthority(authorityName);

            protocolService.protocolEntry(OrganisationDeleteProtocolServiceCSV.OrganisationDeleteProtocol.instance(orga, authorityName));
        } else{
           logger.info("authority does not exist:" + authorityName);
        }
    }

    protected void deleteSubGroups(String orga, String authorityName){
        Set<String> containedAuthorities = authorityService.getContainedAuthorities(AuthorityType.GROUP, authorityName, false);
        List<String> result = new ArrayList<>();
        for(String authority : containedAuthorities){
            authorityService.deleteAuthority(authority);
            result.add(authority);
        }
        protocolService.protocolSubGroups(orga,result);
    }

    protected PersonDeleteResult deleteUser(String orgAuthorityName, String userName) {
        logger.info("deleting user:" + userName);

        //check user exists only in organisation orgAuthorityName
        Set<String> containedAuthorities = authorityService.getContainingAuthorities(AuthorityType.GROUP, userName, false);
        List<String> orgMemberships = containedAuthorities.stream()
                .filter(g -> g.startsWith("GROUP_ORG_"))
                .collect(Collectors.toList());
        if(!orgMemberships.contains(orgAuthorityName)){
            throw new RuntimeException("user " + userName + " is not member of organisation " + orgAuthorityName);
        }

        String orgAuthorityNameSafe = orgAuthorityName + "_" + CCConstants.CCM_VALUE_SCOPE_SAFE;
        orgMemberships = orgMemberships.stream()
                .filter(a -> (!a.equals(orgAuthorityName) && !a.equals(orgAuthorityNameSafe)))
                .collect(Collectors.toList());
        if(orgMemberships.size() > 0){
            throw new RuntimeException("user " + userName + " is member of more than one organisation");
        }


        NodeRef nodeRef = personService.getPerson(userName);
        nodeService.setProperty(nodeRef, QName.createQName(CCConstants.CM_PROP_PERSON_ESPERSONSTATUS), PersonLifecycleService.PersonStatus.todelete.name());

        return personLifecycleService.deletePersons(Arrays.asList(userName), getPersonDeleteOptions()).results.get(0);
    }

    private static @NotNull PersonDeleteOptions getPersonDeleteOptions() {
        PersonDeleteOptions options = new PersonDeleteOptions();
        options.cleanupMetadata = false;
        options.collectionFeedback = new PersonDeleteOptions.DeleteOption(true);
        options.ratings = new PersonDeleteOptions.DeleteOption(true);
        options.comments = new PersonDeleteOptions.DeleteOption(true);
        options.statistics = new PersonDeleteOptions.DeleteOption(true);
        options.stream = new PersonDeleteOptions.DeleteOption(true);

        options.collections = new PersonDeleteOptions.CollectionOptions();
        options.collections.privateCollections = PersonDeleteOptions.DeleteMode.delete;
        options.collections.publicCollections = PersonDeleteOptions.DeleteMode.delete;

        options.homeFolder = new PersonDeleteOptions.HomeFolderOptions();
        options.homeFolder.folders = PersonDeleteOptions.FolderDeleteMode.delete;
        options.homeFolder.ccFiles = PersonDeleteOptions.DeleteMode.delete;
        options.homeFolder.privateFiles = PersonDeleteOptions.DeleteMode.delete;
        options.homeFolder.keepFolderStructure = false;

        options.sharedFolders = new PersonDeleteOptions.SharedFolderOptions();
        options.sharedFolders.folders = PersonDeleteOptions.FolderDeleteMode.delete;
        options.sharedFolders.ccFiles = PersonDeleteOptions.DeleteMode.delete;
        options.sharedFolders.privateFiles = PersonDeleteOptions.DeleteMode.delete;
        options.sharedFolders.move = false;
        return options;
    }


}
