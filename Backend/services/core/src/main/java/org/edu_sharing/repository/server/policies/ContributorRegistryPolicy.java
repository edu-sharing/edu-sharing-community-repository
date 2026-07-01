package org.edu_sharing.repository.server.policies;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.alfresco.repo.node.NodeServicePolicies.OnUpdatePropertiesPolicy;
import org.alfresco.repo.policy.JavaBehaviour;
import org.alfresco.repo.policy.PolicyComponent;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.service.contributor.ContributorEntry;
import org.edu_sharing.service.contributor.ContributorVCardUtil;
import org.edu_sharing.service.contributor.ibatis.ContributorMapper;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * On every {@code ccm:io} property change, captures contributors that carry a valid persistent
 * X- id (X-ORCID / X-GND-URI / X-ROR / X-Wikidata) into the autonomous contributor registry
 * (table {@code edu_contributor}). This keeps the registry populated during normal editing,
 * complementing the initial {@code Release_11_0_MigrateContributors} migration.
 * <p>
 * Deliberately bypasses the {@code TOOLPERMISSION_MANAGE_CONTRIBUTORS} toolpermission by writing
 * through the {@link ContributorMapper} directly instead of the {@code @Permission}-guarded
 * {@code ContributorService#create}. Each new registry entry records the currently authenticated
 * user as its {@code creator}.
 * <p>
 * Performance: bound to the narrowest type ({@code ccm:io}); only acts when a contributor property
 * actually changed (before/after comparison); deduplicates within the write and against the registry
 * before inserting. Runs synchronously in the write transaction (a plain db insert). Any failure is
 * logged and swallowed so a registry problem never fails the node save.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ContributorRegistryPolicy implements OnUpdatePropertiesPolicy {

    private final PolicyComponent policyComponent;
    private final ContributorMapper contributorMapper;

    /** all contributor property QNames (lifecycle + metadata contributers), built once */
    private static final Set<QName> CONTRIBUTOR_PROPS =
            Stream.concat(
                            CCConstants.getLifecycleContributerPropsMap().values().stream(),
                            CCConstants.getMetadataContributerPropsMap().values().stream())
                    .map(QName::createQName)
                    .collect(Collectors.toUnmodifiableSet());

    @PostConstruct
    public void init() {
        policyComponent.bindClassBehaviour(
                OnUpdatePropertiesPolicy.QNAME,
                QName.createQName(CCConstants.CCM_TYPE_IO),
                new JavaBehaviour(this, "onUpdateProperties"));
    }

    @Override
    public void onUpdateProperties(NodeRef nodeRef, Map<QName, Serializable> before, Map<QName, Serializable> after) {
        try {
            Set<String> seenKeys = new HashSet<>();
            for (QName prop : CONTRIBUTOR_PROPS) {
                Serializable afterValue = after.get(prop);
                // only inspect contributor properties whose value actually changed
                if (afterValue == null || Objects.equals(before.get(prop), afterValue)) {
                    continue;
                }
                for (String vcard : asVCardStrings(afterValue)) {
                    registerContributor(vcard, seenKeys);
                }
            }
        } catch (Throwable e) {
            // a registry problem must never fail the node save
            log.error("Failed to capture contributors into the registry for node {}", nodeRef.getId(), e);
        }
    }

    private void registerContributor(String vcard, Set<String> seenKeys) {
        ContributorEntry entry = ContributorVCardUtil.fromVCardString(vcard);
        if (entry == null) {
            return; // unparseable or no persistent X- id -> not a manageable contributor
        }
        if (!seenKeys.add(idKey(entry))) {
            return; // already handled in this write (e.g. same id in multiple roles)
        }
        if (!contributorMapper.findByAnyId(entry.getOrcid(), entry.getGnduri(), entry.getRor(),
                entry.getWikidata(), entry.getEmail()).isEmpty()) {
            return; // already present in the registry
        }
        String creator = AuthenticationUtil.getFullyAuthenticatedUser();
        Date now = new Date();
        entry.setCreator(creator);
        entry.setCreated(now);
        entry.setLastUpdated(now);
        contributorMapper.create(entry);
        log.info("Registered contributor {} ({}) from node edit by {}", idKey(entry), entry.getKind(), creator);
    }

    /** a contributor property is multi-value (a collection of vcard strings), but tolerate a single value too */
    private static Collection<String> asVCardStrings(Serializable value) {
        if (value instanceof Collection) {
            return ((Collection<?>) value).stream()
                    .filter(Objects::nonNull)
                    .map(Object::toString)
                    .collect(Collectors.toList());
        }
        return java.util.List.of(value.toString());
    }

    private static String idKey(ContributorEntry e) {
        return e.getKind() + "|" + e.getOrcid() + "|" + e.getGnduri() + "|" + e.getRor() + "|" + e.getWikidata() + "|" + e.getEmail();
    }
}
