package org.edu_sharing.repository.server.jobs.quartz;



import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
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
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.SearchResultNodeRef;
import org.edu_sharing.service.search.SearchServiceFactory;
import org.edu_sharing.service.search.model.SearchToken;
import org.edu_sharing.service.search.model.SortDefinition;
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
	
	List<NodeRef> list = new ArrayList<>();

	SimpleDateFormat dateFormater = new SimpleDateFormat("yyyy-MM-dd");

	Boolean execute;
	
	public TrashcanCleanerSolr(long timeToKeep, int batchCount, Boolean execute) {
		this.to = new Date(System.currentTimeMillis() - timeToKeep);
		this.batchCount = batchCount;
		this.execute = execute;
		if(this.execute == null) this.execute = Boolean.TRUE;
	}
	
	public void exeute() {
		execute(0);
		
		logger.info("collected " + list.size() +" nodes to delete");
		int deleted = 0;
		int failed = 0;
		for(NodeRef nodeRef : list) {
			logger.info("deleteing from archive:" + nodeRef);
			if(this.execute){
				try {
					nodeService.deleteNode(nodeRef);
					deleted++;
				} catch (Exception e) {
					/**
					 * i.e. the elasticsearch index is behind and still knows a node that was already
					 * removed from the archive store. skip it instead of aborting the whole job.
					 */
					failed++;
					logger.error("could not delete " + nodeRef + " from archive, continuing with next node: " + e.getMessage(), e);
				}
			}
		}
		logger.info("cleaning trashcan finished (collected: " + list.size() + ", deleted: " + deleted + ", failed: " + failed + ")");
	}
	
	private void execute(int page) {

		SearchToken searchToken = new SearchToken();
		searchToken.setFrom(page);
		searchToken.setMaxResult(batchCount);
		searchToken.setSortDefinition(new SortDefinition(List.of(CCConstants.getValidLocalName(ContentModel.PROP_ARCHIVED_DATE.toString())),List.of(true)));
		searchToken.setStoreProtocol(StoreRef.STORE_REF_ARCHIVE_SPACESSTORE.getProtocol());
		searchToken.setStoreName(StoreRef.STORE_REF_ARCHIVE_SPACESSTORE.getIdentifier());
		searchToken.setElasticQuery(QueryBuilders.bool()
				.must( m -> m.bool(b -> b
						.should(s -> s.term(t -> t.field("type").value("ccm:io")))
						.should(s -> s.term(t -> t.field("type").value("ccm:map")))
						.minimumShouldMatch("1"))
				)
				.must(m -> m.range(r -> r.term(t -> t.field("properties.sys:archivedDate.date")
					.lte(dateFormater.format(this.to))))
				)
				.build());

		org.edu_sharing.service.search.SearchService localService = SearchServiceFactory.getLocalService();
		SearchResultNodeRef search = localService.search(searchToken);
		logger.info("found " + search.getData().size() + " results");
		search.getData().forEach(n -> {
			NodeRef nodeRef = new NodeRef(new StoreRef(n.getStoreProtocol(),n.getStoreId()),n.getNodeId());
			if(StoreRef.STORE_REF_ARCHIVE_SPACESSTORE.equals(nodeRef.getStoreRef())) {
				/**
				 * use the properties of the search hit, the node may no longer exist in the repository
				 */
				Map<String,Object> properties = n.getProperties();
				logger.info("adding:" + nodeRef
						+ " " + ((properties != null) ? properties.get(ContentModel.PROP_NAME.toString()) : null)
						+ " " + ((properties != null) ? properties.get(ContentModel.PROP_ARCHIVED_DATE.toString()) : null));
				list.add(nodeRef);
			}else {
				logger.error("wrong store: " + nodeRef);
			}
		});
	}
	
}
