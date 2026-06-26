package org.edu_sharing.repository.update;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.jobs.helper.NodeRunner;
import org.edu_sharing.repository.server.update.UpdateRoutine;
import org.edu_sharing.repository.server.update.UpdateService;
import org.edu_sharing.service.contributor.ContributorEntry;
import org.edu_sharing.service.contributor.ContributorVCardUtil;
import org.edu_sharing.service.contributor.ibatis.ContributorMapper;
import org.edu_sharing.service.nodeservice.RecurseMode;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Migrates the existing embedded contributor vcards into the autonomous edu_contributor registry.
 * <p>
 * Iterates over all ccm:io nodes via {@link NodeRunner} (database/tree traversal - not elasticsearch),
 * reads the embedded contributor vcards, keeps only contributors carrying at least one persistent id,
 * deduplicates them by id and inserts them into the registry. Idempotent - already migrated
 * contributors (matched by id) are skipped, so the routine can be re-run.
 */
@Slf4j
@UpdateService
@RequiredArgsConstructor
public class Release_11_0_MigrateContributors {

    private final NodeService nodeService;
    private final ContributorMapper contributorMapper;

    @UpdateRoutine(
            id = "Release_11_0_MigrateContributors",
            description = "Migrate existing embedded contributors (with persistent ids) into the edu_contributor registry",
            order = 11001,
            isNonTransactional = true,
            async = true,
            blocking = false)
    public void execute() {
        List<String> contributorProps = contributorPropertyQNames();
        Set<String> seenKeys = ConcurrentHashMap.newKeySet();

        NodeRunner runner = new NodeRunner();
        runner.setRunAsSystem(true);
        runner.setTypes(List.of(CCConstants.CCM_TYPE_IO));
        runner.setThreaded(true);
        runner.setTransaction(NodeRunner.TransactionMode.LocalRetrying);
        runner.setKeepModifiedDate(true);
        runner.setRecurseMode(RecurseMode.All);
        runner.setFilter(nodeService::exists);
        runner.setTask(nodeRef -> migrateNode(nodeRef, contributorProps, seenKeys));

        log.info("Starting contributor registry migration");
        int processed = runner.run();
        log.info("Contributor registry migration finished. Processed {} node(s), {} distinct contributor(s) seen",
                processed, seenKeys.size());
    }

    private void migrateNode(NodeRef nodeRef, List<String> contributorProps, Set<String> seenKeys) {
        for (String prop : contributorProps) {
            Serializable value = nodeService.getProperty(nodeRef, QName.createQName(prop));
            for (String vcard : toStringList(value)) {
                ContributorEntry entry = ContributorVCardUtil.fromVCardString(vcard);
                if (entry == null) {
                    continue; // unparseable or no persistent id -> not manageable
                }
                if (!seenKeys.add(idKey(entry))) {
                    continue; // already handled in this run
                }
                if (!contributorMapper.findByAnyId(entry.getOrcid(), entry.getGnduri(), entry.getRor(), entry.getWikidata(), entry.getEmail()).isEmpty()) {
                    continue; // already present in the registry
                }
                Date now = new Date();
                entry.setCreated(now);
                entry.setLastUpdated(now);
                contributorMapper.create(entry);
                log.info("Migrated contributor {} ({})", idKey(entry), entry.getKind());
            }
        }
    }

    private String idKey(ContributorEntry e) {
        return e.getKind() + "|" + e.getOrcid() + "|" + e.getGnduri() + "|" + e.getRor() + "|" + e.getWikidata() + "|" + e.getEmail();
    }

    private List<String> contributorPropertyQNames() {
        Set<String> props = new LinkedHashSet<>();
        props.addAll(CCConstants.getLifecycleContributerPropsMap().values());
        props.addAll(CCConstants.getMetadataContributerPropsMap().values());
        return new ArrayList<>(props);
    }

    @SuppressWarnings("unchecked")
    private List<String> toStringList(Serializable value) {
        List<String> result = new ArrayList<>();
        if (value instanceof List) {
            for (Object o : (List<Object>) value) {
                if (o != null) {
                    result.add(o.toString());
                }
            }
        } else if (value != null) {
            result.add(value.toString());
        }
        return result;
    }
}