package org.edu_sharing.repository.server.jobs.quartz;



import java.text.SimpleDateFormat;
import java.util.Date;

import org.alfresco.model.ContentModel;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchParameters;
import org.alfresco.service.cmr.search.SearchService;
import org.apache.log4j.Logger;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.springframework.context.ApplicationContext;

public class TrashcanCleanerSolr {

	ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
	ServiceRegistry serviceRegistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);
	private NodeService nodeService = serviceRegistry.getNodeService();
	private SearchService searchService = serviceRegistry.getSearchService();
	
	Logger logger = Logger.getLogger(TrashcanCleanerSolr.class);
	
	Date to = null;
	int batchCount;
	
	private static final int PAGE_SIZE = 100;
	
	public TrashcanCleanerSolr(long timeToKeep, int batchCount) {
		this.to = new Date(System.currentTimeMillis() - timeToKeep);
		this.batchCount = batchCount;
	}
	
	public void exeute() {
		execute(0);
	}
	
	private void execute(int page) {
		logger.info("page:" + page);
		SearchParameters sp = new SearchParameters();
		sp.setLanguage(SearchService.LANGUAGE_LUCENE);
		sp.addStore(StoreRef.STORE_REF_ARCHIVE_SPACESSTORE);
		sp.setSkipCount(page);
		sp.setMaxItems(PAGE_SIZE);
		
		SimpleDateFormat dateFormater = new SimpleDateFormat("yyyy-MM-dd");
		sp.setQuery("(TYPE:\"ccm:io\" OR TYPE:\"ccm:map\") AND @sys\\:archivedDate:[MIN TO \"" + dateFormater.format(this.to)+ "\"]");
		
		logger.info("query:" + sp.getQuery());
		ResultSet resultSet = searchService.query(sp);
		
		
		logger.info("page " + page + " from " + resultSet.getNumberFound());
		
		for(NodeRef nodeRef : resultSet.getNodeRefs()) {
			if(StoreRef.STORE_REF_ARCHIVE_SPACESSTORE.equals(nodeRef.getStoreRef())) {
				logger.info("deleteing from archive:" + nodeRef +"  " + nodeService.getProperty(nodeRef, ContentModel.PROP_NAME));
				nodeService.deleteNode(nodeRef);
			}
		}
		
		if(resultSet.hasMore() && (page < (this.batchCount))) {
			execute(page + PAGE_SIZE);
		}
	}
	
}
