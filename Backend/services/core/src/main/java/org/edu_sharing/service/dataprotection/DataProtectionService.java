package org.edu_sharing.service.dataprotection;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import jakarta.annotation.PostConstruct;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.permissions.AccessDeniedException;
import org.alfresco.service.cmr.repository.*;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.namespace.QName;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.examples.Archiver;
import org.apache.commons.compress.utils.FileNameUtils;
import org.apache.commons.io.FileUtils;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.edu_sharing.alfresco.workspace_administration.NodeServiceInterceptor;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.tools.UserEnvironmentTool;
import org.edu_sharing.repository.server.tools.mailtemplates.MailTemplate;
import org.edu_sharing.repository.tools.URLHelper;
import org.edu_sharing.service.authentication.ScopeUserHomeService;
import org.edu_sharing.service.authentication.ScopeUserHomeServiceFactory;
import org.edu_sharing.service.authority.AuthorityServiceHelper;
import org.edu_sharing.service.lifecycle.PersonLifecycleService;
import org.edu_sharing.service.lifecycle.Utils;
import org.edu_sharing.service.nodeservice.NodeServiceFactory;
import org.edu_sharing.service.nodeservice.RecurseMode;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import java.io.*;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.context.event.EventListener;

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
    DataProtectionConfig config;

    @Autowired
    DataProtectionQueue queue;

    @Value("${repository.dataprotection.retentionPeriod:PT240H}")
    private String retentionPeriod;

    PersonLifecycleService personLifecycleService = new PersonLifecycleService();

    QName propMapType = QName.createQName(CCConstants.CCM_PROP_MAP_TYPE);


    String systemFolder;


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

    public void cleanExpired(){
        AuthenticationUtil.runAsSystem(()->{
            List<ChildAssociationRef> childAssocs = nodeService.getChildAssocs(new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, systemFolder));
            List<NodeRef> toRemove = new ArrayList<>();
            childAssocs.forEach(childAssocRef -> {
                NodeRef nodeRef = childAssocRef.getChildRef();
                Date modified = (Date)nodeService.getProperty(nodeRef, ContentModel.PROP_MODIFIED);
                if((System.currentTimeMillis() - modified.getTime()) > Duration.parse(this.retentionPeriod).toMillis()){
                    toRemove.add(nodeRef);
                }
            });
            toRemove.forEach(nodeRef -> {
                log.info("removing gdpr export {} {}",nodeService.getProperty(nodeRef, ContentModel.PROP_NAME),nodeRef);
                nodeService.deleteNode(nodeRef);
            });
            return null;
        });
    }

    public void startExport(){
        List<String> allUsers = queue.getAllUsers();
        for(String user: allUsers) {
            startExport(user);
            queue.removeUsers(List.of(user));
        }
    }

    public void startExport(String userName){
        // be sure systemfolder and target node is created with as admin user so that this files will not be included in export zip
        AuthenticationUtil.runAsSystem(()-> {
            prepare(userName);
            return null;});
        AuthenticationUtil.runAs(() -> {
            exportUserNodes(userName);
            return null;
        }, userName);
    }

    public void prepare(String userName){
        getTargetNode(userName);
    }

    public void exportUserNodes(String userName) throws IOException, ArchiveException {
        String rootPath = config.getMainPath().concat("/"+userName);

        NodeRefResult userHomeResult = getUserHomeNodes(userName, null);
        createStructure(rootPath,"home",buildPathMap(createChildParentMap(userHomeResult.getNodes())));

        List<NodeRef> collectionNodes = getCollectionNodes(userName);
        createStructure(rootPath,"collection",buildPathMap(createChildParentMap(collectionNodes)));

        List<NodeRef> sharedNodes = getSharedNodes(userName,null, Stream.concat(userHomeResult.getIgnored().stream(),Stream.concat(userHomeResult.nodes.stream(),collectionNodes.stream())).collect(Collectors.toList()));
        createStructure(rootPath,"shared",buildPathMap(createChildParentMap(sharedNodes)));

        // safe
        NodeServiceInterceptor.setEduSharingScope("safe");
        NodeRefResult userHomeResultSafe = getUserHomeNodes(userName, "safe");
        createStructure(rootPath,"safe/home",buildPathMap(createChildParentMap(userHomeResultSafe.getNodes())));

        List<NodeRef> sharedNodesSafe = getSharedNodes(userName,"safe", Stream.concat(userHomeResultSafe.getIgnored().stream(),Stream.concat(userHomeResultSafe.nodes.stream(),collectionNodes.stream())).collect(Collectors.toList()));
        createStructure(rootPath,"safe/shared",buildPathMap(createChildParentMap(sharedNodesSafe)));
        NodeServiceInterceptor.setEduSharingScope(null);

        File target = new File(rootPath+".zip");
        archive(new File(rootPath), target);

        NodeRef nodeRef = AuthenticationUtil.runAsSystem(() ->
                persistAndCleanup(userName, target, rootPath)
        );
    }

    private NodeRef persistAndCleanup(String userName, File target, String rootPath) {
        try {
            NodeRef nodeRef = getTargetNode(userName);
            permissionService.setPermission(nodeRef,userName,PermissionService.CONSUMER,true);
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
        NodeRef nodeRef = AuthenticationUtil.runAsSystem(()-> {
            try {
                return new Utils().getNodeRef(systemFolder, userName.concat(".zip"));
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        });
        return nodeRef;
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
                if(nodeService.getType(n).equals(QName.createQName(CCConstants.CCM_TYPE_MAP))){
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
        if(nodeService.getType(nodeRef).equals(QName.createQName(CCConstants.CCM_TYPE_IO))){
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

    public void requestDataProtectionExport(String user){
        if(!user.equals(AuthenticationUtil.getFullyAuthenticatedUser())){
            boolean isAdmin = AuthorityServiceHelper.isAdmin();
            if(!isAdmin){
                throw new SecurityException("admin rights required");
            }
        }
        queue.addUser(user);
    }

    @Data
    @Builder
    public static class NodeRefResult{
        List<NodeRef> nodes;
        List<NodeRef> ignored;
    }
}
