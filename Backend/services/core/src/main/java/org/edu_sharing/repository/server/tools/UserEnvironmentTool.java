/**
 *
 */
package org.edu_sharing.repository.server.tools;

import lombok.extern.slf4j.Slf4j;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.edu_sharing.alfresco.workspace_administration.NodeServiceInterceptor;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.MCAlfrescoAPIClient;
import org.edu_sharing.service.NotAnAdminException;
import org.edu_sharing.service.nodeservice.NodeService;
import org.edu_sharing.service.nodeservice.NodeServiceFactory;
import org.jetbrains.annotations.NotNull;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class UserEnvironmentTool {

    private final MCAlfrescoAPIClient mcBaseClient = new MCAlfrescoAPIClient();
    private final String username;

    private final NodeService nodeService;

    public UserEnvironmentTool() {
        this(AuthenticationUtil.getFullyAuthenticatedUser());
    }

    public void createAllSystemFolders() {
        AuthenticationUtil.runAsSystem(() -> {
            getEdu_SharingSystemFolderBase();
            getEdu_SharingSystemFolderUpdate();
            getEdu_SharingConfigFolder();
            getEdu_SharingContextFolder();
            getEdu_SharingMediacenterFolder();
            getEdu_SharingReportsFolder();
            getEdu_SharingNotifyFolder();
            getEdu_SharingServiceFolder();
            getEdu_SharingTemplateFolder();
            getEdu_SharingValuespaceFolder();
            getEdu_SharingTopicPageTemplatesFolder();
            return null;
        });
    }

    /**
     * use this for running this class in an runAs context
     */
    public UserEnvironmentTool(String runAsUser) {
        this(NodeServiceFactory.getInstance().getLocalService(), runAsUser);
    }

    public UserEnvironmentTool(NodeService nodeService) {
        this(nodeService, AuthenticationUtil.getFullyAuthenticatedUser());
    }

    public UserEnvironmentTool(NodeService nodeService, String runAsUser) {
        username = runAsUser;
        this.nodeService = nodeService;
    }

    public UserEnvironmentTool(Map<String, String> authInfo) {
        this(NodeServiceFactory.getInstance().getLocalService(), authInfo);
    }

    public UserEnvironmentTool(NodeService nodeService, Map<String, String> authInfo) {
        this(nodeService, authInfo.get(CCConstants.AUTH_USERNAME));
    }

    public String getDefaultUserDataFolder() {

        String homeFolderId = mcBaseClient.getHomeFolderID(username);
        log.info("homefolder:{}", homeFolderId);

        Map<String, Object> defaultDataFolderProps = mcBaseClient.getChild(homeFolderId, CCConstants.CCM_TYPE_MAP, CCConstants.CCM_PROP_MAP_TYPE, CCConstants.CCM_VALUE_MAP_TYPE_DOCUMENTS);
        if (defaultDataFolderProps == null) {
            log.error("something went wrong! no datafolder for current user {} found!", username);
            return null;
        }

        return (String) defaultDataFolderProps.get(CCConstants.SYS_PROP_NODE_UID);
    }

    public String getDefaultImageFolder() {

        String homeFolderId = mcBaseClient.getHomeFolderID(username);
        log.info("homefolder:{}", homeFolderId);


        Map<String, Object> defaultImageFolderProps = mcBaseClient.getChild(homeFolderId, CCConstants.CCM_TYPE_MAP, CCConstants.CCM_PROP_MAP_TYPE, CCConstants.CCM_VALUE_MAP_TYPE_IMAGES);
        if (defaultImageFolderProps == null) {
            log.error("something went wrong! no image folder for current user {} found!", username);
            return null;
        }

        return (String) defaultImageFolderProps.get(CCConstants.SYS_PROP_NODE_UID);
    }

    public String getEdu_SharingSystemFolderBase() {
        if (!mcBaseClient.isAdmin() && !AuthenticationUtil.isRunAsUserTheSystemUser()) {
            throw new RuntimeException("Admin group required");
        }

        String companyHomeNodeId = mcBaseClient.getCompanyHomeNodeId();
        NodeRef edu_SharingSysMap = nodeService.getChild(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, companyHomeNodeId, CCConstants.CCM_TYPE_MAP, CCConstants.CCM_PROP_MAP_TYPE, CCConstants.CCM_VALUE_MAP_TYPE_EDU_SHARING_SYSTEM);
        if (edu_SharingSysMap != null) {
            return edu_SharingSysMap.getId();
        }

        String systemFolderName = I18nServer.getTranslationDefaultResourcebundle(CCConstants.I18n_SYSTEMFOLDER_BASE);
        Map<String, Object> newEdu_SharingSysMapProps = new HashMap<>();
        newEdu_SharingSysMapProps.put(CCConstants.CM_NAME, systemFolderName);
        newEdu_SharingSysMapProps.put(CCConstants.CM_PROP_C_TITLE, getLocalizedProperties(CCConstants.I18n_SYSTEMFOLDER_BASE));
        newEdu_SharingSysMapProps.put(CCConstants.CCM_PROP_MAP_TYPE, CCConstants.CCM_VALUE_MAP_TYPE_EDU_SHARING_SYSTEM);
        return mcBaseClient.createNode(companyHomeNodeId, CCConstants.CCM_TYPE_MAP, newEdu_SharingSysMapProps);
    }

    @NotNull
    @SuppressWarnings("deprecation")
    private static Map<String, String> getLocalizedProperties(String i18nId) {
        Map<String, String> i18nTitle = new HashMap<>();
        i18nTitle.put("de_DE", I18nServer.getTranslationDefaultResourcebundle(i18nId, "de_DE"));
        i18nTitle.put("en_EN", I18nServer.getTranslationDefaultResourcebundle(i18nId, "en_EN"));
        i18nTitle.put("en_US", I18nServer.getTranslationDefaultResourcebundle(i18nId, "en_US"));
        return i18nTitle;
    }

    public String getEdu_SharingSystemFolderUpdate() {
        if (!mcBaseClient.isAdmin() && !AuthenticationUtil.isRunAsUserTheSystemUser()) {
            throw new NotAnAdminException();
        }
        return getOrCreateSystemFolderByName(CCConstants.CCM_VALUE_MAP_TYPE_EDU_SHARING_SYSTEM_UPDATE, CCConstants.I18n_SYSTEMFOLDER_UPDATE);
    }

    public String getEdu_SharingNotifyFolder() {
        String systemFolderId = getEdu_SharingSystemFolderBase();

        String currentScope = NodeServiceInterceptor.getEduSharingScope();

        String systemFolderName = I18nServer.getTranslationDefaultResourcebundle(CCConstants.I18n_SYSTEMFOLDER_NOTIFY);
        systemFolderName = (currentScope == null || currentScope.trim().isEmpty()) ? systemFolderName : systemFolderName + "_" + currentScope;
        NodeRef edu_SharingSystemFolderNotify = nodeService.getChild(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, systemFolderId, CCConstants.CCM_TYPE_MAP, CCConstants.CM_NAME, systemFolderName);
        if (edu_SharingSystemFolderNotify != null) {
            return edu_SharingSystemFolderNotify.getId();
        }

        Map<String, Object> newEdu_SharingSysMapProps = new HashMap<>();
        newEdu_SharingSysMapProps.put(CCConstants.CM_NAME, systemFolderName);
        newEdu_SharingSysMapProps.put(CCConstants.CM_PROP_C_TITLE, getLocalizedProperties(CCConstants.I18n_SYSTEMFOLDER_NOTIFY));
        newEdu_SharingSysMapProps.put(CCConstants.CCM_PROP_MAP_TYPE, CCConstants.CCM_VALUE_MAP_TYPE_EDU_SHARING_SYSTEM_NOTIFY);
        return mcBaseClient.createNode(systemFolderId, CCConstants.CCM_TYPE_MAP, newEdu_SharingSysMapProps);
    }

    public String getEdu_SharingContextFolder() {
        return getOrCreateSystemFolderByName(CCConstants.CCM_VALUE_MAP_TYPE_EDU_SHARING_SYSTEM_SERVICE, CCConstants.I18n_SYSTEMFOLDER_CONTEXT);
    }

    public String getEdu_SharingConfigFolder() {
        return getOrCreateSystemFolderByName(CCConstants.CCM_VALUE_MAP_TYPE_EDU_SHARING_SYSTEM_SERVICE, CCConstants.I18n_SYSTEMFOLDER_CONFIG);
    }

    public String getEdu_SharingMediacenterFolder() {
        return getOrCreateSystemFolderByName(CCConstants.CCM_VALUE_MAP_TYPE_EDU_SHARING_SYSTEM_MEDIACENTER, CCConstants.I18n_SYSTEMFOLDER_MEDIACENTER);
    }

    public String getEdu_SharingReportsFolder() {
        return getOrCreateSystemFolderByName(CCConstants.CCM_VALUE_MAP_TYPE_EDU_SHARING_SYSTEM_REPORTS, CCConstants.I18n_SYSTEMFOLDER_REPORTS);
    }

    public String getEdu_SharingTopicPageTemplatesFolder() {
        return getOrCreateSystemFolderByName(CCConstants.CCM_VALUE_MAP_TYPE_EDU_SHARING_SYSTEM_TOPIC_PAGE_TEMPLATES, CCConstants.I18n_SYSTEMFOLDER_TOPIC_PAGE_TEMPLATES, true);
    }

    public String getEdu_SharingServiceFolder() {
        return getOrCreateSystemFolderByName(CCConstants.CCM_VALUE_MAP_TYPE_EDU_SHARING_SYSTEM_SERVICE, CCConstants.I18n_SYSTEMFOLDER_SERVICE);
    }

    public String getEdu_SharingTemplateFolder() {
        return getOrCreateSystemFolderByName(CCConstants.CCM_VALUE_MAP_TYPE_EDU_SHARING_SYSTEM_TEMPLATE, CCConstants.I18n_SYSTEMFOLDER_TEMPLATE);
    }

    public String getEdu_SharingOrganizationDeleteProtocolFolder() {
        return getOrCreateSystemFolderByName(CCConstants.CCM_VALUE_MAP_TYPE_EDU_SHARING_SYSTEM_ORG_DELETE_PROTOCOL, CCConstants.I18n_SYSTEMFOLDER_ORG_DELETE_PROTOCOL);
    }

    public String getEdu_SharingGdprFolder() {
        return getOrCreateSystemFolderByName(CCConstants.CCM_VALUE_MAP_TYPE_EDU_SHARING_SYSTEM_GDPR, CCConstants.I18n_SYSTEMFOLDER_GDPR);
    }

    public String getEdu_SharingAssignmentFolder() {
        return getOrCreateSystemFolderByName(CCConstants.CCM_VALUE_MAP_TYPE_EDU_SHARING_SYSTEM_ASSIGNMENT, CCConstants.I18n_SYSTEMFOLDER_ASSIGNMENT);
    }


    public String getOrCreateSystemFolderByName(String constantName, String i18nId) {
        return getOrCreateSystemFolderByName(constantName, i18nId, false);
    }
    public String getOrCreateSystemFolderByName(String mapType, String i18nFolderNameId, boolean publicRead) {
        String systemFolderId = getEdu_SharingSystemFolderBase();
        Map<String, Object> edu_SharingSystemFolderTemplate = mcBaseClient.getChild(systemFolderId, CCConstants.CCM_TYPE_MAP, CCConstants.CCM_PROP_MAP_TYPE, mapType);
        if (edu_SharingSystemFolderTemplate != null) {
            return (String) edu_SharingSystemFolderTemplate.get(CCConstants.SYS_PROP_NODE_UID);
        }

        String systemFolderName = I18nServer.getTranslationDefaultResourcebundle(i18nFolderNameId);
        Map<String, Object> newEdu_SharingSysMapProps = new HashMap<>();
        newEdu_SharingSysMapProps.put(CCConstants.CM_NAME, systemFolderName);
        newEdu_SharingSysMapProps.put(CCConstants.CM_PROP_C_TITLE, getLocalizedProperties(i18nFolderNameId));
        newEdu_SharingSysMapProps.put(CCConstants.CCM_PROP_MAP_TYPE, mapType);
        String folderId = mcBaseClient.createNode(systemFolderId, CCConstants.CCM_TYPE_MAP, newEdu_SharingSysMapProps);
        if (publicRead) {
            try {
                nodeService.setPermissions(folderId, CCConstants.AUTHORITY_GROUP_EVERYONE, new String[]{CCConstants.PERMISSION_CONSUMER}, null);
            } catch (Exception e) {
                log.warn("Could not set public read permission on system folder {}", folderId, e);
            }
        }
        return folderId;
    }

    /**
     * returns a folder where notify Objects can be safed
     */
    public String getEdu_SharingNotifyFolderToSafe() {
        String notifyFolder = getEdu_SharingNotifyFolder();

        String year = Integer.toString(Calendar.getInstance().get(Calendar.YEAR));
        String yearMapId = getMap(notifyFolder, year, CCConstants.CCM_VALUE_MAP_TYPE_EDU_SHARING_SYSTEM_NOTIFY);

        String month = Integer.toString(Calendar.getInstance().get(Calendar.MONTH));
        String monthMapId = getMap(yearMapId, month, CCConstants.CCM_VALUE_MAP_TYPE_EDU_SHARING_SYSTEM_NOTIFY);

        String day = Integer.toString(Calendar.getInstance().get(Calendar.DAY_OF_MONTH));

        return getMap(monthMapId, day, CCConstants.CCM_VALUE_MAP_TYPE_EDU_SHARING_SYSTEM_NOTIFY);
    }

    public String getEdu_SharingValuespaceFolder() {
        return getOrCreateSystemFolderByName(CCConstants.CCM_VALUE_MAP_TYPE_EDU_SHARING_SYSTEM_VALUESPACE, CCConstants.I18n_SYSTEMFOLDER_VALUESPACE);
    }


    /**
     * returns the child map with name. when it does not exist it will be created
     */
    private String getMap(String parentId, String name, String mapType) {
        NodeRef child = nodeService.getChild(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, parentId, CCConstants.CCM_TYPE_MAP, CCConstants.CM_NAME, name);
        if (child == null) {
            Map<String, Object> props = new HashMap<>();
            props.put(CCConstants.CM_NAME, name);
            props.put(CCConstants.CM_PROP_TITLE, name);
            if (mapType != null) {
                props.put(CCConstants.CCM_PROP_MAP_TYPE, mapType);
            }
            return mcBaseClient.createNode(parentId, CCConstants.CCM_TYPE_MAP, props);
        } else {
            return child.getId();
        }
    }


}
