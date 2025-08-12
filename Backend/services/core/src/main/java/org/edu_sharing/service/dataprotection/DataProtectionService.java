package org.edu_sharing.service.dataprotection;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.permissions.AccessDeniedException;
import org.alfresco.repo.security.person.RegexHomeFolderProvider;
import org.alfresco.service.cmr.repository.*;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.Pair;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.examples.Archiver;
import org.apache.commons.compress.utils.FileNameUtils;
import org.apache.commons.io.FileUtils;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.edu_sharing.alfresco.workspace_administration.NodeServiceInterceptor;
import org.edu_sharing.repository.client.rpc.EduGroup;
import org.edu_sharing.repository.client.rpc.User;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.client.tools.I18nAngular;
import org.edu_sharing.repository.server.tools.UserEnvironmentTool;
import org.edu_sharing.repository.server.tools.mailtemplates.MailTemplate;
import org.edu_sharing.repository.tools.URLHelper;
import org.edu_sharing.service.authentication.ScopeUserHomeService;
import org.edu_sharing.service.authentication.ScopeUserHomeServiceFactory;
import org.edu_sharing.service.authority.AuthorityService;
import org.edu_sharing.service.authority.AuthorityServiceFactory;
import org.edu_sharing.service.authority.AuthorityServiceHelper;
import org.edu_sharing.service.comment.CommentService;
import org.edu_sharing.service.comment.CommentServiceFactory;
import org.edu_sharing.service.dataprotection.queue.DataProtectionQueue;
import org.edu_sharing.service.dataprotection.queue.DataProtectionQueueEntry;
import org.edu_sharing.service.feedback.FeedbackService;
import org.edu_sharing.service.feedback.FeedbackServiceFactory;
import org.edu_sharing.service.lifecycle.PersonLifecycleService;
import org.edu_sharing.service.lifecycle.Utils;
import org.edu_sharing.service.nodeservice.NodeServiceFactory;
import org.edu_sharing.service.nodeservice.RecurseMode;
import org.edu_sharing.service.rating.RatingService;
import org.edu_sharing.service.rating.RatingServiceFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.io.*;
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
@Component
public class DataProtectionService{

    @Autowired
    NodeService nodeService;

    @Autowired
    PersonService personService;

    @Autowired
    ContentService contentService;

    @Autowired
    PermissionService permissionService;

    @Autowired
    RegexHomeFolderProvider regexHomeFolderProvider;

    @Autowired
    DataProtectionConfig config;

    @Autowired
    DataProtectionQueue queue;

    @Autowired
    PDFReport report;

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

    PersonLifecycleService personLifecycleService = new PersonLifecycleService();

    QName propMapType = QName.createQName(CCConstants.CCM_PROP_MAP_TYPE);


    String systemFolder;

    RatingService ratingService;

    FeedbackService feedbackService;

    CommentService commentService;


    @EventListener(ContextRefreshedEvent.class)
    public void onContextRefreshed() {
        log.info("DataProtectionService started");
        AuthenticationUtil.runAsSystem(() -> {
            try {
                ratingService = RatingServiceFactory.getLocalService();
                feedbackService = FeedbackServiceFactory.getLocalService();
                commentService = CommentServiceFactory.getLocalService();

                systemFolder = new UserEnvironmentTool().getEdu_SharingGdprFolder();
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
            return null;
        });
    }

    public void cleanExpired(){
        AuthenticationUtil.runAsSystem(()->{
            List<DataProtectionQueueEntry> entries = queue.get(DataProtectionQueue.Status.FINISHED);
            List<Pair<NodeRef,String>> toRemove = new ArrayList<>();
            entries.forEach(entry -> {
                NodeRef nodeRef = new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE,entry.getNode_id());
                Date modified = (Date)nodeService.getProperty(nodeRef, ContentModel.PROP_MODIFIED);
                if((System.currentTimeMillis() - modified.getTime()) > Duration.parse(this.retentionPeriod).toMillis()){
                    toRemove.add(new Pair<>(nodeRef,entry.getUser()));
                }
            });
            toRemove.forEach(pair -> {
                if(DataProtectionQueue.Status.FINISHED.toString().equals(queue.get(pair.getSecond()).getStatus())){
                    log.info("removing gdpr export {} {}",nodeService.getProperty(pair.getFirst(), ContentModel.PROP_NAME),pair.getFirst());
                    removeNode(pair.getFirst().getId());
                    queue.delete(pair.getSecond());
                }
            });
            return null;
        });
    }

