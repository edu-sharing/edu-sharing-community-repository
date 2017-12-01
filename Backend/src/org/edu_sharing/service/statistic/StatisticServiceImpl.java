package org.edu_sharing.service.statistic;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.edu_sharing.repository.client.rpc.SearchResult;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.AuthenticationTool;
import org.edu_sharing.repository.server.MCAlfrescoBaseClient;
import org.edu_sharing.repository.server.RepoFactory;
import org.edu_sharing.repository.server.authentication.Context;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;

public class StatisticServiceImpl implements StatisticService {

	ApplicationInfo appInfo;
	HashMap<String, String> authInfo;
	MCAlfrescoBaseClient client;
	AuthenticationTool authTool;

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
