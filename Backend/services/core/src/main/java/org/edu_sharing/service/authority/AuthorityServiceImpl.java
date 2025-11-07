package org.edu_sharing.service.authority;

import jakarta.transaction.UserTransaction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.alfresco.model.ContentModel;
import org.alfresco.query.PagingRequest;
import org.alfresco.query.PagingResults;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.repo.security.authority.AuthorityInfo;
import org.alfresco.repo.security.permissions.AccessDeniedException;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.rest.framework.core.exceptions.InvalidArgumentException;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.security.AuthorityType;
import org.alfresco.service.cmr.security.OwnableService;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.transaction.TransactionService;
import org.apache.tika.utils.StringUtils;
import org.edu_sharing.alfresco.lightbend.LightbendConfigLoader;
import org.edu_sharing.alfresco.service.guest.GuestService;
import org.edu_sharing.alfresco.workspace_administration.NodeServiceInterceptor;
import org.edu_sharing.repository.client.rpc.EduGroup;
import org.edu_sharing.repository.client.rpc.User;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.MCAlfrescoAPIClient;
import org.edu_sharing.repository.server.PropertyRequiredException;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.repository.server.tools.KeyTool;
import org.edu_sharing.repository.server.tools.cache.UserCache;
import org.edu_sharing.repository.server.tools.transaction.RetryingTransaction;
import org.edu_sharing.service.InsufficientPermissionException;
import org.edu_sharing.service.NotAnAdminException;
import org.edu_sharing.service.authentication.totp.OneTimeTokenService;
import org.edu_sharing.service.nodeservice.NodeServiceHelper;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;


@Slf4j
@Primary
@Service
@RequiredArgsConstructor
public class AuthorityServiceImpl implements AuthorityService {

    private final UserCache userCache;
    private final org.alfresco.service.cmr.security.AuthorityService authorityService;
    private final NodeService nodeService;
    private final OwnableService ownableService;
    private final PermissionService permissionService;
    //	@Qualifier("eduAuthorityService")
    private final org.edu_sharing.alfresco.service.AuthorityService eduAuthorityService;
    private final GuestService guestService;
    private final Optional<CustomAuthorityAttributesMapping> customAuthorityAttributesMapping;
    private final OneTimeTokenService oneTimeTokenService;
    private final TransactionService transactionService;
    private final RetryingTransactionHelper retryingTransactionHelper;
    private final LightbendConfigLoader lightbendConfigLoader;
    private final PersonService personService;
    private final org.alfresco.service.cmr.security.MutableAuthenticationService authenticationService;
    private final org.alfresco.repo.security.authentication.RepositoryAuthenticationDao authenticationDao;

    /**
     * Returns a property for a certain authority (it will fetch the coressponding node and load the property)
     *
     * @param authority
     * @param property
     * @return
     */
    @Override
    public Object getAuthorityProperty(String authority, String property) {
        NodeRef ref = authorityService.getAuthorityNodeRef(authority);
        if (ref == null)
            return null;
        return nodeService.getProperty(ref, QName.createQName(property));
    }

    /**
     * returns the node id for the given authority (useful if you want to change metadata)
     *
     * @param authority
     * @return
     */
    @Override
    public NodeRef getAuthorityNodeRef(String authority) {
        return authorityService.getAuthorityNodeRef(authority);
    }

    @Override
    public void setAuthorityProperty(String authority, String property, Serializable value) {
        if (value == null) {
            nodeService.removeProperty(authorityService.getAuthorityNodeRef(authority), QName.createQName(property));
        } else {
            nodeService.setProperty(authorityService.getAuthorityNodeRef(authority), QName.createQName(property), value);
        }
    }

    @Override
    public void addAuthorityAspect(String authority, String aspect) {

        NodeRef nodeRef = authorityService.getAuthorityNodeRef(authority);
        if (!nodeService.hasAspect(nodeRef, QName.createQName(aspect))) {
            nodeService.addAspect(nodeRef, QName.createQName(aspect), new HashMap<>());
        }
    }

    @Override
    public void deleteAuthority(String authorityName) {
        retryingTransactionHelper.doInTransaction(() -> {
            String key = authorityName;
            String groupType = (String) getAuthorityProperty(key, CCConstants.CCM_PROP_GROUPEXTENSION_GROUPTYPE);
            if (authorityName.equals(CCConstants.AUTHORITY_GROUP_ALFRESCO_ADMINISTRATORS)) {
                throw new AccessDeniedException("Not allowed to delete group " + CCConstants.AUTHORITY_GROUP_ALFRESCO_ADMINISTRATORS);
            }
            if (groupType != null && groupType.equals(CCConstants.ADMINISTRATORS_GROUP_TYPE) && !new MCAlfrescoAPIClient().isAdmin(AuthenticationUtil.getFullyAuthenticatedUser()))
                throw new AccessDeniedException("An admin group can not be deleted");
            authorityService.deleteAuthority(key, true);

            return null;
        }, false);
    }

