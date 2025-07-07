package org.edu_sharing.alfresco.service;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.model.Repository;
import org.alfresco.repo.node.db.DbNodeServiceImpl;
import org.alfresco.repo.policy.BehaviourFilter;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.service.cmr.repository.*;
import org.alfresco.service.cmr.security.AccessPermission;
import org.alfresco.service.cmr.security.AuthorityType;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.transaction.TransactionService;
import org.apache.commons.lang3.StringUtils;
import org.edu_sharing.alfresco.tools.EduSharingNodeHelper;
import org.edu_sharing.alfresco.workspace_administration.NodeServiceInterceptor;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.tools.cache.RepositoryCache;

import java.io.Serializable;
import java.util.*;

@Slf4j
@RequiredArgsConstructor
public class OrganisationService {

    private final AuthorityService eduAuthorityService;
    private final NodeService nodeService;
    private final DbNodeServiceImpl dbNodeService;
    private final org.alfresco.service.cmr.security.AuthorityService authorityService;
    private final org.alfresco.service.cmr.security.AuthorityService authorityServiceInsecure;
    private final Repository repositoryHelper;
    private final PermissionService permissionService;
    private final BehaviourFilter policyBehaviourFilter;
    private final TransactionService transactionService;
    private final RepositoryCache repositoryCache;

    public static final String ORGANIZATION_GROUP_FOLDER = "EDU_SHARED";

    public static final String CCM_PROP_EDUGROUP_EDU_HOMEDIR = "{http://www.campuscontent.de/model/1.0}edu_homedir";
    public static final String CCM_PROP_EDUGROUP_EDU_UNIQUENAME = "{http://www.campuscontent.de/model/1.0}edu_uniquename";

    public static final QName ASPECT_EDUGROUP = QName.createQName(CCConstants.CCM_ASPECT_EDUGROUP);
    public static final QName PROP_EDUGROUP_EDU_HOMEDIR = QName.createQName(CCConstants.CCM_PROP_EDUGROUP_EDU_HOMEDIR);


    public static final QName ASPECT_EDUGROUP_FOLDER = QName.createQName(CCConstants.CCM_ASPECT_EDUGROUP_FOLDER);
    public static final QName PROP_EDUGROUP_FOLDER_ORGANISATION = QName.createQName(CCConstants.CCM_PROP_EDUGROUP_FOLDER_ORGANISATION);


    @Getter
    boolean useOrgPrefix = true;

    public String createOrganization(String orgName, String groupDisplayName) throws Exception {
        return createOrganization(orgName, groupDisplayName, null, null);
    }

    public String createOrganization(String orgName, String groupDisplayName, String metadataset, String scope) throws Exception {
        String scopePrefix = scope == null || scope.isEmpty() ? "" : "_" + scope;
        orgName += scopePrefix;
        groupDisplayName += scopePrefix;
        String groupName = eduAuthorityService.createOrUpdateGroup(AuthorityService.ORG_GROUP_PREFIX + orgName, groupDisplayName, null, true);

        String authorityAdminGroup = createOrganizationAdminGroup(groupDisplayName, groupName);

        NodeRef shared = getOrganisationFolderRoot();

        String orgFolderName = !groupDisplayName.trim().isEmpty() ? groupDisplayName : orgName;
        orgFolderName = EduSharingNodeHelper.cleanupCmName(orgFolderName);

        NodeRef orgFolder = createNode(shared, CCConstants.CCM_TYPE_MAP, orgFolderName);
        if (metadataset != null) {
            nodeService.setProperty(orgFolder, QName.createQName(CCConstants.CM_PROP_METADATASET_EDU_METADATASET), metadataset.trim());
        }

        bindEduGroupFolder(groupName, orgFolder);

        permissionService.setPermission(orgFolder, PermissionService.GROUP_PREFIX + groupName, PermissionService.CONSUMER, true);
        permissionService.setPermission(orgFolder, PermissionService.GROUP_PREFIX + authorityAdminGroup, PermissionService.COORDINATOR, true);

        if (StringUtils.isNotBlank(scope)) {
            nodeService.setProperty(authorityService.getAuthorityNodeRef(PermissionService.GROUP_PREFIX + groupName), QName.createQName(CCConstants.CCM_PROP_EDUSCOPE_NAME),
                    CCConstants.CCM_VALUE_SCOPE_SAFE);
            nodeService.setProperty(authorityService.getAuthorityNodeRef(PermissionService.GROUP_PREFIX + authorityAdminGroup), QName.createQName(CCConstants.CCM_PROP_EDUSCOPE_NAME),
                    CCConstants.CCM_VALUE_SCOPE_SAFE);
            nodeService.setProperty(orgFolder, QName.createQName(CCConstants.CCM_PROP_EDUSCOPE_NAME),
                    CCConstants.CCM_VALUE_SCOPE_SAFE);
        }

        return groupName;
    }

