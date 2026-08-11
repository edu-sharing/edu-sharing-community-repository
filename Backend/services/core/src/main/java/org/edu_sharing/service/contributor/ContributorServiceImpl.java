package org.edu_sharing.service.contributor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.service.contributor.ibatis.ContributorMapper;
import org.edu_sharing.service.permission.annotation.Permission;
import org.edu_sharing.service.search.SearchService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContributorServiceImpl implements ContributorService {

    private final ContributorMapper contributorMapper;
    private final ContributorPropagationService propagationService;

    /** public autocomplete - no toolpermission required */
    @Override
    @Permission(requiresUser = true)
    public List<ContributorEntry> search(String searchWord, SearchService.ContributorKind kind, int limit) {
        return contributorMapper.search(StringUtils.trimToNull(searchWord), kind, limit <= 0 ? 50 : limit);
    }

    @Override
    @Permission(CCConstants.CCM_VALUE_TOOLPERMISSION_MANAGE_CONTRIBUTORS)
    public List<ContributorEntry> getAll(long skip, int limit) {
        return contributorMapper.getAll(skip, limit <= 0 ? 100 : limit);
    }

    @Override
    @Permission(CCConstants.CCM_VALUE_TOOLPERMISSION_MANAGE_CONTRIBUTORS)
    public long count() {
        return contributorMapper.count();
    }

    @Override
    @Permission(CCConstants.CCM_VALUE_TOOLPERMISSION_MANAGE_CONTRIBUTORS)
    public ContributorPage listManaged(String searchWord, SearchService.ContributorKind kind, List<ContributorIdType> hasIds,
                                       ContributorSortProperty sortBy, boolean ascending, long skip, int limit) {
        String word = StringUtils.trimToNull(searchWord);
        // map the id-type enums to their (whitelisted) db columns - never pass user strings into the sql
        List<String> hasIdColumns = (hasIds == null) ? List.of()
                : hasIds.stream().filter(Objects::nonNull).map(ContributorIdType::getColumn).toList();
        // build a whitelisted ORDER BY expression from the enum + direction
        ContributorSortProperty sort = (sortBy == null) ? ContributorSortProperty.NAME : sortBy;
        String orderBy = sort.toOrderBy(ascending);

        long total = contributorMapper.countManaged(word, kind, hasIdColumns);
        List<ContributorEntry> entries = contributorMapper.listManaged(
                word, kind, hasIdColumns, orderBy, Math.max(0, skip), limit <= 0 ? 50 : limit);
        return new ContributorPage(entries, total);
    }

    @Override
    @Permission(CCConstants.CCM_VALUE_TOOLPERMISSION_MANAGE_CONTRIBUTORS)
    public ContributorEntry getById(long id) {
        return contributorMapper.getById(id);
    }

    @Override
    @Permission(CCConstants.CCM_VALUE_TOOLPERMISSION_MANAGE_CONTRIBUTORS)
    public ContributorEntry create(ContributorEntry entry) {
        validate(entry);
        if (entry.getKind() == null) {
            entry.setKind(deriveKind(entry));
        }
        entry.setId(null);
        entry.setVcard(ContributorVCardUtil.toVCardString(entry));
        return insert(entry);
    }

    /** ungated - no toolpermission (see {@link ContributorService#registerVCardsIfAbsent}) */
    @Override
    @Permission(requiresUser = true)
    public List<ContributorEntry> registerVCardsIfAbsent(Collection<String> vcards, String creator) {
        List<ContributorEntry> created = new ArrayList<>();
        Set<String> seenKeys = new HashSet<>();
        for (String vcard : vcards == null ? List.<String>of() : vcards) {
            // a single vcard may describe a person together with an affiliated organization -> two entries
            for (ContributorEntry entry : ContributorVCardUtil.toEntries(vcard)) {
                if (!seenKeys.add(entry.idKey())) {
                    continue; // same id already handled in this call (e.g. several roles / vcard formattings)
                }
                // email is copied into both the person and the organization record; excluding it from the
                // organization lookup prevents the just-inserted person from masking the organization
                String emailForLookup = entry.getKind() == SearchService.ContributorKind.ORGANIZATION
                        ? null : entry.getEmail();
                if (!contributorMapper.findByAnyId(entry.getOrcid(), entry.getGnduri(), entry.getRor(),
                        entry.getWikidata(), emailForLookup).isEmpty()) {
                    continue; // already present in the registry
                }
                entry.setCreator(creator);
                insert(entry);
                created.add(entry);
                log.info("Registered contributor {} ({}) by {}", entry.idKey(), entry.getKind(), creator);
            }
        }
        return created;
    }

    /** shared persist step: stamps created/lastUpdated and writes the entry to the db */
    private ContributorEntry insert(ContributorEntry entry) {
        Date now = new Date();
        entry.setCreated(now);
        entry.setLastUpdated(now);
        contributorMapper.create(entry);
        return entry;
    }

    @Override
    @Permission(CCConstants.CCM_VALUE_TOOLPERMISSION_MANAGE_CONTRIBUTORS)
    public ContributorEntry update(long id, ContributorEntry entry, boolean applyToExisting) {
        ContributorEntry before = contributorMapper.getById(id);
        if (before == null) {
            throw new IllegalArgumentException("Contributor " + id + " not found");
        }
        validate(entry);

        entry.setId(id);
        if (entry.getKind() == null) {
            entry.setKind(deriveKind(entry));
        }
        entry.setCreated(before.getCreated());
        entry.setLastUpdated(new Date());
        entry.setVcard(ContributorVCardUtil.toVCardString(entry));
        contributorMapper.update(entry);

        if (applyToExisting) {
            // asynchronously rewrite the vcard on all media nodes that still carry the pre-change contributor
            propagationService.applyContributorChange(before, entry);
        }
        return entry;
    }

    @Override
    @Permission(CCConstants.CCM_VALUE_TOOLPERMISSION_MANAGE_CONTRIBUTORS)
    public void delete(long id) {
        // only removes the registry entry - the media keep their embedded contributor untouched
        contributorMapper.delete(id);
    }

    private void validate(ContributorEntry entry) {
        if (entry == null) {
            throw new IllegalArgumentException("contributor must not be null");
        }
        if (!hasAnyId(entry)) {
            throw new IllegalArgumentException("a contributor must carry at least one id (ORCID, GND, ROR or Wikidata) - email alone is not sufficient");
        }
    }

    /** email alone must not qualify - only ORCID/GND/ROR/Wikidata make an entry a manageable contributor */
    private boolean hasAnyId(ContributorEntry entry) {
        return StringUtils.isNotBlank(entry.getOrcid())
                || StringUtils.isNotBlank(entry.getGnduri())
                || StringUtils.isNotBlank(entry.getRor())
                || StringUtils.isNotBlank(entry.getWikidata());
    }

    private SearchService.ContributorKind deriveKind(ContributorEntry entry) {
        boolean orgId = StringUtils.isNotBlank(entry.getRor()) || StringUtils.isNotBlank(entry.getWikidata());
        boolean personId = StringUtils.isNotBlank(entry.getOrcid()) || StringUtils.isNotBlank(entry.getGnduri());
        return orgId && !personId ? SearchService.ContributorKind.ORGANIZATION : SearchService.ContributorKind.PERSON;
    }
}
