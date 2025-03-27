package org.edu_sharing.service.statistic;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface StatisticService {

	Statistics get(String context, List<String> properties, Filter filter) throws Throwable;
}
