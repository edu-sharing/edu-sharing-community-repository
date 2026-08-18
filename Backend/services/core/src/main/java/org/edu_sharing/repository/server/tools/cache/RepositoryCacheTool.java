package org.edu_sharing.repository.server.tools.cache;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.MCAlfrescoAPIClient;

import org.edu_sharing.service.nodeservice.NodeServiceFactory;
import org.edu_sharing.service.nodeservice.RecurseMode;
import org.springframework.context.ApplicationContext;

@Slf4j
public class RepositoryCacheTool {


    ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();

    RepositoryCache repositoryCache = applicationContext.getBean(RepositoryCache.class);
    ServiceRegistry sr = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);
    NodeService nodeService = sr.getNodeService();
    NodeService nodeServiceAlfresco = (NodeService) AlfAppContextGate.getApplicationContext().getBean("alfrescoDefaultDbNodeService");
    MCAlfrescoAPIClient apiClient = new MCAlfrescoAPIClient();

    /**
     * creates a new cache object and puts all subobjects of rootfolderId. when it's
     * finished the new cache object becomes the current cache object
     * <p>
     * usefull when a cache is already filled but must be refreshed
     */
    public void buildNewCache(String rootfolderId) {
        Map<String, Map<String, Object>> newCache = new ConcurrentHashMap<>();

        long startMillies = System.currentTimeMillis();
        log.info("starting getChildrenRecursive");
        Map<NodeRef, Map<String, Object>> childRecursive = buildCache(rootfolderId, CCConstants.CCM_TYPE_IO);
        log.info("getChildrenRecursive returned.starting to copy to cachemap. size:{}", childRecursive.size());
        for (Map.Entry<NodeRef, Map<String, Object>> entry : childRecursive.entrySet()) {
            newCache.put(entry.getKey().getId(), entry.getValue());
        }

        long endMillies = System.currentTimeMillis();
        long diff = (endMillies - startMillies) / 1000;
        long diffMinutes = diff / 60;
        log.info("copy to cachemap finished in seconds:{} around {} minutes size:{} linking the new cache to the cache reference", diff, diffMinutes, newCache.size());

        repositoryCache.setCache(newCache);
        // clear facet cache
        FacetCache.getFacetCache().clear();
        log.info("Facets cleared");
    }

    private static final int THREAD_COUNT = Math.max(1, Math.min(3, Runtime.getRuntime().availableProcessors() - 1));
    private final ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT, r -> {
        Thread t = new Thread(r);
        t.setPriority(Thread.NORM_PRIORITY - 1);
        return t;
    });

    /**
     * builds cache directly to the current cache object, those Objects that are
     * already in cache will not be refreshed
     * <p>
     * usefull after a server reboot cause it's adjuvant to the cachbuilding by user
     * actions
     */
    public void buildStickyCache(String rootfolderId) throws Throwable {


        log.info("preparing first level subfolders for folder: {}", nodeService.getProperty(new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, rootfolderId), ContentModel.PROP_NAME));
        List<ChildAssociationRef> childAssocRefs = nodeService
                .getChildAssocs(new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, rootfolderId));

        int i = 0;

        List<Callable<Void>> threads = new ArrayList<>();

        List<ChildAssociationRef> childRefsThread = new ArrayList<>();

        long time = System.currentTimeMillis();

        log.info("THREAD_COUNT:{}", THREAD_COUNT);
        int modulo = (childAssocRefs.size() > THREAD_COUNT) ? (childAssocRefs.size() / THREAD_COUNT) : 1;

        List<List<String>> threadNodes = new ArrayList<>();

        for (ChildAssociationRef childAssocRef : childAssocRefs) {
            childRefsThread.add(childAssocRef);
            if (i % modulo == 0) {
                List<String> tmp = new ArrayList<>();
                for (ChildAssociationRef childRef : childRefsThread) {
                    tmp.add(childRef.getChildRef().getId());
                }
                threadNodes.add(tmp);
                childRefsThread.clear();
            }
            i++;
        }

        int j = 0;
        for (List<String> threadData : threadNodes) {

            int nr = j;
            threads.add(() -> {

                RunAsWork<Void> runAs = () -> {
                    ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();

                    ServiceRegistry sr = (ServiceRegistry) applicationContext
                            .getBean(ServiceRegistry.SERVICE_REGISTRY);
                    NodeService nodeService = sr.getNodeService();

                    List<String> childRefPage = new ArrayList<>(threadData);
                    log.info("thread nr:{} got's {}", nr, childRefPage.size());
                    for (String cr : childRefPage) {
                        long startMillies = System.currentTimeMillis();

                        log.info("thread nr:{}\tstarting getChildrenRecursive for folder:{}", nr, nodeService
                                .getProperty(new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, cr), ContentModel.PROP_NAME));

                        try {
                            buildCache(cr, CCConstants.CCM_TYPE_IO);

                        } catch (Throwable e) {
                            log.error("thread nr:{}{}", nr, e.getMessage(), e);
                        }
                        long endMillies = System.currentTimeMillis();
                        long diff = (endMillies - startMillies) / 1000;
                        long diffMinutes = diff / 60;
                    }

                    return null;

                };

                AuthenticationUtil.runAsSystem(runAs);

                return null;
            });

            j++;
        }

        log.info("got {} threads", threads.size());
        executor.invokeAll(threads);
        time = (System.currentTimeMillis() - time);
        if (!threads.isEmpty()) {
            log.info("{} Threads finished ({}, {} s -> {}ms per entry)", THREAD_COUNT, threads.size(), time / 1000, time / threads.size());
        }

        FacetCache.getFacetCache().clear();
        log.info("Facets cleared");

        /*
         * MCAlfrescoAPIClient apiClient = (MCAlfrescoAPIClient)mcBaseClient; long
         * startMillies = System.currentTimeMillis();
         * logger.info("starting getChildrenRecursive"); HashMap<String, HashMap<String,
         * Object>> childRecursive =
         * apiClient.getChildrenRecursive(MCAlfrescoAPIClient.storeRef, rootfolderId,
         * CCConstants.CCM_TYPE_IO, null, true); long endMillies =
         * System.currentTimeMillis(); long diff = (endMillies - startMillies)/ 1000;
         * long diffMinutes = diff / 60; logger.
         * info("getChildrenRecursive returned. buildStickyCache finished in seconds:"
         * +diff+" around "+diffMinutes+" minutes" + " size:" +
         * childRecursive.size()+"."); FacetteCache.getFacetteCache().clear();
         * logger.info("Facettes cleared");
         */

    }

    private Map<NodeRef, Map<String, Object>> buildCache(String parent, String type) {
        List<NodeRef> refs = NodeServiceFactory.getInstance().getLocalService().getChildrenRecursive(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, parent, Collections.singletonList(type), RecurseMode.Folders);
        return refs.stream().collect(Collectors.toMap(ref -> ref,
                ref -> {
                    try {
                        return apiClient.getPropertiesCached(ref, true, true, false, nodeServiceAlfresco);
                    } catch (Exception e) {
                        log.debug(e.getMessage());
                        return new HashMap<>();
                    }
                }));
    }

}
