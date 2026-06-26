package org.edu_sharing.metadataset.v2.tools;

import lombok.extern.slf4j.Slf4j;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.NodeRef;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.edu_sharing.alfresco.repository.server.authentication.Context;
import org.apache.log4j.Logger;
import org.edu_sharing.alfresco.service.ConnectionDBAlfresco;
import org.edu_sharing.metadataset.v2.*;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.restservices.shared.MdsQueryCriteria;
import org.edu_sharing.service.authority.AuthorityServiceFactory;
import org.edu_sharing.service.nodeservice.NodeServiceHelper;
import org.edu_sharing.service.search.SearchServiceFactory;
import org.edu_sharing.service.search.Suggestion;

import java.io.Serializable;
import java.security.InvalidParameterException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
public class MetadataSearchHelper {

	static Logger logger = Logger.getLogger(MetadataSearchHelper.class);
	private static MetadataQueryPreprocessor preprocessor = new MetadataQueryPreprocessor(MetadataReader.QUERY_SYNTAX_DSL);

    public static Map<String, String[]> convertCriterias(List<MdsQueryCriteria> criterias) {
        Map<String, String[]> criteriasMap = new HashMap<>();
        if (criterias != null) {
            for (MdsQueryCriteria criteria : criterias) {
                criteriasMap.put(criteria.getProperty(), criteria.getValues().toArray(new String[0]));
            }
        }
        return criteriasMap;
    }

    /**
     * replaces globally supported variables for queries (like ${user.<property>} )
     */
    public static String replaceCommonQueryVariables(String statement) {
        try {
            Pattern pattern = Pattern.compile("(\\$\\{user\\.([a-zA-Z:.]+)})");
            Matcher matcher = pattern.matcher(statement);
            NodeRef ref = null;
            while (matcher.find()) {
                if (ref == null) {
                    ref = AuthorityServiceFactory.getInstance().getLocalService().getAuthorityNodeRef(AuthenticationUtil.getFullyAuthenticatedUser());
                }
                Serializable value = NodeServiceHelper.getPropertyNative(ref, CCConstants.getValidGlobalName(matcher.group(2)));
                if (value == null) {
                    log.warn("Statement had variable {} but the property was not set/found", matcher.group(0));
                    statement = statement.replace(matcher.group(0), "null");
                } else {
                    statement = statement.replace(matcher.group(0), value.toString());
                }
            }

        } catch (Throwable t) {
            log.warn("replaceCommonQueryVariables failed: {}", t.getMessage());
        }
        return statement;
    }

    public static MetadataQueryParameter getParameter(MetadataQueries queries, String queryId, String parameterId) {
        for (MetadataQuery query : queries.getQueries()) {
            if (query.getId().equals(queryId)) {
                for (MetadataQueryParameter parameter : query.getParameters()) {
                    if (parameter.getName().equals(parameterId)) {
                        return parameter;
                    }
                }
                throw new InvalidParameterException("Parameter " + parameterId + " was not found in query " + queryId);
            }
        }
        throw new InvalidParameterException("Query " + queryId + " was not found");
    }

    public static List<? extends Suggestion> getSuggestions(String repoId, MetadataSet mds, String queryId, String parameterId, String value, List<MdsQueryCriteria> criterias) throws IllegalArgumentException {
        return getSuggestions(repoId, mds, queryId, parameterId, value, criterias, null);
    }

    public static List<? extends Suggestion> getSuggestions(String repoId, MetadataSet mds, String queryId, String parameterId, String value, List<MdsQueryCriteria> criterias, String localeParam) throws IllegalArgumentException {

        ApplicationInfo appInfo = "-home-".equals(repoId)
                ? ApplicationInfoList.getHomeRepository()
                : ApplicationInfoList.getRepositoryInfoById(repoId);
        repoId = appInfo.getAppId();

        MetadataWidget widget = mds.findWidget(parameterId);

        String source = widget.getSuggestionSource();
        if (source == null) {
            source = widget.getValues() != null ? MetadataReader.SUGGESTION_SOURCE_MDS : MetadataReader.SUGGESTION_SOURCE_SEARCH;
        }

        // remote repo
        if (!ApplicationInfoList.getHomeRepository().getAppId().equals(repoId) &&
                !ApplicationInfo.REPOSITORY_TYPE_LOCAL.equals(ApplicationInfoList.getRepositoryInfoById(repoId).getRepositoryType())) {
            return SearchServiceFactory.getInstance().getService(repoId).getSuggestions(mds, queryId, parameterId, value, criterias);
        }

        // local repo
        return switch (source) {
            case MetadataReader.SUGGESTION_SOURCE_SEARCH ->
                    SearchServiceFactory.getInstance().getService(repoId).getSuggestions(mds, queryId, parameterId, value, criterias);
            case MetadataReader.SUGGESTION_SOURCE_MDS -> getSuggestionsMds(widget, value);
            case MetadataReader.SUGGESTION_SOURCE_SQL -> getSuggestionsSql(widget, value, localeParam);
            default ->
                    throw new IllegalArgumentException("Unknow suggestionSource " + source + " for widget " + parameterId +
                            ", use " + MetadataReader.SUGGESTION_SOURCE_MDS + ", " +
                            MetadataReader.SUGGESTION_SOURCE_SEARCH + " or " +
                            MetadataReader.SUGGESTION_SOURCE_SQL
                    );
        };
    }