    public void startExport(){
        List<DataProtectionQueueEntry> allUsers = queue.getAll().stream()
                .filter(e ->
                        DataProtectionQueue.Status.REQUESTED.toString().equals(e.getStatus()) || DataProtectionQueue.Status.RUNNING.toString().equals(e.getStatus()))
                .collect(Collectors.toList());
        for(DataProtectionQueueEntry e: allUsers) {
            startExport(e);
        }
    }

    public void startExport(DataProtectionQueueEntry entry){
        String userName = entry.getUser();
        queue.update(userName, null, DataProtectionQueue.Status.RUNNING);
        // be sure systemfolder and target node is created with as admin user so that this files will not be included in export zip
        AuthenticationUtil.runAsSystem(()-> {
            prepare(userName);
            return null;});
        NodeRef nodeRef = AuthenticationUtil.runAs(() -> exportUserNodes(userName), userName);
        queue.update(userName, nodeRef.getId(), DataProtectionQueue.Status.FINISHED);
    }

    public void prepare(String userName){
        getTargetNode(userName);
    }

    public NodeRef exportUserNodes(String userName) throws IOException, ArchiveException {
        log.info("starting for user {}", userName);
        String rootPath = config.getMainPath().concat("/"+userName);

        log.info("collecting home nodes for {}", userName);
        NodeRefResult userHomeResult = getUserHomeNodes(userName, null);
        createStructure(rootPath,"home",buildPathMap(createChildParentMap(userHomeResult.getNodes())));

        log.info("collecting collection nodes for {}", userName);
        List<NodeRef> collectionNodes = getCollectionNodes(userName);
        createStructure(rootPath,"collection",buildPathMap(createChildParentMap(collectionNodes)));

        log.info("collecting shared nodes for {}", userName);
        List<NodeRef> sharedNodes = getSharedNodes(userName,null, Stream.concat(userHomeResult.getIgnored().stream(),Stream.concat(userHomeResult.nodes.stream(),collectionNodes.stream())).collect(Collectors.toList()));
        createStructure(rootPath,"shared",buildPathMap(createChildParentMap(sharedNodes)));

        log.info("collecting feedbacks for {}", userName);
        List<NodeRef> feedBacks = feedbackService.getUsersFeedback(userName);
        createStructure(rootPath,"feedback",buildPathMap(createChildParentMap(feedBacks)));

        log.info("collecting comments for {}", userName);
        List<NodeRef> comments = commentService.getUsersComments(userName);
        createStructure(rootPath,"comment",buildPathMap(createChildParentMap(comments)));

        //List<NodeRef> ratings = ratingService....
        //createStructure(rootPath,"comment",buildPathMap(createChildParentMap(comments)));

        log.info("collecting safe home nodes for {}", userName);
        // safe
        NodeServiceInterceptor.setEduSharingScope("safe");
        NodeRefResult userHomeResultSafe = getUserHomeNodes(userName, "safe");
        createStructure(rootPath,"safe/home",buildPathMap(createChildParentMap(userHomeResultSafe.getNodes())));

        log.info("collecting safe shared nodes for {}", userName);
        List<NodeRef> sharedNodesSafe = getSharedNodes(userName,"safe", Stream.concat(userHomeResultSafe.getIgnored().stream(),Stream.concat(userHomeResultSafe.nodes.stream(),collectionNodes.stream())).collect(Collectors.toList()));
        createStructure(rootPath,"safe/shared",buildPathMap(createChildParentMap(sharedNodesSafe)));
        NodeServiceInterceptor.setEduSharingScope(null);

        log.info("creating report for {}", userName);
        summmaryReport(userName, collectionNodes, feedBacks, comments, rootPath);


        log.info("creating archive for {}", userName);
        File target = new File(rootPath+".zip");
        archive(new File(rootPath), target);

        return AuthenticationUtil.runAsSystem(() -> persistAndCleanup(userName, target, rootPath));
    }

