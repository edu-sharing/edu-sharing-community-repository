package org.edu_sharing.service.dataprotection;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.permissions.AccessDeniedException;
import org.alfresco.repo.security.person.RegexHomeFolderProvider;
import org.alfresco.service.cmr.repository.*;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchParameters;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.cmr.security.AccessPermission;
import org.alfresco.service.cmr.security.AuthorityType;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.namespace.QName;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.examples.Archiver;
import org.apache.commons.compress.utils.FileNameUtils;
import org.apache.commons.io.FileUtils;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.edu_sharing.alfresco.service.search.cmis.Filters;
import org.edu_sharing.alfresco.service.search.cmis.Query;
import org.edu_sharing.alfresco.service.search.cmis.QueryBuilder;
import org.edu_sharing.alfresco.service.search.cmis.QueryStatement;
import org.edu_sharing.alfresco.workspace_administration.NodeServiceInterceptor;
import org.edu_sharing.repository.client.rpc.EduGroup;
import org.edu_sharing.repository.client.rpc.User;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.client.tools.I18nAngular;
import org.edu_sharing.repository.server.jobs.annotations.Queued;
import org.edu_sharing.repository.server.tools.URLTool;
import org.edu_sharing.repository.server.tools.UserEnvironmentTool;
import org.edu_sharing.repository.server.tools.mailtemplates.MailTemplate;
import org.edu_sharing.repository.server.tools.security.RunAsSystem;
import org.edu_sharing.service.authentication.ScopeUserHomeService;
import org.edu_sharing.service.authentication.ScopeUserHomeServiceFactory;
import org.edu_sharing.service.authority.AuthorityService;
import org.edu_sharing.service.authority.AuthorityServiceFactory;
import org.edu_sharing.service.comment.CommentService;
import org.edu_sharing.service.feedback.FeedbackService;
import org.edu_sharing.service.lifecycle.PersonLifecycleService;
import org.edu_sharing.service.lifecycle.Utils;
import org.edu_sharing.service.nodeservice.NodeServiceFactory;
import org.edu_sharing.service.nodeservice.RecurseMode;
import org.edu_sharing.service.permission.annotation.HasRole;
import org.edu_sharing.service.permission.annotation.Permission;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class DataProtectionService {

    private final NodeService nodeService;
    private final org.edu_sharing.service.nodeservice.NodeService eduNodeService;
    private final PersonService personService;
    private final ContentService contentService;
    private final PermissionService permissionService;
    private final RegexHomeFolderProvider regexHomeFolderProvider;
    private final DataProtectionConfig config;
    private final PDFReport report;
    private final FeedbackService feedbackService;
    private final CommentService commentService;
    private final QueryBuilder queryBuilder;
    private final SearchService searchService;


    @Value("${repository.dataprotection.retentionPeriod:PT240H}")
    private String retentionPeriod;

    @Value("${repository.dataprotection.binaryExport:true}")
    private boolean binaryExport;

    @Value("${repository.dataprotection.metadataExport:true}")
    private boolean metadataExport;

    @Value("${repository.dataprotection.summaryExport:true}")
    private boolean summaryExport;

    @Value("${repository.dataprotection.fileName:dataprotectioninfo_edu-sharing}")
    private String fileName;

    private final PersonLifecycleService personLifecycleService = new PersonLifecycleService();
    private final QName propMapType = QName.createQName(CCConstants.CCM_PROP_MAP_TYPE);
    private String systemFolder;

    @EventListener(ContextRefreshedEvent.class)
    public void onContextRefreshed() {
        log.info("DataProtectionService started");
        AuthenticationUtil.runAsSystem(() -> {
            try {
                systemFolder = new UserEnvironmentTool().getEdu_SharingGdprFolder();
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
            return null;
        });
    }

    @RunAsSystem
    public void cleanExpired() {
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        String retentionDate = dateTimeFormatter.format(ZonedDateTime.now(ZoneId.of("UTC")).minus(Duration.parse(this.retentionPeriod)));

        QueryStatement query = Query.select(CCConstants.SYS_PROP_NODE_UID, CCConstants.CM_NAME)
                .from(CCConstants.CCM_TYPE_IO)
                .hasAspect(CCConstants.CCM_ASPECT_GDPR)
                .where(Filters.lt(CCConstants.CM_PROP_C_MODIFIED, retentionDate));

        SearchParameters searchParameters = new SearchParameters();
        searchParameters.setLanguage(org.alfresco.service.cmr.search.SearchService.LANGUAGE_CMIS_ALFRESCO);
        searchParameters.setMaxPermissionChecks(0);
        searchParameters.addStore(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE);
        searchParameters.setQuery(queryBuilder.build(query));
        ResultSet searchResults = searchService.query(searchParameters);

        if (searchResults.getNumberFound() == 0) {
            return;
        }

        searchResults.forEach(item -> {
            try {
                log.info("removing gdpr export {} {}", item.getValue(ContentModel.PROP_NAME), item.getNodeRef().getId());
                eduNodeService.removeNode(item.getNodeRef().getId(), item.getChildAssocRef().getParentRef().getId(), false);
            } catch (Exception e) {
                log.error("error removing expired export {} {}", item.getValue(ContentModel.PROP_NAME), item.getNodeRef().getId(), e);
            }
        });
    }


    @Permission(requiresUser = true)
    public String getDataProtectionNode(@HasRole String userName) {
        QueryStatement query = Query.select(CCConstants.SYS_PROP_NODE_UID)
                .from(CCConstants.CCM_TYPE_IO)
                .hasAspect(CCConstants.CCM_ASPECT_GDPR)
                .where(Filters.eq(CCConstants.CM_PROP_C_CREATOR, userName));

        SearchParameters searchParameters = new SearchParameters();
        searchParameters.setLanguage(SearchService.LANGUAGE_CMIS_ALFRESCO);
        searchParameters.setMaxPermissionChecks(0);
        searchParameters.setMaxItems(1);
        searchParameters.addStore(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE);
        searchParameters.setQuery(queryBuilder.build(query));
        ResultSet searchResults = searchService.query(searchParameters);

        if (searchResults.getNumberFound() == 0) {
            return null;
        }

        return searchResults.iterator().next().getNodeRef().getId();
    }

    public void prepare(String userName) {
        getTargetNode(userName);
    }

    public void exportUserNodes(String userName) throws IOException, ArchiveException {
        log.info("starting for user {}", userName);
        String rootPath = config.getMainPath().concat("/" + userName);

        log.info("collecting home nodes for {}", userName);
        NodeRefResult userHomeResult = getUserHomeNodes(userName, null);
        createStructure(rootPath, "home", buildPathMap(createChildParentMap(userHomeResult.getNodes())));

        log.info("collecting collection nodes for {}", userName);
        List<NodeRef> collectionNodes = getCollectionNodes(userName);
        createStructure(rootPath, "collection", buildPathMap(createChildParentMap(collectionNodes)));

        log.info("collecting shared nodes for {}", userName);
        List<NodeRef> sharedNodes = getSharedNodes(userName, null, Stream.concat(userHomeResult.getIgnored().stream(), Stream.concat(userHomeResult.nodes.stream(), collectionNodes.stream())).toList());
        createStructure(rootPath, "shared", buildPathMap(createChildParentMap(sharedNodes)));

        log.info("collecting feedbacks for {}", userName);
        List<NodeRef> feedBacks = feedbackService.getUsersFeedback(userName);
        createStructure(rootPath, "feedback", buildPathMap(createChildParentMap(feedBacks)));

        log.info("collecting comments for {}", userName);
        List<NodeRef> comments = commentService.getUsersComments(userName);
        createStructure(rootPath, "comment", buildPathMap(createChildParentMap(comments)));

        //List<NodeRef> ratings = ratingService....
        //createStructure(rootPath,"comment",buildPathMap(createChildParentMap(comments)));

        log.info("collecting safe home nodes for {}", userName);
        // safe
        NodeServiceInterceptor.setEduSharingScope("safe");
        NodeRefResult userHomeResultSafe = getUserHomeNodes(userName, "safe");
        createStructure(rootPath, "safe/home", buildPathMap(createChildParentMap(userHomeResultSafe.getNodes())));

        log.info("collecting safe shared nodes for {}", userName);
        List<NodeRef> sharedNodesSafe = getSharedNodes(userName, "safe", Stream.concat(userHomeResultSafe.getIgnored().stream(), Stream.concat(userHomeResultSafe.nodes.stream(), collectionNodes.stream())).toList());
        createStructure(rootPath, "safe/shared", buildPathMap(createChildParentMap(sharedNodesSafe)));
        NodeServiceInterceptor.setEduSharingScope(null);

        log.info("creating report for {}", userName);
        File reportFile = summaryReport(userName, collectionNodes, feedBacks, comments, rootPath);


        log.info("creating archive for {}", userName);
        File target;
        String mimeType;
        if (reportOnly() && reportFile != null) {
            target = new File(rootPath + ".pdf");
            Files.copy(reportFile.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
            mimeType = "application/pdf";
        } else {
            target = new File(rootPath + ".zip");
            archive(new File(rootPath), target);
            mimeType = "application/zip";
        }

        AuthenticationUtil.runAsSystem(() -> persistAndCleanup(userName, target, rootPath, mimeType));
    }

    private File summaryReport(String userName, List<NodeRef> collectionNodes, List<NodeRef> feedBacks, List<NodeRef> comments, String rootPath) {
        if (!summaryExport) {
            return null;
        }

        // filter collection refs for report
        collectionNodes = collectionNodes.stream().filter(c -> nodeService.getType(c).equals(QName.createQName(CCConstants.CCM_TYPE_MAP))).toList();

        List<NodeRef> privateCollections = collectionNodes.stream().filter(n -> !isSharedNode(n, userName)).toList();
        List<NodeRef> sharedCollections = collectionNodes.stream().filter(n -> isSharedNode(n, userName)).toList();


        AuthorityService authorityService = AuthorityServiceFactory.getInstance().getLocalService();
        Set<String> groupSet = authorityService.getMemberships(userName);
        ArrayList<EduGroup> allEduGroups = AuthenticationUtil.runAsSystem(() -> authorityService.getAllEduGroups(userName, false));
        List<String> groupList = groupSet.stream()
                .filter(g -> (!g.startsWith("GROUP_ORG") && !g.startsWith("GROUP_MEDIA_CENTER")))
                .map(g ->  (String)authorityService.getAuthorityProperty(g,CCConstants.CM_PROP_AUTHORITY_AUTHORITYDISPLAYNAME))
                .toList();
        User user = authorityService.getUser(userName);

        List<String> mediacenterList = groupSet.stream()
                .filter(g -> g.startsWith("GROUP_MEDIA_CENTER") && !g.contains("_PROXY_"))
                .map(g -> (String)authorityService.getAuthorityProperty(g,CCConstants.CM_PROP_AUTHORITY_AUTHORITYDISPLAYNAME))
                .collect(Collectors.toList());

        /**
         * @TODO use profile data or something dynamic determine locale and timezone
         */
        ZoneId zone = ZoneId.of("Europe/Berlin");
        Locale locale = Locale.GERMANY;

        Date firstLogin = (Date) user.getProperties().get(CCConstants.PROP_USER_ESFIRSTLOGIN);
        Date lastLogin = (Date) user.getProperties().get(CCConstants.PROP_USER_ESLASTLOGIN);
        String role = (String) user.getProperties().get(CCConstants.CM_PROP_PERSON_EDU_SCHOOL_PRIMARY_AFFILIATION);
        List<String> roles = role == null ? null : Stream.of(role).map(r -> I18nAngular.getTranslationAngular("common", "USER.PRIMARY_AFFILIATION." + r)).toList();
        ArrayList<String> secondaryUserName = (ArrayList<String>) user.getProperties().get(CCConstants.PROP_USER_SECONDARY_IDS);
        PDFReport.Data.DataBuilder reportData = PDFReport.Data.builder()
                .userName(userName)
                .secondaryUserName(secondaryUserName)
                .firstName(user.getGivenName())
                .lastName(user.getSurname())
                .firstLogin(formatDate(firstLogin, zone, locale, FormatStyle.MEDIUM, true))
                .lastLogin(formatDate(lastLogin, zone, locale, FormatStyle.MEDIUM, true))
                .currentDate(formatDate(new Date(), zone, locale, FormatStyle.MEDIUM, false))
                .email(user.getEmail())
                .roles(roles)
                .privateCollections(getNameList(privateCollections))
                .sharedCollections(getNameList(sharedCollections))
                //todo
                .ratings(List.of())
                .feedbacks(getNameList(feedBacks))
                .comments(getNameList(comments))
                .groupList(groupList)
                .mediacenterList(mediacenterList);

        if (allEduGroups != null && !allEduGroups.isEmpty()) {
            //reportData.schoolName(allEduGroups.stream().map(e -> (e.getGroupDisplayName() +"("+e.getGroupId()+")")).collect(Collectors.joining(",")));
            reportData.schoolDisplayName(allEduGroups.stream()
                    .map(e -> {
                            String groupName = e.getGroupname().replace("GROUP_ORG_","");
                            if(e.getGroupDisplayName() != null && e.getGroupDisplayName().contains(groupName)){
                                return e.getGroupDisplayName();
                            }else{
                                return (e.getGroupDisplayName() +"("+groupName+")");
                            }
                        }
                    )
                    .collect(Collectors.joining(",")));
        }

        String reportDirectory = rootPath.concat("/report");
        File dir = new File(reportDirectory);
        if (!dir.exists()) {
            boolean mkdirs = dir.mkdirs();// creates parent folders as needed
            if (!mkdirs) {
                throw new RuntimeException("Unable to create directory " + dir.getAbsolutePath());
            }
        }

        return report.report(reportData.build(), dir);
    }

    boolean isSharedNode(NodeRef nodeRef, String userName) {
        Set<AccessPermission> allSetPermissions = permissionService.getAllSetPermissions(nodeRef);
        List<AccessPermission> perms = allSetPermissions.stream().filter(a -> !userName.equals(a.getAuthority()) && !AuthorityType.OWNER.equals(a.getAuthorityType()))
                .toList();
        return !perms.isEmpty();
    }

    private List<String> getNameList(List<NodeRef> nodes) {
        return nodes.stream()
                .map(n -> (String) nodeService.getProperty(n, ContentModel.PROP_NAME))
                .toList();
    }

    private NodeRef persistAndCleanup(String userName, File target, String rootPath, String mimeType) {
        try {
            NodeRef nodeRef = getTargetNode(userName);
            permissionService.setPermission(nodeRef, userName, PermissionService.CONSUMER, true);
            nodeService.addAspect(nodeRef, QName.createQName(CCConstants.CCM_ASPECT_GDPR), Collections.emptyMap());
            nodeService.removeAspect(nodeRef, ContentModel.ASPECT_VERSIONABLE);
            ContentWriter writer = contentService.getWriter(nodeRef, ContentModel.PROP_CONTENT, true);
            writer.setMimetype(mimeType);
            writer.addListener(() -> {
                try {
                    FileUtils.deleteDirectory(new File(rootPath));
                    FileUtils.delete(target);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                sendMail(userName, nodeRef);
            });
            writer.putContent(target);

            return nodeRef;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    private NodeRef getTargetNode(String userName) {
        NodeRef personNodeRef = personService.getPersonOrNull(userName);
        List<String> homeFolderPath = regexHomeFolderProvider.getHomeFolderPath(personNodeRef);
        Utils utils = new Utils();
        String parentId = systemFolder;
        try {
            for (String pathElement : homeFolderPath) {
                NodeRef rs = utils.getNodeRef(parentId, CCConstants.CCM_TYPE_MAP, pathElement);
                parentId = rs.getId();
            }
            return utils.getNodeRef(parentId, CCConstants.CCM_TYPE_IO, getFileName());
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    private String getFileName() {
        Date date = new Date(); // or your custom date
        SimpleDateFormat formatter = new SimpleDateFormat("yyMMdd");
        String formatted = formatter.format(date);
        String fileExtension = (reportOnly()) ? ".pdf" : ".zip";
        return formatted.concat("_").concat(fileName).concat(fileExtension);
    }

    public boolean reportOnly() {
        return summaryExport && !metadataExport;
    }

    private void createStructure(String rootPath, String subPath, Map<NodeRef, String> pathMap) throws IOException {
        for (Map.Entry<NodeRef, String> e : pathMap.entrySet()) {
            NodeRef nodeRef = e.getKey();
            String path = e.getValue();
            String nodePath = rootPath.concat("/" + subPath + "/").concat(path);
            File dir = new File(nodePath);
            if (!dir.exists()) {
                boolean mkdirs = dir.mkdirs();// creates parent folders as needed
                if (!mkdirs) {
                    throw new RuntimeException("Unable to create directory " + dir.getAbsolutePath());
                }
            }
            String fileName = nodePath.concat("/" + nodeService.getProperty(nodeRef, ContentModel.PROP_NAME));
            writeNodePropertiesToJson(nodeRef, new File(fileName + ".json"));
            writeContent(nodeRef, ContentModel.PROP_CONTENT, new File(fileName));
            writeContent(nodeRef, QName.createQName(CCConstants.CCM_PROP_IO_USERDEFINED_PREVIEW), new File(fileName + ".png"));
        }
    }

    private NodeRefResult getUserHomeNodes(String userName, String scope) {
        NodeRef userHome = getUserHome(userName, scope);
        if (userHome == null) {
            return NodeRefResult.builder().nodes(new ArrayList<>()).ignored(new ArrayList<>()).build();
        }
        List<NodeRef> levelOne = nodeService.getChildAssocs(userHome).stream().map(ChildAssociationRef::getChildRef).toList();
        // remove shared folder
        List<NodeRef> ignored = levelOne.stream().filter(n -> CCConstants.CCM_VALUE_MAP_TYPE_EDUGROUP.equals(nodeService.getProperty(n, propMapType))).toList();
        levelOne = levelOne.stream().filter(n -> !CCConstants.CCM_VALUE_MAP_TYPE_EDUGROUP.equals(nodeService.getProperty(n, propMapType))).toList();

        List<NodeRef> result = new ArrayList<>(levelOne);
        levelOne.forEach(n -> {
            QName type = nodeService.getType(n);
            if (QName.createQName(CCConstants.CCM_TYPE_MAP).equals(type) || QName.createQName(CCConstants.CM_TYPE_FOLDER).equals(type)) {
                result.addAll(NodeServiceFactory.getInstance().getLocalService().getChildrenRecursive(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, n.getId(), null, RecurseMode.All));
            }
        });
        return NodeRefResult.builder().nodes(result).ignored(ignored).build();
    }

    List<NodeRef> getCollectionNodes(String userName) {
        List<NodeRef> all = new ArrayList<>(personLifecycleService.getAllNodeRefs(userName, CCConstants.CCM_TYPE_MAP, null).stream()
                .filter((ref) -> nodeService.hasAspect(ref, QName.createQName(CCConstants.CCM_ASPECT_COLLECTION)))
                .toList());
        all.addAll(personLifecycleService.getAllNodeRefs(userName, CCConstants.CCM_TYPE_IO, null).stream()
                .filter((ref) -> nodeService.hasAspect(ref, QName.createQName(CCConstants.CCM_ASPECT_COLLECTION_IO_REFERENCE)))
                .toList());
        return all;
    }

    List<NodeRef> getSharedNodes(String userName, String scope, List<NodeRef> filesToIgnore) {
        List<NodeRef> all = new ArrayList<>(personLifecycleService.getAllNodeRefs(userName, CCConstants.CCM_TYPE_IO, scope).stream()
                .filter((ref) -> !filesToIgnore.contains(ref))
                .toList());
        all.addAll(personLifecycleService.getAllNodeRefs(userName, CCConstants.CCM_TYPE_MAP, scope).stream()
                .filter((ref) -> !filesToIgnore.contains(ref))
                .filter((ref) -> !nodeService.hasAspect(ref, QName.createQName(CCConstants.CCM_ASPECT_COLLECTION)))
                .toList());
        return all;
    }

    private NodeRef getUserHome(String userName, String scope) {
        NodeRef personNodeRef = personService.getPersonOrNull(userName);
        NodeRef homeFolder;
        if (scope == null) {
            homeFolder = personLifecycleService.getHomeFolder(personNodeRef);
            if (homeFolder == null) {
                log.info("Person {} does not have a home folder, skipping it", userName);
                return null;
            }
        } else {
            ScopeUserHomeService scopeUserHomeService = ScopeUserHomeServiceFactory.getScopeUserHomeService();
            homeFolder = scopeUserHomeService.getUserHome((String) nodeService.getProperty(personNodeRef, QName.createQName(CCConstants.CM_PROP_PERSON_USERNAME)), scope, false);
            if (homeFolder == null) {
                log.info("Person {} does not have a scope folder for {}, skipping it", userName, scope);
                return null;
            }
        }
        return homeFolder;
    }


    private Map<NodeRef, Optional<NodeRef>> createChildParentMap(List<NodeRef> nodeRefs) {
        if (!metadataExport) return new HashMap<>();
        return nodeRefs.stream().collect(Collectors.toMap(n -> n, n -> {
            try {
                NodeRef parent = nodeService.getPrimaryParent(n).getParentRef();
                if (nodeRefs.contains(parent)) {
                    return Optional.of(parent);
                }
            } catch (AccessDeniedException ignored) {
            }
            return Optional.empty();
        }));
    }

    private Map<NodeRef, String> buildPathMap(Map<NodeRef, Optional<NodeRef>> childParentMap) {
        Map<NodeRef, String> pathMap = new HashMap<>();
        childParentMap.forEach((k, v) -> {
            String path = pathMap.get(k);
            if (path == null) {
                NodeRef pathEle = k;
                // build path from child to ancestors
                do {
                    String pathEleName = (String) nodeService.getProperty(pathEle, ContentModel.PROP_NAME);
                    path = (path == null) ? pathEleName : pathEleName.concat("/" + path);
                    pathEle = childParentMap.get(pathEle).orElse(null);
                } while (pathEle != null);

                // duplicate handling
                int suffix = 0;
                while (pathMap.containsValue(path)) {
                    if (suffix > 0) {
                        path = path.substring(0, path.length() - 3);
                    }
                    suffix++;
                    path = path.concat("_" + suffix);
                }
                pathMap.put(k, path);
            }
        });
        return pathMap;
    }

    private void writeNodePropertiesToJson(NodeRef nodeRef, File outputFile) throws IOException {
        Map<QName, Serializable> properties = nodeService.getProperties(nodeRef);
        Map<String, Object> jsonCompatibleProps = new HashMap<>();

        for (Map.Entry<QName, Serializable> entry : properties.entrySet()) {
            String key = entry.getKey().toPrefixString(); // e.g., cm:name
            jsonCompatibleProps.put(key, entry.getValue());
        }

        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT); // Pretty print
        mapper.writeValue(outputFile, jsonCompatibleProps);
    }

    private void writeContent(NodeRef nodeRef, QName contentProperty, File outputFile) throws IOException {
        if (!binaryExport) return;
        if (nodeService.getType(nodeRef).equals(QName.createQName(CCConstants.CCM_TYPE_IO))
                && !nodeService.hasAspect(nodeRef, QName.createQName(CCConstants.CCM_ASPECT_COLLECTION_IO_REFERENCE))) {
            ContentReader reader = contentService.getReader(nodeRef, contentProperty);
            if (reader != null && reader.exists()) {
                try (InputStream in = reader.getContentInputStream(); OutputStream out = new FileOutputStream(outputFile)) {
                    IOUtils.copy(in, out);
                }
            }
        }
    }

    private void sendMail(String user, NodeRef nodeRef) {
        NodeRef personRef = personService.getPersonOrNull(user);
        String firstname = (String) nodeService.getProperty(personRef, ContentModel.PROP_FIRSTNAME);
        String lastName = (String) nodeService.getProperty(personRef, ContentModel.PROP_LASTNAME);
        String email = (String) nodeService.getProperty(personRef, ContentModel.PROP_EMAIL);
        String downloadUrl = URLTool.getDownloadServletUrl(nodeRef.getId(), null, true);

        if (email == null) return;
        Map<String, String> replace = new HashMap<>();
        replace.put("firstName", firstname);
        replace.put("lastName", lastName);
        replace.put("link", downloadUrl);
        replace.put("retentionPeriod", Duration.parse(retentionPeriod).toDays() + "");
        try {
            String template = "gdpr";
            MailTemplate.sendMail(email, template, replace);
        } catch (Exception e) {
            log.warn("Can not send status notify mail to user: {}", e.getMessage(), e);
        }
    }


    private void archive(File directory, File destination) throws IOException, ArchiveException {
        String format = FileNameUtils.getExtension(destination.toPath());
        new Archiver().create(format, destination, directory);
    }

    @Queued(unique = true)
    @Permission(requiresUser = true)
    public void requestDataProtectionExport(@HasRole String user) {

        AuthenticationUtil.runAsSystem(() -> {
            prepare(user);
            return null;
        });

        try {
            exportUserNodes(user);
        } catch (IOException e) {
            log.error("Error while exporting user nodes", e);
        } catch (ArchiveException e) {
            log.error("Error while archiving user nodes", e);
        }
    }

    private String formatDate(Date date, ZoneId zone, Locale locale, FormatStyle style, boolean includeTime) {
        if (date == null) {
            return null;
        }

        ZonedDateTime zonedDateTime = date.toInstant().atZone(zone);
        DateTimeFormatter formatter = (includeTime) ? DateTimeFormatter.ofLocalizedDateTime(style) : DateTimeFormatter.ofLocalizedDate(style);
        formatter = formatter.withLocale(locale)
                .withZone(zone);

        return formatter.format(zonedDateTime);
    }


    @Data
    @Builder
    public static class NodeRefResult {
        List<NodeRef> nodes;
        List<NodeRef> ignored;
    }
}
