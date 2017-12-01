package org.edu_sharing.service.statistic;

import java.util.List;
import java.util.Map;

public interface StatisticService {

	public Statistics get(String context, List<String> properties, Filter filter) throws Throwable;
	
}
