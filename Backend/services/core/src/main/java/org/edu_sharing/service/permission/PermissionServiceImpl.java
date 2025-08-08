package org.edu_sharing.service.permission;

import com.google.gson.Gson;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.policy.BehaviourFilter;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.cmr.security.*;
import org.alfresco.service.namespace.QName;
import org.apache.commons.lang3.StringUtils;
import org.edu_sharing.alfresco.service.EduSharingCustomPermissionService;
import org.edu_sharing.alfresco.service.OrganisationService;
import org.edu_sharing.alfresco.service.guest.GuestService;
import org.edu_sharing.alfresco.service.toolpermission.ToolPermissionException;
import org.edu_sharing.alfresco.workspace_administration.NodeServiceInterceptor;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.rpc.*;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.MCAlfrescoAPIClient;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.repository.server.tools.PropertiesHelper;
import org.edu_sharing.repository.server.tools.cache.RepositoryCache;
import org.edu_sharing.service.InsufficientPermissionException;
import org.edu_sharing.service.authority.AuthorityServiceFactory;
import org.edu_sharing.service.authority.AuthorityServiceHelper;
import org.edu_sharing.service.collection.CollectionServiceFactory;
import org.edu_sharing.service.nodeservice.NodeServiceFactory;
import org.edu_sharing.service.notification.NotificationServiceFactoryUtility;
import org.edu_sharing.service.share.GlobalShareService;
import org.edu_sharing.service.share.GlobalShareServiceImpl;
import org.edu_sharing.service.toolpermission.ToolPermissionService;
import org.jetbrains.annotations.Nullable;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.Collection;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

@Slf4j
@Service(value = "permissionServiceImpl")
public class PermissionServiceImpl implements org.edu_sharing.service.permission.PermissionService {

    public static final String NODE_PUBLISHED = "NODE_PUBLISHED";
    // the maximal number of "notify" entries in the PH_HISTORY field that are serialized
    private static final int MAX_NOTIFY_HISTORY_LENGTH = 100;
    private final EduSharingCustomPermissionService customPermissionService;
    private final GlobalShareService globalShareService;
    private final NodeService nodeService;
    private final PersonService personService;
    private final ApplicationInfo appInfo;
    private final ToolPermissionService toolPermission;
    private final org.edu_sharing.service.nodeservice.NodeService eduNodeService;

    private final TimedPermissionMapper timedPermissionMapper;

    private final ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
    private final OrganisationService organisationService;
    //    private final ServiceRegistry serviceRegistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);
    private final AuthorityService authorityService;
    private final BehaviourFilter policyBehaviourFilter;

    private final MCAlfrescoAPIClient repoClient = new MCAlfrescoAPIClient();
    private final GuestService guestService;
    private final PermissionService permissionService;
    private final RetryingTransactionHelper retryingTransactionHelper;
    private final RepositoryCache repositoryCache;
    private final OwnableService ownableService;

    public PermissionServiceImpl(
            ToolPermissionService toolPermissionService,
            org.edu_sharing.service.nodeservice.NodeService nodeService,
            TimedPermissionMapper timedPermissionMapper,
            Optional<EduSharingCustomPermissionService> customPermissionService,
            OrganisationService organisationService,
            BehaviourFilter policyBehaviourFilter,
            AuthorityService authorityService,
            RetryingTransactionHelper retryingTransactionHelper,
            RepositoryCache repositoryCache,
            GuestService guestService,
            PermissionService permissionService,
            PersonService personService,
            NodeService alfNodeService,
            OwnableService ownableService
    ) {
        appInfo = ApplicationInfoList.getHomeRepository();
        this.eduNodeService = nodeService;
        this.toolPermission = toolPermissionService;
        this.customPermissionService = customPermissionService.orElse(null);
        this.nodeService = alfNodeService;
        this.globalShareService = new GlobalShareServiceImpl(this);
        this.personService = personService;
        this.timedPermissionMapper = timedPermissionMapper;
        this.organisationService = organisationService;
        this.policyBehaviourFilter = policyBehaviourFilter;
        this.authorityService = authorityService;
        this.retryingTransactionHelper = retryingTransactionHelper;
        this.repositoryCache = repositoryCache;
        this.guestService = guestService;
        this.permissionService = permissionService;
        this.ownableService = ownableService;
    }


    //TODO Thread safe / blocking for multiple users
    public void setPermissions(String nodeId, List<ACE> aces, Boolean inheritPermissions, String mailText, Boolean sendMail,
                               Boolean sendCopy) throws Throwable {

        NodeRef nodeRef = new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, nodeId);
        ACL currentACL = getPermissions(nodeId);