    private void summmaryReport(String userName, List<NodeRef> collectionNodes, List<NodeRef> feedBacks, List<NodeRef> comments, String rootPath) {
        if(!summaryExport) return;
        List<NodeRef> privateCollections = collectionNodes.stream().filter(n -> "MY".equals(nodeService.getProperty(n, QName.createQName(CCConstants.CCM_PROP_MAP_COLLECTIONSCOPE)))).collect(Collectors.toList());
        List<NodeRef> sharedCollections =  collectionNodes.stream().filter(n -> {
            String scope = (String)nodeService.getProperty(n, QName.createQName(CCConstants.CCM_PROP_MAP_COLLECTIONSCOPE));
            return "CUSTOM_PUBLIC".equals(scope) || "CUSTOM".equals(scope);
        }).collect(Collectors.toList());


        AuthorityService authorityService = AuthorityServiceFactory.getLocalService();
        Set<String> groupSet = authorityService.getMemberships(userName);
        ArrayList<EduGroup> allEduGroups = AuthenticationUtil.runAsSystem(() -> authorityService.getAllEduGroups(userName));
        List<String> groupList = groupSet.stream().map(g ->  (String)authorityService.getAuthorityProperty(g,CCConstants.CM_PROP_AUTHORITY_AUTHORITYDISPLAYNAME)).collect(Collectors.toList());
        User user = authorityService.getUser(userName);

        /**
         * @TODO use profile data or something dynamic determine locale and timezone
         */
        ZoneId zone = ZoneId.of("Europe/Berlin");
        Locale locale = Locale.GERMANY;

        Date firstLogin = (Date)user.getProperties().get(CCConstants.PROP_USER_ESFIRSTLOGIN);
        Date lastLogin = (Date)user.getProperties().get(CCConstants.PROP_USER_ESLASTLOGIN);
        String role = (String)user.getProperties().get(CCConstants.CM_PROP_PERSON_EDU_SCHOOL_PRIMARY_AFFILIATION);
        List<String> roles = role == null ? null : Stream.of(role).map(r -> I18nAngular.getTranslationAngular("common","USER.PRIMARY_AFFILIATION."+r)).collect(Collectors.toList());
        EduGroup eduGroup = allEduGroups != null && !allEduGroups.isEmpty() ? allEduGroups.get(0) : null;
        String secondaryUserName = (String)user.getProperties().get(CCConstants.PROP_USER_SECONDARY_IDS);
        PDFReport.Data.DataBuilder reportData = PDFReport.Data.builder()
                .userName(userName)
                .secondaryUserName(secondaryUserName)
                .firstName(user.getGivenName())
                .lastName(user.getSurname())
                .firstLogin(formatDate(firstLogin,zone,locale,FormatStyle.MEDIUM,true))
                .lastLogin(formatDate(lastLogin,zone,locale,FormatStyle.MEDIUM, true))
                .currentDate(formatDate(new Date(),zone,locale,FormatStyle.MEDIUM,false))
                .email(user.getEmail())
                .roles(roles)
                .privateCollections(getNameList(privateCollections))
                .sharedCollections(getNameList(sharedCollections))
        //todo
                .ratings(List.of())
                .feedbacks(getNameList(feedBacks))
                .comments(getNameList(comments))
                .groupList(groupList);

        if(eduGroup != null) {
            reportData.schoolName(eduGroup.getGroupId());
            reportData.schoolDisplayName(eduGroup.getGroupDisplayName());
        }

        String reportDirectory = rootPath.concat("/report");
        File dir = new File(reportDirectory);
        if (!dir.exists()) {
            boolean mkdirs = dir.mkdirs();// creates parent folders as needed
            if (!mkdirs) {
                throw new RuntimeException("Unable to create directory " + dir.getAbsolutePath());
            }
        }

        report.report(reportData.build(), dir );
    }

