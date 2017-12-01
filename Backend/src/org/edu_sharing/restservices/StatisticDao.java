package org.edu_sharing.restservices;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.security.PersonService;
import org.apache.lucene.queryParser.QueryParser;
import org.edu_sharing.metadataset.v2.MetadataReaderV2;
import org.edu_sharing.repository.client.rpc.metadataset.MetadataSet;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.restservices.statistic.v1.model.Filter;
import org.edu_sharing.restservices.statistic.v1.model.FilterEntry;
import org.edu_sharing.restservices.statistic.v1.model.StatisticEntity;
import org.edu_sharing.restservices.statistic.v1.model.StatisticEntry;
import org.edu_sharing.restservices.statistic.v1.model.Statistics;
import org.edu_sharing.service.mime.MimeTypesV2;
import org.edu_sharing.service.statistic.StatisticService;
import org.edu_sharing.service.statistic.StatisticServiceFactory;
import org.edu_sharing.service.statistic.StatisticsGlobal;

import com.ibm.icu.text.SimpleDateFormat;

public class StatisticDao {
	private static final int DAY_HISTORY_COUNT = 14;
	private static final int MONTH_HISTORY_COUNT = 12;
	public static StatisticsGlobal getGlobal() throws DAOException {
		try {
			StatisticsGlobal statistics=new StatisticsGlobal();
			StatisticsGlobal.User user=new StatisticsGlobal.User();
			user.overall=countUser(null);
			statistics.setUser(user);			
			StatisticsGlobal.Materials materials=new StatisticsGlobal.Materials();
			materials.overall=(countElements(null));
			materials.image=(countElements(escapeProperty(CCConstants.LOM_PROP_TECHNICAL_FORMAT)+":\"image/*\""));
			materials.video=(countElements(escapeProperty(CCConstants.LOM_PROP_TECHNICAL_FORMAT)+":\"video/*\""));
			materials.link=(countElements(escapeProperty(CCConstants.CCM_PROP_IO_WWWURL)+":\"*\""));
			materials.text=(countElements(getQuery(escapeProperty(CCConstants.LOM_PROP_TECHNICAL_FORMAT),MimeTypesV2.WORD)));
			materials.spreadsheet=(countElements(getQuery(escapeProperty(CCConstants.LOM_PROP_TECHNICAL_FORMAT),MimeTypesV2.EXCEL)));
			materials.presentation=(countElements(getQuery(escapeProperty(CCConstants.LOM_PROP_TECHNICAL_FORMAT),MimeTypesV2.POWERPOINT)));
			statistics.setMaterials(materials);
			StatisticsGlobal.Licenses licenses=new StatisticsGlobal.Licenses();
			licenses.CC_0=(countElements(escapeProperty(CCConstants.CCM_PROP_IO_COMMONLICENSE_KEY)+":\"CC_0\""));
			licenses.CC_BY=(countElements(escapeProperty(CCConstants.CCM_PROP_IO_COMMONLICENSE_KEY)+":\"CC_BY*\""));
			licenses.PDM=(countElements(escapeProperty(CCConstants.CCM_PROP_IO_COMMONLICENSE_KEY)+":\"PDM\""));
			licenses.unknown=materials.overall-licenses.CC_0-licenses.CC_BY-licenses.PDM;
			statistics.setLicenses(licenses);
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");			
			Date today=new Date();
			Calendar c = Calendar.getInstance();
			c.add(Calendar.DATE, -DAY_HISTORY_COUNT); 
			List<StatisticsGlobal.History> historyDaily=new ArrayList<>();
			while(c.getTime().compareTo(today)<=0) {
				String date=sdf.format(c.getTime());
				String range="["+QueryParser.escape(date)+" TO "+QueryParser.escape(date)+"]";
				StatisticsGlobal.History history=new StatisticsGlobal.History();
				history.date=date;
				history.created=countElements(escapeProperty(CCConstants.CM_PROP_C_CREATED)+":"+range);
				history.modified=countElements(escapeProperty(CCConstants.CM_PROP_C_MODIFIED)+":"+range);
				//history.modified-=history.created; // dont count also created ones
				historyDaily.add(history);
				c.add(Calendar.DATE, 1);
			}
			
			c = Calendar.getInstance();
			c.add(Calendar.MONTH,-MONTH_HISTORY_COUNT); 
			List<StatisticsGlobal.History> historyMonthly=new ArrayList<>();
			while(c.getTime().compareTo(today)<=0) {
				c.set(Calendar.DAY_OF_MONTH, c.getActualMaximum(Calendar.DAY_OF_MONTH));
				String dateTo=sdf.format(c.getTime());
				c.set(Calendar.DAY_OF_MONTH, c.getActualMinimum(Calendar.DAY_OF_MONTH));
				String dateFrom=sdf.format(c.getTime());
				String range="["+QueryParser.escape(dateFrom)+" TO "+QueryParser.escape(dateTo)+"]";
				StatisticsGlobal.History history=new StatisticsGlobal.History();
				history.date=dateFrom.substring(0, dateFrom.length()-3);
				history.created=countElements(escapeProperty(CCConstants.CM_PROP_C_CREATED)+":"+range);
				history.modified=countElements(escapeProperty(CCConstants.CM_PROP_C_MODIFIED)+":"+range);
				//history.modified-=history.created; // dont count also created ones
				historyMonthly.add(history);
				c.add(Calendar.MONTH, 1);
			}
			
			
			statistics.setHistory_daily(historyDaily);
			statistics.setHistory_monthly(historyMonthly);
			return statistics;
		}
		catch(Throwable t) {
			throw DAOException.mapping(t);
		}
	}
	private static String escapeProperty(String property) {
		return "@"+CCConstants.getValidLocalName(property).replace(":","\\:");
	}
	private static String getQuery(String property, List<String> values) {
		String lucene="";
		for(String value : values) {
			if(!lucene.isEmpty())
				lucene+=" OR ";
			lucene+=property + ":\"" + value + "\"";
		}
		return lucene;
	}
	public static int countElements(String lucene) throws Throwable {
		StatisticService statisticService = StatisticServiceFactory
				.getStatisticService(ApplicationInfoList.getHomeRepository().getAppId());
		return (int)statisticService.countForQuery(CCConstants.metadatasetdefault_id, "ngsearch",CCConstants.getValidLocalName(CCConstants.CCM_TYPE_IO), lucene);
	}
	public static int countUser(String lucene) throws Throwable {
		// does not work at the moment because of scoped search service and permissions
		StatisticService statisticService = StatisticServiceFactory
				.getStatisticService(ApplicationInfoList.getHomeRepository().getAppId());
		return (int)statisticService.countForQuery(CCConstants.metadatasetdefault_id, "ngsearch","cm:person", lucene);
	}
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
