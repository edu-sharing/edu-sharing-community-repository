package org.edu_sharing.repository.server.jobs.quartz;

import lombok.Setter;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.model.Repository;
import org.alfresco.repo.policy.BehaviourFilter;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.version.VersionModel;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.security.OwnableService;
import org.alfresco.service.cmr.version.Version;
import org.alfresco.service.cmr.version.VersionService;
import org.alfresco.service.cmr.version.VersionType;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.GUID;
import org.apache.commons.lang3.StringUtils;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.jobs.quartz.annotation.JobDescription;
import org.edu_sharing.repository.server.jobs.quartz.annotation.JobFieldDescription;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Creates a synthetic, self-contained test set of nodes carrying the legacy
 * ccm:permission_history / ccm:shares data (ph_users, ph_invited, ph_modified, ph_action and a
 * legacy ccm:share child node WITHOUT a corresponding ShareInfo row) - i.e. the pre-migration state
 * that {@code Release_11_0_ShareInfos} is meant to clean up.
 * <p>
 * Every scenario is created once in the workspace store; a subset is additionally moved to the
 * archive/trashcan store and a subset is versioned (and then diverged from its frozen version) so
 * the resulting test set exercises workspace, archive and version2Store the same way real
 * unmigrated data would.
 * <p>
 * Trigger via {@code POST /rest/admin/v1/job/org.edu_sharing.repository.server.jobs.quartz.GenerateShareInfosTestSetJob/sync}
 * to get the created node ids back in the response, or without {@code /sync} to run it
 * asynchronously and read the summary from the server log.
 */
@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
@JobDescription(description = "Creates a test set of nodes (workspace + archive + version2Store) carrying legacy ccm:permission_history/ccm:shares data, to verify Release_11_0_ShareInfos end to end. Safe to run repeatedly - each run gets its own folder.")
public class GenerateShareInfosTestSetJob extends AbstractJobMapAnnotationParams {

    @JobFieldDescription(description = "folder id to create the test set folder in; defaults to company home")
    private String parentFolder;

    @JobFieldDescription(description = "how many nodes to create per scenario", sampleValue = "2")
    private Integer nodesPerScenario;

    @JobFieldDescription(description = "usernames that must already exist in the target system; used for ph_users/ph_invited/share-creator variation. First entry becomes the node creator. Provide at least 2 for realistic overlap/leftover scenarios.", sampleValue = "[\"admin\"]")
    private List<String> testUsers;

    @JobFieldDescription(description = "additionally move one node per scenario to the archive/trashcan store", sampleValue = "true")
    private Boolean archiveSubset;

    @JobFieldDescription(description = "additionally create a version snapshot for one node per scenario, then change the live properties, so the version2Store copy ends up out of sync with the live node", sampleValue = "true")
    private Boolean versionSubset;

    @JobFieldDescription(description = "seed for the random user assignment, for reproducible test runs; omit for a random seed")
    private Long randomSeed;