    @Override
    public synchronized boolean hasModifyAccessToGroup(String groupName) {
        Set<String> memberships = authorityService.getAuthorities();
        if (AuthorityServiceHelper.isAdmin()) {
            return true;
        }
        if (!lightbendConfigLoader.getConfig().getBoolean("repository.organizations.admins.canManage")) {
            return false;
        }
        // Detect the group prefix and decide
        String[] split = groupName.split("_");
        if (split.length < 2)
            return false;
        String adminGroup = PermissionService.GROUP_PREFIX + split[0] + "_" + org.edu_sharing.alfresco.service.AuthorityService.ADMINISTRATORS_GROUP;
        if (memberships.contains(adminGroup)) {
            return isAdminGroup(adminGroup);
        }
        return false;
    }

    private boolean isAdminGroup(String group) {
        return groupIsOfType(group, org.edu_sharing.alfresco.service.AuthorityService.ADMINISTRATORS_GROUP_TYPE) ||
                groupIsOfType(group, org.edu_sharing.alfresco.service.AuthorityService.MEDIACENTER_ADMINISTRATORS_GROUP_TYPE);
    }

    private boolean groupIsOfType(String adminGroup, String type) {
        String typeProp = (String) nodeService.getProperty(authorityService.getAuthorityNodeRef(adminGroup), QName.createQName(CCConstants.CCM_PROP_GROUPEXTENSION_GROUPTYPE));
        return typeProp.equals(type);
    }

    @Override
    public boolean isGuest() {
        try {
            return guestService.isGuestUser(AuthenticationUtil.getFullyAuthenticatedUser());
        } catch (Throwable ignored) {
        }
        return false;
    }

