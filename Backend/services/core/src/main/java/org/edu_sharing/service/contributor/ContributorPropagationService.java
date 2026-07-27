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
 * Matching compares the field values of the component that the changed contributor represents (person or
 * organization) against the - possibly combined - vcard stored on the node, as it was <i>before</i> the
 * change. Comparing the full component (not just the persistent id) prevents confusing two distinct persons
 * sharing a name; ignoring the other component lets a combined "person + organization" vcard still match its
 * managed entry. A coarse CMIS prefilter (LIKE on a distinctive id token) narrows the candidate set, the
 * exact comparison happens in Java. On a match only the matched component of the vcard is rewritten.
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
                    if (updateNode(row.getNodeRef(), before, after, contributorProps)) {
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
     * Rewrite all contributor properties of a single node, replacing the matching component of entries that
     * match {@code before}.
     *
     * @return true if at least one property of the node was changed
     */
    @SuppressWarnings("unchecked")
    private boolean updateNode(NodeRef nodeRef, ContributorEntry before, ContributorEntry after, List<String> contributorProps) {
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
                if (stored != null && componentMatch(before, stored)) {
                    // rewrite only the matched component, keeping any other component of a combined vcard intact
                    applyComponent(stored, after, before.getKind());
                    newVCards.add(ContributorVCardUtil.toVCardString(stored));
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
     * Compare the fields of the component that {@code before} represents against the (possibly combined) stored
     * vcard: person fields (name, title, ORCID, GND) for a person, organization fields (org, ROR, Wikidata) for
     * an organization, plus the shared fields (email, url) that belong to both. Fields of the other component
     * are ignored so that a combined vcard still matches its managed person / organization entry. The
     * persistent ids stay part of the comparison, so two distinct contributors sharing a name are never confused.
     */
    private boolean componentMatch(ContributorEntry before, ContributorEntry stored) {
        // email and url are shared by both components
        if (!eq(before.getEmail(), stored.getEmail()) || !eq(before.getUrl(), stored.getUrl())) {
            return false;
        }
        if (before.getKind() == org.edu_sharing.service.search.SearchService.ContributorKind.ORGANIZATION) {
            return eq(before.getOrg(), stored.getOrg())
                    && eq(before.getRor(), stored.getRor())
                    && eq(before.getWikidata(), stored.getWikidata());
        }
        return eq(before.getGivenname(), stored.getGivenname())
                && eq(before.getSurname(), stored.getSurname())
                && eq(before.getTitle(), stored.getTitle())
                && eq(before.getOrcid(), stored.getOrcid())
                && eq(before.getGnduri(), stored.getGnduri());
    }

    /**
     * Overwrite the fields of the matched component in {@code target} with the values from {@code after},
     * leaving the fields of the other component untouched. Email and url are shared by both components and are
     * therefore always rewritten.
     */
    private void applyComponent(ContributorEntry target, ContributorEntry after,
                                org.edu_sharing.service.search.SearchService.ContributorKind kind) {
        target.setEmail(after.getEmail());
        target.setUrl(after.getUrl());
        if (kind == org.edu_sharing.service.search.SearchService.ContributorKind.ORGANIZATION) {
            target.setOrg(after.getOrg());
            target.setRor(after.getRor());
            target.setWikidata(after.getWikidata());
        } else {
            target.setTitle(after.getTitle());
            target.setGivenname(after.getGivenname());
            target.setSurname(after.getSurname());
            target.setUid(after.getUid());
            target.setOrcid(after.getOrcid());
            target.setGnduri(after.getGnduri());
        }
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
