package org.edu_sharing.service.lifecycle;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.policy.BehaviourFilter;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.*;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.Pair;
import org.alfresco.util.TempFileProvider;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.tools.UserEnvironmentTool;
import org.edu_sharing.service.nodeservice.NodeServiceFactory;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.ApplicationContext;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class OrganisationDeleteProtocolService {

    ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
    ServiceRegistry serviceRegistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);
    protected NodeService nodeService = serviceRegistry.getNodeService();

    ContentService contentService = serviceRegistry.getContentService();

    BehaviourFilter behaviourFilter = (BehaviourFilter) applicationContext.getBean("policyBehaviourFilter");

    protected File tempFile;

    String organisation;

    protected OrganisationDeleteProtocolService(String organisation){
        this.organisation = organisation;
        this.tempFile = createProtocolTempFile(organisation);
    }

    public abstract void protocolEntry(OrganisationDeleteProtocolService.OrganisationDeleteProtocol protEntry);

    public abstract String getProtcolFormatSuffix();

    public abstract void cleanUp();

    private File createProtocolTempFile(String organisation){
        return TempFileProvider.createTempFile(organisation, getProtcolFormatSuffix());
    }

    public void writeProtocolToAlfrescoNode(String mimeType) {
        NodeRef protocolNodeRef = null;
        try {
            protocolNodeRef = getProtocolNodeRef(getFileName(organisation));
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }

        try {
            //prevent that failed virusscan breakes protokoll write
            NodeRef fNodeRef = protocolNodeRef;
            serviceRegistry.getRetryingTransactionHelper().doInTransaction(() -> {
                behaviourFilter.disableBehaviour(fNodeRef);
                NodeServiceFactory.getLocalService().writeContent(
                        StoreRef.STORE_REF_WORKSPACE_SPACESSTORE,
                        fNodeRef.getId(),
                        new FileInputStream(tempFile),
                        mimeType,
                        null,
                        CCConstants.CM_PROP_CONTENT);
                behaviourFilter.enableBehaviour(fNodeRef);
                return null;
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private @NotNull ContentWriter getContentWriter(NodeRef protocolNodeRef) {
        ContentWriter contentWriter = contentService.getWriter(protocolNodeRef, ContentModel.PROP_CONTENT, true);
        contentWriter.setMimetype("text/csv");
        return contentWriter;
    }

    protected String getFileName(String organisation){
        return organisation+"."+getProtcolFormatSuffix();
    }

    protected NodeRef getProtocolNodeRef(String name) throws Throwable {
        String systemFolderNodeId = new UserEnvironmentTool().getEdu_SharingOrganisatinDeleteProtocolFolder();
        NodeRef parent = new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE,systemFolderNodeId);
        NodeRef protocolNodeRef = NodeServiceFactory.getLocalService().getChild(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE,parent.getId(), CCConstants.CCM_TYPE_IO,CCConstants.CM_NAME, name);
        if(protocolNodeRef == null){
            String assocName = "{" + CCConstants.NAMESPACE_CCM + "}" + name;
            HashMap<QName, Serializable> props = new HashMap<>();
            props.put(ContentModel.PROP_NAME, name);
            return nodeService.createNode(parent,
                    QName.createQName(CCConstants.CM_ASSOC_FOLDER_CONTAINS),
                    QName.createQName(assocName),
                    QName.createQName(CCConstants.CCM_TYPE_IO),
                    props).getChildRef();
        }
        return protocolNodeRef;
    }

    public ContentReader getContentReader(){
        try {
            NodeRef protocolNodeRef = getProtocolNodeRef(getFileName(organisation));
            return contentService.getReader(protocolNodeRef, ContentModel.PROP_CONTENT);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public void protocolError(String orga, String user, String errorMessage) {
        OrganisationDeleteProtocol protEntry = new OrganisationDeleteProtocol();
        protEntry.event = OrganisationDeleteProtocol.EVENTS.ERROR.name();
        protEntry.authority = user;
        protEntry.collections = 0;
        protEntry.date = new Date();
        protEntry.docs = 0;
        protEntry.folders = 0;
        protEntry.message = errorMessage;
        protEntry.organisation = orga;
        protocolEntry(protEntry);
    }

    public void protocolPersons(String orga, PersonDeleteResult rs) {
        OrganisationDeleteProtocol protEntry = OrganisationDeleteProtocol.instance(orga,rs.authorityName);

        {
            Pair<Integer, Integer> subCount = countFilesFolders(rs.homeFolder.get(PersonLifecycleService.DEFAULT_SCOPE));
            protEntry.docs = subCount.getFirst();
            protEntry.folders = subCount.getSecond();

        }

        {
            Pair<Integer, Integer> subCount = countFilesFolders(rs.sharedFolders.get(PersonLifecycleService.DEFAULT_SCOPE));
            protEntry.docsShared = subCount.getFirst();
            protEntry.foldersShared = subCount.getSecond();

        }

        {
            Pair<Integer, Integer> subCount = countFilesFolders(rs.homeFolder.get(CCConstants.CCM_VALUE_SCOPE_SAFE));
            protEntry.docsSafe = subCount.getFirst();
            protEntry.foldersSafe = subCount.getSecond();
        }

        {
            Pair<Integer, Integer> subCount = countFilesFolders(rs.sharedFolders.get(CCConstants.CCM_VALUE_SCOPE_SAFE));
            protEntry.docsSharedSafe = subCount.getFirst();
            /**
             * foldersSharedSafe @see PersonLifecycleService.handleForeignFiles
             * can be 1 to 3 folders more than expected cause folders like this are also counted (createdBy)
             * /app:company_home/ccm:SCOPES/ccm:safe
             * /app:company_home/ccm:SCOPES/ccm:safe/ccm:Bv (user:BvlZac)
             * /app:company_home/ccm:EDU_SHARED/ccm:Test_x0020_School_safe
             */
            protEntry.foldersSharedSafe = subCount.getSecond();
        }

        protEntry.collections = 0;
        protEntry.collectionRefs = 0;
        rs.collections.getCollections().stream().forEach(e -> {
            if (e.getType().equals(CCConstants.CCM_TYPE_MAP)) {
                protEntry.collections++;
            }
        });
        rs.collections.getRefs().stream().forEach(e ->{
            if (e.getType().equals(CCConstants.CCM_TYPE_IO)) {
                protEntry.collectionRefs++;
            }
        });


        protocolEntry(protEntry);
    }

    /**
     *
     * @param counts
     * @return <fileCount,folderCount>
     */
    private Pair<Integer,Integer> countFilesFolders(PersonDeleteResult.Counts counts){
        AtomicInteger docs = new AtomicInteger(0);
        AtomicInteger folders = new AtomicInteger(0);
        counts.getElements().stream().forEach(e -> {
            if(e.getType().equals(CCConstants.CCM_TYPE_IO)){
                docs.set(docs.incrementAndGet());
            }
            if(e.getType().equals(CCConstants.CCM_TYPE_MAP)){
                folders.set(folders.incrementAndGet());
            }
        });
        return new Pair<>(docs.get(),folders.get());
    }

    public void protocolSubGroups(String orga, List<String> subgroups){

        subgroups.stream().forEach(s -> {
            OrganisationDeleteProtocol protEntry = OrganisationDeleteProtocol.instance(orga,s);
            protocolEntry(protEntry);
        });
    }

    public static class OrganisationDeleteProtocol {
        Date date;
        String event;
        String message;
        String organisation;
        String authority;
        int docs;
        int docsShared;
        int docsSafe;
        int docsSharedSafe;
        int folders;
        int foldersShared;
        int foldersSafe;
        int foldersSharedSafe;
        int collections;
        int collectionRefs;

        enum EVENTS {ERROR, INFO}


        /**
         * returns with in
         * @param organisation
         * @param authority
         * @return
         */
        public static OrganisationDeleteProtocol instance(String organisation, String authority){
            OrganisationDeleteProtocol protEntry = new OrganisationDeleteProtocol();
            protEntry.event = EVENTS.INFO.name();
            protEntry.organisation = organisation;
            protEntry.authority = authority;
            protEntry.date = new Date();
            return protEntry;
        }

        public static String[] getHeader(){
            return new String[]{"date", "event", "organisation","authority",
                    "docs",
                    "docsShared",
                    "docsSafe",
                    "docsSharedSafe",
                    "folders",
                    "foldersShared",
                    "foldersSafe",
                    "foldersSharedSafe",
                    "collections",
                    "collectionRefs",
                    "message"};
        }

        public String[] getArray(){
            return new String[]{new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").format(date), event, organisation,authority,
                    new Integer(docs).toString(),
                    new Integer(docsShared).toString(),
                    new Integer(docsSafe).toString(),
                    new Integer(docsSharedSafe).toString(),
                    new Integer(folders).toString(),
                    new Integer(foldersShared).toString(),
                    new Integer(foldersSafe).toString(),
                    new Integer(foldersSharedSafe).toString(),
                    new Integer(collections).toString(),
                    new Integer(collectionRefs).toString(),
                    message
            };
        }
    }
}