    private synchronized boolean hasAdminAccessToGroup(String groupName, String postfix) {
        try {
            // strip the group prefix, if present
            if (groupName.startsWith(PermissionService.GROUP_PREFIX)) {
                groupName = groupName.substring(PermissionService.GROUP_PREFIX.length());
            }
            if (AuthenticationUtil.isRunAsUserTheSystemUser()) {
                return true;
            }
            Set<String> admins = authorityService.getContainedAuthorities(AuthorityType.USER, CCConstants.AUTHORITY_GROUP_ALFRESCO_ADMINISTRATORS, false);
            if (admins.contains(AuthenticationUtil.getRunAsUser())) {
                return true;
            }
            Set<String> memberships = authorityService.getAuthorities();
            String group = PermissionService.GROUP_PREFIX + AuthorityService.getGroupName(postfix, groupName);
            if (memberships.contains(group))
                return isAdminGroup(group);

            // Detect the group prefix and decide
            String[] split = groupName.split("_");
            if (split.length < 2)
                return false;
            group = PermissionService.GROUP_PREFIX + split[0] + "_" + postfix;
            if (memberships.contains(group))
                return isAdminGroup(group);

        } catch (Throwable t) {
            t.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean hasAdminAccessToGroup(String groupName) {
        if (AuthenticationUtil.isRunAsUserTheSystemUser() || AuthorityServiceHelper.isAdmin()) {
            return true;
        }
        return
                lightbendConfigLoader.getConfig().getBoolean("repository.organizations.admins.canManage") &&
                        hasAdminAccessToGroup(groupName, org.edu_sharing.alfresco.service.AuthorityService.ADMINISTRATORS_GROUP);
    }

    @Override
    public boolean hasAdminAccessToMediacenter(String groupName) {
        return hasAdminAccessToGroup(groupName, org.edu_sharing.alfresco.service.AuthorityService.MEDIACENTER_ADMINISTRATORS_GROUP);
    }

    public String getProperty(String authorityName, String property) {
        return (String) nodeService.getProperty(authorityService.getAuthorityNodeRef(authorityName), QName.createQName(property));
    }

    @Override
    public synchronized boolean hasAdminAccessToOrganization(String orgName) {
        try {
            Set<String> memberships = authorityService.getAuthorities();
            if (AuthorityServiceHelper.isAdmin()) {
                return true;
            }
            if (!lightbendConfigLoader.getConfig().getBoolean("repository.organizations.admins.canManage")) {
                return false;
            }


            String group = PermissionService.GROUP_PREFIX + AuthorityService.getGroupName(org.edu_sharing.alfresco.service.AuthorityService.ADMINISTRATORS_GROUP, orgName);
            if (memberships.contains(group))
                return groupIsOfType(group, CCConstants.ADMINISTRATORS_GROUP_TYPE);

        } catch (Throwable t) {
            log.error("Error while getting Admin access:" + orgName, t);
        }
        return false;
    }

    @Override
    public boolean isGlobalAdmin() {
        try {
            String user = AuthenticationUtil.getFullyAuthenticatedUser();
            if ("admin".equals(user)) {
                return true;
            }
            if (AuthenticationUtil.isRunAsUserTheSystemUser()) {
                return true;
            }
            return getMemberships(user).contains(CCConstants.AUTHORITY_GROUP_ALFRESCO_ADMINISTRATORS);
        } catch (Throwable t) {
            return false;
        }
    }

    @Override
    public ArrayList<EduGroup> getAllEduGroups(String authority) {
        Set<String> authoritiesForUser = authorityService.getAuthoritiesForUser(authority);
        ArrayList<EduGroup> result = new ArrayList<>();

        for (String a : authoritiesForUser) {
            EduGroup eg = getEduGroup(a);
            if (eg != null) result.add(eg);
        }

        return result;
    }

    @Override
    public EduGroup getEduGroup(String authority) {
        if (!authority.startsWith(PermissionService.GROUP_PREFIX)) {
            authority = PermissionService.GROUP_PREFIX + authority;
        }
        NodeRef nodeRef = authorityService.getAuthorityNodeRef(authority);
        if (nodeRef == null) {
            return null;
        }

        NodeRef nodeRefEduGroupHomeDir = (NodeRef) nodeService.getProperty(nodeRef,
                QName.createQName(CCConstants.CCM_PROP_EDUGROUP_EDU_HOMEDIR));
        if (nodeRefEduGroupHomeDir != null) {

            Map<QName, Serializable> folderProps = nodeService.getProperties(nodeRefEduGroupHomeDir);
            EduGroup eduGroup = new EduGroup();
            eduGroup.setFolderId((String) folderProps.get(QName.createQName(CCConstants.SYS_PROP_NODE_UID)));
            eduGroup.setFolderName((String) folderProps.get(QName.createQName(CCConstants.CM_NAME)));

            Map<QName, Serializable> groupProps = nodeService.getProperties(nodeRef);

            eduGroup.setGroupId((String) groupProps.get(QName.createQName(CCConstants.SYS_PROP_NODE_UID)));
            eduGroup.setGroupname(
                    (String) groupProps.get(QName.createQName(CCConstants.CM_PROP_AUTHORITY_AUTHORITYNAME)));
            eduGroup.setGroupDisplayName(
                    (String) groupProps.get(QName.createQName(CCConstants.CM_PROP_AUTHORITY_AUTHORITYDISPLAYNAME)));
            eduGroup.setScope((String) groupProps.get(QName.createQName(CCConstants.CCM_PROP_EDUSCOPE_NAME)));

            return eduGroup;
        }

        return null;
    }

    @Override
    public ArrayList<EduGroup> getEduGroups() {
        String currentScope = NodeServiceInterceptor.getEduSharingScope();
        return getEduGroups(currentScope);
    }

    @Override
    public ArrayList<EduGroup> getEduGroups(String authority, String scope) {
        ArrayList<EduGroup> result = new ArrayList<>();

        for (EduGroup eduGroup : getAllEduGroups(authority)) {
            if ((eduGroup.getScope() == null && scope == null)
                    || (eduGroup.getScope() != null && eduGroup.getScope().equals(scope))) {
                result.add(eduGroup);
            }
        }
        return result;
    }

    @Override
    public Set<String> getMemberships(String username) {
        if (!AuthenticationUtil.isRunAsUserTheSystemUser() && AuthenticationUtil.getFullyAuthenticatedUser() != null && AuthenticationUtil.getFullyAuthenticatedUser().equals(username)) {
            return authorityService.getAuthorities();
        } else {
            return authorityService.getAuthoritiesForUser(username);
        }
    }


    @Override
    public String createGroup(String groupName, String displayName, String parentGroup) throws Exception {
        if (parentGroup != null && parentGroup.isEmpty())
            parentGroup = null;

        if (parentGroup == null) {
            if (!isGlobalAdmin())
                throw new AccessDeniedException("No permission to create global group");
        } else if (!hasAdminAccessToGroup(parentGroup)) {
            throw new AccessDeniedException("No permission to create group in " + parentGroup);
        }
        final String parentGroupFinal = parentGroup;
        return AuthenticationUtil.runAsSystem(new RunAsWork<String>() {

            @Override
            public String doWork() throws Exception {
                return createGroupInternal(groupName, displayName, parentGroupFinal);
            }
        });
    }

    private String getGroupNodeId(String groupName) {

        return transactionService.getRetryingTransactionHelper().doInTransaction(
                () -> {
                    String key = groupName.startsWith(PermissionService.GROUP_PREFIX) ? groupName : PermissionService.GROUP_PREFIX + groupName;

                    return authorityService.authorityExists(key)
                            ? authorityService.getAuthorityNodeRef(key).getId()
                            : null;
                }, true);

    }

    private String createGroupInternal(String groupName, String displayName, String parentGroup) {
        if (parentGroup != null) {
            if (getGroupNodeId(parentGroup) == null) {
                throw new IllegalArgumentException("parent group " + parentGroup + " does not exists");
            }
        }
        String name = org.edu_sharing.alfresco.service.AuthorityService.getGroupName(groupName, parentGroup);
        String key = PermissionService.GROUP_PREFIX + name;
        if (authorityService.authorityExists(key)) {
            throw new DuplicateKeyException(key);
        }
        return transactionService.getRetryingTransactionHelper().doInTransaction(
                () -> {
                    authorityService.createAuthority(AuthorityType.GROUP, name, displayName, authorityService.getDefaultZones());
                    if (parentGroup != null) {
                        addMemberships(parentGroup, List.of(key));
                    }
                    return name;
                }
                , false);
    }

    @Override
    public void createGroupWithType(String groupName, String displayName, String parentGroup, String groupType) throws Exception {
        String group = createGroup(groupName, displayName, parentGroup);
        addAuthorityAspect(PermissionService.GROUP_PREFIX + group, CCConstants.CCM_ASPECT_GROUPEXTENSION);
        setAuthorityProperty(PermissionService.GROUP_PREFIX + group, CCConstants.CCM_PROP_GROUPEXTENSION_GROUPTYPE, groupType);

    }

    public EduGroup getOrCreateEduGroup(EduGroup eduGroup, EduGroup unscopedEduGroup, String folderParentId) {

        AuthenticationUtil.RunAsWork<EduGroup> createEduGroupWorker = new AuthenticationUtil.RunAsWork<EduGroup>() {
            @Override
            public EduGroup doWork() throws Exception {
                ReentrantLock lock = new ReentrantLock();
                lock.lock();
                UserTransaction transaction = transactionService.getNonPropagatingUserTransaction(false);
                if (!authorityService.getAllAuthorities(AuthorityType.GROUP).contains(eduGroup.getGroupname())) {
                    try {
                        transaction.begin();

                        String authority = authorityService.createAuthority(AuthorityType.GROUP,
                                eduGroup.getGroupname().replaceAll("GROUP_", ""));
                        NodeRef nodeRef = authorityService.getAuthorityNodeRef(authority);
                        nodeService.setProperty(nodeRef,
                                QName.createQName(CCConstants.CM_PROP_AUTHORITY_AUTHORITYDISPLAYNAME),
                                eduGroup.getGroupDisplayName());

                        // scope
                        Map<QName, Serializable> propsAspectEduScope = new HashMap<>();
                        propsAspectEduScope.put(QName.createQName(CCConstants.CCM_PROP_EDUSCOPE_NAME),
                                eduGroup.getScope());
                        nodeService.addAspect(nodeRef, QName.createQName(CCConstants.CCM_ASPECT_EDUSCOPE),
                                propsAspectEduScope);

                        String eduGroupHomeFolderId = eduGroup.getFolderId();
                        if (eduGroupHomeFolderId == null) {
                            String folderName = eduGroup.getFolderName();
                            if (folderName == null) {
                                folderName = eduGroup.getGroupname().replace(PermissionService.GROUP_PREFIX, "");
                                if (eduGroup.getScope() != null) {
                                    folderName = folderName + "_" + eduGroup.getScope();
                                }
                            }
                            folderName = NodeServiceHelper.cleanupCmName(folderName);
                            Map<QName, Serializable> folderProps = new HashMap<>();
                            folderProps.put(ContentModel.PROP_NAME, folderName);

                            String assocName = "{" + CCConstants.NAMESPACE_CCM + "}" + folderName;
                            ChildAssociationRef newNode = nodeService.createNode(new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, folderParentId),
                                    ContentModel.ASSOC_CONTAINS, QName.createQName(assocName),
                                    QName.createQName(CCConstants.CCM_TYPE_MAP), folderProps);

                            nodeService.addAspect(newNode.getChildRef(), QName.createQName(CCConstants.CCM_ASPECT_EDUSCOPE),
                                    propsAspectEduScope);
                            ownableService.setOwner(newNode.getChildRef(), AuthenticationUtil.getRunAsUser());
                            permissionService.setPermission(newNode.getChildRef(), eduGroup.getGroupname(), PermissionService.READ, true);
                            permissionService.setInheritParentPermissions(newNode.getChildRef(), false);
                            eduGroupHomeFolderId = newNode.getChildRef().getId();
                        }

                        // edugroup aspect
                        Map<QName, Serializable> propsAspectEduGroup = new HashMap<>();
                        propsAspectEduGroup.put(QName.createQName(CCConstants.CCM_PROP_EDUGROUP_EDU_HOMEDIR),
                                new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, eduGroupHomeFolderId));
                        nodeService.addAspect(nodeRef, QName.createQName(CCConstants.CCM_ASPECT_EDUGROUP),
                                propsAspectEduGroup);


                        //copy ORG_ADMINISTRATORS
                        if (unscopedEduGroup != null && eduGroup.getScope() != null) {
                            Set<String> containedAuthorities = authorityService.getContainedAuthorities(AuthorityType.GROUP, unscopedEduGroup.getGroupname(), true);
                            for (String containedAuthority : containedAuthorities) {
                                NodeRef containedAuthorityNodeRef = authorityService.getAuthorityNodeRef(containedAuthority);
                                String groupType = (String) nodeService.getProperty(containedAuthorityNodeRef, QName.createQName(CCConstants.CCM_PROP_GROUPEXTENSION_GROUPTYPE));
                                if (CCConstants.ADMINISTRATORS_GROUP_TYPE.equals(groupType)) {
                                    String groupname = (String) nodeService.getProperty(containedAuthorityNodeRef, QName.createQName(CCConstants.CM_PROP_AUTHORITY_AUTHORITYNAME));
                                    groupname = groupname.replace(AuthorityType.GROUP.getPrefixString(), "");
                                    String groupDisplayName = (String) nodeService.getProperty(containedAuthorityNodeRef, QName.createQName(CCConstants.CM_PROP_AUTHORITY_AUTHORITYDISPLAYNAME));
                                    String groupAdministrators = authorityService.createAuthority(AuthorityType.GROUP, groupname + "_" + eduGroup.getScope(), groupDisplayName + "_" + eduGroup.getScope(), authorityService.getDefaultZones());
                                    NodeRef groupAdministratorsNodeRef = authorityService.getAuthorityNodeRef(groupAdministrators);
                                    nodeService.setProperty(groupAdministratorsNodeRef, QName.createQName(CCConstants.CCM_PROP_GROUPEXTENSION_GROUPTYPE), CCConstants.ADMINISTRATORS_GROUP_TYPE);
                                    //scope
                                    nodeService.addAspect(groupAdministratorsNodeRef, QName.createQName(CCConstants.CCM_ASPECT_EDUSCOPE), propsAspectEduScope);

                                    authorityService.addAuthority(eduGroup.getGroupname(), groupAdministrators);
                                    permissionService.setPermission(new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, eduGroupHomeFolderId), groupAdministrators, CCConstants.PERMISSION_ES_CHILD_MANAGER, true);
                                }
                            }
                        }


                    } catch (Throwable e) {
                        log.error(e.getMessage(), e);
                        try {
                            transaction.rollback();
                        } catch (Exception e2) {
                            log.error(e.getMessage(), e2);
                        }
                    }
                    transaction.commit();
                }

                lock.unlock();

                return getEduGroup(eduGroup.getGroupname());
            }
        };

