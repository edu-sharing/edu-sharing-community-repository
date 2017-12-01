package org.edu_sharing.restservices;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.restservices.statistic.v1.model.Filter;
import org.edu_sharing.restservices.statistic.v1.model.FilterEntry;
import org.edu_sharing.restservices.statistic.v1.model.StatisticEntity;
import org.edu_sharing.restservices.statistic.v1.model.StatisticEntry;
import org.edu_sharing.restservices.statistic.v1.model.Statistics;
import org.edu_sharing.service.statistic.StatisticService;
import org.edu_sharing.service.statistic.StatisticServiceFactory;

public class StatisticDao {

	public Statistics get(String context, List<String> properties, Filter filter) throws DAOException {

		try {

			org.edu_sharing.service.statistic.Filter backendFilter = new org.edu_sharing.service.statistic.Filter();

			for (FilterEntry entry : filter.getEntries()) {
				org.edu_sharing.service.statistic.FilterEntry backendFilterEntry = new org.edu_sharing.service.statistic.FilterEntry();
				backendFilterEntry.setProperty(entry.getProperty());
				backendFilterEntry.setValues(entry.getValues());
				backendFilter.getEntries().add(backendFilterEntry);
			}

			StatisticService statisticService = StatisticServiceFactory
					.getStatisticService(ApplicationInfoList.getHomeRepository().getAppId());
			org.edu_sharing.service.statistic.Statistics statisticsBackend = statisticService.get(context, properties,
					backendFilter);
			Statistics statistics = new Statistics();
			for (org.edu_sharing.service.statistic.StatisticEntry entry : statisticsBackend.getEntries()) {
				StatisticEntry statEntry = new StatisticEntry();
				statEntry.setProperty(entry.getProperty());
				List<StatisticEntity> entities = new ArrayList<StatisticEntity>();
				for (Map.Entry<String, Integer> statEntity : entry.getStatistic().entrySet()) {
					StatisticEntity entity = new StatisticEntity();
					entity.setValue(statEntity.getKey());
					entity.setCount(statEntity.getValue());
					entities.add(entity);
				}

				statEntry.setEntities(entities);
				statistics.getEntries().add(statEntry);
			}

			return statistics;
		} catch (Throwable t) {
			throw DAOException.mapping(t);
		}
	}

}