    private static List<? extends Suggestion> getSuggestionsSql(MetadataWidget widget,
                                                                String value, String localeParam) throws IllegalArgumentException {
        String query = widget.getSuggestionQuery();

        //default is english, german not present in dnb factual terms
        String locale = "en";
        if(localeParam == null) {
            Context context = Context.getCurrentInstance();
            if (context != null) {
                String ctxLocale = null;
                if (context.getRequest() != null) {
                    String tmp = context.getRequest().getHeader("locale");
                    if (StringUtils.isNotBlank(tmp)) {
                        ctxLocale = tmp.split("_")[0];
                    }
                }
                if (StringUtils.isBlank(ctxLocale) && (StringUtils.isNotBlank(context.getLocale()))) {
                    ctxLocale = context.getLocale().split("_")[0];
                    ;
                }
                if (StringUtils.isNotBlank(ctxLocale)) {
                    locale = ctxLocale;
                }
            }
        }else{
            locale = localeParam;
        }
        query = query.replace("{{locale}}", locale);


        List<Suggestion> result = new ArrayList<>();
        Connection con;
        PreparedStatement statement;
        if (StringUtils.isBlank(query)) {
            throw new IllegalArgumentException("suggestionSource " + MetadataReader.SUGGESTION_SOURCE_SQL + " at widget " + widget.getId() + " needs an suggestionQuery, but none was found");
        }

        ConnectionDBAlfresco dbAlf = new ConnectionDBAlfresco();
        SqlSessionFactory sf = dbAlf.getSqlSessionFactoryBean();
        try (SqlSession sqlSession = sf.openSession()) {
            con = sqlSession.getConnection();//dbAlf.getConnection();
            statement = con.prepareStatement(query);

            value = StringEscapeUtils.escapeSql(value);

            //statement.setString(1,"%" + value.toLowerCase() + "%");
            long countParams = query.chars().filter(ch -> ch == '?').count();
            for (int i = 1; i <= countParams; i++) {
                statement.setString(i, "%" + value.toLowerCase() + "%");
            }

            java.sql.ResultSet resultSet = statement.executeQuery();

            while (resultSet.next()) {
                String kwValue = resultSet.getString(1);
                Suggestion sqlKw = new Suggestion();
                sqlKw.setKey(kwValue.trim());

                try {
                    String displayString = resultSet.getString(2);
                    sqlKw.setDisplayString(displayString);
                } catch (SQLException e) {
                    //no display string in result
                }

                try{
                    String translation = resultSet.getString(3);
                    sqlKw.setTranslation(translation);
                }catch (SQLException e) {
                    //no translation string in result
                }

                try{
                    String url = resultSet.getString(4);
                    sqlKw.setUrl(url);
                }catch (SQLException e) {
                    //no url string in result
                }

                result.add(sqlKw);
            }
        } catch (Throwable e) {
            log.debug(e.getMessage(), e);
        }
        return result;
    }

    private static List<? extends Suggestion> getSuggestionsMds(MetadataWidget widget,
                                                                String value) throws IllegalArgumentException {
        if (widget.getValues() == null)
            throw new IllegalArgumentException("Requested suggestion type " + MetadataReader.SUGGESTION_SOURCE_MDS + " for widget " + widget.getId() + ", but widget has no values attached");
        List<Suggestion> result = new ArrayList<>();
        value = value.toLowerCase();
        for (MetadataKey key : widget.getValues()) {
            if (key.getKey().toLowerCase().contains(value)
                    || key.getCaption().toLowerCase().contains(value)
            ) {
                Suggestion dto = new Suggestion();
                dto.setKey(key.getKey());
                dto.setDisplayString(key.getCaption());
                result.add(dto);
            }
        }
        return result;
    }
	private static String getSearchString(String field,String[] types) {
		String result = "(";

		Iterator<String> iter = Arrays.asList(types).iterator();
		while (iter.hasNext()) {
			String key = iter.next();
			result = result +field+":\"" + key + "\"";
			if (iter.hasNext())
				result = result + " OR ";

		}
		result = result + ")";
		return result;
	}

	private static String applyCondition(MetadataQueryBase query, String lucene) {
		for(MetadataQueryCondition condition : query.getConditions()){
			boolean conditionState= MetadataHelper.checkConditionTrue(condition.getCondition());
			if(conditionState && condition.getQueryTrue()!=null) {
				String conditionString = condition.getQueryTrue();
				conditionString = replaceCommonQueryVariables(conditionString);
				lucene += " AND (" + conditionString + ")";
			}
			if(!conditionState && condition.getQueryFalse()!=null) {
				String conditionString =condition.getQueryFalse();
				conditionString = replaceCommonQueryVariables(conditionString);
				lucene += " AND (" + conditionString + ")";
			}
		}

		return lucene;
	}

}
