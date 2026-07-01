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
import org.edu_sharing.service.contributor.ContributorServiceFactory;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.Collection;
import java.util.LinkedHashSet;
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
 * The actual registration (parsing, deduplication, existence check, insert) lives in
 * {@link org.edu_sharing.service.contributor.ContributorService#registerVCardsIfAbsent} - a single
 * source of truth shared with the migration. That method is intentionally not
 * {@code @Permission}-guarded, so this policy bypasses the {@code TOOLPERMISSION_MANAGE_CONTRIBUTORS}
 * toolpermission; the currently authenticated user is recorded as the {@code creator}.
 * <p>
 * This policy only contributes its node-specific concern: detecting which contributor property
 * actually changed. Performance: bound to the narrowest type ({@code ccm:io}); only acts when a
 * contributor property changed (before/after comparison). Any failure is logged and swallowed so a
 * registry problem never fails the node save.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ContributorRegistryPolicy implements OnUpdatePropertiesPolicy {

    private final PolicyComponent policyComponent;

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
            Set<String> changedVCards = new LinkedHashSet<>();
            for (QName prop : CONTRIBUTOR_PROPS) {
                Serializable afterValue = after.get(prop);
                // only inspect contributor properties whose value actually changed
                if (afterValue == null || Objects.equals(before.get(prop), afterValue)) {
                    continue;
                }
                changedVCards.addAll(asVCardStrings(afterValue));
            }
            if (changedVCards.isEmpty()) {
                return;
            }
            ContributorServiceFactory.getInstance().getLocalService()
                    .registerVCardsIfAbsent(changedVCards, AuthenticationUtil.getFullyAuthenticatedUser());
        } catch (Throwable e) {
            // a registry problem must never fail the node save
            log.error("Failed to capture contributors into the registry for node {}", nodeRef.getId(), e);
        }
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
}
