package org.edu_sharing.service.usage;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.MLText;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.ISO8601DateFormat;
import org.apache.commons.lang3.StringUtils;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.MCAlfrescoClient;
import org.edu_sharing.repository.server.RepoFactory;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.repository.server.tools.DateTool;
import org.edu_sharing.repository.server.tools.cache.RepositoryCache;
import org.edu_sharing.service.authentication.SSOAuthorityMapper;
import org.edu_sharing.service.collection.CollectionServiceFactory;
import org.edu_sharing.service.nodeservice.NodeServiceFactory;
import org.edu_sharing.service.nodeservice.NodeServiceHelper;
import org.edu_sharing.service.permission.PermissionServiceFactory;
import org.springframework.context.ApplicationContext;

@Slf4j
public class Usage2Service {


    public static final String MISSING_PARAM = "MISSING_PARAM";
    public static final String NO_CCPUBLISH_PERMISSION = "NO_CCPUBLISH_PERMISSION";


    private final ServiceRegistry serviceRegistry;
    private final RepositoryCache repositoryCache;


    UsageDAO usageDao;

    public Usage2Service() {
        ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
        serviceRegistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);
        repositoryCache = applicationContext.getBean(RepositoryCache.class);
        usageDao = new AlfServicesWrapper();
    }

    public Usage getUsage(String lmsId, String courseId, String parentNodeId, String resourceId) throws Usage2Exception {

        AuthenticationUtil.RunAsWork<Usage> runAs = () -> {
            Usage result = null;
            Map<String, Object> usage = null;
            try {
                usage = usageDao.getUsage(lmsId, courseId, parentNodeId, resourceId);
                if (usage != null) {
                    result = getUsageResult(usage);
                }
                return result;
            } catch (Throwable e) {
                log.error(e.getMessage(), e);
                throw new RuntimeException(e);
            }
        };

        log.info("return");
        try {
            return AuthenticationUtil.runAsSystem(runAs);
        } catch (Throwable e) {
            throw new Usage2Exception(e.getCause());
        }
    }


    public List<Usage> getUsagesByCourse(String appId, String courseId) {

        AuthenticationUtil.RunAsWork<List<Usage>> runAs = () -> {
            List<Usage> result = new ArrayList<>();
            try {
                Map<String, Map<String, Object>> usages = usageDao.getUsagesByCourse(appId, courseId);
                for (Map.Entry<String, Map<String, Object>> entry : usages.entrySet()) {
                    result.add(getUsageResult(entry.getValue()));
                }
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                throw new UsageException(e.getMessage(), e);
            }

            return result;
        };

        return AuthenticationUtil.runAsSystem(runAs);
    }

    public List<Usage> getUsages(String appId) throws UsageException {
        return getUsages(appId, null, null);
    }

    public List<Usage> getUsages(String appId, Long from, Long to) throws UsageException {

        List<Usage> result = new ArrayList<>();

        try {
            AuthenticationUtil.runAsSystem(() -> {
                Map<String, Map<String, Object>> usages = usageDao.getUsagesByAppId(appId, from, to);
                for (Map.Entry<String, Map<String, Object>> entry : usages.entrySet()) {
                    result.add(getUsageResult(entry.getValue()));
                }
                return null;
            });
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new UsageException(e.getMessage(), e);
        }

        return result;
    }

    public List<Usage> getUsages(String repositoryId,
                                 String nodeId,
                                 Long from,
                                 Long to) throws Exception {

        if ("-home-".equals(repositoryId)) {
            repositoryId = ApplicationInfoList.getHomeRepository().getAppId();
        }
        List<Usage> result = new ArrayList<>();

        for (Map.Entry<String, Map<String, Object>> entry : usageDao.getUsages(repositoryId, nodeId, from, to).entrySet()) {
            result.add(getUsageResult(entry.getValue()));
        }

        return result;
    }


    public Usage setUsage(String repoId, String userIn, String lmsId, String courseId, String parentNodeId, String userMail, Calendar fromUsed, Calendar toUsed, int distinctPersons, String _version, String resourceId, String xmlParams) throws UsageException {
        if (StringUtils.isBlank(userIn) || StringUtils.isBlank(lmsId) || StringUtils.isBlank(courseId) || StringUtils.isBlank(parentNodeId)) {
            throw new UsageException(MISSING_PARAM, null);
        }
        // if the user is admin, map it for the requesting repo
        final String user = SSOAuthorityMapper.mapAdminAuthority(userIn, lmsId);
        RunAsWork<Usage> runAs = () -> {
            try {
                log.info("before alfServicesWrapper.hasPermissions");

                Map<String, Object> usage = usageDao.getUsage(lmsId, courseId, parentNodeId, resourceId);

                //only check publish permission for new content so that an teacher who modifies the course/wysiwyg can safe changes of permission
                if (usage == null) {
                    String usageNodeId = parentNodeId;
                    // for collection references, we always rely on the main object permissions
                    if (NodeServiceFactory.getLocalService().hasAspect(StoreRef.PROTOCOL_WORKSPACE, StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(), usageNodeId, CCConstants.CCM_ASPECT_COLLECTION_IO_REFERENCE)) {
                        usageNodeId = NodeServiceHelper.getProperty(new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, usageNodeId), CCConstants.CCM_PROP_IO_ORIGINAL);
                    }
                    boolean hasPublishPerm = ((MCAlfrescoClient) RepoFactory.getInstance(ApplicationInfoList.getHomeRepository().getAppId(),
                            (Map<String, String>) null)).hasPermissions(usageNodeId, user, new String[]{CCConstants.PERMISSION_CC_PUBLISH});

                    if (!hasPublishPerm) {
                        log.info("User {} has no publish permission on {}", user, usageNodeId);
                        if (!parentNodeId.equals(usageNodeId)) {
                            log.info("The element {} is a collection ref for object {}, but the user is missing " + CCConstants.PERMISSION_CC_PUBLISH + " on the primary object", parentNodeId, usageNodeId);
                        }
                        throw new UsageException(NO_CCPUBLISH_PERMISSION, null);
                    }
                }

                return setUsageInternal(repoId, user, lmsId, courseId, parentNodeId, userMail, fromUsed, toUsed, distinctPersons, _version, resourceId, xmlParams);
            } catch (Throwable e) {
                log.error(e.getMessage(), e);
                throw new UsageException(e.getMessage(), e);
            }
        };

        return AuthenticationUtil.runAsSystem(runAs);
    }

    /**
     * this method will not run as system and will not check any permissions
     * used by collections
     */
    public Usage setUsageInternal(String repoId, String user, String lmsId, String courseId, String parentNodeId, String userMail, Calendar fromUsed, Calendar toUsed, int distinctPersons, String version, String resourceId, String xmlParams) throws Exception {
        Map<String, Object> properties = new HashMap<>();
        Map<String, Object> usage = usageDao.getUsage(lmsId, courseId, parentNodeId, resourceId);
        String guid = null;
        NodeRef personRef = serviceRegistry.getPersonService().getPerson(user);
        if (personRef != null) {
            guid = NodeServiceHelper.getProperty(personRef, CCConstants.CM_PROP_PERSON_GUID);
        }

        if (guid == null) {
            guid = user;
        }

        properties.put(CCConstants.CCM_PROP_USAGE_APPID, lmsId);
        properties.put(CCConstants.CCM_PROP_USAGE_COURSEID, courseId);
        properties.put(CCConstants.CCM_PROP_USAGE_PARENTNODEID, parentNodeId);
        properties.put(CCConstants.CCM_PROP_USAGE_APPUSER, user);
        properties.put(CCConstants.CCM_PROP_USAGE_APPUSERMAIL, userMail);
        if (fromUsed != null) {
            properties.put(CCConstants.CCM_PROP_USAGE_FROM, ISO8601DateFormat.format(fromUsed.getTime()));
        }
        if (toUsed != null) {
            properties.put(CCConstants.CCM_PROP_USAGE_TO, ISO8601DateFormat.format(toUsed.getTime()));
        }
        properties.put(CCConstants.CCM_PROP_USAGE_MAXPERSONS, distinctPersons);
        properties.put(CCConstants.CCM_PROP_USAGE_COUNTER, "1");

        if (StringUtils.isBlank(version)) {
            Object ov = serviceRegistry.getNodeService().getProperty(new NodeRef(AlfServicesWrapper.storeRef, parentNodeId), QName.createQName(CCConstants.LOM_PROP_LIFECYCLE_VERSION));

            if (ov != null) {
                version = (ov instanceof MLText) ? ((MLText) ov).getDefaultValue() : ov.toString();
            }
            version = (version == null) ? (String) serviceRegistry.getNodeService().getProperty(new NodeRef(AlfServicesWrapper.storeRef, parentNodeId), ContentModel.PROP_VERSION_LABEL) : version;
        }

        if (version != null) {
            properties.put(CCConstants.CCM_PROP_USAGE_VERSION, version);
        }

        properties.put(CCConstants.CCM_PROP_USAGE_RESSOURCEID, resourceId);
        properties.put(CCConstants.CCM_PROP_USAGE_XMLPARAMS, xmlParams);

        if (guid != null) {
            properties.put(CCConstants.CCM_PROP_USAGE_GUID, guid);
        }


        // if null only set counter @TODO Unique constraint in Schema that
        // prevents bypassing unique with standard alfresco services

        String usageNodeId = null;
        if (usage != null) {
            log.info("usage != null");
            String counter = (String) usage.get(CCConstants.CCM_PROP_USAGE_COUNTER);

            usageNodeId = (String) usage.get(CCConstants.SYS_PROP_NODE_UID);
            log.info("usageNodeId:{}", usageNodeId);


            log.info("before updating usage with props:");
            for (Map.Entry<String, Object> entry : properties.entrySet()) {
                log.info("key:{} val:{}", entry.getKey(), entry.getValue());
            }

            usageDao.updateUsage(usageNodeId, properties);
        } else {
            log.info("usage is null");
            usageNodeId = usageDao.createUsage(parentNodeId, properties);
        }


        Usage result = getUsageResult(usageDao.getUsage(usageNodeId));

        //remove IO from cache so that the gui gets the new usage count
        repositoryCache.remove(parentNodeId);

        log.info("returning");
        return result;
    }


    public List<Usage> getUsageByParentNodeId(String repoId, String user, String parentNodeId) throws UsageException {
        log.info("starting");


        if (!PermissionServiceFactory.getLocalService().hasPermission(StoreRef.PROTOCOL_WORKSPACE,
                StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(),
                parentNodeId,
                PermissionService.READ)) {
            return new ArrayList<>();
        }

        try {
            return AuthenticationUtil.runAsSystem(() -> {
                Map<String, Map<String, Object>> usages = usageDao.getUsages(parentNodeId);
                log.info("usages.keySet().size():{}", usages.size());
                ArrayList<Usage> result = new ArrayList<>();
                for (String key : usages.keySet()) {
                    result.add(getUsageResult(usages.get(key)));
                }
                addUsagesFromReferenceObjects(parentNodeId, result);
                return result;
            });
        } catch (Throwable e) {
            log.error(e.getMessage(), e);
            throw new UsageException(e.getMessage(), e);
        }
    }

    /**
     * Add indirect usages which are attached to collection reference objects
     */
    private void addUsagesFromReferenceObjects(String parentNodeId, ArrayList<Usage> result) {
        List<org.edu_sharing.service.model.NodeRef> nodes = CollectionServiceFactory.getLocalService().getReferenceObjects(parentNodeId);
        for (org.edu_sharing.service.model.NodeRef node : nodes) {
            Map<String, Map<String, Object>> usages = usageDao.getUsages(node.getNodeId());
            for (String key : usages.keySet()) {
                Usage usage = getUsageResult(usages.get(key));
                usage.setType(Usage.Type.INDIRECT);
                result.add(usage);
            }
        }
    }

    public boolean deleteUsage(String repoId, String user, String lmsId, String courseId, String parentNodeId, String resourceId) throws UsageException {
        log.info("starting");

        AuthenticationUtil.RunAsWork<Boolean> runAs = new AuthenticationUtil.RunAsWork<Boolean>() {
            @Override
            public Boolean doWork() throws Exception {
                try {
                    usageDao.removeUsage(lmsId, courseId, parentNodeId, resourceId);

                    //remove IO from cache so that the gui gets the new usage count
                    repositoryCache.remove(parentNodeId);

                    return true;
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                    throw new UsageException(e.getMessage(), e);
                }
            }
        };
        return AuthenticationUtil.runAsSystem(runAs);
    }


    public Usage getUsageResult(Map<String, Object> usage) {
        log.info("starting");
        Usage usageResult = new Usage();
        usageResult.setAppUser((String) usage.get(CCConstants.CCM_PROP_USAGE_APPUSER));
        usageResult.setAppUserMail((String) usage.get(CCConstants.CCM_PROP_USAGE_APPUSERMAIL));
        usageResult.setCourseId((String) usage.get(CCConstants.CCM_PROP_USAGE_COURSEID));

        String maxPersonsString = (String) usage.get(CCConstants.CCM_PROP_USAGE_MAXPERSONS);
        if (maxPersonsString != null) usageResult.setDistinctPersons(Integer.parseInt(maxPersonsString));

        usageResult.setUsageVersion((String) usage.get(CCConstants.CCM_PROP_USAGE_VERSION));
        usageResult.setUsageXmlParams((String) usage.get(CCConstants.CCM_PROP_USAGE_XMLPARAMS));

        Object usageFrom = usage.get(CCConstants.CCM_PROP_USAGE_FROM);
        log.info("CCM_PROP_USAGE_FROM:" + usageFrom);

        if (usageFrom != null) {
            Calendar calFrom = Calendar.getInstance();

            Date usageFromDate = new DateTool().getDate((String) usageFrom);
            if (usageFromDate != null) {
                calFrom.setTime(usageFromDate);
                usageResult.setFromUsed(calFrom);
            }
        }
        usageResult.setLmsId((String) usage.get(CCConstants.CCM_PROP_USAGE_APPID));
        usageResult.setParentNodeId((String) usage.get(CCConstants.CCM_PROP_USAGE_PARENTNODEID));

        usageResult.setNodeId((String) usage.get(CCConstants.SYS_PROP_NODE_UID));

        usageResult.setResourceId((String) usage.get(CCConstants.CCM_PROP_USAGE_RESSOURCEID));

        usageResult.setGuid((String) usage.get(CCConstants.CCM_PROP_USAGE_GUID));

        Object usageTo = usage.get(CCConstants.CCM_PROP_USAGE_TO);
        if (usageTo != null) {
            Calendar calTo = Calendar.getInstance();
            Date usageToDate = new DateTool().getDate((String) usageTo);
            if (usageToDate != null) {
                calTo.setTime(usageToDate);
                usageResult.setToUsed(calTo);
            }
        }

        Object usageCounter = usage.get(CCConstants.CCM_PROP_USAGE_COUNTER);
        log.info("CCM_PROP_USAGE_COUNTER:{}", usageCounter);
        if (usageCounter != null) {
            usageResult.setUsageCounter(Integer.parseInt((String) usageCounter));
        }

        String modified = (String) usage.get(CCConstants.CM_PROP_C_MODIFIED);
        usageResult.setModified(new Date(Long.parseLong(modified)));

        String created = (String) usage.get(CCConstants.CM_PROP_C_CREATED);
        usageResult.setCreated(new Date(Long.parseLong(created)));

        log.info("returning");
        return usageResult;
    }
}
