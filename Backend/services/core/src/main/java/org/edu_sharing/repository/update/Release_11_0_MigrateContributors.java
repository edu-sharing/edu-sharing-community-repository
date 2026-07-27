package org.edu_sharing.repository.update;

import lombok.extern.slf4j.Slf4j;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.edu_sharing.repository.server.update.UpdateRoutine;
import org.edu_sharing.repository.server.update.UpdateService;
import org.edu_sharing.service.contributor.ContributorEntry;
import org.edu_sharing.service.contributor.ContributorServiceFactory;
import org.edu_sharing.service.search.SearchServiceFactory;

import java.util.List;
import java.util.Set;

/**
 * Migrates the existing embedded contributor vcards into the autonomous edu_contributor registry.
 * <p>
 * Enumerates the distinct contributor vcards via the elasticsearch contributor aggregation
 * (see {@link org.edu_sharing.service.search.SearchService#getAllContributorVCards()}). That query
 * already restricts to contributors carrying a persistent X- id (ORCID/GND for persons, ROR/Wikidata
 * for organizations) - mirroring what the {@code searchContributors} autocomplete surfaces, i.e. an
 * email alone is not enough to be migrated. The actual registration (parsing, deduplication,
 * existence check, insert) is delegated to
 * {@link org.edu_sharing.service.contributor.ContributorService#registerVCardsIfAbsent} - the single
 * source of truth shared with the capture policy. Idempotent - already migrated contributors (matched
 * by id) are skipped, so the routine can be re-run. The migrating user is recorded as the creator.
 */
@Slf4j
@UpdateService
public class Release_11_0_MigrateContributors {

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

        // record the user running the migration as the creator of the registry entries
        String creator = AuthenticationUtil.getFullyAuthenticatedUser();
        List<ContributorEntry> created = ContributorServiceFactory.getInstance().getLocalService()
                .registerVCardsIfAbsent(vcards, creator);
        log.info("Contributor registry migration finished. Migrated {} contributor(s)", created.size());
    }
}