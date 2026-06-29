package org.edu_sharing.repository.update;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.edu_sharing.repository.server.update.UpdateRoutine;
import org.edu_sharing.repository.server.update.UpdateService;
import org.edu_sharing.service.contributor.ContributorEntry;
import org.edu_sharing.service.contributor.ContributorVCardUtil;
import org.edu_sharing.service.contributor.ibatis.ContributorMapper;
import org.edu_sharing.service.search.SearchServiceFactory;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/**
 * Migrates the existing embedded contributor vcards into the autonomous edu_contributor registry.
 * <p>
 * Enumerates the distinct contributor vcards via the elasticsearch contributor aggregation
 * (see {@link org.edu_sharing.service.search.SearchService#getAllContributorVCards()}). That query
 * already restricts to contributors carrying a persistent X- id (ORCID/GND for persons, ROR/Wikidata
 * for organizations) - mirroring what the {@code searchContributors} autocomplete surfaces, i.e. an
 * email alone is not enough to be migrated. They are deduplicated by id and inserted into the
 * registry. Idempotent - already migrated contributors (matched by id) are skipped, so the routine can
 * be re-run.
 */
@Slf4j
@UpdateService
@RequiredArgsConstructor
public class Release_11_0_MigrateContributors {

    private final ContributorMapper contributorMapper;

    @UpdateRoutine(
            id = "Release_11_0_MigrateContributors",
            description = "Migrate existing embedded contributors (with persistent ids) into the edu_contributor registry",
            order = 11001,
            isNonTransactional = true,
            async = true,
            blocking = false)
    public void execute() throws Exception {
        log.info("Starting contributor registry migration");
        Set<String> vcards = SearchServiceFactory.getInstance().getLocalService().getAllContributorVCards();
        log.info("Found {} distinct contributor vcards in the index", vcards.size());

        Set<String> seenKeys = new HashSet<>();
        int created = 0;
        for (String vcard : vcards) {
            ContributorEntry entry = ContributorVCardUtil.fromVCardString(vcard);
            if (entry == null) {
                continue; // unparseable or no persistent id -> not manageable
            }
            if (!seenKeys.add(idKey(entry))) {
                continue; // already handled in this run (e.g. different vcard formatting, same ids)
            }
            if (!contributorMapper.findByAnyId(entry.getOrcid(), entry.getGnduri(), entry.getRor(), entry.getWikidata(), entry.getEmail()).isEmpty()) {
                continue; // already present in the registry
            }
            Date now = new Date();
            entry.setCreated(now);
            entry.setLastUpdated(now);
            contributorMapper.create(entry);
            created++;
            log.info("Migrated contributor {} ({})", idKey(entry), entry.getKind());
        }
        log.info("Contributor registry migration finished. Migrated {} contributor(s)", created);
    }

    private String idKey(ContributorEntry e) {
        return e.getKind() + "|" + e.getOrcid() + "|" + e.getGnduri() + "|" + e.getRor() + "|" + e.getWikidata() + "|" + e.getEmail();
    }
}