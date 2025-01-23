package org.edu_sharing.service.statistic;

import java.util.*;

import org.alfresco.repo.search.impl.solr.ESSearchParameters;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchParameters;
import org.alfresco.service.cmr.search.SearchParameters.FieldFacet;
import org.alfresco.service.cmr.search.SearchParameters.FieldFacetMethod;
import org.alfresco.service.cmr.search.SearchParameters.FieldFacetSort;
import org.alfresco.util.Pair;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.metadataset.v2.MetadataReader;
import org.edu_sharing.metadataset.v2.MetadataSet;
import org.edu_sharing.metadataset.v2.tools.MetadataHelper;
import org.edu_sharing.repository.client.rpc.SearchResult;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.AuthenticationTool;
import org.edu_sharing.repository.server.MCAlfrescoBaseClient;
import org.edu_sharing.repository.server.RepoFactory;
import org.edu_sharing.alfresco.repository.server.authentication.Context;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.springframework.context.ApplicationContext;

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
			String query = "";

			if (context != null) {

				if (context.equals("-root-")) {
					context = client.getHomeFolderID((String) client.getAuthenticationInfo().get(CCConstants.AUTH_USERNAME));
				}
				
				String pathParent = (context != null) ? client.getPath(context) : "";

				query = "PATH:\""+pathParent+"//.\"";
			}

			for (FilterEntry entry : filter.getEntries()) {
				for (String val : entry.getValues()) {
					
					String prop = entry.getProperty();
					String shortProp = CCConstants.getValidLocalName(prop);
					if(shortProp != null) prop = shortProp;
					
					prop = "@" + prop.replaceFirst(":", "\\\\:");
					query += (query.length() > 0) ? " AND " : "";
					query += prop + ":\"" + val + "\"";
				}
			}

			SearchResult result = this.client.searchSolr(query, 0, 0, properties, 1, -1);
			Map<String, Map<String, Integer>> facettes = result.getCountedProps();

			Statistics stats = new Statistics();
			for (Map.Entry<String, Map<String, Integer>> entry : facettes.entrySet()) {
				StatisticEntry statEntry = new StatisticEntry();
				statEntry.setProperty(entry.getKey());
				statEntry.setStatistic(entry.getValue());
				stats.getEntries().add(statEntry);
			}
			return stats;

	}
}
