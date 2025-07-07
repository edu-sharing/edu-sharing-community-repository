package org.edu_sharing.repository.server.importer;

import lombok.extern.slf4j.Slf4j;
import org.alfresco.repo.policy.BehaviourFilter;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.version.Version2Model;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.namespace.QName;
import org.edu_sharing.alfresco.service.search.CMISSearchHelper;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.metadataset.v2.MetadataSet;
import org.edu_sharing.metadataset.v2.tools.MetadataHelper;
import org.edu_sharing.metadataset.v2.tools.MetadataSearchHelper;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.repository.server.tools.cache.RepositoryCache;
import org.edu_sharing.service.search.Suggestion;
import org.springframework.context.ApplicationContext;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class FactualTermDisplayUpdater {

    String appId = ApplicationInfoList.getHomeRepository().getAppId();
    MetadataSet mds;

    ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
    ServiceRegistry serviceRegistry = applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY, ServiceRegistry.class);
    BehaviourFilter policyBehaviourFilter = applicationContext.getBean("policyBehaviourFilter", BehaviourFilter.class);
    NodeService nodeService = serviceRegistry.getNodeService();
    RepositoryCache repositoryCache = applicationContext.getBean(RepositoryCache.class);

    public FactualTermDisplayUpdater() throws Exception {
        mds = MetadataHelper.getMetadataset(ApplicationInfoList.getHomeRepository(),"-default-");
    }


    public void updateDisplayStrings(String key) throws IllegalArgumentException {
        AuthenticationUtil.runAsSystem(()->{
            runForStore(key, StoreRef.STORE_REF_WORKSPACE_SPACESSTORE);
            runForStore(key, StoreRef.STORE_REF_ARCHIVE_SPACESSTORE);
            runForStore(key, new StoreRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getProtocol(),Version2Model.STORE_ID));
            return null;
        });
    }

    private void runForStore(String key, StoreRef storeRef) throws IllegalArgumentException {
        Map<String,Object> filter = new HashMap<>();
        filter.put(CCConstants.CCM_PROP_IO_REPL_CLASSIFICATION_KEYWORD, key);
        List<NodeRef> nodeRefs = CMISSearchHelper.fetchNodesByTypeAndFilters(CCConstants.CCM_TYPE_IO,filter,storeRef);
        log.info("found {} io's with classification_keyword:{} in store:{}", nodeRefs.size(), key, storeRef);
        if(storeRef.equals(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE)
                || storeRef.equals(StoreRef.STORE_REF_ARCHIVE_SPACESSTORE)){
            List<NodeRef> nodeRefsMap = CMISSearchHelper.fetchNodesByTypeAndFilters(CCConstants.CCM_TYPE_MAP,filter,storeRef);
            nodeRefs.addAll(nodeRefsMap);
            log.info("found {} map's with classification_keyword:{} in store:{}", nodeRefsMap.size(), key, storeRef);
        }

        for(NodeRef nodeRef : nodeRefs){
            resetDisplayProperty(key, nodeRef);
        }
    }

    public void resetDisplayProperty(NodeRef nodeRef){
        resetDisplayProperty(null, nodeRef);
    }

    private void resetDisplayProperty(String key, NodeRef nodeRef) {
        List<String> keys = (List<String>) nodeService.getProperty(nodeRef, QName.createQName(CCConstants.CCM_PROP_IO_REPL_CLASSIFICATION_KEYWORD));
        if(keys == null || keys.isEmpty()){
            return;
        }
        ArrayList<String> displays = new ArrayList<>();
        for(String k : keys){
            List<? extends Suggestion> suggestions = MetadataSearchHelper.getSuggestions(appId, mds, "ngsearch",
                    CCConstants.getValidLocalName(CCConstants.CCM_PROP_IO_REPL_CLASSIFICATION_KEYWORD), k, null);
            if(suggestions == null || suggestions.isEmpty()){
                log.info("no caption value found for key: {} nodeRef:{}", k, nodeRef);
                continue;
            }
            displays.add(suggestions.get(0).getDisplayString());
        }
        if(displays.isEmpty()){
            log.info("no caption values found nodeRef:{}", nodeRef);
            return;
        }
        log.info("updateing;{};{}", nodeRef, key);
        serviceRegistry.getRetryingTransactionHelper().doInTransaction(()->{
            try {

                policyBehaviourFilter.disableBehaviour(nodeRef);
                setProperty(nodeRef,QName.createQName(CCConstants.CCM_PROP_IO_REPL_CLASSIFICATION_KEYWORD_DISPLAY),displays);
                repositoryCache.remove(nodeRef.getId());
            }finally {
                policyBehaviourFilter.enableBehaviour(nodeRef);
            }
            return null;
        });
    }

    private void setProperty(NodeRef nodeRef, QName qName, Serializable serializable){
        nodeService.setProperty(nodeRef, qName, serializable);
        if(nodeRef.getStoreRef().getIdentifier().equals(Version2Model.STORE_ID)){
            QName qnameV = QName.createQName(Version2Model.NAMESPACE_URI, Version2Model.PROP_METADATA_PREFIX + qName.toString());
            nodeService.setProperty(nodeRef, qnameV, serializable);
        }
    }
}