        // remove the inherited from the old and new
        List<ACE> acesNew = new ArrayList<>(aces);
        acesNew = addCollectionCoordinatorPermission(new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, nodeId), acesNew);
        acesNew.removeIf(ACE::isInherited);

        List<ACE> acesOld = new ArrayList<>(List.of(currentACL.getAces()));
        acesOld.removeIf(ACE::isInherited);


        List<ACE> acesToAdd = new ArrayList<>(); // set of aces to add instantly
        List<ACE> acesToUpdate = new ArrayList<>(); // set of aces to update instantly
        List<ACE> acesToRemove = new ArrayList<>(); // set of aces to remove instantly, including timed permissions
        List<ACE> acesNotChanged = new ArrayList<>(); // set of aces where active permissions not changed


        Set<ACE> activeTimedAces = new HashSet<>(); // set of timed aces which should be active
        Set<ACE> inactiveAces = new HashSet<>(); // set of timed aces which need to be stored in db


        // remove the ones that are already set (didn't change)
        long now = new Date().getTime();
        Iterator<ACE> iteratorNew = acesNew.iterator();
        while (iteratorNew.hasNext()) {
            ACE ace = iteratorNew.next();
            boolean remove = false;

            // future aces
            if ((ace.getFrom() != null && ace.getFrom() > now) && (ace.getTo() == null || ace.getTo() > now)) {
                inactiveAces.add(ace);
                remove = true;
            }

            // aces wich should be activated right now
            if ((ace.getFrom() != null || ace.getTo() != null) && (ace.getFrom() == null || ace.getFrom() < now) && (ace.getTo() == null || ace.getTo() > now)) {
                activeTimedAces.add(ace);
            }

            // flag to remove expired aces from acesNew
            if (ace.getTo() != null && ace.getTo() <= now) {
                remove = true;
            }

            if (acesOld.contains(ace)) {
                acesNotChanged.add(ace);
                remove = true;

                if (ace.getTo() != null && ace.getTo() <= now) {
                    acesToRemove.add(ace);
                }
            }

            if (remove) {
                iteratorNew.remove();
            }
        }

        List<String> aceOldAuthorityList = acesOld.stream().map(ACE::getAuthority).toList();
        for (ACE aceNew : acesNew) {
            if (aceOldAuthorityList.contains(aceNew.getAuthority())) {
                acesToUpdate.add(aceNew);
            } else {
                acesToAdd.add(aceNew);
            }
        }

        for (ACE aceOld : acesOld) {
            if (!aceOld.isInherited() && activeTimedAces.stream().anyMatch(x -> Objects.equals(x.getPermission(), aceOld.getPermission()) && Objects.equals(x.getAuthority(), aceOld.getAuthority()))) {
                continue;
            }

            if (!acesToUpdate.contains(aceOld) && !acesNotChanged.contains(aceOld) && !inactiveAces.contains(aceOld)) {
                acesToRemove.add(aceOld);
            }
        }

        boolean createNotify = false;
        if (!acesToRemove.isEmpty()) {
            removePermissions(nodeId, acesToRemove.toArray(new ACE[0]));
            createNotify = true;
        }

        if (!acesToAdd.isEmpty()) {
            Map<String, String[]> authPermissions = new HashMap<>();
            for (ACE toAdd : acesToAdd) {
                String[] permissions = authPermissions.get(toAdd.getAuthority());
                if (permissions == null) {
                    permissions = new String[]{toAdd.getPermission()};
                } else {
                    ArrayList<String> plist = new ArrayList<>(Arrays.asList(permissions));
                    plist.add(toAdd.getPermission());
                    permissions = plist.toArray(new String[0]);
                }
                authPermissions.put(toAdd.getAuthority(), permissions);
            }
            addPermissions(nodeId, authPermissions, inheritPermissions, mailText,
                    sendMail, sendCopy);
        }

        if (!acesToUpdate.isEmpty()) {
            for (ACE toUpdate : acesToUpdate) {
                setPermissions(nodeId, toUpdate.getAuthority(), new String[]{toUpdate.getPermission()}, null);
            }
            createNotify = true;
        }


        if (inheritPermissions != null && inheritPermissions != getPermissions(nodeId).isInherited()) {
            setPermissions(nodeId, null, null, inheritPermissions);
            createNotify = true;
        }


        for (ACE ace : inactiveAces) {
            TimedPermission permission = createTimedPermission(nodeId, ace, false);
            timedPermissionMapper.save(permission);
        }

        for (ACE ace : activeTimedAces) {
            TimedPermission permission = createTimedPermission(nodeId, ace, true);
            timedPermissionMapper.save(permission);
        }

        if (createNotify) {
            createNotifyObject(nodeId, AuthenticationUtil.getFullyAuthenticatedUser(),
                    CCConstants.CCM_VALUE_NOTIFY_ACTION_PERMISSION_CHANGE);
        }

        if (nodeService.hasAspect(nodeRef, QName.createQName(CCConstants.CCM_ASPECT_COLLECTION))) {
            CollectionServiceFactory.getCollectionService(appInfo.getAppId()).updateScope(nodeRef, aces);
        }
    }

    @Override
    public void updateTimedPermissions() {
        List<TimedPermission> permissionsToAdd = timedPermissionMapper.findAllByFromAfterAndNotActivated(new Date());
        List<TimedPermission> permissionsToRemove = timedPermissionMapper.findAllByToBefore(new Date());

        for (TimedPermission timedPermission : permissionsToAdd) {
            retryingTransactionHelper.doInTransaction(() ->
                    AuthenticationUtil.runAs(() -> {
                        NodeRef nodeRef = new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, timedPermission.getNode_id());

                        try {
                            if (!nodeService.exists(nodeRef)) {
                                timedPermissionMapper.delete(timedPermission);
                                return null;
                            }
                            addPermissions(timedPermission.getNode_id(),
                                    Map.of(timedPermission.getAuthority(), new String[]{timedPermission.getPermission()}), false, null, false, timedPermission.getUser());
                            if (timedPermission.getTo() == null) {
                                timedPermissionMapper.delete(timedPermission);
                            } else {
                                timedPermission.setActivated(true);
                                timedPermissionMapper.save(timedPermission);
                            }
                        } catch (Throwable e) {
                            log.error(e.getMessage(), e);
                        }
                        return null;
                    }, timedPermission.getUser()));
        }

        for (TimedPermission timedPermission : permissionsToRemove) {
            retryingTransactionHelper.doInTransaction(() ->
                    AuthenticationUtil.runAs(() -> {
                        try {
                            NodeRef nodeRef = new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, timedPermission.getNode_id());
                            if (nodeService.exists(nodeRef)) {
                                permissionService.deletePermission(
                                        nodeRef,
                                        timedPermission.getAuthority(),
                                        timedPermission.getPermission());
                                createNotifyObject(timedPermission.getNode_id(), timedPermission.getUser(), CCConstants.CCM_VALUE_NOTIFY_ACTION_PERMISSION_CHANGE);
                            }
                            timedPermissionMapper.delete(timedPermission);
                        } catch (Throwable e) {
                            log.error(e.getMessage(), e);
                        }
                        return null;
                    }, timedPermission.getUser()));
        }
    }

    @Override
    public void addPermissions(String _nodeId, Map<String, String[]> _authPerm, Boolean _inheritPermissions,
                               String _mailText, Boolean _sendMail, Boolean _sendCopy) throws Throwable {
        String user = AuthenticationUtil.getFullyAuthenticatedUser();
        addPermissions(_nodeId, _authPerm, _inheritPermissions, _mailText, _sendMail, user);
    }

    private void addPermissions(String _nodeId, Map<String, String[]> _authPerm, Boolean _inheritPermissions, String _mailText, Boolean _sendMail, String user) throws Throwable {

        for (String authority : _authPerm.keySet()) {
            String[] permissions = _authPerm.get(authority);
            setPermissions(_nodeId, authority, permissions, _inheritPermissions);

            AuthorityType authorityType = AuthorityType.getAuthorityType(authority);

            if (!AuthenticationUtil.isRunAsUserTheSystemUser()) {
                if (AuthorityType.USER.equals(authorityType)) {
                    addToRecent(personService.getPerson(authority));
                }
                // send group email notifications
                if (AuthorityType.GROUP.equals(authorityType)) {
                    addToRecent(authorityService.getAuthorityNodeRef(authority));
                }
            }
        }

        createNotifyObject(_nodeId, user, CCConstants.CCM_VALUE_NOTIFY_ACTION_PERMISSION_ADD);

        for (String authority : _authPerm.keySet()) {
            String[] permissions = _authPerm.get(authority);
            if (_sendMail) {
                String nodeType = eduNodeService.getType(StoreRef.PROTOCOL_WORKSPACE, StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(), _nodeId);
                Map<String, Object> props = eduNodeService.getProperties(StoreRef.PROTOCOL_WORKSPACE, StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(), _nodeId);
                List<String> aspects = Arrays.asList(eduNodeService.getAspects(StoreRef.PROTOCOL_WORKSPACE, StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(), _nodeId));
                NotificationServiceFactoryUtility.getLocalService()
                        .notifyPermissionChanged(user, authority, _nodeId, nodeType, aspects, props, permissions, _mailText);
            }
        }
    }


    /**
     * Add the authority into the recent list of the users authorities
     */
    private void addToRecent(NodeRef authority) {
        addToRecentProperty(CCConstants.CCM_PROP_PERSON_RECENTLY_INVITED, authority);
    }

    /**
     * add nodeRef to recent elements list for a property with "NodeRef" list type
     * Use also @getRecentProperty to get the current list
     */
    @Override
    public void addToRecentProperty(String property, NodeRef elementAdd) {
        nodeService.setProperty(personService.getPerson(AuthenticationUtil.getFullyAuthenticatedUser()),
                QName.createQName(property),
                PropertiesHelper.addToRecentProperty(elementAdd, getRecentProperty(property), 10));
    }

    @Override
    public List<String> getRecentlyInvited() {
        return getRecentProperty(CCConstants.CCM_PROP_PERSON_RECENTLY_INVITED).stream().map((n) -> {
            if (nodeService.getType(n).equals(QName.createQName(CCConstants.CM_TYPE_PERSON))) {
                return (String) nodeService.getProperty(n, QName.createQName(CCConstants.CM_PROP_PERSON_USERNAME));
            } else {
                return (String) nodeService.getProperty(n, QName.createQName(CCConstants.CM_PROP_AUTHORITY_NAME));
            }
        }).collect(toList());
    }

    @Override
    public ArrayList<NodeRef> getRecentProperty(String property) {
        List<NodeRef> data = (List<NodeRef>) nodeService.getProperty(personService.getPerson(AuthenticationUtil.getFullyAuthenticatedUser()),
                QName.createQName(property));
        if (data == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(data);

    }

    @Override
    public List<Notify> getNotifyList(final String nodeId) {
        if (!toolPermission.hasToolPermission(CCConstants.CCM_VALUE_TOOLPERMISSION_INVITE_HISTORY)) {
            throw new ToolPermissionException(CCConstants.CCM_VALUE_TOOLPERMISSION_INVITE_HISTORY);
        }

        Comparator<Notify> c = (o1, o2) -> {

            if (o1.getCreated().getTime() == o2.getCreated().getTime()) {
                return 0;
            } else if (o1.getCreated().getTime() > o2.getCreated().getTime()) {
                return -1;
            } else if (o1.getCreated().getTime() < o2.getCreated().getTime()) {
                return 1;
            }

            return 0;
        };

        Gson gson = new Gson();
        List<String> jsonHistory = (List<String>) nodeService.getProperty(new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, nodeId), QName.createQName(CCConstants.CCM_PROP_PH_HISTORY));

        List<Notify> notifyList = new ArrayList<>();
        if (jsonHistory != null) {
            for (String json : jsonHistory) {
                Notify notify = gson.fromJson(json, Notify.class);
                try {
                    if (personService.personExists(notify.getUser().getAuthorityName())) {
                        NodeRef personRef = personService.getPerson(notify.getUser().getAuthorityName(), false);
                        Map<QName, Serializable> personProps = nodeService.getProperties(personRef);
                        notify.getUser().setGivenName((String) personProps.get(QName.createQName(CCConstants.CM_PROP_PERSON_FIRSTNAME)));
                        notify.getUser().setSurname((String) personProps.get(QName.createQName(CCConstants.CM_PROP_PERSON_LASTNAME)));
                        notify.getUser().setEmail((String) personProps.get(QName.createQName(CCConstants.CM_PROP_PERSON_EMAIL)));
                    }
                } catch (NoSuchPersonException e) {
                    log.warn("Notify could not be fully resolved, may contains deleted/invalid user", e);
                }
                // @todo overwrite acl user firstname, lastname, email

                notifyList.add(notify);
            }

            notifyList.sort(c);
        }

        System.out.println("NOTIFYLIST:" + notifyList.size());
        return notifyList;
    }

    public void setPermissions(String nodeId, List<ACE> aces) throws Exception {
        setPermissions(nodeId, aces, null);
    }

    /**
     * set's all local permissions contained in the aces array, removes all
     * permissions that are not in the ace array
     */
    public void setPermissions(String nodeId, List<ACE> aces, Boolean inheritPermission) throws Exception {

        if (inheritPermission != null) {
            if (!toolPermission.hasToolPermission(CCConstants.CCM_VALUE_TOOLPERMISSION_INVITE) && !isSharedNode(nodeId)) {
                throw new ToolPermissionException(CCConstants.CCM_VALUE_TOOLPERMISSION_INVITE);
            }
            if (!toolPermission.hasToolPermission(CCConstants.CCM_VALUE_TOOLPERMISSION_INVITE_SHARE) && isSharedNode(nodeId)) {
                throw new ToolPermissionException(CCConstants.CCM_VALUE_TOOLPERMISSION_INVITE_SHARE);
            }
        }

        checkCanManagePermissions(nodeId, aces);

        NodeRef nodeRef = new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, nodeId);

        String authorityAdministrator = getAdminAuthority(nodeRef);

        aces = addCollectionCoordinatorPermission(nodeRef, aces);
        if (aces != null) {
            for (ACE ace : aces) {

                if (!authorityService.authorityExists(ace.getAuthority())
                        && !"GROUP_EVERYONE".equals(ace.getAuthority())) {
                    throw new Exception("authority " + ace.getAuthority() + " does not exist!");
                }
                String permission = ace.getPermission();
                // prevent authorityAdministrator ace is changed
                if (!ace.isInherited()
                        && (authorityAdministrator == null || !authorityAdministrator.equals(ace.getAuthority()))) {
                    permissionService.setPermission(nodeRef, ace.getAuthority(), permission, true);
                }
            }
        }

        ArrayList<AccessPermission> toRemove = new ArrayList<>();
        Set<AccessPermission> allSetPerm = permissionService.getAllSetPermissions(nodeRef);

        for (AccessPermission accessPerm : allSetPerm) {
            if (accessPerm.isInherited()) {
                continue;
            }
            if (!containslocalPerm(aces, accessPerm.getAuthority(), accessPerm.getPermission())) {
                if (authorityAdministrator == null || !(authorityAdministrator.equals(accessPerm.getAuthority())
                        && PermissionService.COORDINATOR.equals(accessPerm.getPermission()))) {
                    toRemove.add(accessPerm);
                }
            }
        }

        for (AccessPermission accessPerm : toRemove) {
            permissionService.deletePermission(nodeRef, accessPerm.getAuthority(), accessPerm.getPermission());
        }

        if (inheritPermission != null) {
            log.info("setInheritParentPermissions " + inheritPermission);
            permissionService.setInheritParentPermissions(nodeRef, inheritPermission);
        }
    }

    @Override
    public void setPermissionInherit(String nodeId, boolean inheritPermission) throws Exception {
        NodeRef nodeRef = new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, nodeId);
        permissionService.setInheritParentPermissions(nodeRef, inheritPermission);
    }

    private List<ACE> addCollectionCoordinatorPermission(NodeRef nodeRef, List<ACE> aces) {
        if (!nodeService.hasAspect(nodeRef, QName.createQName(CCConstants.CCM_ASPECT_COLLECTION)))
            return aces;

        org.edu_sharing.repository.client.rpc.ACE coordinator = new org.edu_sharing.repository.client.rpc.ACE();
        coordinator.setAuthority(AuthenticationUtil.getFullyAuthenticatedUser());
        coordinator.setAuthorityType(org.edu_sharing.restservices.shared.Authority.Type.USER.name());
        coordinator.setPermission(CCConstants.PERMISSION_COORDINATOR);
        if (aces != null && aces.contains(coordinator)) {
            return aces;
        }

        if (aces == null) {
            aces = new ArrayList<>();
        }

        List<ACE> newAces = new ArrayList<>(aces);
        newAces.add(coordinator);
        return newAces;
    }

    /**
     * returns admin authority if context is an edugroup
     */
    String getAdminAuthority(NodeRef nodeRef) {
        return AuthenticationUtil.runAsSystem(() -> {
            String authorityAdministrator = null;
            if (isSharedNode(nodeRef.getId())) {
                Set<AccessPermission> allSetPermissions = permissionService.getAllSetPermissions(nodeRef);
                for (AccessPermission ap : allSetPermissions) {
                    NodeRef authorityNodeRef = authorityService.getAuthorityNodeRef(ap.getAuthority());
                    if (authorityNodeRef != null) {
                        String groupType = (String) nodeService.getProperty(authorityNodeRef,
                                QName.createQName(CCConstants.CCM_PROP_GROUPEXTENSION_GROUPTYPE));
                        if (CCConstants.ADMINISTRATORS_GROUP_TYPE.equals(groupType)
                                && ap.getPermission().equals(PermissionService.COORDINATOR)) {
                            authorityAdministrator = ap.getAuthority();
                        }
                    }
                }
            }
            return authorityAdministrator;
        });
    }

    private boolean containslocalPerm(List<ACE> aces, String eduAuthority, String eduPermission) {
        log.info("eduAuthority:" + eduAuthority + " eduPermission:" + eduPermission);
        if (aces == null)
            return false;
        for (ACE ace : aces) {
            if (ace.isInherited()) {
                continue;
            }
            log.info("ace.getAuthority():" + ace.getAuthority() + " ace.getPermission():" + ace.getPermission());
            if (ace.getAuthority().equals(eduAuthority) && ace.getPermission().equals(eduPermission)) {
                return true;
            }
        }
        return false;
    }

    private void checkCanManagePermissions(String node, String authority) throws Exception {
        ACE ace = new ACE();
        ace.setAuthority(authority);
        checkCanManagePermissions(node, List.of(ace));
    }

    private void checkCanManagePermissions(String nodeId, List<ACE> aces) {
        boolean hasUsers = false, hasAll = false;
        if (aces != null) {
            for (ACE ace : aces) {

                if (ace.getAuthority() != null && ace.getAuthority().equals("GROUP_EVERYONE")) {
                    hasAll = true;
                } else {
                    hasUsers = true;
                }
            }
        }

        // not required anymore, also private files can be shared in scope
        /*
         * if(!shared && NodeServiceInterceptor.getEduSharingScope()!=null){
         * if(QName.createQName(CCConstants.CCM_TYPE_NOTIFY).equals(nodeService.getType(
         * new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE,nodeId)))){ // allow
         * notify objects to share } else { throw new
         * Exception("Setting Permissions for private files in scope is not allowed"); }
         * }
         */

        if (!toolPermission.hasToolPermission(CCConstants.CCM_VALUE_TOOLPERMISSION_INVITE_SAFE)
                && NodeServiceInterceptor.getEduSharingScope() != null) {
            throw new ToolPermissionException(CCConstants.CCM_VALUE_TOOLPERMISSION_INVITE_SAFE);
        }
        if (!toolPermission.hasToolPermission(CCConstants.CCM_VALUE_TOOLPERMISSION_INVITE_ALLAUTHORITIES) && hasAll) {
            throw new ToolPermissionException(CCConstants.CCM_VALUE_TOOLPERMISSION_INVITE_ALLAUTHORITIES);
        }
        if (NodeServiceInterceptor.getEduSharingScope() != null && hasAll) {
            throw new SecurityException("Inviting of " + CCConstants.AUTHORITY_GROUP_EVERYONE + " is not allowed in scope " + NodeServiceInterceptor.getEduSharingScope());
        }
        if (!toolPermission.hasToolPermission(CCConstants.CCM_VALUE_TOOLPERMISSION_INVITE) && hasUsers && !isSharedNode(nodeId)) {
            throw new ToolPermissionException(CCConstants.CCM_VALUE_TOOLPERMISSION_INVITE);
        }
        if (!toolPermission.hasToolPermission(CCConstants.CCM_VALUE_TOOLPERMISSION_INVITE_SHARE) && hasUsers
                && isSharedNode(nodeId)) {
            throw new ToolPermissionException(CCConstants.CCM_VALUE_TOOLPERMISSION_INVITE_SHARE);
        }
    }

    /**
     * true if this node is in a shared context ("My shared files"), false if it's
     * in users home
     */
    private boolean isSharedNode(String nodeId) {
        try {
            String groupFolderId = repoClient.getGroupFolderId(AuthenticationUtil.getFullyAuthenticatedUser());
            List<String> sharedFolderIds = new ArrayList<>();

            if (groupFolderId != null) {
                List<ChildAssociationRef> children = NodeServiceFactory.getLocalService().getChildrenChildAssociationRef(groupFolderId);
                for (ChildAssociationRef key : children) {
                    sharedFolderIds.add(key.getChildRef().getId());
                }
            }
            if (sharedFolderIds.isEmpty())
                return false;

            NodeRef last = new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, nodeId);
            while (last != null) {
                if (sharedFolderIds.contains(last.getId()))
                    return true;
                last = repoClient.getParent(last).getParentRef();
            }
        } catch (Throwable t) {
            log.warn(t.getMessage());
        }
        return false;
    }

    public void addPermissions(String nodeId, ACE[] aces) {

        List<ACE> newAces = new ArrayList<>(List.of(aces));
        Iterator<ACE> newAcesIterator = newAces.iterator();
        long now = new Date().getTime();

        // TODO timed permissions
        Set<ACE> inactiveAces = new HashSet<>();
        Set<ACE> activeTimedAces = new HashSet<>();
        while (newAcesIterator.hasNext()) {
            ACE ace = newAcesIterator.next();
            boolean remove = false;
            // future aces
            if ((ace.getFrom() != null && ace.getFrom() > now) && (ace.getTo() == null || ace.getTo() > now)) {
                inactiveAces.add(ace);
                remove = true;
            }

            // aces wich should be activated right now
            if ((ace.getFrom() != null || ace.getTo() != null) && (ace.getFrom() == null || ace.getFrom() <= now) && (ace.getTo() == null || ace.getTo() > now)) {
                activeTimedAces.add(ace);
            }

            // flag to remove expired aces from acesNew
            if (ace.getTo() != null && ace.getTo() <= now) {
                remove = true;
            }

            if (remove) {
                newAcesIterator.remove();
            }
        }

        retryingTransactionHelper.doInTransaction(() -> {

            checkCanManagePermissions(nodeId, Arrays.asList(aces));
            NodeRef nodeRef = new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, nodeId);

            for (ACE ace : newAces) {

                if (ace == null) {
                    continue;
                }

                if (!authorityService.authorityExists(ace.getAuthority())
                        && !"GROUP_EVERYONE".equals(ace.getAuthority())) {
                    throw new Exception("authority " + ace.getAuthority() + " does not exist!");
                }

                String permission = ace.getPermission();

                if (!ace.isInherited()) {
                    permissionService.setPermission(nodeRef, ace.getAuthority(), permission, true);
                }
            }
            inactiveAces.forEach(x -> timedPermissionMapper.save(createTimedPermission(nodeId, x, false)));
            activeTimedAces.forEach(x -> timedPermissionMapper.save(createTimedPermission(nodeId, x, true)));

            return null;
        }, false);

    }

    public void removePermissions(String nodeId, ACE[] aces) {
        List<ACE> acesToRemove = new ArrayList<>(List.of(aces));
        Iterator<ACE> newAcesIterator = acesToRemove.iterator();
        long now = new Date().getTime();

        Set<ACE> timedAcesToRemove = new HashSet<>();
        while (newAcesIterator.hasNext()) {
            ACE ace = newAcesIterator.next();
            boolean remove = false;
            if (ace.getFrom() != null && ace.getFrom() > now) {
                timedAcesToRemove.add(ace);
                remove = true;
            }

            if (ace.getTo() != null) {
                timedAcesToRemove.add(ace);
                remove = true;
            }

            if (remove) {
                newAcesIterator.remove();
            }
        }

        retryingTransactionHelper.doInTransaction(() -> {

            checkCanManagePermissions(nodeId, Arrays.asList(aces));

            NodeRef nodeRef = new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, nodeId);
            boolean isGlobalAdmin = AuthorityServiceFactory.getLocalService().isGlobalAdmin();
            String fullyAuthenticatedUser = AuthenticationUtil.getFullyAuthenticatedUser();

            String adminAuthority = getAdminAuthority(nodeRef);

            for (ACE ace : acesToRemove) {

                if (ace == null) {
                    continue;
                }

                if (!authorityService.authorityExists(ace.getAuthority())
                        && !"GROUP_EVERYONE".equals(ace.getAuthority())) {
                    throw new Exception("authority " + ace.getAuthority() + " does not exist!");
                }

                if (StringUtils.isNotBlank(adminAuthority)
                        && adminAuthority.equals(ace.getAuthority())
                        && PermissionService.COORDINATOR.equals(ace.getPermission())) {
                    continue;
                }

                if (!isGlobalAdmin && ace.getAuthority().equals(fullyAuthenticatedUser)) {
                    String owner = ownableService.getOwner(nodeRef);
                    if (!fullyAuthenticatedUser.equals(owner)) {
                        log.warn("user should not uninvite himself");
                        continue;
                    }
                }

                if (!ace.isInherited()) {
                    permissionService.deletePermission(nodeRef, ace.getAuthority(), ace.getPermission());
                }
            }

            timedAcesToRemove.forEach(x -> {
                TimedPermission permission = createTimedPermission(nodeId, x, false);
                timedPermissionMapper.delete(permission);
            });

            return null;
        }, false);
    }

    private TimedPermission createTimedPermission(String nodeId, ACE ace, boolean activated) {
        TimedPermission permission = new TimedPermission();
        permission.setNode_id(nodeId);
        if (ace.getTo() != null) {
            permission.setTo(new Date(ace.getTo()));
        }
        if (ace.getFrom() != null) {
            permission.setFrom(new Date(ace.getFrom()));
        }
        permission.setAuthority(ace.getAuthority());
        permission.setPermission(ace.getPermission());
        permission.setActivated(activated);
        permission.setUser(AuthenticationUtil.getFullyAuthenticatedUser());
        return permission;
    }

    /**
     * set's permission for one authority, leaves permissions already set for the
     * authority
     */
    public void setPermissions(String nodeId, String authority, String[] permissions, Boolean inheritPermission)
            throws Exception {
        checkCanManagePermissions(nodeId, authority);

        NodeRef nodeRef = new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, nodeId);

        String adminAuthority = getAdminAuthority(nodeRef);

        if (permissions != null) {
            for (String permission : permissions) {

                if (StringUtils.isNotBlank(adminAuthority) && adminAuthority.equals(authority)
                        && PermissionService.COORDINATOR.equals(permission)) {
                    continue;
                }

                permissionService.setPermission(new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, nodeId), authority, permission, true);

            }
        }

        if (inheritPermission != null) {
            log.info("setInheritParentPermissions " + inheritPermission);
            permissionService.setInheritParentPermissions(nodeRef, inheritPermission);
        }

    }

    @Override
    public void removeAllPermissions(String nodeId) throws Exception {
        permissionService.deletePermissions(new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, nodeId));
        timedPermissionMapper.deleteAllByNodeId(nodeId);
    }

    public List<String> getOrganizationsOfUser() {
        List<String> eduGroupAuthorityNames = organisationService.getMyOrganisations(true);
        if (customPermissionService != null) {
            return customPermissionService.getLocalOrganizations(eduGroupAuthorityNames);
        }
        return eduGroupAuthorityNames;
    }

    public void createNotifyObject(final String nodeId, final String user, final String action) {

        NodeRef nodeRef = new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, nodeId);
        retryingTransactionHelper.doInTransaction(() -> {
            try {
                policyBehaviourFilter.disableBehaviour(nodeRef);
                if (!nodeService.hasAspect(nodeRef, QName.createQName(CCConstants.CCM_ASPECT_PERMISSION_HISTORY))) {
                    nodeService.addAspect(nodeRef, QName.createQName(CCConstants.CCM_ASPECT_PERMISSION_HISTORY), null);
                }

                nodeService.setProperty(nodeRef, QName.createQName(CCConstants.CCM_PROP_PH_ACTION), action);

                Date created = new Date();
                addUserToSharedList(user, nodeRef, created);


                //ObjectMapper jsonMapper = new ObjectMapper();
                Gson gson = new Gson();
                Notify n = new Notify();
                try {
                    ACL acl = getPermissions(nodeId);
                    // set of all authority names that are not inherited, but explicitly set
                    nodeService.setProperty(nodeRef, QName.createQName(CCConstants.CCM_PROP_PH_INVITED), PermissionServiceHelper.getExplicitAuthoritiesFromACL(acl));

                    acl.setAces(acl.getAces());


                    n.setAcl(acl);
                    n.setCreated(created);
                    n.setNotifyAction(action);
                    n.setNotifyUser(user);
                    User u = new User();
                    u.setAuthorityName(user);
                    u.setUsername(user);
                    n.setUser(u);


                    String jsonStringACL = gson.toJson(n);
                    List<String> history = (List<String>) nodeService.getProperty(nodeRef, QName.createQName(CCConstants.CCM_PROP_PH_HISTORY));
                    history = (history == null) ? new ArrayList<>() : history;
                    while (history.size() > MAX_NOTIFY_HISTORY_LENGTH) {
                        history.remove(0);
                    }
                    history.add(jsonStringACL);
                    nodeService.setProperty(nodeRef, QName.createQName(CCConstants.CCM_PROP_PH_HISTORY), new ArrayList<>(history));
                    cleanUpSharedList(nodeRef);
                } catch (Exception e1) {
                    log.warn("Error setting permission history", e1);
                }
            } finally {
                policyBehaviourFilter.enableBehaviour(nodeRef);
            }
            // remove from cache so that the ccm:ph_* properties getting updated
            repositoryCache.remove(nodeRef.getId());
            return null;
        });
    }

    @Override
    public void addUserToSharedList(String user, NodeRef nodeRef) {
        addUserToSharedList(user, nodeRef, new Date());
    }

    private void addUserToSharedList(String user, NodeRef nodeRef, Date created) {
        ArrayList<String> phUsers = (ArrayList<String>) nodeService.getProperty(nodeRef, QName.createQName(CCConstants.CCM_PROP_PH_USERS));
        if (phUsers == null) {
            phUsers = new ArrayList<>();
        }
        if (!phUsers.contains(user)) {
            phUsers.add(user);
        }
        nodeService.setProperty(nodeRef, QName.createQName(CCConstants.CCM_PROP_PH_USERS), phUsers);
        nodeService.setProperty(nodeRef, QName.createQName(CCConstants.CCM_PROP_PH_MODIFIED), created);
    }

    @Override
    public void cleanUpSharedList(NodeRef nodeRef) {

        try {
            ArrayList<String> phUsers = (ArrayList<String>) nodeService.getProperty(nodeRef, QName.createQName(CCConstants.CCM_PROP_PH_USERS));
            if (phUsers == null || phUsers.isEmpty()) {
                return;
            }
            List<Notify> notifyList = getNotifyList(nodeRef.getId());
            Notify predecessor = null;
            Map<String, List<ACE>> userAddAcesList = new HashMap<>();

            notifyList.sort(Comparator.comparing(Notify::getCreated));
            // collect user addes ace's
            for (Notify notify : notifyList) {
                log.debug("Notify e:{} a:{} u:{} c:{} date:{}", notify.getNotifyEvent(), notify.getNotifyAction(), notify.getNotifyUser(), notify.getChange(), notify.getCreated());
                if (CCConstants.CCM_VALUE_NOTIFY_ACTION_PERMISSION_ADD.equals(notify.getNotifyAction())) {
                    if (predecessor == null) {
                        List<ACE> addedAcesForUser = new ArrayList<>(Arrays.asList(notify.getAcl().getAces()));
                        addedAcesForUser = filterACEList(addedAcesForUser, notify.getNotifyUser());
                        userAddAcesList.put(notify.getNotifyUser(), addedAcesForUser);
                        predecessor = notify;
                    } else {
                        List<ACE> notifyAces = new ArrayList<>(Arrays.asList(notify.getAcl().getAces()));
                        boolean isDiff = notifyAces.removeAll(Arrays.asList(predecessor.getAcl().getAces()));
                        if (isDiff) {
                            List<ACE> addedAcesForUser = userAddAcesList.get(notify.getNotifyUser());
                            if (addedAcesForUser == null) addedAcesForUser = new ArrayList<>();
                            addedAcesForUser.addAll(notifyAces);
                            addedAcesForUser = filterACEList(addedAcesForUser, notify.getNotifyUser());
                            userAddAcesList.put(notify.getNotifyUser(), addedAcesForUser);
                        }
                    }
                }
            }
            /*
              find out if current aces still contains at least one user added ace
              if not collect the user in remove list
             */
            Set<String> removePhUsers = new HashSet<>();
            if (!notifyList.isEmpty()) {
                Notify currentNotify = notifyList.get(notifyList.size() - 1);
                for (Map.Entry<String, List<ACE>> entry : userAddAcesList.entrySet()) {
                    if (entry.getValue() == null || entry.getValue().isEmpty()) {
                        removePhUsers.add(entry.getKey());
                        continue;
                    }
                    if (currentNotify.getAcl() == null
                            || currentNotify.getAcl().getAces() == null
                            || currentNotify.getAcl().getAces().length == 0) {
                        removePhUsers.add(entry.getKey());
                        continue;
                    }

                    List<ACE> userAddedAces = entry.getValue();
                    List<ACE> remainingUserAddedAces = Arrays.stream(currentNotify.getAcl().getAces())
                            .filter(userAddedAces::contains)
                            .toList();

                    if (remainingUserAddedAces.isEmpty()) {
                        removePhUsers.add(entry.getKey());
                    }
                }
            }
            /*
              remove users that are in ph_users but never added an permission.
              i.e. only did "change permission", "remove permission"
             */
            boolean hasShares = Arrays.stream(globalShareService.getShares(nodeRef.getId()))
                    .anyMatch(x -> x.getExpiryDate() >= new Date().getTime());

            if (!hasShares) {
                removePhUsers.addAll(phUsers.stream().filter(u -> !userAddAcesList.containsKey(u))
                        .toList());
            }
            // remove users from PH_USERS property
            if (phUsers.removeAll(removePhUsers)) {
                nodeService.setProperty(nodeRef, QName.createQName(CCConstants.CCM_PROP_PH_USERS), phUsers);
            }

        } catch (Throwable e) {
            log.error(e.getMessage(), e);
        }
    }

    private List<ACE> filterACEList(List<ACE> aces, String user) {
        return aces.stream().filter(ace -> !"ROLE_OWNER".equals(ace.getAuthority()) && !user.equals(ace.getAuthority()))
                .collect(Collectors.toList());
    }

    @Override
    public boolean hasPermission(String storeProtocol, String storeId, String nodeId, String permission) {
        return hasAllPermissions(storeProtocol, storeId, nodeId, new String[]{permission}).get(permission);
    }

    @Override
    public boolean hasPermission(String storeProtocol, String storeId, String nodeId, String authority, String permission) {
        return hasAllPermissions(storeProtocol, storeId, nodeId, authority, new String[]{permission}).get(permission);
    }

    @Override
    public Map<String, Boolean> hasAllPermissions(String storeProtocol, String storeId, String nodeId, String authority,
                                                  String[] permissions) {
        return AuthenticationUtil.runAs(() -> hasAllPermissions(storeProtocol, storeId, nodeId, permissions), authority);
    }

    @Override
    public Map<String, Boolean> hasAllPermissions(String storeProtocol, String storeId, String nodeId,
                                                  String[] permissions) {
        boolean guest = guestService.isGuestUser(AuthenticationUtil.getFullyAuthenticatedUser());
        Map<String, Boolean> result = new HashMap<>();
        NodeRef nodeRef = new NodeRef(new StoreRef(storeProtocol, storeId), nodeId);
        if (permissions != null) {
            for (String permission : permissions) {
                AccessStatus accessStatus = permissionService.hasPermission(nodeRef, permission);
                // Guest only has read permissions, no modify permissions
                if (guest && !Arrays.asList(GUEST_PERMISSIONS).contains(permission)) {
                    accessStatus = AccessStatus.DENIED;
                }
                if (accessStatus.equals(AccessStatus.ALLOWED)) {
                    result.put(permission, Boolean.TRUE);
                } else {
                    result.put(permission, Boolean.FALSE);
                }
            }
        }
        return result;
    }

    @Override
    public ACL getPermissions(String nodeId) throws Exception {
        return retryingTransactionHelper.doInTransaction(() -> {
            NodeRef nodeRef = new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, nodeId);
            Set<AccessPermission> permSet = permissionService.getAllSetPermissions(nodeRef);
            ACL result = new ACL();
            ArrayList<ACE> aces = new ArrayList<>();
            permSet.stream()
                    .map(ace -> getAce(nodeRef, ace.getAuthority(), ace.getPermission(), ace.getAccessStatus().name(), ace.isInherited(), null, null))
                    .filter(Objects::nonNull)
                    .forEach(aces::add);

            List<TimedPermission> timedPermissions = timedPermissionMapper.findAllByNodeId(nodeRef.getId());
            timedPermissions.stream()
                    .map(x -> getAce(nodeRef, x.getAuthority(), x.getPermission(), "ALLOWED", false, x.getFrom(), x.getTo()))
                    .forEach(aces::add);

            result.setAces(aces.toArray(new ACE[0]));

            log.debug("permissionService.getInheritParentPermissions(nodeRef):{}", permissionService.getInheritParentPermissions(nodeRef));
            boolean isInherited = permissionService.getInheritParentPermissions(nodeRef);

            result.setInherited(isInherited);
            return result;

        }, false);
    }

    private @Nullable ACE getAce(NodeRef nodeRef, String authority, String permission, String accessStatus, boolean inherited, Date from, Date to) {
        ACE aceResult = new ACE();
        aceResult.setAuthority(authority);
        aceResult.setPermission(permission);
        aceResult.setInherited(inherited);
        aceResult.setFrom(Optional.ofNullable(from).map(Date::getTime).orElse(null));
        aceResult.setTo(Optional.ofNullable(to).map(Date::getTime).orElse(null));
        // to be compatible with WS API where positiv access status is called "accepted"
        // in GUI we compare with "acepted"
        if (accessStatus.trim().equals("ALLOWED")) {
            accessStatus = "acepted";
        }

        aceResult.setAccessStatus(accessStatus);
        aceResult.setAuthorityType(AuthorityType.getAuthorityType(authority).name());

        if (AuthorityType.getAuthorityType(authority).equals(AuthorityType.USER) ||
                AuthorityType.getAuthorityType(authority).equals(AuthorityType.OWNER)) {

            NodeRef personNodeRef;
            if (AuthorityType.getAuthorityType(authority).equals(AuthorityType.OWNER)) {
                personNodeRef = personService.getPersonOrNull(ownableService.getOwner(nodeRef));
            } else {
                personNodeRef = personService.getPersonOrNull(authority);
            }

            if (personNodeRef != null) {
                Map<QName, Serializable> personProps = nodeService.getProperties(personNodeRef);
                User user = new User();
                user.setNodeId(personNodeRef.getId());
                user.setEmail((String) personProps.get(ContentModel.PROP_EMAIL));
                user.setGivenName((String) personProps.get(ContentModel.PROP_FIRSTNAME));
                user.setSurname((String) personProps.get(ContentModel.PROP_LASTNAME));
                user.setEditable(
                        AuthorityServiceHelper.isAdmin() ||
                                !Objects.equals(AuthenticationUtil.getFullyAuthenticatedUser(), authority)
                );

                String repository = (String) personProps.get(QName.createQName(CCConstants.PROP_USER_REPOSITORYID));
                if (StringUtils.isBlank(repository)) {
                    repository = appInfo.getAppId();
                }

                user.setRepositoryId(repository);
                user.setUsername((String) personProps.get(ContentModel.PROP_USERNAME));
                aceResult.setUser(user);
            } else {
                User user = new User();
                user.setUsername(authority);
                aceResult.setUser(user);
            }
        }


        if (AuthorityType.getAuthorityType(authority).equals(AuthorityType.GROUP)) {
            NodeRef groupNodeRef = authorityService.getAuthorityNodeRef(authority);
            if (groupNodeRef == null) {
                log.debug("authority {} does not exist. will continue", authority);
                return null;
            }

            Map<QName, Serializable> groupProps = nodeService.getProperties(groupNodeRef);
            Group group = new Group();
            group.setName(authority);
            group.setDisplayName((String) groupProps.get(ContentModel.PROP_AUTHORITY_DISPLAY_NAME));
            group.setNodeId(groupNodeRef.getId());
            group.setRepositoryId(appInfo.getAppId());
            group.setAuthorityType(AuthorityType.getAuthorityType(authority).name());
            group.setScope((String) nodeService.getProperty(nodeRef, QName.createQName(CCConstants.CCM_PROP_SCOPE_TYPE)));

            NodeRef authorityNodeRef = authorityService.getAuthorityNodeRef(authority);
            if (authorityNodeRef != null) {
                String groupType = (String) nodeService.getProperty(authorityNodeRef, QName.createQName(CCConstants.CCM_PROP_GROUPEXTENSION_GROUPTYPE));
                if (groupType != null) {
                    group.setGroupType(groupType);
                    if (CCConstants.ADMINISTRATORS_GROUP_TYPE.equals(groupType) && permission.equals(PermissionService.COORDINATOR)) {
                        group.setEditable(!isSharedNode(nodeRef.getId()));
                    }
                }
            }
            aceResult.setGroup(group);
        }

        log.debug("authority{} Permission:{} ACCESS STATUS:{}isInherited:{} getInheritParentPermissions(nodeRef):{}", authority, permission, aceResult.getAccessStatus(), inherited, permissionService.getInheritParentPermissions(nodeRef));
        return aceResult;
    }


    public boolean isAdminOrSystem() {
        return Arrays.asList(AuthenticationUtil.SYSTEM_USER_NAME, ApplicationInfoList.getHomeRepository().getUsername()).contains(AuthenticationUtil.getFullyAuthenticatedUser()) || AuthenticationUtil.isRunAsUserTheSystemUser() || AuthorityServiceHelper.isAdmin();
    }

    @Override
    public List<String> getPermissionsForAuthority(String nodeId, String authorityId, Collection<String> permissions) throws InsufficientPermissionException {
        if (!authorityId.equals(AuthenticationUtil.getFullyAuthenticatedUser())) {
            if (!isAdminOrSystem()) {
                if (!hasPermission(StoreRef.PROTOCOL_WORKSPACE, StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(), nodeId, PermissionService.READ_PERMISSIONS)) {
                    throw new InsufficientPermissionException("Current user is missing " + PermissionService.READ_PERMISSIONS + " for this node");
                }
            }
        }

        if (!CCConstants.AUTHORITY_GROUP_EVERYONE.equals(authorityId) && !"System".equals(authorityId) && !authorityService.authorityExists(authorityId)) {
            throw new IllegalArgumentException("Authority " + authorityId + " does not exist");
        }
        return AuthenticationUtil.runAs(() -> {
            List<String> result = new ArrayList<>();
            NodeRef nodeRef = new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, nodeId);

            for (String permission : permissions) {
                if (permissionService.hasPermission(nodeRef, permission).equals(AccessStatus.ALLOWED)) {
                    result.add(permission);
                }
            }
            return result;
        }, CCConstants.AUTHORITY_GROUP_EVERYONE.equals(authorityId) ? AuthenticationUtil.getGuestUserName() : authorityId);
    }

    /**
     * return explicitly set permissions for this node
     * Inherited or permissions from groups are ignored
     */
    @Override
    public List<String> getExplicitPermissionsForAuthority(String nodeId, String authorityId) throws InsufficientPermissionException {
        if (!authorityId.equals(AuthenticationUtil.getFullyAuthenticatedUser()) && !isAdminOrSystem()) {
            if (!getPermissionsForAuthority(nodeId, AuthenticationUtil.getFullyAuthenticatedUser())
                    .contains(PermissionService.READ_PERMISSIONS)) {
                throw new InsufficientPermissionException("Current user is missing " + PermissionService.READ_PERMISSIONS + " for this node");
            }
        }

        if (!CCConstants.AUTHORITY_GROUP_EVERYONE.equals(authorityId) && !authorityService.authorityExists(authorityId)) {
            throw new IllegalArgumentException("Authority " + authorityId + " does not exist");
        }
        NodeRef nodeRef = new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, nodeId);
        List<String> result = new ArrayList<>();
        Set<AccessPermission> permissions = permissionService.getAllSetPermissions(nodeRef);
        for (AccessPermission permission : permissions) {
            if (permission.getAuthority().equals(authorityId) &&
                    CCConstants.getPermissionList().contains(permission.getPermission())) {
                result.add(permission.getPermission());
            }
        }
        return result;
    }

    @Override
    public void setPermission(String nodeId, String authority, String permission) {
        permissionService.setPermission(new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, nodeId), authority, permission, true);
    }
}
