package org.edu_sharing.repository.update;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.security.OwnableService;
import org.alfresco.service.namespace.QName;
import org.edu_sharing.alfresco.service.OrganisationService;
import org.edu_sharing.repository.client.rpc.Share;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.jobs.helper.NodeRunner;
import org.edu_sharing.repository.server.update.UpdateRoutine;
import org.edu_sharing.repository.server.update.UpdateService;
import org.edu_sharing.service.nodeservice.RecurseMode;
import org.edu_sharing.service.share.GlobalShareService;
import org.edu_sharing.service.share.ShareInfoService;
import org.edu_sharing.service.share.ShareInfoServiceImpl;
import org.edu_sharing.service.share.ShareType;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@UpdateService
@RequiredArgsConstructor
public class Release_11_0_ShareInfos {


    private final NodeService nodeService;
    private final OwnableService ownableService;
    private final ShareInfoServiceImpl shareInfoService;
    private final GlobalShareService globalShareService;

    @UpdateRoutine(
            id = "Release_11_0_ShareInfos",
            description = "Migrate ph_users and ph_invite to shareInfo",
            order = 11000,
            auto = true)
    public void execute() {
        NodeRunner runner = new NodeRunner();
        runner.setRunAsSystem(true);
        runner.setTypes(List.of(CCConstants.CCM_TYPE_IO, CCConstants.CCM_TYPE_MAP));
        runner.setThreaded(true);
        runner.setTransaction(NodeRunner.TransactionMode.LocalRetrying);
        runner.setKeepModifiedDate(true);
        runner.setRecurseMode(RecurseMode.All);

        runner.setFilter(nodeService::exists);
        runner.setTask(nodeRef -> {
            String creator = (String) nodeService.getProperty(nodeRef, ContentModel.PROP_CREATOR);

            List<String> rawUsers = (List<String>) nodeService.getProperty(nodeRef, QName.createQName(CCConstants.CCM_PROP_PH_USERS));
            List<String> rawInvited = (List<String>) nodeService.getProperty(nodeRef, QName.createQName(CCConstants.CCM_PROP_PH_INVITED));
            if (rawUsers == null) rawUsers = new ArrayList<>();
            if (rawInvited == null) rawInvited = new ArrayList<>();

            Set<String> users =  new HashSet<>(rawUsers);
            Set<String> invited = new HashSet<>(rawInvited);
            Share[] shares = globalShareService.getShares(nodeRef.getId());

            for (Share share : shares) {
                String shareNodeId = share.getNodeId();
                NodeRef nodeRefShare = new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, shareNodeId);
                String shareCreator = (String) nodeService.getProperty(nodeRefShare, ContentModel.PROP_CREATOR);

                shareInfoService.createShare(nodeRef.getId(), shareCreator, shareNodeId, ShareType.LINK);
                users.remove(shareCreator);
                log.info("Created link share for {}: by: {} - with: {}", nodeRef.getId(), shareCreator, share.getNodeId());
            }

            users.remove(creator);
            for (String authority : invited) {
                shareInfoService.createShare(nodeRef.getId(), creator, authority, ShareType.AUTHORITY);
                log.info("Created authority share for {}: by: {} - with: {}", nodeRef.getId(), creator, authority);
            }

            if(!users.isEmpty()){
                log.warn("ShareInfos for {} are not complete. Missing users: {}", nodeRef.getId(), users);
            }

            nodeService.removeProperty(nodeRef, QName.createQName(CCConstants.CCM_PROP_PH_USERS));
            nodeService.removeProperty(nodeRef, QName.createQName(CCConstants.CCM_PROP_PH_INVITED));
            nodeService.removeProperty(nodeRef, QName.createQName(CCConstants.CCM_PROP_PH_MODIFIED));
            nodeService.removeProperty(nodeRef, QName.createQName(CCConstants.CCM_PROP_PH_ACTION));
        });

        log.info("Starting ShareInfos update");
        int processed = runner.run();
        log.info("ShareInfos for all nodes updated. Processed: {}", processed);
    }
}
