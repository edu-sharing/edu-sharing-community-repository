package org.edu_sharing.service.statistic;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
import org.edu_sharing.alfresco.repository.server.authentication.Context;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.AuthenticationTool;
import org.edu_sharing.repository.server.MCAlfrescoBaseClient;
import org.edu_sharing.repository.server.RepoFactory;
import org.edu_sharing.repository.server.SearchResultNodeRef;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.restservices.shared.NodeSearch;
import org.edu_sharing.service.search.SearchService;
import org.edu_sharing.service.search.SearchServiceFactory;
import org.edu_sharing.service.search.model.SearchToken;
import org.springframework.context.ApplicationContext;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class StatisticServiceImpl implements StatisticService {

	ApplicationInfo appInfo;
	Map<String, String> authInfo;
	MCAlfrescoBaseClient client;
	AuthenticationTool authTool;

	ApplicationContext alfApplicationContext = AlfAppContextGate.getApplicationContext();
	org.alfresco.service.cmr.search.SearchService searchService = (org.alfresco.service.cmr.search.SearchService) alfApplicationContext
			.getBean("scopedSearchService");
	
	public StatisticServiceImpl(String appId) {
		try {
			this.appInfo = ApplicationInfoList.getRepositoryInfoById(appId);
			this.authTool = RepoFactory.getAuthenticationToolInstance(appId);
			this.authInfo = this.authTool.validateAuthentication(Context.getCurrentInstance().getCurrentInstance().getRequest().getSession());
			this.client = (MCAlfrescoBaseClient) RepoFactory.getInstance(appId, this.authInfo);
		} catch (Throwable e) {
			throw new RuntimeException(e.getMessage());
		}
	}

	

	@Override
	public Statistics get(String context, List<String> properties, Filter filter) throws Throwable {
		BoolQuery.Builder query = QueryBuilders.bool();

		if (context != null) {

			if (context.equals("-root-")) {
				context = client.getHomeFolderID((String) client.getAuthenticationInfo().get(CCConstants.AUTH_USERNAME));
			}

			String pathParent = (context != null) ? client.getPath(context) : "";
			query.must(m -> m.wildcard(QueryBuilders.wildcard()
					.field("fulldisplaypath")
					.value(pathParent.replaceFirst("/","")+"*")
					.build()
			));

		}

		for (FilterEntry entry : filter.getEntries()) {
			for (String val : entry.getValues()) {

				String prop = entry.getProperty();
				String shortProp = Objects.requireNonNullElse(CCConstants.getValidLocalName(prop), prop);

				query.must(m -> m.term(t -> t.field("properties." + shortProp).value(val)));
			}
		}

		SearchService localService = SearchServiceFactory.getInstance().getLocalService();
		SearchToken searchToken = new SearchToken();
		searchToken.setFrom(0);
		searchToken.setMaxResult(0);
		searchToken.setElasticQuery(query.build());
		searchToken.setFacets(properties);
		SearchResultNodeRef search = localService.search(searchToken);

		Statistics stats = new Statistics();
		search.getFacets().forEach(f -> {
			StatisticEntry statEntry = new StatisticEntry();
			statEntry.setProperty(f.getProperty());
			statEntry.setStatistic(f.getValues().stream().collect(Collectors.toMap(NodeSearch.Facet.Value::getValue, NodeSearch.Facet.Value::getCount)));
			stats.getEntries().add(statEntry);
		});
		return stats;
	}
}
