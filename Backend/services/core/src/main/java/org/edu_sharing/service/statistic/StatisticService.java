package org.edu_sharing.service.statistic;

import org.edu_sharing.restservices.search.v1.model.SearchFacet;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface StatisticService {

	Statistics get(String context, List<SearchFacet> properties, Filter filter) throws Throwable;
}
