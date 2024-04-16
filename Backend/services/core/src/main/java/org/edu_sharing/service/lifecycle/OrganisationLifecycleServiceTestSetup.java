package org.edu_sharing.service.lifecycle;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.DuplicateChildNodeNameException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.namespace.QName;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.edu_sharing.alfresco.service.OrganisationService;
import org.edu_sharing.alfresco.workspace_administration.NodeServiceInterceptor;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.service.authentication.ScopeUserHomeServiceFactory;
import org.springframework.context.ApplicationContext;

import java.io.Serializable;
import java.util.*;

public class OrganisationLifecycleServiceTestSetup {


    ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
    OrganisationService organisationService = (OrganisationService) applicationContext.getBean("eduOrganisationService");

    ServiceRegistry serviceRegistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);
    AuthorityService authorityService = serviceRegistry.getAuthorityService();
    PersonService personService = serviceRegistry.getPersonService();
    NodeService nodeService = serviceRegistry.getNodeService();


    int numberOfPersons = 10;

    int numberOfHomeDocs = 12;

    //+ 3 (dokuments,images,shared)
    int numberOfHomeFolders = 9;

    int numberOfSharedDocs = 10;
    int numberOfSharedFolders = 4;


    List<NodeRef> persons = new ArrayList<>();
    String orgAuthorityName;

    public OrganisationLifecycleServiceTestSetup(){

        AuthenticationUtil.runAsSystem(() -> {
            testSetup();
            return null;
        });

        testSetupPersons();
    }

    private void testSetup(){
        try {
            orgAuthorityName = "GROUP_" + organisationService.createOrganization("testSchool", "Test School");

            //create persons
            //TODO safe
            for(int i = 0; i < numberOfPersons; i++){
                Map<QName, Serializable> personProps = new HashMap<>();
                personProps.put(ContentModel.PROP_FIRSTNAME, RandomStringUtils.random(6, true, false));
                personProps.put(ContentModel.PROP_LASTNAME, RandomStringUtils.random(10, true, false));
                personProps.put(ContentModel.PROP_EMAIL, RandomStringUtils.random(6, true, false) +"@test.de");
                personProps.put(ContentModel.PROP_USERNAME,RandomStringUtils.random(6, true, false));
                persons.add(personService.createPerson(personProps));
                authorityService.addAuthority(orgAuthorityName, (String)personProps.get(ContentModel.PROP_USERNAME));
                String organisationAdminGroup = organisationService.getOrganisationAdminGroup(orgAuthorityName);
                authorityService.addAuthority(organisationAdminGroup, (String)personProps.get(ContentModel.PROP_USERNAME));
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void testSetupPersons(){
        for(NodeRef person : persons){
            String user = AuthenticationUtil.runAsSystem(() -> (String)nodeService.getProperty(person,ContentModel.PROP_USERNAME));

            //home
            NodeRef rootFolder = AuthenticationUtil.runAsSystem(() ->(NodeRef) nodeService.getProperty(person, ContentModel.PROP_HOMEFOLDER));
            createFoldersFiles(user, numberOfHomeFolders, numberOfHomeDocs, rootFolder);

            //shared
            Map<QName, Serializable> orgProps = AuthenticationUtil.runAsSystem(() -> organisationService.getOrganisation(organisationService.getCleanName(orgAuthorityName)));
            NodeRef orgFolder = (NodeRef) orgProps.get(QName.createQName(CCConstants.CCM_PROP_EDUGROUP_EDU_HOMEDIR));
            createFoldersFiles(user,numberOfSharedFolders,numberOfSharedDocs,orgFolder);


            //safe
            NodeServiceInterceptor.setEduSharingScope(CCConstants.CCM_VALUE_SCOPE_SAFE);

            NodeRef ref  = AuthenticationUtil.runAsSystem(() -> {
                        AuthenticationUtil.setFullyAuthenticatedUser(user);
                        AuthenticationUtil.setRunAsUserSystem();
                        return  ScopeUserHomeServiceFactory.getScopeUserHomeService().getUserHome(user, CCConstants.CCM_VALUE_SCOPE_SAFE, true);
                    }
            );
            createFoldersFiles(user, numberOfHomeFolders, numberOfHomeDocs, ref);

            //create/get safe org
            String authorityNameOrgSafe = orgAuthorityName+"_safe";
            Map<QName, Serializable> orgSafeProps = AuthenticationUtil.runAsSystem(() -> organisationService.getOrganisation(organisationService.getCleanName(authorityNameOrgSafe)));
            NodeRef orgSafeFolder = (NodeRef) orgSafeProps.get(QName.createQName(CCConstants.CCM_PROP_EDUGROUP_EDU_HOMEDIR));
            String organisationAdminGroup = AuthenticationUtil.runAsSystem(() -> organisationService.getOrganisationAdminGroup(authorityNameOrgSafe));
            AuthenticationUtil.runAsSystem(() -> {authorityService.addAuthority(organisationAdminGroup, user);return null;});

            createFoldersFiles(user,numberOfSharedFolders,numberOfSharedDocs,orgSafeFolder);

            NodeServiceInterceptor.setEduSharingScope(null);
        }
    }

    private void createFoldersFiles(String user, int nrOfFolders, int nrOfDocs, NodeRef parent){
        AuthenticationUtil.runAs(() -> {
            List<NodeRef> folderList = new ArrayList<>();
            for(int i = 0; i  < nrOfFolders; i++){
                Map<QName,Serializable> folderProps = new HashMap<>();
                folderProps.put(ContentModel.PROP_NAME,RandomStringUtils.random(6, true, false));
                folderList.add(createNode(parent, CCConstants.CCM_TYPE_MAP, folderProps));
            }

            for(int i = 0; i < nrOfDocs; i++){
                int parentFolderIdx = RandomUtils.nextInt(0,folderList.size() -1);
                Map<QName,Serializable> props = new HashMap<>();
                props.put(ContentModel.PROP_NAME, RandomStringUtils.random(6, true, false)+"."+RandomStringUtils.random(3, true, false));
                createNode(folderList.get(parentFolderIdx),CCConstants.CCM_TYPE_IO,props);
            }
            return null;
        },user);
    }

    private NodeRef createNode(NodeRef parent,String type, Map<QName,Serializable> props){
        try {
            return nodeService.createNode(parent,
                    ContentModel.ASSOC_CONTAINS,
                    QName.createQName("{"+CCConstants.NAMESPACE_CCM+"}"+props.get(ContentModel.PROP_NAME)),
                    QName.createQName(type),
                    props).getChildRef();
        }catch (DuplicateChildNodeNameException e){
            props.put(ContentModel.PROP_NAME,props.get(ContentModel.PROP_NAME) + RandomStringUtils.randomAscii(10));
            return nodeService.createNode(parent,
                    ContentModel.ASSOC_CONTAINS,
                    QName.createQName("{"+CCConstants.NAMESPACE_CCM+"}"+props.get(ContentModel.PROP_NAME)),
                    QName.createQName(type),
                    props).getChildRef();
        }
    }

}
