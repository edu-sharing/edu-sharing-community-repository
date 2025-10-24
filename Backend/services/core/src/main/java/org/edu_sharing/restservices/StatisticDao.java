package org.edu_sharing.restservices;

import lombok.NonNull;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.edu_sharing.alfresco.lightbend.LightbendConfigLoader;
import org.edu_sharing.metadataset.v2.MetadataKey;
import org.edu_sharing.metadataset.v2.MetadataSet;
import org.edu_sharing.metadataset.v2.MetadataWidget;
import org.edu_sharing.metadataset.v2.tools.MetadataHelper;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.client.tools.I18nAngular;
import org.edu_sharing.repository.server.MCAlfrescoAPIClient;
import org.edu_sharing.repository.server.SearchResultNodeRef;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.restservices.search.v1.model.SearchFacet;
import org.edu_sharing.restservices.shared.NodeSearch;
import org.edu_sharing.restservices.statistic.v1.model.*;
import org.edu_sharing.service.mime.MimeTypesV2;
import org.edu_sharing.service.search.SearchService;
import org.edu_sharing.service.search.SearchServiceFactory;
import org.edu_sharing.service.search.model.SearchToken;
import org.edu_sharing.service.search.model.SortDefinition;
import org.edu_sharing.service.statistic.StatisticService;
import org.edu_sharing.service.statistic.StatisticServiceFactory;
import org.edu_sharing.service.statistic.StatisticsGlobal;

import java.util.*;
import java.util.stream.Collectors;

public class StatisticDao {
    private static Map<String,String> SUB_GROUP_MAPPING=new HashMap<>();
    static{
        SUB_GROUP_MAPPING.put("subject",null);
        SUB_GROUP_MAPPING.put("keywords",CCConstants.LOM_PROP_GENERAL_KEYWORD);
        SUB_GROUP_MAPPING.put("language",CCConstants.LOM_PROP_GENERAL_LANGUAGE);
        SUB_GROUP_MAPPING.put("fileFormat",CCConstants.LOM_PROP_TECHNICAL_FORMAT);
        SUB_GROUP_MAPPING.put("encodingFormat",CCConstants.LOM_PROP_TECHNICAL_FORMAT);
        SUB_GROUP_MAPPING.put("learningResourceType",CCConstants.LOM_PROP_EDUCATIONAL_LEARNINGRESOURCETYPE);
        SUB_GROUP_MAPPING.put("educationalUse",CCConstants.LOM_PROP_EDUCATIONAL_CONTEXT);
        SUB_GROUP_MAPPING.put("intendedEndUserRole",CCConstants.CCM_PROP_IO_REPL_EDUCATIONAL_INTENDEDENDUSERROLE);
    }

	public static StatisticsGlobal getGlobal(String group, @NonNull List<String> subGroup) throws DAOException {
		try {
			String property = "repository.statistics.api.enabled";
			boolean activate = LightbendConfigLoader.get().getBoolean(property);
			if (!activate && !new MCAlfrescoAPIClient().isAdmin()) {
				throw new SecurityException(property + " is not set to true in config. No access allowed");
			}

			List<SearchFacet> facets = subGroup.stream()
					.filter(f -> SUB_GROUP_MAPPING.get(f) != null)
					.map(sg -> SUB_GROUP_MAPPING.get(sg))
					.map(CCConstants::getValidLocalName)
					.map(sg -> new SearchFacet(sg, null))
					.collect(Collectors.toList());

			StatisticsGlobal statistics = new StatisticsGlobal();
			statistics.setUser(getUser());

			if (group == null || group.trim().isEmpty()) {
				group = "license";
			}
			String mdsProp = group;
			StatisticsGlobal.StatisticsGroup overall = new StatisticsGlobal.StatisticsGroup();
			MetadataSet mds = MetadataHelper.getMetadataset(ApplicationInfoList.getHomeRepository(), CCConstants.metadatasetdefault_id);
			MetadataWidget mdsLicenseWidget = mds.findWidget("license");

			SearchResultNodeRef srOverall = search(Map.of(), facets, mds);
			overall.count = (srOverall.getNodeCount());
			overall.subGroups = getFacets(srOverall,subGroup);
			statistics.setOverall(overall);
			List<StatisticsGlobal.StatisticsKeyGroup> groups = new ArrayList<>();
			statistics.setGroups(groups);
			mdsLicenseWidget.getValues().stream().map(MetadataKey::getKey).collect(Collectors.toList())
					.forEach(v -> {
						SearchResultNodeRef srGroup = search(Map.of(mdsProp, new String[]{v}), facets, mds);
						if (srGroup.getNodeCount() > 0) {
							StatisticsGlobal.StatisticsKeyGroup g = new StatisticsGlobal.StatisticsKeyGroup();
							g.key = v;
							g.displayName = mdsLicenseWidget.getValues().stream().filter(vs -> vs.getKey().equals(v)).findFirst().map(m -> m.getCaption()).orElse(v);//I18nAngular.getTranslationAngular("common", "LICENSE." + v);
							g.count = srGroup.getNodeCount();
							g.subGroups = getFacets(srGroup,subGroup);
							groups.add(g);
						}
					});

			return statistics;
		}catch(Throwable t) {
			throw DAOException.mapping(t);
		}
	}

