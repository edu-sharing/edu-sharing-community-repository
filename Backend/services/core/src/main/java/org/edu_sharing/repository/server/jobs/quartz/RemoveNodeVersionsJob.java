package org.edu_sharing.repository.server.jobs.quartz;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.version.Version;
import org.alfresco.service.cmr.version.VersionHistory;
import org.alfresco.service.cmr.version.VersionService;
import org.apache.commons.lang3.StringUtils;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.jobs.helper.NodeRunner;
import org.edu_sharing.repository.server.jobs.quartz.annotation.JobDescription;
import org.edu_sharing.repository.server.jobs.quartz.annotation.JobFieldDescription;
import org.edu_sharing.service.nodeservice.NodeService;
import org.edu_sharing.service.usage.Usage;
import org.edu_sharing.service.usage.Usage2Service;
import org.jetbrains.annotations.NotNull;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * when a version history entry is removed the node is deleted in alf_node, but a new node with type_qname_id targeting
 * to alf_qname local_nome "deleted" is created. for this node there is one entry in alf_node_properties which is
 * keeping the original dbid from the version entry. this job helps reducing entries in alf_node properties.
 */
@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
@Slf4j
@JobDescription(description = "Removes versions of node which are not referenced")
public class RemoveNodeVersionsJob extends AbstractJobMapAnnotationParams {

    // all versions with the labels listed here will NOT be deleted
    static final List<String> BLOCKED_VERSION_LABELS = Arrays.asList(
            CCConstants.VERSION_COMMENT_BULK_CREATE,
            CCConstants.VERSION_COMMENT_BULK_UPDATE,
            CCConstants.VERSION_COMMENT_BULK_UPDATE_RESYNC,
            CCConstants.VERSION_COMMENT_BULK_MIGRATION
    );

    @Setter
    @JobFieldDescription(
            description = "Declares the age of an version. If an version is older than the given value it will be deleted. Defined by duration according to ISO 8601: https://en.wikipedia.org/wiki/ISO_8601#Durations",
            sampleValue = "P1D")
    private String olderThan;

    @Setter
    @JobFieldDescription(
            description = "Specifies the minimum number of versions which should be keep in respect to there age. 0 defines all should be deleted",
            sampleValue = "0")
    private int keepAtLeast = 0;

    @Setter
    @JobFieldDescription(description = "Folder id to start from")
    private String startFolder;

    @Setter
    private Usage2Service usage2Service;

    @Autowired
    @Setter
    private VersionService versionService;

    @Autowired
    @Setter
    private NodeService nodeService;

    @Override
    public void executeInternal(JobExecutionContext jobExecutionContext) {
        List<NodeRef> nodeRefs = Collections.synchronizedList(new ArrayList<>());
        NodeRunner runner = new NodeRunner();
        runner.setThreaded(true);
        runner.setRunAsSystem(true);
        runner.setKeepModifiedDate(true);
        runner.setTransaction(NodeRunner.TransactionMode.Local);
        runner.setTypes(Collections.singletonList(CCConstants.CCM_TYPE_IO));

        if (StringUtils.isNotBlank(startFolder)) {
            runner.setStartFolder(startFolder);
        }

        runner.setTask(nodeRefs::add);
        runner.run();

        nodeRefs.forEach(n -> AuthenticationUtil.runAsSystem(() -> {
            try {
                handleNode(n);
            }catch (Exception e){
                log.error("Could not handle node {}", n, e);
            }
            return null;
        }));

    }

    public void handleNode(@NotNull NodeRef node) {
        String replicationSourceId = nodeService.getProperty(node.getStoreRef().getProtocol(), node.getStoreRef().getIdentifier(), node.getId(), CCConstants.CCM_PROP_IO_REPLICATIONSOURCEID);
        long timeSpan = StringUtils.isNotBlank(olderThan)
                ? Duration.parse(olderThan).toMillis()
                : -1;

        VersionHistory versionHistory = versionService.getVersionHistory(node);
        if (versionHistory == null) {
            return;
        }

        //Version headVersion = versionHistory.getHeadVersion();
        //Date refDate = headVersion.getFrozenModifiedDate();
        Date refDate = new Date();
        final List<Usage> usages;
        try {
            usages = usage2Service.getUsages("-home-", node.getId(), null, null);
        } catch (Exception e) {
            log.warn("node {} is be skipped due to a usage request failure", node, e);
            return;
        }

        String versionInUse = nodeService.getProperty(node.getStoreRef().getProtocol(), node.getStoreRef().getIdentifier(), node.getId(), CCConstants.LOM_PROP_LIFECYCLE_VERSION);

        List<Version> versionsToDelete = versionHistory.getAllVersions().stream()
                .skip(keepAtLeast)
                .filter(version -> !Objects.equals(version.getVersionLabel(), versionInUse))
                .filter(version -> !BLOCKED_VERSION_LABELS.contains(version.getVersionLabel()))
                .filter(version -> Math.abs(refDate.getTime() - version.getFrozenModifiedDate().getTime()) > timeSpan)
                .filter(version -> usages.stream().noneMatch(x -> Objects.equals(x.getUsageVersion(), version.getVersionLabel())))
                .collect(Collectors.toList());

        versionsToDelete.forEach(version -> {
            log.info("deleteing version node:{} replicationSourceId:{} versionLabel:{}", node, replicationSourceId, version.getVersionLabel());
            versionService.deleteVersion(node, version);
        });
    }
}