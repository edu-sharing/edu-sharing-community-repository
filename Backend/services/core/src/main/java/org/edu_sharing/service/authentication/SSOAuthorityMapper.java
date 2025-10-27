package org.edu_sharing.service.authentication;

import com.typesafe.config.Config;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationException;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.security.AuthorityType;
import org.alfresco.service.cmr.security.MutableAuthenticationService;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.transaction.TransactionService;
import org.apache.commons.lang3.StringUtils;
import org.edu_sharing.alfresco.authentication.HttpContext;
import org.edu_sharing.alfresco.lightbend.LightbendConfigLoader;
import org.edu_sharing.alfresco.service.OrganisationService;
import org.edu_sharing.alfresco.service.guest.GuestService;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.lightbend.ConfigurationProperties;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.repository.server.tools.KeyTool;
import org.edu_sharing.repository.server.tools.security.RunAsSystem;
import org.edu_sharing.repository.server.tools.transaction.RetryingTransaction;
import org.edu_sharing.service.authentication.sso.mapping.CustomGroupMapping;
import org.edu_sharing.service.authentication.sso.mapping.Mapping;
import org.edu_sharing.service.authentication.sso.mapping.UserNameMapping;
import org.edu_sharing.spring.scope.refresh.annotations.RefreshScope;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.regex.Pattern;

/**
 *
 * @author rudi
 * <p>
 * this class does the job of creating and updating users and organizing
 * group membership in an sso context
 * <p>
 * the use of this class requires an preciding sso authentication
 * process that delivers user data and group memberships i.e.: -
 * shibboleth serviceprovider - cas - edu-sharing auth by app
 * <p>
 * sso data will be mapped. mapping can be configured in
 * edu-sharing-sso-context.xml
 *
 */
@Slf4j
@Component
@RefreshScope
@RequiredArgsConstructor
public class SSOAuthorityMapper {
    /**
     * SSO type Shibboleth, AuthByApp
     */
    public static final String PARAM_SSO_TYPE = "SSO_TYPE";
    public static final String PARAM_SSO_OAUTH_REG_KEY = "OAUTH_REG_KEY";
    public static final String PARAM_SSO_OAUTH_CONTEXT = "OAUTH_CONTEXT";
    public static final String PARAM_AUTHBYAPP_APP_IP = "APP_IP";
    public static final String PARAM_AUTHBYAPP_APP_ID = "APP_ID";


    public static final String SSO_TYPE_OAUTH = "oAuth";
    public static final String SSO_TYPE_SAML2 = "saml2";
    public static final String SSO_TYPE_EXTERNAL = "external";
    public static final String SSO_TYPE_AuthByApp = "auth_by_app";
    public static final String SSO_TYPE_LTI = "lti";

    public static final String SSO_REFERER = "SSO_REFERER";

    private static final List<String> shibbolethAuthTypes = List.of(SSO_TYPE_OAUTH, SSO_TYPE_SAML2, SSO_TYPE_EXTERNAL);


    public static boolean isShibbolethAuthType(String authType) {
        return shibbolethAuthTypes.contains(authType);
    }

    @Data
    @ConfigurationProperties(prefix = "edu-sharing.sso.authority-mapper")
    public static class Configuration {
        private boolean createUser = true;
        private boolean updateUser = true;
        private boolean createGroups = true;
        private boolean hashUserName = false;
        private boolean hashGroupNames = false;
        private boolean updateMemberships = true;
        private boolean debug = false;
        private String mappingGroupBuilderClass;
        private boolean authByAppCheckClientIp = true;
        private String organisationParam;
        private String globalGroupsParam;
    }


    private final AuthorityService authorityService;
    private final PersonService personService;
    private final MutableAuthenticationService authenticationService;
    private final TransactionService transactionService;
    private final OrganisationService organisationService;
    private final NodeService nodeService;
    private final GuestService guestService;
    private final LightbendConfigLoader lightbendConfigLoader;
    private final SSOMappingProvider ssoMappingProvider;
    private final Configuration mappingConfig;
    private final Optional<CustomGroupMapping> customGroupMapping;
    private final Optional<UserNameMapping> userNameMapping;


