package org.edu_sharing.service.statistic;

public class StatisticServiceFactory {
	public static StatisticService getStatisticService(String appId){
		return new StatisticServiceImpl(appId);
	}
}