    private final ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
    private final ServiceRegistry serviceRegistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);
    private final NodeService nodeService = serviceRegistry.getNodeService();
    private final VersionService versionService = serviceRegistry.getVersionService();
    private final Repository repositoryHelper = (Repository) applicationContext.getBean("repositoryHelper");

    @Autowired
    private BehaviourFilter policyBehaviourFilter;
    @Autowired
    private OwnableService ownableService;

    private List<String> users;
    private Random random;
    private final Map<String, Object> result = new LinkedHashMap<>();

    @Override
    protected void executeInternal(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        users = (testUsers == null || testUsers.isEmpty()) ? List.of("admin") : testUsers;
        if (users.size() < 2) {
            logger.warn("Only one testUser given (" + users.get(0) + ") - every scenario that needs a sharer/recipient pair degrades to using that same user twice");
        }
        int perScenario = (nodesPerScenario == null || nodesPerScenario < 1) ? 2 : nodesPerScenario;
        boolean doArchive = archiveSubset == null || archiveSubset;
        boolean doVersion = versionSubset == null || versionSubset;
        random = (randomSeed != null) ? new Random(randomSeed) : new Random();

        AuthenticationUtil.runAsSystem(() -> {
            NodeRef parent = StringUtils.isNotBlank(parentFolder)
                    ? new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, parentFolder)
                    : repositoryHelper.getCompanyHome();

            NodeRef folder = createNode(parent, ContentModel.TYPE_FOLDER,
                    "ShareInfos_Testset_" + GUID.generate(), randomUser());
            result.put("folder", folder.getId());
            logger.info("Created test set folder " + folder.getId());

            Map<String, List<NodeRef>> scenarioNodes = new LinkedHashMap<>();
            scenarioNodes.put("ph_users_only", createScenarioPhUsersOnly(folder, perScenario));
            scenarioNodes.put("ph_users_includes_creator", createScenarioPhUsersIncludesCreator(folder, perScenario));
            scenarioNodes.put("ph_invited_only_map", createScenarioPhInvitedOnlyOnMap(folder, perScenario));
            scenarioNodes.put("legacy_link_share_only", createScenarioLegacyShareOnly(folder, perScenario));
            scenarioNodes.put("legacy_link_share_plus_ph_users_overlap", createScenarioShareOverlap(folder, perScenario));
            scenarioNodes.put("legacy_link_share_plus_ph_users_leftover", createScenarioShareLeftover(folder, perScenario));
            scenarioNodes.put("negative_control_no_aspect", createScenarioNegativeControl(folder, perScenario));

            Map<String, Object> scenarioResult = new LinkedHashMap<>();
            scenarioNodes.forEach((key, nodes) -> scenarioResult.put(key, nodes.stream().map(NodeRef::getId).toList()));
            result.put("scenarios", scenarioResult);

            if (doVersion) {
                Map<String, Object> versioned = new LinkedHashMap<>();
                versioned.put("ph_users_includes_creator", versionAndDiverge(scenarioNodes.get("ph_users_includes_creator").get(0)));
                versioned.put("legacy_link_share_plus_ph_users_overlap", versionAndDiverge(scenarioNodes.get("legacy_link_share_plus_ph_users_overlap").get(0)));
                result.put("versioned", versioned);
            }

            if (doArchive) {
                Map<String, String> archived = new LinkedHashMap<>();
                archived.put("ph_users_only", archive(scenarioNodes.get("ph_users_only").get(0)));
                archived.put("ph_invited_only_map", archive(scenarioNodes.get("ph_invited_only_map").get(0)));
                archived.put("legacy_link_share_only", archive(scenarioNodes.get("legacy_link_share_only").get(0)));
                archived.put("legacy_link_share_plus_ph_users_leftover", archive(scenarioNodes.get("legacy_link_share_plus_ph_users_leftover").get(0)));
                result.put("archived", archived);
            }

            logger.info("ShareInfos test set complete: " + result);
            return null;
        });

        storeJobResultData(result);
    }

    // ---- scenarios -----------------------------------------------------------------------------
    // a material has exactly one creator - any legacy share child node it has is created by that
    // SAME person (never a second, independently-drawn identity). Every (sharer, recipient) pair
    // below is drawn via randomDistinctPair()/randomOtherUser() so a user is never recorded as
    // sharing with themselves; the one intentional exception is the "overlap" scenario, where the
    // creator re-appears in ph_users - that's the same identity in two roles about the same node,
    // not a share directed at themselves.

    private List<NodeRef> createScenarioPhUsersOnly(NodeRef parent, int count) {
        List<NodeRef> nodes = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String[] pair = randomDistinctPair(); // [creator, target]
            NodeRef node = createIo(parent, "ph_users_only_" + i, pair[0]);
            // creator is NOT in ph_users -> exercises the "sharedBy = first of ph_users" fallback
            setPermissionHistory(node, List.of(pair[1]), List.of());
            logger.info("ph_users_only #" + i + ": creator=" + pair[0] + " ph_users=[" + pair[1] + "]");
            nodes.add(node);
        }
        return nodes;
    }

    private List<NodeRef> createScenarioPhUsersIncludesCreator(NodeRef parent, int count) {
        List<NodeRef> nodes = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String[] pair = randomDistinctPair(); // [creator, other]
            NodeRef node = createIo(parent, "ph_users_includes_creator_" + i, pair[0]);
            // creator is part of ph_users -> sharedBy stays the creator; the other user can't be
            // attributed to anything and is expected to end up in the "missing users" warning -
            // this mirrors the real ambiguity of the legacy data documented in the migration itself
            setPermissionHistory(node, List.of(pair[0], pair[1]), List.of());
            logger.info("ph_users_includes_creator #" + i + ": creator=" + pair[0] + " ph_users=[" + pair[0] + "," + pair[1] + "]");
            nodes.add(node);
        }
        return nodes;
    }

    private List<NodeRef> createScenarioPhInvitedOnlyOnMap(NodeRef parent, int count) {
        List<NodeRef> nodes = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String[] pair = randomDistinctPair(); // [creator, invited]
            NodeRef node = createMap(parent, "ph_invited_only_map_" + i, pair[0]);
            setPermissionHistory(node, List.of(), List.of(pair[1]));
            logger.info("ph_invited_only_map #" + i + ": creator=" + pair[0] + " ph_invited=[" + pair[1] + "]");
            nodes.add(node);
        }
        return nodes;
    }

    private List<NodeRef> createScenarioLegacyShareOnly(NodeRef parent, int count) {
        List<NodeRef> nodes = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            // the material's creator is also the one who shares it - a material has exactly one creator
            String creator = randomUser();
            NodeRef node = createIo(parent, "legacy_link_share_only_" + i, creator);
            createLegacyLinkShare(node, creator);
            logger.info("legacy_link_share_only #" + i + ": creator=shareCreator=" + creator);
            nodes.add(node);
        }
        return nodes;
    }

    private List<NodeRef> createScenarioShareOverlap(NodeRef parent, int count) {
        List<NodeRef> nodes = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String creator = randomUser();
            NodeRef node = createIo(parent, "legacy_link_share_plus_ph_users_overlap_" + i, creator);
            createLegacyLinkShare(node, creator);
            // ph_users only contains the creator (who is also the share's creator) -> fully
            // accounted for, no warning expected
            setPermissionHistory(node, List.of(creator), List.of());
            logger.info("legacy_link_share_plus_ph_users_overlap #" + i + ": creator=shareCreator=ph_users=" + creator);
            nodes.add(node);
        }
        return nodes;
    }

    private List<NodeRef> createScenarioShareLeftover(NodeRef parent, int count) {
        List<NodeRef> nodes = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String creator = randomUser();
            String leftover = randomOtherUser(creator); // must differ: the leftover user must not
            // be the same person who created and shared the material
            NodeRef node = createIo(parent, "legacy_link_share_plus_ph_users_leftover_" + i, creator);
            createLegacyLinkShare(node, creator);
            // leftover can't be attributed to the share or to sharedBy -> expected to trigger the
            // "ShareInfos ... are not complete" warning in Release_11_0_ShareInfos
            setPermissionHistory(node, List.of(creator, leftover), List.of());
            logger.info("legacy_link_share_plus_ph_users_leftover #" + i + ": creator=shareCreator=" + creator + " leftover=" + leftover);
            nodes.add(node);
        }
        return nodes;
    }

    private List<NodeRef> createScenarioNegativeControl(NodeRef parent, int count) {
        List<NodeRef> nodes = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            // neither ccm:permission_history nor ccm:shares -> must NOT be picked up by the
            // aspect-based collector in NodeRunner/NodeCollectorCmis
            nodes.add(createIo(parent, "negative_control_no_aspect_" + i, randomUser()));
        }
        return nodes;
    }

    // ---- randomization helpers -------------------------------------------------------------------

    /** a random username drawn from {@link #testUsers} */
    private String randomUser() {
        return users.get(random.nextInt(users.size()));
    }

    /**
     * a random username guaranteed to differ from {@code exclude} (as long as at least 2 testUsers
     * are configured; degrades to returning {@code exclude} otherwise - already warned about at startup)
     */
    private String randomOtherUser(String exclude) {
        List<String> pool = new ArrayList<>(users);
        pool.remove(exclude);
        if (pool.isEmpty()) {
            return exclude;
        }
        return pool.get(random.nextInt(pool.size()));
    }

    /**
     * two random usernames guaranteed to differ (as long as at least 2 testUsers are configured) -
     * use this for every sharer/recipient pair, since a user must never end up sharing with themselves
     */
    private String[] randomDistinctPair() {
        String a = randomUser();
        String b = randomOtherUser(a);
        return new String[]{a, b};
    }

    // ---- node/property helpers -------------------------------------------------------------------

    private NodeRef createIo(NodeRef parent, String name, String creator) {
        return createNode(parent, QName.createQName(CCConstants.CCM_TYPE_IO), name, creator);
    }

    private NodeRef createMap(NodeRef parent, String name, String creator) {
        return createNode(parent, QName.createQName(CCConstants.CCM_TYPE_MAP), name, creator);
    }

    /**
     * cm:creator/cm:created are only ever taken from the properties map passed to createNode() -
     * setting them via a later setProperty() call is silently ignored regardless of behaviour
     * filters, so PROP_CREATOR must go into the initial props map (same pattern as
     * HomeFolderTool.createMap)
     */
    private NodeRef createNode(NodeRef parent, QName type, String name, String creator) {
        Map<QName, Serializable> props = new HashMap<>();
        props.put(ContentModel.PROP_NAME, name);
        props.put(ContentModel.PROP_CREATOR, creator);
        ChildAssociationRef assoc = nodeService.createNode(parent, ContentModel.ASSOC_CONTAINS,
                QName.createQName(CCConstants.NAMESPACE_CCM, QName.createValidLocalName(name)), type, props);

        policyBehaviourFilter.disableBehaviour();
        try {
            nodeService.setProperty(assoc.getChildRef(), QName.createQName(CCConstants.CM_PROP_C_CREATOR), creator);
        } finally {
            policyBehaviourFilter.enableBehaviour();
        }
        ownableService.setOwner(assoc.getChildRef(), creator);

        return assoc.getChildRef();
    }

    /**
     * sets ccm:ph_users / ccm:ph_invited (plus ph_modified/ph_action) directly, bypassing every
     * current-day service - this is exactly the pre-migration state, no ShareInfo row exists for it
     */
    private void setPermissionHistory(NodeRef nodeRef, List<String> phUsers, List<String> phInvited) {
        if (!phUsers.isEmpty()) {
            nodeService.setProperty(nodeRef, QName.createQName(CCConstants.CCM_PROP_PH_USERS), new ArrayList<>(phUsers));
        }
        if (!phInvited.isEmpty()) {
            nodeService.setProperty(nodeRef, QName.createQName(CCConstants.CCM_PROP_PH_INVITED), new ArrayList<>(phInvited));
        }
        nodeService.setProperty(nodeRef, QName.createQName(CCConstants.CCM_PROP_PH_MODIFIED), new Date());
        nodeService.setProperty(nodeRef, QName.createQName(CCConstants.CCM_PROP_PH_ACTION), "modify");
    }

    /**
     * creates the legacy ccm:share child node (aspect + child, no ShareInfo row) the same way
     * {@code GlobalShareServiceImpl.createShare} used to, minus the ShareInfo bookkeeping it now
     * also performs - that bookkeeping is exactly what Release_11_0_ShareInfos is supposed to backfill
     */
    private NodeRef createLegacyLinkShare(NodeRef ioNode, String shareCreator) {
        QName sharesAspect = QName.createQName(CCConstants.CCM_ASPECT_SHARES);
        if (!nodeService.hasAspect(ioNode, sharesAspect)) {
            nodeService.addAspect(ioNode, sharesAspect, null);
        }
        Map<QName, Serializable> props = new HashMap<>();
        props.put(QName.createQName(CCConstants.CCM_PROP_SHARE_EXPIRYDATE), -1L);
        props.put(QName.createQName(CCConstants.CCM_PROP_SHARE_MAIL), "LINK");
        props.put(QName.createQName(CCConstants.CCM_PROP_SHARE_TOKEN), GUID.generate());
        props.put(QName.createQName(CCConstants.CCM_PROP_SHARE_DOWNLOAD_COUNTER), 0);
        props.put(ContentModel.PROP_CREATOR, shareCreator);

        ChildAssociationRef assoc = nodeService.createNode(ioNode,
                QName.createQName(CCConstants.CCM_ASSOC_ASSIGNED_SHARES),
                QName.createQName(CCConstants.NAMESPACE_CCM, GUID.generate()),
                QName.createQName(CCConstants.CCM_TYPE_SHARE), props);
        return assoc.getChildRef();
    }

    /**
     * freezes the node's current state (incl. ccm:ph_* / ccm:shares) into version2Store, then
     * changes the live ph_users so the frozen copy ends up holding data the live node no longer has -
     * exactly the case where cleaning up the live node alone would leave version2Store untouched
     */
    private Map<String, String> versionAndDiverge(NodeRef nodeRef) {
        Version version = versionService.createVersion(nodeRef, Map.of(VersionModel.PROP_VERSION_TYPE, VersionType.MAJOR));
        NodeRef frozen = version.getFrozenStateNodeRef();

        nodeService.setProperty(nodeRef, QName.createQName(CCConstants.CCM_PROP_PH_USERS), new ArrayList<>(List.of(randomUser())));

        Map<String, String> mapping = new LinkedHashMap<>();
        mapping.put("liveNodeId", nodeRef.getId());
        mapping.put("frozenVersionNodeId", frozen.getId());
        mapping.put("versionLabel", version.getVersionLabel());
        return mapping;
    }

    private String archive(NodeRef nodeRef) {
        String id = nodeRef.getId();
        nodeService.deleteNode(nodeRef);
        return id;
    }
}
