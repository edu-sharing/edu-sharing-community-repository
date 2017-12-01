package org.edu_sharing.service.statistic;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.repo.search.impl.solr.ESSearchParameters;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchParameters;
import org.apache.lucene.queryParser.QueryParser;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.metadataset.v2.MetadataReaderV2;
import org.edu_sharing.metadataset.v2.MetadataSearchHelper;
import org.edu_sharing.metadataset.v2.MetadataSetV2;
import org.edu_sharing.repository.client.rpc.SearchResult;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.AuthenticationTool;
import org.edu_sharing.repository.server.MCAlfrescoBaseClient;
import org.edu_sharing.repository.server.RepoFactory;
import org.edu_sharing.repository.server.authentication.Context;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.springframework.context.ApplicationContext;

import com.drew.metadata.Metadata;

public class StatisticServiceImpl implements StatisticService {

	ApplicationInfo appInfo;
	HashMap<String, String> authInfo;
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
	public long countForQuery(String mdsId,String queryId,String type,String customLucene) throws Throwable {
		MetadataSetV2 set = MetadataReaderV2.getMetadataset(appInfo, mdsId);
		String lucene=set.getQueries().getBasequery();
		String basequery=set.getQueries().findQuery(queryId).getBasequery();
		if(basequery!=null && !basequery.trim().isEmpty()) {
			lucene+=" AND ("+basequery+")";
		}
		lucene+=" AND TYPE:\""+QueryParser.escape(type)+"\"";
		if(customLucene!=null)
			lucene+=" AND ("+customLucene+")";
		SearchParameters searchParameters = new ESSearchParameters();
		searchParameters.addStore(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE);
		searchParameters.setLanguage(org.alfresco.service.cmr.search.SearchService.LANGUAGE_LUCENE);
		searchParameters.setQuery(lucene);
		searchParameters.setSkipCount(0);
		searchParameters.setMaxItems(0);
		ResultSet result = searchService.query(searchParameters);
		return result.getNumberFound();
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
