package org.edu_sharing.restservices;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.security.PersonService;
import org.apache.lucene.queryParser.QueryParser;
import org.edu_sharing.metadataset.v2.MetadataReaderV2;
import org.edu_sharing.metadataset.v2.MetadataSetV2;
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
import org.edu_sharing.service.statistic.StatisticsGlobal.License.Facette;
import org.edu_sharing.service.statistic.StatisticsGlobal.Materials;

import com.ibm.icu.text.SimpleDateFormat;

public class StatisticDao {
	private static final int DAY_HISTORY_COUNT = 14;
	private static final int MONTH_HISTORY_COUNT = 12;
	public static StatisticsGlobal getGlobal(List<String> properties) throws DAOException {
		try {
			if(properties==null) {
				properties = new ArrayList<>();
			}
			properties.add(CCConstants.getValidLocalName(CCConstants.LOM_PROP_TECHNICAL_FORMAT));
			StatisticsGlobal statistics=new StatisticsGlobal();
			StatisticsGlobal.Repository repository=new StatisticsGlobal.Repository();
			repository.name=ApplicationInfoList.getHomeRepository().getAppCaption();
			repository.domain=ApplicationInfoList.getHomeRepository().getDomain();
			repository.queryTime=System.currentTimeMillis()/1000;
			statistics.setRepository(repository);
			StatisticsGlobal.User user=new StatisticsGlobal.User();
			user.count=countUser(null);
			statistics.setUser(user);			
			List<StatisticsGlobal.License> licenses=new ArrayList<>();
			StatisticsGlobal.License entry=new StatisticsGlobal.License();
			entry.name=null;
			entry.count=(countElements(null));
			licenses.add(entry);
			entry.facettes=getFacettes(null,properties);
			for(String license : CCConstants.getAllLicenseKeys()) {
				String lucene=escapeProperty(CCConstants.CCM_PROP_IO_COMMONLICENSE_KEY)+":\""+license+"\"";
				entry=new StatisticsGlobal.License();
				entry.name=license;
				entry.count=countElements(lucene);
				if(entry.count>0) {
					licenses.add(entry);
				}
				entry.facettes=getFacettes(lucene,properties);
				
			}
			Materials materials = new StatisticsGlobal.Materials();
			materials.licenses=licenses;
			statistics.setMaterials(materials);
			
			return statistics;
		}
		catch(Throwable t) {
			throw DAOException.mapping(t);
		}
	}
	private static List<Facette> getFacettes(String lucene,List<String> properties) throws Throwable {
		List<Map<String, Integer>> data = countFacettes(lucene, properties);
		int i=0;
		List<Facette> facettes = new ArrayList<>();
		for(String prop : properties) {
			Facette facette = new StatisticsGlobal.License.Facette();
			facette.name=prop;
			Map<String, Integer> counts = data.get(i++);
			if(prop.equals(CCConstants.getValidLocalName(CCConstants.LOM_PROP_TECHNICAL_FORMAT))) {
				Map<String, Integer> countsSum=new HashMap<>();
				for(String key : counts.keySet()) {
					String mapped=MimeTypesV2.getTypeFromMimetype(key);
					if(countsSum.containsKey(mapped)) {
						countsSum.put(mapped, countsSum.get(mapped)+counts.get(key));
					}
					else {
						countsSum.put(mapped, counts.get(key));
					}
				}
				counts=countsSum;
			}
			facette.count=counts;
			facettes.add(facette);
		}
		return facettes;
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
		return (int)statisticService.countForQuery(CCConstants.metadatasetdefault_id, MetadataSetV2.DEFAULT_CLIENT_QUERY,CCConstants.getValidLocalName(CCConstants.CCM_TYPE_IO), lucene);
	}
	public static List<Map<String,Integer>> countFacettes(String lucene,List<String> facettes) throws Throwable {
		StatisticService statisticService = StatisticServiceFactory
				.getStatisticService(ApplicationInfoList.getHomeRepository().getAppId());
		return statisticService.countFacettesForQuery(CCConstants.metadatasetdefault_id, MetadataSetV2.DEFAULT_CLIENT_QUERY,CCConstants.getValidLocalName(CCConstants.CCM_TYPE_IO), lucene,facettes);
	}
	public static int countUser(String lucene) throws Throwable {
		// does not work at the moment because of scoped search service and permissions
		StatisticService statisticService = StatisticServiceFactory
				.getStatisticService(ApplicationInfoList.getHomeRepository().getAppId());
		return (int)statisticService.countForQuery(CCConstants.metadatasetdefault_id, MetadataSetV2.DEFAULT_CLIENT_QUERY,"cm:person", lucene);
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