        return AuthenticationUtil.runAs(createEduGroupWorker, ApplicationInfoList.getHomeRepository().getUsername());

    }

    @Override
    public boolean authorityExists(String authority) {
        return authorityService.authorityExists(authority);
    }

    /**
     * returns null when user not exists
     */
    @Override
    public Map<String, Serializable> getUserInfo(String userName) throws Exception {
        User user = getUser(userName);
        if (user == null) return null;
        return user.getProperties();
    }

    @Override
    public User getUser(String userName) {
        return userCache.getUser(userName);
    }

    @Override
    public void createOrUpdateUser(Map<String, Serializable> userInfo) throws Exception {

        String currentUser = AuthenticationUtil.getRunAsUser();

        if (userInfo == null) {
            throw new PropertyRequiredException(CCConstants.CM_PROP_PERSON_USERNAME);
        }

        String userName = (String) userInfo.get(CCConstants.CM_PROP_PERSON_USERNAME);
        String firstName = (String) userInfo.get(CCConstants.CM_PROP_PERSON_FIRSTNAME);
        String lastName = (String) userInfo.get(CCConstants.CM_PROP_PERSON_LASTNAME);
        String email = (String) userInfo.get(CCConstants.CM_PROP_PERSON_EMAIL);
        NodeRef currentUserRef = personService.getPersonOrNull(userName);
        if (StringUtils.isBlank(userName)) {
            throw new PropertyRequiredException(CCConstants.CM_PROP_PERSON_USERNAME);
        }

        if (StringUtils.isBlank(firstName) &&
                (
                        currentUserRef == null || !StringUtils.isBlank((String) NodeServiceHelper.getPropertyNative(currentUserRef, CCConstants.CM_PROP_PERSON_FIRSTNAME))
                )
        ) {
            throw new PropertyRequiredException(CCConstants.CM_PROP_PERSON_FIRSTNAME);
        }

        if (StringUtils.isBlank(lastName) &&
                (
                        currentUserRef == null || !StringUtils.isBlank((String) NodeServiceHelper.getPropertyNative(currentUserRef, CCConstants.CM_PROP_PERSON_LASTNAME))
                )) {
            throw new PropertyRequiredException(CCConstants.CM_PROP_PERSON_LASTNAME);
        }

        if (StringUtils.isBlank(email) &&
                (
                        currentUserRef == null || !StringUtils.isBlank((String) NodeServiceHelper.getPropertyNative(currentUserRef, CCConstants.CM_PROP_PERSON_EMAIL))
                )) {
            throw new PropertyRequiredException(CCConstants.CM_PROP_PERSON_EMAIL);
        }

        if (!currentUser.equals(userName) && !AuthorityServiceFactory.getInstance().getLocalService().isGlobalAdmin()) {
            throw new NotAnAdminException();
        }
        if (!lightbendConfigLoader.getConfig().getIsNull("repository.personActiveStatus")) {
            String personActiveStatus = lightbendConfigLoader.getConfig().getString("repository.personActiveStatus");
            //if configured initialize with active status
            if (personActiveStatus != null) {
                userInfo.put(CCConstants.CM_PROP_PERSON_ESPERSONSTATUS, personActiveStatus);
            }
        }

        if (!lightbendConfigLoader.getConfig().getIsNull("repository.personActiveStatus")) {
            String personActiveStatus = lightbendConfigLoader.getConfig().getString("repository.personActiveStatus");
            //if configured initialize with active status
            if (personActiveStatus != null && !personActiveStatus.trim().isEmpty()) {
                userInfo.put(CCConstants.CM_PROP_PERSON_ESPERSONSTATUS, personActiveStatus);
            }
        }

        retryingTransactionHelper.doInTransaction((RetryingTransactionCallback<Void>) () -> {
                    Throwable runAs = AuthenticationUtil.runAs(() -> {
                                try {
                                    if (personService.personExists(userName)) {
                                        personService.setPersonProperties(userName, transformQName(userInfo));
                                    } else {
                                        personService.createPerson(transformQName(userInfo));
                                    }
                                    addUserExtensionAspect(userName);
                                    userCache.refresh(userName);

                                } catch (Throwable e) {
                                    log.error(e.getMessage(), e);
                                    return e;
                                }

                                return null;
                            },
                            ApplicationInfoList.getHomeRepository().getUsername());

                    if (runAs != null) {
                        throw runAs;
                    }

                    return null;
                },
                false);
    }

    private void addUserExtensionAspect(String userName) {
        if (!nodeService.hasAspect(personService.getPerson(userName), QName.createQName(CCConstants.CCM_ASPECT_USER_EXTENSION)))
            nodeService.addAspect(personService.getPerson(userName), QName.createQName(CCConstants.CCM_ASPECT_USER_EXTENSION), null);
    }

    private Map<QName, Serializable> transformQName(Map<String, Serializable> data) {
        Map<QName, Serializable> transformed = new HashMap<>();
        for (String key : data.keySet()) {
            transformed.put(QName.createQName(key), data.get(key));
        }
        return transformed;
    }

    @RetryingTransaction(readonly = true)
    public Set<String> getMembershipsOfGroup(String groupName) {
        String key = groupName.startsWith(PermissionService.GROUP_PREFIX) ? groupName : PermissionService.GROUP_PREFIX + groupName;
        return authorityService.getContainedAuthorities(null, key, true);
    }

    @RetryingTransaction(readonly = true)
    @Override
    public Set<String> getMembershipsOfGroupRecursively(String groupName) {
        String key = groupName.startsWith(PermissionService.GROUP_PREFIX) ? groupName : PermissionService.GROUP_PREFIX + groupName;
        Set<String> authorities = authorityService.getContainedAuthorities(null, key, true);
        Set<String> result = authorities
                .stream()
                .filter(x -> AuthorityType.getAuthorityType(x) != AuthorityType.GROUP)
                .collect(Collectors.toSet());


        authorities.stream()
                .filter(authority -> AuthorityType.getAuthorityType(authority) == AuthorityType.GROUP)
                .map(this::getMembershipsOfGroupRecursively)
                .forEach(result::addAll);

        return result;
    }


    @Override
    public void addMemberships(String groupName, Collection<String> members) {

        retryingTransactionHelper.doInTransaction((RetryingTransactionCallback<Void>) () -> {
            eduAuthorityService.addMemberships(groupName, members.toArray(new String[0]));
            return null;
        }, false);

    }

    @Override
    public void removeMemberships(String groupName, Collection<String> members) {


        retryingTransactionHelper.doInTransaction((RetryingTransactionCallback<Void>) () -> {
            String key = groupName.startsWith(PermissionService.GROUP_PREFIX) ? groupName : PermissionService.GROUP_PREFIX + groupName;

            for (String member : members) {

                if (member == null) {
                    continue;
                }

                authorityService.removeAuthority(key, member);
            }

            return null;
        }, false);

    }

    @Override
    public Map<String, Serializable> getProfileSettingsProperties(String userName, String profileSettingsProperty) {
        String user = userName;
        //check if userName exist, if not get login USER
        if (user == null)
            user = AuthenticationUtil.getFullyAuthenticatedUser();

        User userObj = this.getUser(user);
        Map<String, Serializable> profileSettings = userObj.getProfileSettings();
        if (profileSettings == null) {
            profileSettings = new HashMap<>();
            NodeRef personRef = personService.getPerson(user, false);
            for (String property : CCConstants.getAllPropertiesOfProfileSettings()) {
                profileSettings.put(property, nodeService.getProperty(personRef, QName.createQName(property)));
            }
            userObj.setProfileSettings(profileSettings);
            userCache.put(user, userObj);
        }

        Map<String, Serializable> result = new HashMap<>();

        List<String> properties = new ArrayList<>();// ProfileSettings property to return
        // If profileSettingsProperty==null than  Get all Properties for ProfileSettings
        if (profileSettingsProperty == null)
            properties = CCConstants.getAllPropertiesOfProfileSettings();
        else
            properties.add(profileSettingsProperty);

        for (String property : properties) {
            Serializable profileSetting = userObj.getProfileSettings().get(property);
            if (profileSetting != null) {
                result.put(property, profileSetting);
            }
        }

        return result;
    }

    @Override
    public String getSubgroupByType(String parentGroup, String groupType) {
        return retryingTransactionHelper.doInTransaction(() -> {
            Optional<String> first = authorityService.getContainedAuthorities(AuthorityType.GROUP, parentGroup, true).stream().filter(
                    (g) -> groupType.equals(nodeService.getProperty(getAuthorityNodeRef(g), QName.createQName(CCConstants.CCM_PROP_GROUPEXTENSION_GROUPTYPE))
                    )
            ).findFirst();
            return first.orElse(null);
        });
    }

    @Override
    public Map<String, Serializable> getCustomAttributes(String authorityName) {
        if (customAuthorityAttributesMapping.isPresent()) {
            return customAuthorityAttributesMapping.get().onGetAuthorityAttributes(authorityName);
        } else {
            log.debug("No customAuthorityAttributesMapping, will fetch nothing");
            return null;
        }
    }

    @Override
    public void setCustomAttributes(String authorityName, Map<String, Serializable> customAttributes) {
        if (customAuthorityAttributesMapping.isPresent()) {
            customAuthorityAttributesMapping.get().onSetAuthorityAttributes(authorityName, customAttributes);
        } else {
            log.debug("No customAuthorityAttributesMapping, will map nothing");
        }
    }

    @Override
    public void createProxyUser() {

        if (personService.personExists(CCConstants.PROXY_USER)) {
            return;
        }

        Map<QName, Serializable> properties = new HashMap<>();
        properties.put(ContentModel.PROP_USERNAME, CCConstants.PROXY_USER);
        personService.createPerson(properties);
        authenticationService.createAuthentication(CCConstants.PROXY_USER, new KeyTool().getRandomPassword().toCharArray());
    }

    @Override
    public String[] searchGroupNames(String pattern) {
        return retryingTransactionHelper.doInTransaction(() -> {
            PagingResults<AuthorityInfo> groupReq =
                    authorityService.getAuthoritiesInfo(
                            AuthorityType.GROUP,
                            null,
                            pattern,
                            null,
                            true,
                            new PagingRequest(Integer.MAX_VALUE, null));

            List<String> groupNames = new ArrayList<>();
            for (AuthorityInfo groupInfo : groupReq.getPage()) {
                groupNames.add(groupInfo.getAuthorityName());
            }

            return groupNames.toArray(new String[0]);
        }, true);

    }

    @Override
    public String generate2FaCode(String username) {
        //check if userName exist, if not get login USER
        String user = (username == null)
                ? AuthenticationUtil.getFullyAuthenticatedUser()
                : username;

        if (!canChange2Fa(user)) {
            throw new InsufficientPermissionException("You are not allowed to activate 2 factor authorization");
        }

        String secret = oneTimeTokenService.generateKey();
        retryingTransactionHelper.doInTransaction(() -> {
            Throwable runAs = AuthenticationUtil.runAs(() -> {
                try {
                    NodeRef personNodeRef = authenticationDao.getUserOrNull(user);
                    if (personNodeRef == null) {
                        return null;
                    }

                    nodeService.setProperty(personNodeRef, QName.createQName(CCConstants.CCM_PROP_PERSON_2FA_SECRET), secret);
                    nodeService.setProperty(personNodeRef, QName.createQName(CCConstants.CCM_PROP_PERSON_2FA_ACTIVATED), false);
                } catch (Throwable e) {
                    log.error(e.getMessage(), e);
                    return e;
                }
                return null;
            }, ApplicationInfoList.getHomeRepository().getUsername());

            if (runAs != null) {
                throw runAs;
            }

            return null;
        }, false);
        return secret;
    }

    @Override
    public boolean is2FaActive(String username) {
        String user = (username == null)
                ? AuthenticationUtil.getFullyAuthenticatedUser()
                : username;

        if (!canChange2Fa(user)) {
            throw new InsufficientPermissionException("You are not allowed to check 2 factor authorization");
        }

//        Boolean status = retryingTransactionHelper.doInTransaction(() -> AuthenticationUtil.runAs(() -> {
        NodeRef personNodeRef = authenticationDao.getUserOrNull(user);
        if (personNodeRef == null) {
            return false;
        }
        Boolean status = (Boolean) nodeService.getProperty(personNodeRef, QName.createQName(CCConstants.CCM_PROP_PERSON_2FA_ACTIVATED));
//        return (Boolean) nodeService.getProperty(personNodeRef, QName.createQName(CCConstants.CCM_PROP_PERSON_2FA_ACTIVATED));
//            }, ApplicationInfoList.getHomeRepository().getUsername()));

        return status != null && status;
    }

    @Override
    public void activate2Fa(String username, int code) {
        //check if userName exist, if not get login USER
        String user = (username == null)
                ? AuthenticationUtil.getFullyAuthenticatedUser()
                : username;

        if (!canChange2Fa(user)) {
            throw new InsufficientPermissionException("You are not allowed to activate 2 factor authorization");
        }

        if (!validate2Fa(user, code, true)) {
            throw new InvalidArgumentException("Invalid 2FA code");
        }

        retryingTransactionHelper.doInTransaction(() -> {
            Throwable runAs = AuthenticationUtil.runAs(() -> {
                try {
                    NodeRef personNodeRef = authenticationDao.getUserOrNull(user);
                    if (personNodeRef == null) {
                        return null;
                    }
                    nodeService.setProperty(personNodeRef, QName.createQName(CCConstants.CCM_PROP_PERSON_2FA_ACTIVATED), true);
                } catch (Throwable e) {
                    log.error(e.getMessage(), e);
                    return e;
                }
                return null;
            }, ApplicationInfoList.getHomeRepository().getUsername());

            if (runAs != null) {
                throw runAs;
            }

            return null;
        }, false);
    }

    boolean canChange2Fa(@NotNull String username) {
        if (isGlobalAdmin()) {
            return true;
        }

        String authenticatedUser = AuthenticationUtil.getFullyAuthenticatedUser();
        return username.equals(authenticatedUser);
    }

    @Override
    public void deactivate2Fa(String username) {

        //check if userName exist, if not get login USER
        String user = (username == null)
                ? AuthenticationUtil.getFullyAuthenticatedUser()
                : username;

        if (!canChange2Fa(user)) {
            throw new InsufficientPermissionException("You are not allowed to deactivate 2 factor authorization");
        }

        retryingTransactionHelper.doInTransaction(() -> {
            Throwable runAs = AuthenticationUtil.runAs(() -> {
                try {
                    NodeRef personNodeRef = authenticationDao.getUserOrNull(user);
                    if (personNodeRef == null) {
                        return null;
                    }
                    nodeService.setProperty(personNodeRef, QName.createQName(CCConstants.CCM_PROP_PERSON_2FA_ACTIVATED), false);
                } catch (Throwable e) {
                    log.error(e.getMessage(), e);
                    return e;
                }
                return null;
            }, ApplicationInfoList.getHomeRepository().getUsername());

            if (runAs != null) {
                throw runAs;
            }

            return null;
        }, false);
    }

    @Override
    public boolean validate2Fa(String username, int code) {
        return validate2Fa(username, code, false);
    }

    public boolean validate2Fa(String username, int code, boolean ignoreActivationStatus) {
        String secret = retryingTransactionHelper.doInTransaction(() ->
                AuthenticationUtil.runAs(() -> {
                    NodeRef personNodeRef = authenticationDao.getUserOrNull(username);
                    if (personNodeRef == null) {
                        return null;
                    }

                    Boolean isActivated = (Boolean) nodeService.getProperty(personNodeRef, QName.createQName(CCConstants.CCM_PROP_PERSON_2FA_ACTIVATED));
                    if (!ignoreActivationStatus && isActivated != null && !isActivated) {
                        return null;
                    }

                    return (String) nodeService.getProperty(personNodeRef, QName.createQName(CCConstants.CCM_PROP_PERSON_2FA_SECRET));

                }, ApplicationInfoList.getHomeRepository().getUsername()), false);

        if (secret == null) {
            return true;
        }

        return oneTimeTokenService.isValid(secret, code);
    }


    @Override
    public QRCode2Fa generate2FaQRCode(String username) {
        //check if userName exist, if not get login USER
        String user = (username == null)
                ? AuthenticationUtil.getFullyAuthenticatedUser()
                : username;

        if (!canChange2Fa(user)) {
            throw new InsufficientPermissionException("You are not allowed to generate a 2 factor authorization");
        }

        String secret = retryingTransactionHelper.doInTransaction(() ->
                AuthenticationUtil.runAs(() -> {
                    NodeRef personNodeRef = authenticationDao.getUserOrNull(username);
                    if (personNodeRef == null) {
                        return null;
                    }
                    return (String) nodeService.getProperty(personNodeRef, QName.createQName(CCConstants.CCM_PROP_PERSON_2FA_SECRET));
                }, AuthenticationUtil.getFullyAuthenticatedUser()), false);

        return new QRCode2Fa(oneTimeTokenService.generateQRCode(username, secret), secret);
    }
}
