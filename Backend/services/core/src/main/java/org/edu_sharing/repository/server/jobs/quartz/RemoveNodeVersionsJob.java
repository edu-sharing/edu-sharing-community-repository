package org.edu_sharing.repository.server.jobs.quartz;

import lombok.Setter;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.version.Version;
import org.alfresco.service.cmr.version.VersionHistory;
import org.alfresco.service.cmr.version.VersionService;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.jobs.helper.NodeRunner;
import org.edu_sharing.repository.server.jobs.quartz.annotation.JobDescription;
import org.edu_sharing.repository.server.jobs.quartz.annotation.JobFieldDescription;
import org.edu_sharing.service.nodeservice.NodeService;
import org.edu_sharing.service.nodeservice.NodeServiceFactory;
import org.edu_sharing.service.usage.Usage;
import org.edu_sharing.service.usage.Usage2Service;
import org.jetbrains.annotations.NotNull;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@JobDescription(description = "Removes versions of node which are not referenced")
public class RemoveNodeVersionsJob extends AbstractJobMapAnnotationParams {

    protected Logger logger = Logger.getLogger(RemoveNodeVersionsJob.class);

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
    private  String startFolder;

    @Autowired
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
        NodeRunner runner = new NodeRunner();
        runner.setThreaded(true);
        runner.setRunAsSystem(true);
        runner.setKeepModifiedDate(true);
        runner.setTransaction(NodeRunner.TransactionMode.Local);
        runner.setTypes(Collections.singletonList(CCConstants.CCM_TYPE_IO));

        if (StringUtils.isNotBlank(startFolder)) {
            runner.setStartFolder(startFolder);
        }

        runner.setTask(this::handleNode);
        runner.run();
    }

    public void handleNode(@NotNull NodeRef node) {
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
            logger.warn("node " + node + " is be skipped due to a usage request failure", e);
            return;
        }

        String versionInUse = nodeService.getProperty(node.getStoreRef().getProtocol(),node.getStoreRef().getIdentifier(), node.getId(),  CCConstants.LOM_PROP_LIFECYCLE_VERSION);

        List<Version> versionsToDelete = versionHistory.getAllVersions().stream()
                .skip(keepAtLeast)
                .filter(version -> !Objects.equals(version.getVersionLabel(), versionInUse))
                .filter(version -> Math.abs(refDate.getTime() - version.getFrozenModifiedDate().getTime()) > timeSpan)
                .filter(version -> usages.stream().noneMatch(x-> Objects.equals(x.getUsageVersion(), version.getVersionLabel())))
                .collect(Collectors.toList());

        versionsToDelete.forEach(version -> versionService.deleteVersion(node, version));
    }

    @Override
    public Class[] getJobClasses() {
        return allJobs;
    }
}
