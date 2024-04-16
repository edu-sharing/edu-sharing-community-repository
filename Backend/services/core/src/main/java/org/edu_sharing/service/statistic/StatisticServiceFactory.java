package org.edu_sharing.service.statistic;

import org.edu_sharing.repository.server.tools.ApplicationInfoList;

public class StatisticServiceFactory {
	public static StatisticService getStatisticService(String appId){
		return new StatisticServiceImpl(appId);
	}

	public static StatisticService getLocalService() {
		return new StatisticServiceImpl(ApplicationInfoList.getHomeRepository().getAppId());
	}
}
