package org.edu_sharing.repository.update;

import lombok.extern.slf4j.Slf4j;
import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.update.UpdateRoutine;
import org.edu_sharing.repository.server.update.UpdateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

@Slf4j
@UpdateService
public class Release_1_7_UnmountGroupFolders {


    private final NodeService nodeService;
    private final NodeService ns;

    //very important: use the alfrescoDefaultDbNodeService defined in custom-core-services-context.xml
    //cause of overwriten getChild... methods in org.edu_sharing.alfresco.fixes.DbNodeServiceImpl
    //create here cause of authentication of Update servlet is passed instead of in constructor
    @Autowired
    public Release_1_7_UnmountGroupFolders(@Qualifier("alfrescoDefaultDbNodeService") NodeService nodeService, NodeService ns) {
        this.nodeService = nodeService;
        this.ns = ns;
    }


    @UpdateRoutine(id = "Release_1_7_UnmountGroupFolders",
            description = "unmounts edu group folders out of userhomes",
            order = 1701,
            auto = true)
    public void execute(boolean test) {

        log.debug("starting with testmode=" + test);
        umountEduGroupFoldersInUserHomes(test);
        log.debug("finished with testmode=" + test);
    }


    public void unmountEduGroupFoldersInUserHomes(NodeRef groupNodeRef, boolean test) {
        if (nodeService.hasAspect(groupNodeRef, QName.createQName(CCConstants.CCM_ASPECT_EDUGROUP))) {
            NodeRef eduGroupHomeDirRef = (NodeRef) nodeService.getProperty(groupNodeRef, QName.createQName(CCConstants.CCM_PROP_EDUGROUP_EDU_HOMEDIR));

            String groupname = (String) nodeService.getProperty(groupNodeRef, ContentModel.PROP_NAME);

            if (eduGroupHomeDirRef == null || !nodeService.exists(eduGroupHomeDirRef)) {
                log.warn("eduGroupHomeDirRef of group " + groupname + " does not exist");
                return;
            }

            if (eduGroupHomeDirRef != null) {
                int counter = 0;
                List<ChildAssociationRef> childAssocRefs = nodeService.getChildAssocs(groupNodeRef, new HashSet(Arrays.asList(new QName[]{ContentModel.TYPE_PERSON})));
                log.debug("unmounting groupfolder for group " + groupname + " for " + childAssocRefs.size() + " persons");
                for (ChildAssociationRef personChildRef : childAssocRefs) {
                    NodeRef personHomeFolderNodeRef = (NodeRef) nodeService.getProperty(personChildRef.getChildRef(), ContentModel.PROP_HOMEFOLDER);
                    List<ChildAssociationRef> childAssocMapTypes = nodeService.getChildAssocsByPropertyValue(personHomeFolderNodeRef, QName.createQName(CCConstants.CCM_PROP_MAP_TYPE), CCConstants.CCM_VALUE_MAP_TYPE_EDUGROUP);
                    if (childAssocMapTypes != null && childAssocMapTypes.size() == 1) {
                        ChildAssociationRef childAssocRefGroupFolder = childAssocMapTypes.get(0);

                        List<ChildAssociationRef> groupFolderChildren = nodeService.getChildAssocs(childAssocRefGroupFolder.getChildRef());
                        for (ChildAssociationRef groupFolderChild : groupFolderChildren) {
                            if (groupFolderChild.getChildRef().equals(eduGroupHomeDirRef)) {
                                if (!test) {
                                    ns.removeChild(childAssocRefGroupFolder.getChildRef(), eduGroupHomeDirRef);
                                }
                                counter++;

                                if ((counter % 10) == 0) {
                                    log.debug("unmounted group folder of group " + groupname + " for " + counter + " persons");
                                }
                            }
                        }

                    } else {
                        log.debug("can not unmount group Folder for person:" + personChildRef.getChildRef() + " cause of missing or invalid " + CCConstants.CCM_VALUE_MAP_TYPE_EDUGROUP + " Folder");
                    }
                }
                log.debug("unmounted eduGroupHomeDirRef:" + eduGroupHomeDirRef + " in " + counter + " user homes");
            }
        }
    }

    public void umountEduGroupFoldersInUserHomes(boolean test) {
        NodeRef rootNode = nodeService.getRootNode(new StoreRef(StoreRef.PROTOCOL_WORKSPACE, "SpacesStore"));
        List<ChildAssociationRef> rootChildAssocs = nodeService.getChildAssocs(rootNode);
        for (ChildAssociationRef childAssocRef : rootChildAssocs) {

            if (childAssocRef.getQName().equals(QName.createQName("{" + NamespaceService.SYSTEM_MODEL_1_0_URI + "}system"))) {

                List<ChildAssociationRef> systemChildren = nodeService.getChildAssocs(childAssocRef.getChildRef());

                for (ChildAssociationRef sysChild : systemChildren) {
                    if (sysChild.getQName().equals(QName.createQName("{" + NamespaceService.SYSTEM_MODEL_1_0_URI + "}authorities"))) {
                        List<ChildAssociationRef> authorities = nodeService.getChildAssocs(sysChild.getChildRef());

                        for (ChildAssociationRef authorityChild : authorities) {
                            log.debug("found authority" + authorityChild.getQName());
                            this.unmountEduGroupFoldersInUserHomes(authorityChild.getChildRef(), test);
                        }

                    }
                }

            }
        }
    }
}