    public NodeRef getOrganisationFolderRoot() {
        NodeRef companyHome = repositoryHelper.getCompanyHome();

        NodeRef shared = findNodeByName(companyHome, ORGANIZATION_GROUP_FOLDER);

        if (shared == null) {
            shared = createNode(companyHome, CCConstants.CCM_TYPE_MAP, ORGANIZATION_GROUP_FOLDER);
            permissionService.setInheritParentPermissions(shared, false);
        }
        return shared;
    }

    public String createOrganizationAdminGroup(String orgAuthorityName) {
        return createOrganizationAdminGroup(authorityService.getAuthorityDisplayName(orgAuthorityName),
                orgAuthorityName.replace(PermissionService.GROUP_PREFIX, ""));
    }

    private String createOrganizationAdminGroup(String groupDisplayName, String groupName) {
        String authorityAdmins = eduAuthorityService.createOrUpdateGroup(AuthorityService.ADMINISTRATORS_GROUP, groupDisplayName + AuthorityService.ADMINISTRATORS_GROUP_DISPLAY_POSTFIX, groupName, true);

        addAspect(PermissionService.GROUP_PREFIX + authorityAdmins, CCConstants.CCM_ASPECT_GROUPEXTENSION);

        nodeService.setProperty(authorityService.getAuthorityNodeRef(PermissionService.GROUP_PREFIX + authorityAdmins), QName.createQName(CCConstants.CCM_PROP_GROUPEXTENSION_GROUPTYPE),
                AuthorityService.ADMINISTRATORS_GROUP_TYPE);
        return authorityAdmins;
    }

    public void syncOrganisationFolder(String authorityName) {
        NodeRef authorityNodeRef = authorityService.getAuthorityNodeRef(authorityName);
        if (authorityNodeRef == null) {
            log.error("authority not found " + authorityName);
            return;
        }

        if (!nodeService.hasAspect(authorityNodeRef, QName.createQName(CCConstants.CCM_ASPECT_EDUGROUP))) return;

        NodeRef orgFolder = (NodeRef) nodeService.getProperty(authorityNodeRef, QName.createQName(CCConstants.CCM_PROP_EDUGROUP_EDU_HOMEDIR));
        if (orgFolder == null) return;

        String displayName = (String) nodeService.getProperty(authorityNodeRef, ContentModel.PROP_AUTHORITY_DISPLAY_NAME);
        String folderName = (String) nodeService.getProperty(orgFolder, ContentModel.PROP_NAME);

        if (!displayName.equals(folderName)) {
            log.info("syncing organisation folder name:" + folderName + "with displayName:" + displayName);
            String newFolderName = EduSharingNodeHelper.cleanupCmName(displayName);
            /*
             * use dbnodeservice here to prevent DuplicateChildNodeNameException
             * leads to transaction status = 1. So give client code the opportunity to
             * react on this exception in the same transaction
             */
            dbNodeService.setProperty(orgFolder, ContentModel.PROP_NAME, newFolderName);
            dbNodeService.setProperty(orgFolder, ContentModel.PROP_TITLE, newFolderName);
            repositoryCache.remove(orgFolder.getId());
        }
    }

    /**
     * @param orgName with or without GROUP_ORG PREFIX
     *                <p>
     *                for performance reason it uses insecure authorityServiceInsecure and dbNodeService (30%)
     */
    public Map<QName, Serializable> getOrganisation(String orgName) {

        //prevent a normal group gets switched to an organisation
        if (orgName.startsWith(AuthorityType.GROUP.getPrefixString()) && !hasOrganisationPrefix(orgName)) {
            log.error("orgName {} is not an Organisation", orgName);
            return null;
        }

        String authorityName = getCleanName(orgName);
        authorityName = AuthorityType.GROUP.getPrefixString() + AuthorityService.ORG_GROUP_PREFIX + authorityName;

        NodeRef authorityNodeRef = authorityServiceInsecure.getAuthorityNodeRef(authorityName);
        if (authorityNodeRef == null) return null;

        if (!dbNodeService.hasAspect(authorityNodeRef, ASPECT_EDUGROUP)) {
            log.error("authority: {} missing edugroup aspect", authorityName);
            return null;
        }

        return dbNodeService.getProperties(authorityNodeRef);
    }