    public boolean isAuthByAppCheckClientIp() {
        return mappingConfig.isAuthByAppCheckClientIp();
    }

    public static String mapAdminAuthority(String authority, String appid) {
        // when coming from the native app, do not scope
        if (ApplicationInfoList.getHomeRepository().getAppId().equals(appid)) {
            return authority;
        }
        return AuthenticationUtil.runAsSystem(() -> {
            ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
            ServiceRegistry serviceRegistry = (ServiceRegistry) applicationContext.getBean("ServiceRegistry");

            boolean scope;
            // a new person, does not need to be scoped
            if (!serviceRegistry.getPersonService().personExists(authority)) {
                scope = false;
            } // the main user (admin) has to be scoped
            else if (authority.trim().equals(ApplicationInfoList.getHomeRepository().getUsername())) {
                scope = true;
            } // the user has to be scoped if he/she is an admin
            else {
                Set<String> memberships = serviceRegistry.getAuthorityService().getAuthoritiesForUser(authority);
                scope = memberships != null && memberships.contains(CCConstants.AUTHORITY_GROUP_ALFRESCO_ADMINISTRATORS);
            }

            if (scope) {
                return authority + "@" + appid;
            } else {
                return authority;
            }
        });
    }


    /**
     * @return username: means the user exists (before or was created) null when
     * the user does not exist and cannot be created
     */
    @RunAsSystem
    @RetryingTransaction
    public String mapAuthority(final Map<String, String> ssoAttributes) {
        final Mapping mapping = ssoMappingProvider.getMapping(ssoAttributes);
        if (mappingConfig.isDebug()) {
            for (Map.Entry<String, String> ssoAttribute : ssoAttributes.entrySet()) {
                log.info("sso attribute: {} value: {}", ssoAttribute.getKey(), ssoAttribute.getValue());
            }
        }

        String tmpUserName = ssoAttributes.get(getSSOUsernameProp(ssoAttributes));
        if (StringUtils.isBlank(tmpUserName)) {
            logErrorParams("userName", ssoAttributes);
            throw new AuthenticationException(AuthenticationExceptionMessages.MISSING_PARAM);
        }

        //guest does not exsist in user store but exsist as a person, so user will not be found and trying to create person -> user already exsists
        if (guestService.isGuestUser(tmpUserName)) {
            return tmpUserName;
        }

        if (userNameMapping.isPresent()) {
            tmpUserName = userNameMapping.get().apply(tmpUserName);
        }

        String ssoType = ssoAttributes.get(PARAM_SSO_TYPE);
        if (ssoType == null) {
            logErrorParams(PARAM_SSO_TYPE, ssoAttributes);
            throw new AuthenticationException(AuthenticationExceptionMessages.MISSING_PARAM);
        }

        // TODO can be deleted?
//        if (requiredAttributes != null && !requiredAttributes.isEmpty()) {
//
//            requiredAttributes.keySet().forEach(r -> {
//                String value = ssoAttributes.get(r);
//                if (value == null)
//                    throw new AuthenticationException(AuthenticationExceptionMessages.MISSING_PARAM);
//                try {
//                    if (!value.matches(requiredAttributes.get(r))) {
//                        log.debug("required attribute " + r + " " + value + " not matches " + requiredAttributes.get(r));
//                        throw new AuthenticationException(AuthenticationExceptionMessages.SSO_REQ_ATT_NOT_MATCHES);
//                    }
//                } catch (PatternSyntaxException e) {
//                    log.error("wrong required attribute pattern for:" + r + " pattern:" + requiredAttributes.get(r) + ". " + e.getMessage());
//                    throw new AuthenticationException(AuthenticationExceptionMessages.SSO_REQ_ATT_PATTERN_SYNTAX);
//                }
//            });
//        }

        String appId = ssoAttributes.get(PARAM_AUTHBYAPP_APP_ID);
        Optional<ApplicationInfo> appInfo = Optional.ofNullable(appId).map(ApplicationInfoList::getRepositoryInfoById);
        boolean whitelistedUser = false;
        if (SSO_TYPE_AuthByApp.equals(ssoType)) {
            String userWhiteList = appInfo.map(ApplicationInfo::getAuthByAppUserWhitelist).orElse(null);
            if (StringUtils.isNotBlank(userWhiteList)) {
                List<String> userList = Arrays.asList(userWhiteList.split(","));
                whitelistedUser = true;
                if (!userList.contains(tmpUserName)) {
                    throw new AuthenticationException(AuthenticationExceptionMessages.NOT_IN_WHITELIST);
                }
            }
        }

        if (SSO_TYPE_AuthByApp.equals(ssoType)) {
            tmpUserName = mapAdminAuthority(tmpUserName, ssoAttributes.get(PARAM_AUTHBYAPP_APP_ID));
        }


        final String originalUsername = tmpUserName;

        // moodle hashes the username
        final String userName = (mappingConfig.isHashUserName() && !ssoType.equals(SSO_TYPE_AuthByApp)) ? digest(tmpUserName) : tmpUserName;


        try {
            boolean createUser = mappingConfig.isCreateUser();
            boolean updateUser = mappingConfig.isUpdateUser();

            boolean createGroups = mappingConfig.isCreateGroups();
            boolean hashGroupNames = mappingConfig.isHashGroupNames();
            boolean updateMemberships = mappingConfig.isUpdateMemberships();

            if (whitelistedUser) {
                createUser = false;
                updateUser = false;
                createGroups = false;
                hashGroupNames = false;
                updateMemberships = false;
            }

            /*
             * SSO_TYPE_AuthByApp: need the Application type to
             * decide if Crud Operations on user and group are
             * allowed
             */
            if (ssoType.equals(SSO_TYPE_AuthByApp)) {

                if (StringUtils.isBlank(appId)) {
                    logErrorParams(PARAM_AUTHBYAPP_APP_ID, ssoAttributes);
                    throw new AuthenticationException(AuthenticationExceptionMessages.MISSING_PARAM);
                }

                if (ApplicationInfo.TYPE_RENDERSERVICE.equals(appInfo.map(ApplicationInfo::getType).orElse(null))) {
                    createUser = false;
                    updateUser = false;
                    createGroups = false;
                    hashGroupNames = false;
                    updateMemberships = false;
                }
            }


            boolean personExists = personService.personExists(userName);
            log.debug("ut status:{}", transactionService.getUserTransaction().getStatus());

            if (!personExists && !createUser) {
                log.info("personExists == null && !createUser -> returning null");
                return null;
            }

            // person
            Map<String, String> personMapping = mapping.getPerson();
            Map<QName, Serializable> personProperties = new HashMap<>();

            for (Map.Entry<String, String> ssoAttribute : ssoAttributes.entrySet()) {

                if (StringUtils.isBlank(personMapping.get(ssoAttribute.getKey()))) {
                    log.debug("missing mapping entry for sso person attribute {}", ssoAttribute.getKey());
                    continue;
                }

                QName alfrescoProperty = QName.createQName(personMapping.get(ssoAttribute.getKey()));
                personProperties.put(alfrescoProperty, ssoAttribute.getValue());
            }

            if (!personProperties.isEmpty()) {
                // TODO can be deleted?
                // if (mappingConfig.getPersonMappingCondition() != null && !mappingConfig.getPersonMappingCondition().isTrue(ssoAttributes)) {
                //     log.info("PersonMappingCondition is false for user:{}. will not create.", userName);
                //     return null;
                // }
                Config config = lightbendConfigLoader.getConfig();

                //check active status
                if (personExists) {

                    if (!config.getIsNull("repository.personActiveStatus")) {
                        String personActiveStatus = config.getString("repository.personActiveStatus");
                        NodeRef nodeRefPerson = personService.getPerson(userName);
                        String personStatus = (String) nodeService.getProperty(nodeRefPerson, QName.createQName(CCConstants.CM_PROP_PERSON_ESPERSONSTATUS));
                        if (!personActiveStatus.equals(personStatus)) {
                            throw new AuthenticationException(AuthenticationExceptionMessages.USER_BLOCKED);
                        }
                    }

                }

                if (!personExists) {
                    authenticationService.createAuthentication(userName, new KeyTool().getRandomPassword().toCharArray());
                    //authenticationDao.createUser(userName, new KeyTool().getRandomPassword().toCharArray());

                    // set username to the same we get from sso
                    // context
                    personProperties.put(ContentModel.PROP_USERNAME, userName);

                    //so we can find out where the user comes from
                    if (appInfo.isPresent() && ApplicationInfo.TYPE_REPOSITORY.equals(appInfo.map(ApplicationInfo::getType).orElse(null))) {
                        personProperties.put(QName.createQName(CCConstants.PROP_USER_REPOSITORYID), appInfo.map(ApplicationInfo::getAppId).orElse(null));
                    }

                    personProperties.put(QName.createQName(CCConstants.PROP_USER_ESSSOTYPE), ssoType);

                    if (mappingConfig.isHashUserName()) {
                        personProperties.put(QName.createQName(CCConstants.CM_PROP_PERSON_ESORIGINALUID), originalUsername);
                    }

                    if (!config.getIsNull("repository.personActiveStatus")) {
                        String personActiveStatus = config.getString("repository.personActiveStatus");
                        //if configured, initialize with active status
                        if (personActiveStatus != null && !personActiveStatus.trim().isEmpty()) {
                            personProperties.put(QName.createQName(CCConstants.CM_PROP_PERSON_ESPERSONSTATUS), personActiveStatus);
                        }
                    }


                    personService.createPerson(personProperties);
                } else if (updateUser) {

                    //don't update the username (this leads to lowercase username when lowercase username comes with sso data
                    personProperties.remove(ContentModel.PROP_USERNAME);
                    personService.setPersonProperties(userName, personProperties);
                }


            } else {
                log.warn("no personproperties delivered by sso context for user {}", userName);
            }

            /*
             * to get the existent username, this can be different to the parameter username
             * when this has another case(lower/upper) than the person object username
             */
            NodeRef personNodeRef = personService.getPersonOrNull(userName);
            if (personNodeRef == null) {
                log.error("person {} does not exist. can not create authentication object in userstore.", userName);
                return null;
            }
            String existentUserName = (String) nodeService.getProperty(personNodeRef, ContentModel.PROP_USERNAME);

            if (!authenticationService.authenticationExists(existentUserName)) {
                log.info("no authentication object for {} trying to create!", userName);
                authenticationService.createAuthentication(existentUserName, new KeyTool().getRandomPassword().toCharArray());
            }
            // group memberships
            List<Mapping.Group> mappingGroups = new ArrayList<>(mapping.getGroup().values());

            // * add moodle global groups
            String lmsGlobalGroups = (mappingConfig.getGlobalGroupsParam() != null) ? ssoAttributes.get(mappingConfig.getGlobalGroupsParam()) : null;

            //only if organisationparam is configured
            String organisationName = (mappingConfig.getOrganisationParam() != null) ? ssoAttributes.get(mappingConfig.getOrganisationParam()) : null;
            String organisationDisplayName = null;

            String existingOrganisationName = null;

            if (customGroupMapping.isPresent()) {
                customGroupMapping.get().setSSOAuthorityMapper(SSOAuthorityMapper.this);
                customGroupMapping.get().map(ssoAttributes);
            }

            // * create eduGroup for affiliation
            if (StringUtils.isNotBlank(organisationName)) {

                organisationDisplayName = ssoAttributes.get(mappingConfig.getOrganisationParam() + "name");

                if (organisationDisplayName == null) {
                    organisationDisplayName = organisationName;
                }

                Map<QName, Serializable> orgProps = organisationService.getOrganisation(organisationName);
                if (orgProps != null) {
                    existingOrganisationName = (String) orgProps.get(ContentModel.PROP_AUTHORITY_NAME);
                }

                if (existingOrganisationName == null) {

                    String metadataSetId = isShibbolethAuthType(ssoType) ? HttpContext.getCurrentMetadataSet() : null;

                    existingOrganisationName = organisationService.createOrganization(organisationName, organisationDisplayName, metadataSetId, null);
                    existingOrganisationName = AuthorityType.GROUP.getPrefixString() + existingOrganisationName;
                } else {
                    String currentDisplayname = (String) orgProps.get(ContentModel.PROP_AUTHORITY_DISPLAY_NAME);
                    if (currentDisplayname == null || !currentDisplayname.equals(organisationDisplayName)) {
                        authorityService.setAuthorityDisplayName((String) orgProps.get(ContentModel.PROP_AUTHORITY_NAME), organisationDisplayName);
                    }
                }

                if (updateMemberships) {
                    Set<String> userAuthorities = authorityService.getAuthoritiesForUser(userName);
                    if (!userAuthorities.contains(existingOrganisationName)) {
                        authorityService.addAuthority(existingOrganisationName, userName);
                    }
                }

            }

            // create LMS globalGroups
            organisationName = (organisationName == null) ? "" : organisationName;
            if (StringUtils.isNotBlank(lmsGlobalGroups)) {
                JSONArray globalGroupsJA = new JSONArray(lmsGlobalGroups);
                List<Mapping.Group> lmsGlobalGroupsList = new ArrayList<>();
                for (int i = 0; i < globalGroupsJA.length(); i++) {
                    JSONObject globalGroupJO = (JSONObject) globalGroupsJA.get(i);
                    String name = globalGroupJO.getString("name");
                    String groupName = organisationName + "_" + name;
                    String groupDisplayName = name + " (" + organisationDisplayName + ")";

                    Mapping.Group mappingGroup = new Mapping.Group();
                    mappingGroup.setGroup(groupName);
                    mappingGroup.setDisplayName(groupDisplayName);
                    mappingGroup.setMatcher(".*"); // -> anymatch
                    lmsGlobalGroupsList.add(mappingGroup);
                }


                mappingGroups.addAll(lmsGlobalGroupsList);
            }


            List<String> currentGroupsForUser = new ArrayList<>();
            for (Mapping.Group mappingGroup : mappingGroups) {

                String groupName = mappingGroup.getGroup();
                String alfrescoGroupName = AuthorityType.GROUP.getPrefixString() + groupName;
                if (hashGroupNames) {
                    groupName = digest(groupName);
                }

                if (StringUtils.isBlank(groupName)) {
                    log.error("alfresco groupName is null or length 0");
                    continue;
                }

                if (isMatchingGroup(ssoAttributes, mappingGroup)) {

                    currentGroupsForUser.add(alfrescoGroupName);

                    if (createGroups && !authorityService.authorityExists(AuthorityType.GROUP.getPrefixString() + groupName)) {
                        String newGroupName = authorityService.createAuthority(AuthorityType.GROUP, mappingGroup.getGroup(), mappingGroup.getDisplayName(), authorityService.getDefaultZones());
                        NodeRef nodeRef = authorityService.getAuthorityNodeRef(newGroupName);

                        Map<QName, Serializable> aspectProperties = new HashMap<>();
                        aspectProperties.put(QName.createQName(CCConstants.CCM_PROP_GROUPEXTENSION_GROUPSOURCE), appId);
                        nodeService.addAspect(nodeRef, QName.createQName(CCConstants.CCM_ASPECT_GROUPEXTENSION), aspectProperties);
                    }

                    if (updateMemberships) {

                        if (authorityService.authorityExists(alfrescoGroupName)) {

                            Set<String> authoritiesForUser = authorityService.getAuthoritiesForUser(userName);
                            if (!authoritiesForUser.contains(alfrescoGroupName)) {
                                log.debug("will add " + userName + " in " + alfrescoGroupName);
                                authorityService.addAuthority(alfrescoGroupName, userName);
                            }
                        } else {
                            log.error("Authority " + groupName + " does not exist!");
                        }
                    }

                } else {

                    // remove memberships for group mapping defined in edu-sharing-sso-context.xml
                    if (updateMemberships) {
                        log.debug("condition for alfresco group " + mappingGroup.getGroup() + " does not match will remove membership if exists");
                        Set<String> authoritiesForUser = authorityService.getAuthoritiesForUser(userName);
                        if (authoritiesForUser.contains(alfrescoGroupName)) {
                            log.debug("will remove " + userName + " from " + alfrescoGroupName);
                            authorityService.removeAuthority(alfrescoGroupName, userName);
                        }
                    }
                }
            }

            // removeuser from groups that came from the lms or other application but the user is no longer in
            if (updateMemberships) {
                Set<String> authoritiesForUser = authorityService.getAuthoritiesForUser(userName);
                for (String authorityForUser : authoritiesForUser) {
                    NodeRef groupNodeRef = authorityService.getAuthorityNodeRef(authorityForUser);
                    if (appId == null || groupNodeRef == null) {
                        continue;
                    }
                    String groupSource = (String) nodeService.getProperty(groupNodeRef, QName.createQName(CCConstants.CCM_PROP_GROUPEXTENSION_GROUPSOURCE));

                    if (groupSource != null && !groupSource.trim().isEmpty() && appId.equals(groupSource)) {
                        if (!currentGroupsForUser.contains(authorityForUser)) {
                            authorityService.removeAuthority(authorityForUser, userName);
                        }
                    }

                }
            }

            //handle parent group
            for (Mapping.Group mappingGroup : mappingGroups) {
                if (isMatchingGroup(ssoAttributes, mappingGroup)) {
                    if (StringUtils.isNotBlank(mappingGroup.getParentGroup())) {
                        log.debug("checking if {} is in {}", mappingGroup.getGroup(), mappingGroup.getParentGroup());
                        Set<String> containedAuthorities = authorityService.getContainedAuthorities(AuthorityType.GROUP, AuthorityType.GROUP.getPrefixString() + mappingGroup.getParentGroup(), false);
                        if (!containedAuthorities.contains(AuthorityType.GROUP.getPrefixString() + mappingGroup.getGroup())) {
                            log.info("adding:{} to:{}", mappingGroup.getGroup(), mappingGroup.getParentGroup());
                            authorityService.addAuthority(AuthorityType.GROUP.getPrefixString() + mappingGroup.getParentGroup(), AuthorityType.GROUP.getPrefixString() + mappingGroup.getGroup());
                        }
                    } else {

                        // add to organization
                        if (existingOrganisationName != null) {
                            log.debug("checking if: {} is in org: {}", mappingGroup.getGroup(), existingOrganisationName);
                            Set<String> containedAuthorities = authorityService.getContainedAuthorities(AuthorityType.GROUP, existingOrganisationName, false);
                            if (containedAuthorities != null && !containedAuthorities.contains(AuthorityType.GROUP.getPrefixString() + mappingGroup.getGroup())) {
                                log.info("adding:{} to:{}", mappingGroup.getGroup(), existingOrganisationName);
                                authorityService.addAuthority(existingOrganisationName, AuthorityType.GROUP.getPrefixString() + mappingGroup.getGroup());
                            }
                        }
                    }
                }
            }

            if (ssoType.equals(SSO_TYPE_AuthByApp)
                    && ApplicationInfo.TYPE_REPOSITORY.equals(appInfo.map(ApplicationInfo::getType).orElse(null))
                    && !ApplicationInfoList.getHomeRepository().getAppId().equals(appInfo.map(ApplicationInfo::getAppId).orElse(null))) {

                //edu-sharing federated global groups
                String gg = ssoAttributes.get(CCConstants.EDU_SHARING_GLOBAL_GROUPS);
                List<String> globalGroupsMembership = StringUtils.isNotBlank(gg) ? Arrays.asList(gg.split(";")) : new ArrayList<>();

                //remove user from global group
                Set<String> authoritiesForUser = authorityService.getAuthoritiesForUser(userName);
                for (String authority : authoritiesForUser) {
                    NodeRef authorityNodeRef = authorityService.getAuthorityNodeRef(authority);
                    //i.e. EVERYONE is null
                    if (authorityNodeRef == null) continue;
                    String scopeType = (String) nodeService.getProperty(authorityNodeRef, QName.createQName(CCConstants.CCM_PROP_SCOPE_TYPE));

                    if (CCConstants.CCM_VALUE_SCOPETYPE_GLOBAL.equals(scopeType) && !globalGroupsMembership.contains(authority)) {
                        authorityService.removeAuthority(authority, userName);
                    }
                }

                //add user to edu-sharing global groups
                for (String globalGroup : globalGroupsMembership) {
                    if (authorityService.authorityExists(globalGroup)) {
                        NodeRef authorityNodeRef = authorityService.getAuthorityNodeRef(globalGroup);
                        if (authorityNodeRef == null) continue;
                        String scopeType = (String) nodeService.getProperty(authorityNodeRef, QName.createQName(CCConstants.CCM_PROP_SCOPE_TYPE));

                        if (CCConstants.CCM_VALUE_SCOPETYPE_GLOBAL.equals(scopeType) && !authoritiesForUser.contains(globalGroup)) {
                            authorityService.addAuthority(globalGroup, userName);
                        }
                    }
                }

            }
            return userName;

        } catch (AuthenticationException e) {
            throw e;
        } catch (Throwable e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    private static boolean isMatchingGroup(Map<String, String> ssoAttributes, Mapping.Group mappingGroup) {
        return Pattern.matches(mappingGroup.getMatcher(), Optional.of(mappingGroup).map(Mapping.Group::getAttribute).map(ssoAttributes::get).orElse(null));
    }

    public String digest(String str) {
        try {

            MessageDigest m = MessageDigest.getInstance("MD5");
            m.reset();
            m.update(str.getBytes());
            byte[] digest = m.digest();

            StringBuilder hashText = new StringBuilder(new BigInteger(1, digest).toString(16));

            while (hashText.length() < 32) {
                hashText.insert(0, "0");
            }
            return hashText.toString();

        } catch (NoSuchAlgorithmException e) {
            log.error(e.getMessage(), e);
        }

        return null;
    }

    public String getSSOUsernameProp(Map<String, String> ssoAttributes) {
        return getUserAttribute(CCConstants.CM_PROP_PERSON_USERNAME, ssoAttributes);
    }

    public String getUserAttribute(String alfrescoUserAtt, Map<String, String> ssoAttributes) {
        final Mapping mapping = ssoMappingProvider.getMapping(ssoAttributes);
        return mapping.getPerson().inverseBidiMap().get(alfrescoUserAtt);
    }

    private void logErrorParams(String missing, Map<String, String> ssoAttributes) {
        StringBuilder logString = new StringBuilder();
        for (Map.Entry<String, String> entry : ssoAttributes.entrySet()) {
            logString.append(entry.getKey()).append(": ").append(entry.getValue()).append("; ");
        }
        log.error("missing:" + missing + " got:(" + logString + ")");
    }

}