    private static StatisticsGlobal.StatisticsUser getUser() throws Exception {
        StatisticsGlobal.StatisticsUser user = new StatisticsGlobal.StatisticsUser();
        AuthenticationUtil.runAsSystem(()-> {
            user.count = SearchServiceFactory.getLocalService().searchUsers("*", true, 0, 0, new SortDefinition(), null).getTotalCount();
            return null;
        });
        return user;
    }

	private static List<StatisticsGlobal.StatisticsGroup.StatisticsSubGroup> getFacets(SearchResultNodeRef sr, List<String> facetsInput){
		List<StatisticsGlobal.StatisticsGroup.StatisticsSubGroup> facets = new ArrayList<>();
		List<NodeSearch.Facet> facetsRs = sr.getFacets();

		for(String userFacet : facetsInput){
			String facetProp = CCConstants.getValidLocalName(SUB_GROUP_MAPPING.get(userFacet));
			NodeSearch.Facet facet = facetsRs.stream().filter(f -> f.getProperty().equals(facetProp)).findFirst().orElse(null);
			if(facet != null){
				StatisticsGlobal.StatisticsGroup.StatisticsSubGroup group = new StatisticsGlobal.StatisticsGroup.StatisticsSubGroup();
				group.id = userFacet;
				List<StatisticsGlobal.StatisticsGroup.StatisticsSubGroup.SubGroupItem> subGroups = new ArrayList<>();
				if(userFacet.equals("fileFormat")){
					Map<String, Long> countsSum=new HashMap<>();
					facet.getValues().stream().forEach(v -> {
						String mappedMime=MimeTypesV2.getTypeFromMimetype(v.getValue());
						if(countsSum.containsKey(mappedMime)) {
							countsSum.put(mappedMime, countsSum.get(mappedMime) + v.getCount());
						} else {
							countsSum.put(mappedMime, v.getCount());
						}
					});
					countsSum.remove("file");
					countsSum.entrySet().stream().forEach(e -> {
						subGroups.add(new StatisticsGlobal.StatisticsGroup.StatisticsSubGroup.SubGroupItem(e.getKey(),I18nAngular.getTranslationAngular("common","MEDIATYPE."+e.getKey()), e.getValue()));
					});
				}else{
					facet.getValues().stream().forEach(v -> {
						subGroups.add(new StatisticsGlobal.StatisticsGroup.StatisticsSubGroup.SubGroupItem(v.getValue(), v.getCount()));
					});
				}
				group.count = subGroups;
				facets.add(group);
			}
		}
		return facets;
	}


	public Statistics get(String context, List<SearchFacet> properties, Filter filter) throws DAOException {

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
				List<StatisticEntity> entities = new ArrayList<>();
				for (Map.Entry<String, Long> statEntity : entry.getStatistic().entrySet()) {
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

	static SearchResultNodeRef search(Map<String,String[]> criterias, List<SearchFacet> facets, MetadataSet mds) {
		try {
			SearchService localService = SearchServiceFactory.getLocalService();

			SearchToken token = new SearchToken();
			token.setFacets(facets);
			token.setFacetsMinCount(1);
			token.setFrom(0);
			token.setMaxResult(0);
			return localService.search(mds, "ngsearch", criterias, token);
		}catch (Throwable e) {
			throw new RuntimeException(e);
		}
	}

}
