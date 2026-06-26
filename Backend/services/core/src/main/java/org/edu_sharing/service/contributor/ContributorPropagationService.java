package org.edu_sharing.service.contributor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.ResultSetRow;
import org.alfresco.service.cmr.search.SearchParameters;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.QName;
import org.apache.commons.lang3.StringUtils;
import org.edu_sharing.alfresco.service.search.cmis.Filters;
import org.edu_sharing.alfresco.service.search.cmis.Predicate;
import org.edu_sharing.alfresco.service.search.cmis.Query;
import org.edu_sharing.alfresco.service.search.cmis.QueryBuilder;
import org.edu_sharing.alfresco.service.search.cmis.QueryStatement;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.jobs.annotations.Queued;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Applies a contributor change to all media nodes that currently carry the (unchanged) contributor.
 * <p>
 * Matching follows the requirement to compare against <b>all field values</b> of the contributor as it
 * was <i>before</i> the change - not just the persistent id - so that two distinct persons sharing a name
 * are never confused. A coarse CMIS prefilter (LIKE on a distinctive id token) narrows the candidate set,
 * the exact full-field comparison happens in Java.
 * <p>
 * Runs asynchronously via {@link Queued} so the admin request returns immediately.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContributorPropagationService {

    private static final int PAGE_SIZE = 500;

    private final QueryBuilder queryBuilder;
    private final SearchService searchService;
    private final NodeService nodeService;

    /**
     * Find all nodes referencing {@code before} (full-field match) and rewrite the embedded vcard to {@code after}.
     */
    @Queued(unique = true, group = "contributor-propagation")
    public void applyContributorChange(ContributorEntry before, ContributorEntry after) {
        AuthenticationUtil.runAsSystem(() -> {
            try {
                doApply(before, after);
            } catch (Exception e) {
                log.error("Failed to propagate contributor change for {}", describe(before), e);
                throw new RuntimeException(e);
            }
            return null;
        });
    }

    private void doApply(ContributorEntry before, ContributorEntry after) {
        String token = distinctiveToken(before);
        if (token == null) {
            log.warn("Contributor {} has no persistent id, skipping propagation", describe(before));
            return;
        }
        List<String> contributorProps = contributorPropertyQNames();
        String afterVCard = ContributorVCardUtil.toVCardString(after);

        QueryStatement query = Query.select(CCConstants.SYS_PROP_NODE_UID)
                .from(CCConstants.CCM_TYPE_IO)
                .where(Filters.or(contributorProps.stream()
                        .map(prop -> Filters.like(prop, "%" + token + "%"))
                        .toArray(Predicate[]::new)));
        String cmisQuery = queryBuilder.build(query);
        log.info("Propagating contributor change, candidate query: {}", cmisQuery);

        int updatedNodes = 0;
        int skip = 0;
        while (true) {
            SearchParameters params = new SearchParameters();
            params.setLanguage(SearchService.LANGUAGE_CMIS_ALFRESCO);
            params.addStore(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE);
            params.setMaxPermissionChecks(0);
            params.setSkipCount(skip);
            params.setMaxItems(PAGE_SIZE);
            params.setQuery(cmisQuery);

            ResultSet rs = searchService.query(params);
            try {
                for (ResultSetRow row : rs) {
                    if (updateNode(row.getNodeRef(), before, afterVCard, contributorProps)) {
                        updatedNodes++;
                    }
                }
                if (rs.length() < PAGE_SIZE) {
                    break;
                }
            } finally {
                rs.close();
            }
            skip += PAGE_SIZE;
        }
        log.info("Propagated contributor change to {} node(s) for {}", updatedNodes, describe(before));
    }

    /**
     * Rewrite all contributor properties of a single node, replacing entries that fully match {@code before}.
     *
     * @return true if at least one property of the node was changed
     */
    @SuppressWarnings("unchecked")
    private boolean updateNode(NodeRef nodeRef, ContributorEntry before, String afterVCard, List<String> contributorProps) {
        boolean nodeChanged = false;
        for (String prop : contributorProps) {
            QName qName = QName.createQName(prop);
            Serializable value = nodeService.getProperty(nodeRef, qName);
            if (value == null) {
                continue;
            }
            List<String> vcards = toStringList(value);
            boolean propChanged = false;
            List<String> newVCards = new ArrayList<>(vcards.size());
            for (String vcard : vcards) {
                ContributorEntry stored = ContributorVCardUtil.fromVCardString(vcard);
                if (stored != null && fullFieldMatch(before, stored)) {
                    newVCards.add(afterVCard);
                    propChanged = true;
                } else {
                    newVCards.add(vcard);
                }
            }
            if (propChanged) {
                nodeService.setProperty(nodeRef, qName, (Serializable) newVCards);
                nodeChanged = true;
            }
        }
        if (nodeChanged) {
            log.debug("Updated contributor on node {}", nodeRef.getId());
        }
        return nodeChanged;
    }

    /**
     * Compare all relevant fields of the contributor (name, org, email, url, all persistent ids).
     * Two contributors only match if every field is equal - this prevents over-matching on homonyms.
     */
    private boolean fullFieldMatch(ContributorEntry a, ContributorEntry b) {
        return eq(a.getGivenname(), b.getGivenname())
                && eq(a.getSurname(), b.getSurname())
                && eq(a.getTitle(), b.getTitle())
                && eq(a.getOrg(), b.getOrg())
                && eq(a.getEmail(), b.getEmail())
                && eq(a.getUrl(), b.getUrl())
                && eq(a.getOrcid(), b.getOrcid())
                && eq(a.getGnduri(), b.getGnduri())
                && eq(a.getRor(), b.getRor())
                && eq(a.getWikidata(), b.getWikidata());
    }

    private boolean eq(String a, String b) {
        return Objects.equals(StringUtils.trimToNull(a), StringUtils.trimToNull(b));
    }

    private String distinctiveToken(ContributorEntry entry) {
        if (StringUtils.isNotBlank(entry.getOrcid())) return entry.getOrcid();
        if (StringUtils.isNotBlank(entry.getGnduri())) return entry.getGnduri();
        if (StringUtils.isNotBlank(entry.getRor())) return entry.getRor();
        if (StringUtils.isNotBlank(entry.getWikidata())) return entry.getWikidata();
        if (StringUtils.isNotBlank(entry.getEmail())) return entry.getEmail();
        return null;
    }

    private List<String> contributorPropertyQNames() {
        Set<String> props = new LinkedHashSet<>();
        props.addAll(CCConstants.getLifecycleContributerPropsMap().values());
        props.addAll(CCConstants.getMetadataContributerPropsMap().values());
        return new ArrayList<>(props);
    }

    @SuppressWarnings("unchecked")
    private List<String> toStringList(Serializable value) {
        if (value instanceof List) {
            List<String> result = new ArrayList<>();
            for (Object o : (List<Object>) value) {
                if (o != null) {
                    result.add(o.toString());
                }
            }
            return result;
        }
        return new ArrayList<>(List.of(value.toString()));
    }

    private String describe(ContributorEntry entry) {
        return entry == null ? "null" : (entry.getId() + "/" + distinctiveToken(entry));
    }
}
