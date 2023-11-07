package org.edu_sharing.repository.update;

import lombok.extern.slf4j.Slf4j;
import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.CopyService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.namespace.QName;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.tools.UserEnvironmentTool;
import org.edu_sharing.repository.server.tools.UserEnvironmentToolFactory;
import org.edu_sharing.repository.server.update.UpdateRoutine;
import org.edu_sharing.repository.server.update.UpdateService;
import org.edu_sharing.service.nodeservice.NodeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

@Slf4j
@UpdateService
public class Release_8_0_Migrate_Database_Scripts {
    public static final String FILE_SUFFIX = ".properties";

    private final NodeService nodeService;
    private final CopyService copyService;
    private final UserEnvironmentToolFactory userEnvironmentToolFactory;


    @Autowired
    public Release_8_0_Migrate_Database_Scripts(NodeService nodeService, CopyService copyService, UserEnvironmentToolFactory userEnvironmentToolFactory) {
        this.nodeService = nodeService;
        this.copyService = copyService;
        this.userEnvironmentToolFactory = userEnvironmentToolFactory;
    }


    @UpdateRoutine(
            id = "Release_8_0_Migrate_Database_Scripts",
            description = "Migrates all database scripts and removes the .properties name",
            order = 8000,
            auto = true,
            isNonTransactional = true
    )
    public void execute(boolean test) throws Throwable {
        String eduSystemFolderUpdate = userEnvironmentToolFactory.createUserEnvironmentTool().getEdu_SharingSystemFolderUpdate();
        NodeRef updateFolder = new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, eduSystemFolderUpdate);
        nodeService.getChildrenChildAssociationRefType(
                updateFolder.getId(), CCConstants.CCM_TYPE_SYSUPDATE
        ).stream().map(ChildAssociationRef::getChildRef).filter(child ->
                nodeService.getProperty(
                        child.getStoreRef().getProtocol(),
                        child.getStoreRef().getIdentifier(),
                        child.getId(),
                        CCConstants.CM_NAME).startsWith(SQLUpdater.ID) &&
                        nodeService.getProperty(
                                        child.getStoreRef().getProtocol(),
                                        child.getStoreRef().getIdentifier(),
                                        child.getId(),
                                        CCConstants.CM_NAME)
                                .endsWith(FILE_SUFFIX)
        ).forEach(child -> {
            String name = nodeService.getProperty(
                    child.getStoreRef().getProtocol(),
                    child.getStoreRef().getIdentifier(),
                    child.getId(),
                    CCConstants.CM_NAME);
            String newName = nodeService.getProperty(
                    child.getStoreRef().getProtocol(),
                    child.getStoreRef().getIdentifier(),
                    child.getId()
                    , CCConstants.CM_NAME).substring(0, name.length() - FILE_SUFFIX.length());
            log.info("Copy update info from " + name + " to " + newName + " (" + child + ")");
            if (!test) {
                NodeRef newNode = copyService.copyAndRename(
                        child,
                        updateFolder,
                        ContentModel.ASSOC_CONTAINS,
                        QName.createQName(newName),
                        false
                );
                nodeService.setProperty(
                        newNode.getStoreRef().getProtocol(),
                        newNode.getStoreRef().getIdentifier(),
                        newNode.getId(),
                        CCConstants.CM_NAME, newName, false);
            }
        });
    }


}