    private List<String> getNameList(List<NodeRef> nodes){
        return nodes.stream()
                .map(n -> (String)nodeService.getProperty(n, ContentModel.PROP_NAME))
                .collect(Collectors.toList());
    }

    private NodeRef persistAndCleanup(String userName, File target, String rootPath) {
        try {
            NodeRef nodeRef = getTargetNode(userName);
            permissionService.setPermission(nodeRef,userName,PermissionService.CONSUMER,true);
            nodeService.removeAspect(nodeRef,ContentModel.ASPECT_VERSIONABLE);
            ContentWriter writer = contentService.getWriter(nodeRef, ContentModel.PROP_CONTENT, true);
            writer.addListener(() -> {
                try {
                    FileUtils.deleteDirectory(new File(rootPath));
                    FileUtils.delete(target);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                sendMail(userName,nodeRef);
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
            return new Utils().getNodeRef(parentId, CCConstants.CCM_TYPE_IO, getFileName());
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    private String getFileName(){
        Date date = new Date(); // or your custom date
        SimpleDateFormat formatter = new SimpleDateFormat("yyMMdd");
        String formatted = formatter.format(date);
        return formatted.concat("_").concat(fileName).concat(".zip");
    }

    private void createStructure(String rootPath, String subPath, HashMap<NodeRef, String> pathMap) throws IOException {
        for(Map.Entry<NodeRef,String> e : pathMap.entrySet()){
            NodeRef nodeRef = e.getKey();
            String path = e.getValue();
            String nodePath = rootPath.concat("/" +subPath+"/").concat(path);
            File dir = new File(nodePath);
            if (!dir.exists()) {
                boolean mkdirs = dir.mkdirs();// creates parent folders as needed
                if (!mkdirs) {
                    throw new RuntimeException("Unable to create directory " + dir.getAbsolutePath());
                }
            }
            String fileName = nodePath.concat("/"+nodeService.getProperty(nodeRef,ContentModel.PROP_NAME));
            writeNodePropertiesToJson(nodeRef,new File(fileName+".json"));
            writeContent(nodeRef,ContentModel.PROP_CONTENT,new File(fileName));
            writeContent(nodeRef,QName.createQName(CCConstants.CCM_PROP_IO_USERDEFINED_PREVIEW),new File(fileName+".png"));
        }
    }

    private NodeRefResult getUserHomeNodes(String userName, String scope){
        NodeRef userHome = getUserHome(userName, scope);
        if(userHome == null){
            return NodeRefResult.builder().nodes(new ArrayList<>()).ignored(new ArrayList<>()).build();
        }
        List<NodeRef> levelOne = nodeService.getChildAssocs(userHome).stream().map(ChildAssociationRef::getChildRef).collect(Collectors.toList());
        // remove shared folder
        List<NodeRef> ignored = levelOne.stream().filter(n -> CCConstants.CCM_VALUE_MAP_TYPE_EDUGROUP.equals(nodeService.getProperty(n,propMapType))).collect(Collectors.toList());
        levelOne = levelOne.stream().filter(n -> !CCConstants.CCM_VALUE_MAP_TYPE_EDUGROUP.equals(nodeService.getProperty(n,propMapType))).collect(Collectors.toList());

        List<NodeRef> result = new ArrayList<>(levelOne);
        levelOne.forEach(n -> {
            QName type = nodeService.getType(n);
                if(QName.createQName(CCConstants.CCM_TYPE_MAP).equals(type) || QName.createQName(CCConstants.CM_TYPE_FOLDER).equals(type) ){
                    result.addAll(NodeServiceFactory.getLocalService().getChildrenRecursive(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, n.getId(), null, RecurseMode.All));
                }
        });
        return NodeRefResult.builder().nodes(result).ignored(ignored).build();
    }

    List<NodeRef> getCollectionNodes(String userName){
        List<NodeRef> all = personLifecycleService.getAllNodeRefs(userName,CCConstants.CCM_TYPE_MAP,null).stream().
                filter((ref)->nodeService.hasAspect(ref,QName.createQName(CCConstants.CCM_ASPECT_COLLECTION))).
                collect(Collectors.toList());
        all.addAll(personLifecycleService.getAllNodeRefs(userName,CCConstants.CCM_TYPE_IO,null).stream().
                filter((ref)->nodeService.hasAspect(ref,QName.createQName(CCConstants.CCM_ASPECT_COLLECTION_IO_REFERENCE))).
                collect(Collectors.toList()));
       return all;
    }

    List<NodeRef> getSharedNodes(String userName, String scope, List<NodeRef> filesToIgnore){
        List<NodeRef> all = personLifecycleService.getAllNodeRefs(userName, CCConstants.CCM_TYPE_IO,scope).stream().
                filter((ref)->!filesToIgnore.contains(ref)).
                collect(Collectors.toList());
        all.addAll(personLifecycleService.getAllNodeRefs(userName, CCConstants.CCM_TYPE_MAP,scope).stream().
                filter((ref)->!filesToIgnore.contains(ref)).
                filter((ref)->!nodeService.hasAspect(ref, QName.createQName(CCConstants.CCM_ASPECT_COLLECTION))).
                collect(Collectors.toList()));
        return all;
    }

    private NodeRef getUserHome(String userName, String scope){
        NodeRef personNodeRef = personService.getPersonOrNull(userName);
        NodeRef homeFolder;
        if(scope==null){
            homeFolder = personLifecycleService.getHomeFolder(personNodeRef);
            if(homeFolder==null){
                log.info("Person "+userName+" does not have a home folder, skipping it");
                return null;
            }
        }
        else{
            ScopeUserHomeService scopeUserHomeService = ScopeUserHomeServiceFactory.getScopeUserHomeService();
            homeFolder = scopeUserHomeService.getUserHome((String) nodeService.getProperty(personNodeRef, QName.createQName(CCConstants.CM_PROP_PERSON_USERNAME)), scope, false);
            if(homeFolder==null){
                log.info("Person "+userName+" does not have a scope folder for "+scope+", skipping it");
                return null;
            }
        }
        return homeFolder;
    }


    private Map<NodeRef, Optional<NodeRef>>  createChildParentMap(List<NodeRef> nodeRefs){
        if(!metadataExport) return new HashMap<>();
        return nodeRefs.stream().collect(Collectors.toMap(n -> n, n -> {
            try {
                NodeRef parent = nodeService.getPrimaryParent(n).getParentRef();
                if(nodeRefs.contains(parent)) {
                    return Optional.of(parent);
                }
            } catch (AccessDeniedException e) {
            }
            return Optional.empty();
        }));
    }

    private HashMap<NodeRef, String> buildPathMap(Map<NodeRef, Optional<NodeRef>> childParentMap){
        HashMap<NodeRef, String> pathMap = new HashMap<>();
        childParentMap.forEach((k,v)->{
            String path = pathMap.get(k);
            if(path == null){
                NodeRef pathEle = k;
                // build path from child to ancestors
                do{
                    String pathEleName = (String)nodeService.getProperty(pathEle, ContentModel.PROP_NAME);
                    path = (path == null) ? pathEleName : pathEleName.concat("/"+path);
                    pathEle = childParentMap.get(pathEle).orElse(null);
                }while(pathEle != null);

                // duplicate handling
                int suffix = 0;
                while(pathMap.values().contains(path)){
                    if(suffix > 0){
                        path = path.substring(0,path.length()-3);
                    }
                    suffix++;
                    path = path.concat("_"+suffix);
                };
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
        if(!binaryExport) return;
        if(nodeService.getType(nodeRef).equals(QName.createQName(CCConstants.CCM_TYPE_IO))
                && !nodeService.hasAspect(nodeRef,QName.createQName(CCConstants.CCM_ASPECT_COLLECTION_IO_REFERENCE))){
            ContentReader reader = contentService.getReader(nodeRef, contentProperty);
            if(reader != null && reader.exists()){
                try (InputStream in = reader.getContentInputStream(); OutputStream out = new FileOutputStream(outputFile)) {
                    IOUtils.copy( in,out);
                }
            }
        }
    }

    private void sendMail(String user, NodeRef nodeRef){
        NodeRef personRef = personService.getPersonOrNull(user);
        String firstname = (String)nodeService.getProperty(personRef, ContentModel.PROP_FIRSTNAME);
        String lastName = (String)nodeService.getProperty(personRef,ContentModel.PROP_LASTNAME);
        String email = (String)nodeService.getProperty(personRef, ContentModel.PROP_EMAIL);
        if(email == null) return;
        Map<String, String> replace = new HashMap<>();
        replace.put("firstName", firstname);
        replace.put("lastName", lastName);
        replace.put("link", URLHelper.getNgRenderNodeUrl(nodeRef.getId(), null, true));
        replace.put("retentionPeriod", Duration.parse(retentionPeriod).toDays()+"");
        try {
            String template = "gdpr";
            MailTemplate.sendMail(email, template, replace);
        } catch (Exception e) {
            log.warn("Can not send status notify mail to user: " + e.getMessage(), e);
        }
    }



    private void archive(File directory, File destination) throws IOException, ArchiveException {
        String format = FileNameUtils.getExtension(destination.toPath());
        new Archiver().create(format, destination, directory);
    }

    public boolean requestDataProtectionExport(String user){
        if(!user.equals(AuthenticationUtil.getFullyAuthenticatedUser())){
            boolean isAdmin = AuthorityServiceHelper.isAdmin();
            if(!isAdmin){
                throw new SecurityException("admin rights required");
            }
        }
        DataProtectionQueueEntry entry = queue.get(user);
        if(entry == null){
            queue.add(user);
            return true;
        }else if(entry.getStatus().equals(DataProtectionQueue.Status.FINISHED.toString())){
            removeNode(entry.getNode_id());
            entry.setRequested(new Date());
            entry.setStatus(DataProtectionQueue.Status.REQUESTED.toString());
            entry.setNode_id(null);
            entry.setFinished(null);
            queue.update(entry);
            return true;
        }
        return false;
    }

    private static void removeNode(String nodeId) {
        org.edu_sharing.service.nodeservice.NodeService eduNodeService = NodeServiceFactory.getLocalService();
        eduNodeService.removeNode(nodeId,null,false);
    }

    public DataProtectionQueueEntry getDataProtectionQueueEntry(String user){
        return queue.get(user);
    }

    private String formatDate(Date date, ZoneId zone, Locale locale, FormatStyle style, boolean includeTime) {
        if(date == null) return null;
        ZonedDateTime zonedDateTime = date.toInstant().atZone(zone);

        DateTimeFormatter formatter = (includeTime) ? DateTimeFormatter.ofLocalizedDateTime(style) : DateTimeFormatter.ofLocalizedDate(style);
        formatter = formatter.withLocale(locale)
                .withZone(zone);

        return formatter.format(zonedDateTime);
    }

    @Data
    @Builder
    public static class NodeRefResult{
        List<NodeRef> nodes;
        List<NodeRef> ignored;
    }
}
