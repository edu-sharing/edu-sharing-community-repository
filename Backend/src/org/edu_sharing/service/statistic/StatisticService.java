package org.edu_sharing.service.statistic;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface StatisticService {

	public Statistics get(String context, List<String> properties, Filter filter) throws Throwable;

	long countForQuery(String mdsId, String queryId, String type, String customLucene) throws Throwable;

	public List<Map<String, Integer>> countFacettesForQuery(String mdsId, String queryId,
			String type, String lucene, Collection<String> facettes) throws Throwable;
	
}