    public List<Map<QName, Serializable>> getOrganisations() {
        List<Map<QName, Serializable>> organisations = new ArrayList<>();

        log.info("collecting authorities");
        Set<String> authorities = authorityServiceInsecure.getAllAuthorities(AuthorityType.GROUP);
        log.info("finished collecting authorities: {}", authorities.size());

        log.info("collecting organisations");
        for (String authorityName : authorities) {
            if (hasOrganisationPrefix(authorityName)) {
                Map<QName, Serializable> organisation = getOrganisation(authorityName);
                if (organisation != null) organisations.add(organisation);
            }
        }
        log.info("finished collecting organisations: {}", organisations.size());
        return organisations;
    }

    private boolean hasOrganisationPrefix(String authorityName) {
        return authorityName.startsWith(AuthorityType.GROUP.getPrefixString() + AuthorityService.ORG_GROUP_PREFIX);
    }

    public String getCleanName(String fullOrgName) {
        return fullOrgName.replace(AuthorityType.GROUP.getPrefixString(), "")
                .replace(AuthorityService.ORG_GROUP_PREFIX, "");
    }


    public void syncOrganisationFolderName(boolean execute) {

        for (Map<QName, Serializable> eduGroupProps : getOrganisations()) {
            NodeRef eduGroupFolder = (NodeRef) eduGroupProps.get(QName.createQName(CCM_PROP_EDUGROUP_EDU_HOMEDIR));
            NodeRef organisationNodeRef = new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, (String) eduGroupProps.get(ContentModel.PROP_NODE_UUID));
            String authorityName = (String) eduGroupProps.get(ContentModel.PROP_AUTHORITY_NAME);
            String displayName = (String) nodeService.getProperty(organisationNodeRef, ContentModel.PROP_AUTHORITY_DISPLAY_NAME);
            String folderName = (String) nodeService.getProperty(eduGroupFolder, ContentModel.PROP_NAME);
            if (displayName == null || displayName.trim().isEmpty()) {
                log.error("display name of authority is null or empty " + authorityName);
                continue;
            }

            if (!displayName.equals(folderName)) {
                log.info("syncing organisation folder name: {} with displayName: {}" ,folderName, displayName);
                if (execute) {
                    String newFolderName = EduSharingNodeHelper.cleanupCmName(displayName);
                    this.transactionService.getRetryingTransactionHelper().doInTransaction(() -> {

                        try {
                            policyBehaviourFilter.disableBehaviour(eduGroupFolder);
                            nodeService.setProperty(eduGroupFolder, ContentModel.PROP_NAME, newFolderName);
                            nodeService.setProperty(eduGroupFolder, ContentModel.PROP_TITLE, newFolderName);
                            repositoryCache.remove(eduGroupFolder.getId());
                        } catch (DuplicateChildNodeNameException e) {
                            log.error("duplicate organisation name: \"{}\" for {}. fix by hand", newFolderName, authorityName);
                        } catch (Throwable e) {
                            log.error(e.getMessage(), e);
                        } finally {
                            policyBehaviourFilter.enableBehaviour(eduGroupFolder);
                        }
                        return null;
                    });
                }
            }
        }
    }

    private void addAspect(String authority, String aspect) {
        nodeService.addAspect(authorityService.getAuthorityNodeRef(authority), QName.createQName(aspect), new HashMap<>());
    }

    public void bindEduGroupFolder(String groupName, NodeRef folder) throws Exception {

        NodeRef authorityNodeRef = authorityService.getAuthorityNodeRef(PermissionService.GROUP_PREFIX + groupName);

        if (authorityNodeRef == null) {
            throw new Exception("Group does not exist");
        }

        if (!nodeService.exists(folder)) {
            throw new Exception("Folder does not exist");
        }

        Map<QName, Serializable> params = new HashMap<>();
        params.put(QName.createQName(CCM_PROP_EDUGROUP_EDU_HOMEDIR), folder);
        params.put(QName.createQName(CCM_PROP_EDUGROUP_EDU_UNIQUENAME), groupName);

        nodeService.addAspect(authorityNodeRef, ASPECT_EDUGROUP, params);

        bindEduGroupToFolder(PermissionService.GROUP_PREFIX + groupName, folder);
    }

    public void bindEduGroupToFolder(String authorityName, NodeRef folder) throws Exception {
        if (!hasOrganisationPrefix(authorityName)) {
            throw new Exception("authorityName " + authorityName + " is no organisation");
        }

        if (folder == null) {
            log.error("no homefolder provided");
            return;
        }

        if (!nodeService.exists(folder)) {
            log.error("folder does not exist");
            return;
        }

        if (!nodeService.hasAspect(folder, ASPECT_EDUGROUP_FOLDER)) {
            NodeRef authorityNodeRef = authorityService.getAuthorityNodeRef(authorityName);
            if (authorityNodeRef != null) {
                log.info("adding aspect " + OrganisationService.ASPECT_EDUGROUP_FOLDER + " to the organisation folder of organisation " + authorityName);
                Map<QName, Serializable> aspectProps = new HashMap<>();
                aspectProps.put(OrganisationService.PROP_EDUGROUP_FOLDER_ORGANISATION, authorityNodeRef);
                nodeService.addAspect(folder, OrganisationService.ASPECT_EDUGROUP_FOLDER, aspectProps);
            }
        } else {
            log.warn("folder " + folder + " already has aspect " + ASPECT_EDUGROUP_FOLDER);
        }
    }

    private NodeRef createNode(NodeRef parent, String type, String name) {
        Map<QName, Serializable> propsOrgFolder = new HashMap<>();
        propsOrgFolder.put(ContentModel.PROP_NAME, name);
        String assocName = "{" + CCConstants.NAMESPACE_CCM + "}" + name;
        return nodeService.createNode(parent, ContentModel.ASSOC_CONTAINS, QName.createQName(assocName), QName.createQName(type), propsOrgFolder).getChildRef();
    }

    private NodeRef findNodeByName(NodeRef parent, String name) {
        /*
         * getChildAssocsByPropertyValue does not allow search of system
         * maintained properties List<ChildAssociationRef> children =
         * nodeService.getChildAssocsByPropertyValue( new
         * NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE,parentId),
         * QName.createQName(CCConstants.CM_NAME), name);
         */
        List<ChildAssociationRef> children = nodeService.getChildAssocs(parent);
        for (ChildAssociationRef child : children) {
            String childName = (String) nodeService.getProperty(child.getChildRef(), QName.createQName(CCConstants.CM_NAME));
            if (childName.equals(name))
                return child.getChildRef();
        }
        return null;
    }


    /**
     * @return Organisation admin Group
     */
    public String getOrganisationAdminGroup(String organisationName) {
        String authorityName = getAuthorityName(organisationName);
        log.debug("organisationName:{}authorityName:{}", organisationName, authorityName);
        NodeRef eduGroupNodeRef = authorityService.getAuthorityNodeRef(authorityName);
        List<ChildAssociationRef> childGroups = nodeService.getChildAssocs(eduGroupNodeRef);
        for (ChildAssociationRef childGroup : childGroups) {
            String grouptype = (String) nodeService.getProperty(childGroup.getChildRef(), QName.createQName(CCConstants.CCM_PROP_GROUPEXTENSION_GROUPTYPE));
            if (CCConstants.ADMINISTRATORS_GROUP_TYPE.equals(grouptype)) {
                return (String) nodeService.getProperty(childGroup.getChildRef(), QName.createQName(CCConstants.CM_PROP_AUTHORITY_AUTHORITYNAME));
            }
        }

        return null;
    }

    /**
     * runs over organisation homefolder recursively and
     * adds ORG_ADMIN Group as Coordinator if not already set
     *
     */
    public void setOrgAdminPermissions(String organisationName, boolean execute) {
        log.debug("inviting orgadmin group as coordinator for org:" + organisationName);
        String authorityName = getAuthorityName(organisationName);
        NodeRef orgNodeRef = authorityService.getAuthorityNodeRef(authorityName);
        NodeRef eduGroupHomeDir = (NodeRef) nodeService.getProperty(orgNodeRef, QName.createQName(CCConstants.CCM_PROP_EDUGROUP_EDU_HOMEDIR));
        if (eduGroupHomeDir == null) {
            log.debug(organisationName + " is no organisation");
            return;
        }

        String adminGroup = getOrganisationAdminGroup(organisationName);
        if (adminGroup == null) {
            log.error("could not find admin group for organisationName:" + organisationName);
            return;
        }
        setOrgAdminPermissions(eduGroupHomeDir, adminGroup, execute);

    }

    private void setOrgAdminPermissions(NodeRef parent, String adminAuthority, boolean execute) {
        List<ChildAssociationRef> childAssocs = nodeService.getChildAssocs(parent);
        for (ChildAssociationRef childRef : childAssocs) {

            NodeRef nodeRef = childRef.getChildRef();
            setOrgAdminPermissionsOnNode(adminAuthority, execute, nodeRef);

            if (nodeService.getType(childRef.getChildRef()).equals(QName.createQName(CCConstants.CCM_TYPE_MAP))) {
                setOrgAdminPermissions(childRef.getChildRef(), adminAuthority, execute);
            }
        }
    }

    public void setOrgAdminPermissionsOnNode(String adminAuthority, boolean execute, NodeRef nodeRef) {
        Set<AccessPermission> allSetPerms = permissionService.getAllSetPermissions(nodeRef);

        boolean isAlreadySet = false;
        for (AccessPermission perm : allSetPerms) {
            if (perm.getAuthority().equals(adminAuthority)
                    && perm.getPermission().equals(PermissionService.COORDINATOR)
                    && !perm.isInherited()) {
                isAlreadySet = true;
            }
        }
        if (!isAlreadySet) {
            log.debug("will set org admingroup as Coordnator for:" +
                    nodeRef + " " +
                    nodeService.getProperty(nodeRef, ContentModel.PROP_NAME));
            if (execute) {
                this.transactionService.getRetryingTransactionHelper().doInTransaction(() -> {
                    try {
                        policyBehaviourFilter.disableBehaviour(nodeRef);
                        permissionService.setPermission(nodeRef, adminAuthority, PermissionService.COORDINATOR, true);
                    } finally {
                        policyBehaviourFilter.enableBehaviour(nodeRef);
                    }
                    return null;
                });
            }
        }
    }

    /**
     * GROUP_ and ORG_ Prefixes are added if they ar not present
     *
     */
    public String getAuthorityName(String organisationName) {
        organisationName = organisationName.replaceFirst(PermissionService.GROUP_PREFIX, "");
        organisationName = organisationName.replaceFirst(AuthorityService.ORG_GROUP_PREFIX, "");

        if (this.isUseOrgPrefix()) {
            return PermissionService.GROUP_PREFIX + AuthorityService.ORG_GROUP_PREFIX + organisationName;
        } else {
            return PermissionService.GROUP_PREFIX + organisationName;
        }
    }

    public List<String> getMyOrganisations(boolean scoped) {
        Set<String> authorities = authorityService.getContainingAuthorities(AuthorityType.GROUP, AuthenticationUtil.getFullyAuthenticatedUser(), true);
        List<String> organisations = new ArrayList<>();
        for (String authority : authorities) {
            NodeRef nodeRefAuthority = authorityService.getAuthorityNodeRef(authority);
            if (nodeService.hasAspect(nodeRefAuthority, QName.createQName(CCConstants.CCM_ASPECT_EDUGROUP))) {

                String eduGroupScope = (String) nodeService.getProperty(nodeRefAuthority, QName.createQName(CCConstants.CCM_PROP_EDUSCOPE_NAME));

                boolean add = authorities.contains(CCConstants.AUTHORITY_GROUP_ALFRESCO_ADMINISTRATORS)
                        || authorities.contains(authority);

                if (scoped) {
                    String currentScope = NodeServiceInterceptor.getEduSharingScope();
                    if (eduGroupScope == null && currentScope != null) {
                        add = false;
                    }
                    if (eduGroupScope != null && !eduGroupScope.equals(currentScope)) {
                        add = false;
                    }

                }

                if (add) {
                    organisations.add(authority);
                }
            }
        }
        return organisations;
    }

    public void unbindEduGroupFolder(String groupName, String folderId) throws Exception {

        transactionService.getRetryingTransactionHelper().doInTransaction(

                (RetryingTransactionHelper.RetryingTransactionCallback<Void>) () -> {
                    if (authorityService.isAdminAuthority(AuthenticationUtil.getRunAsUser())) {

                        NodeRef authorityNodeRef = authorityService.getAuthorityNodeRef(PermissionService.GROUP_PREFIX + groupName);

                        if (authorityNodeRef == null) {
                            return null;
                        }

                        NodeRef folderNodeRef = new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, folderId);

                        if (!nodeService.exists(folderNodeRef)) {
                            return null;
                        }

                        if (!nodeService.getType(authorityNodeRef).equals(QName.createQName(CCConstants.CM_TYPE_AUTHORITY_CONTAINER))) {
                            throw new Exception(authorityNodeRef + " is no Group");
                        }

                        // remove aspect from group
                        nodeService.removeAspect(authorityNodeRef, OrganisationService.ASPECT_EDUGROUP);

                        //remove Aspect from folder
                        nodeService.removeAspect(folderNodeRef, OrganisationService.ASPECT_EDUGROUP_FOLDER);
                    }

                    return null;
                }, false);
    }
}
