package org.edu_sharing.metadataset.v2.tools;

import org.apache.log4j.Logger;
import org.edu_sharing.metadataset.v2.*;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.AuthenticationToolAPI;
import org.edu_sharing.restservices.mds.v1.model.MdsWidget;
import org.edu_sharing.service.search.model.SearchToken;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.IncludeExclude;
import org.elasticsearch.xcontent.ParseField;
import org.elasticsearch.xcontent.XContentBuilder;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.security.InvalidParameterException;
import java.util.*;

public class MetadataElasticSearchHelper extends MetadataSearchHelper {
    static Logger logger = Logger.getLogger(MetadataElasticSearchHelper.class);

    /**
     * make the query builder readable if print to string to improve debugging
     */
    public static class ReadableWrapperQueryBuilder extends WrapperQueryBuilder {
        private static final ParseField QUERY_FIELD = new ParseField("query");
        private final String _source;
        private MetadataQueryParameter parameter;

        public ReadableWrapperQueryBuilder(String source) {
            super(source);
            this._source = source;
        }
        public ReadableWrapperQueryBuilder(String source, MetadataQueryParameter parameter) {
            this(source);
            this.parameter = parameter;
        }
        @Override
        protected void doXContent(XContentBuilder builder, Params params) throws IOException {
            if(_source != null && builder.humanReadable()) {
                try{
                    new JSONObject(_source);
                } catch(JSONException e){
                    logger.warn("The given json is invalid: " + e.getMessage());
                    logger.warn("Query: " + (parameter != null ? (parameter.getName() + ": ") : "") + _source);
                }
            }
            super.doXContent(builder, params);
        }
    }

    public static QueryBuilder getElasticSearchQuery(SearchToken searchToken, MetadataQueries queries,MetadataQuery query, Map<String,String[]> parameters) throws IllegalArgumentException {
        return getElasticSearchQuery(searchToken, queries,query,parameters,true);
    }

    public static QueryBuilder getElasticSearchQuery(SearchToken searchToken, MetadataQueries queries,MetadataQuery query, Map<String,String[]> parameters, Boolean asFilter) throws IllegalArgumentException {

        String baseQuery = replaceCommonQueryVariables(query.getBasequery().get(null));
        String baseQueryConditional = replaceCommonQueryVariables(
                query.findBasequery(parameters == null ? null : parameters.keySet())
        );
        QueryBuilder baseQueryBuilder;
        if(baseQuery.equals(baseQueryConditional)) {
            baseQueryBuilder = QueryBuilders.wrapperQuery(baseQuery);
        } else {
            baseQueryBuilder = QueryBuilders.boolQuery().must(
                    QueryBuilders.wrapperQuery(baseQuery)
            ).must(
                    QueryBuilders.wrapperQuery(baseQueryConditional)
            );
        }

        BoolQueryBuilder result = QueryBuilders.boolQuery();

        if(asFilter == null || (asFilter.booleanValue() == query.getBasequeryAsFilter())){
            baseQueryBuilder = new ReadableWrapperQueryBuilder(baseQuery);
            result.must(baseQueryBuilder);
        }

        for (String name : parameters.keySet()) {
            MetadataQueryParameter parameter = query.findParameterByName(name);
            if (parameter == null)
                throw new IllegalArgumentException("Could not find parameter " + name + " in the query " + query.getId());

            String[] values = parameters.get(parameter.getName());
            /**
             * @TODO MetadataSearchHelper check if ignoreable is needed
             */
            if ((values == null || values.length == 0)) {
                //if(parameter.getIgnorable()==0)
                continue;
            }

            if(asFilter != null && parameter.isAsFilter() != asFilter.booleanValue()){
                continue;
            }

            QueryBuilder queryBuilderParam = null;
            if(query.isApplyBasequery()){
                if(queries.findBasequery(parameters.keySet())!=null &&
                        !queries.findBasequery(parameters.keySet()).isEmpty()) {
                    result.must(new ReadableWrapperQueryBuilder(queries.findBasequery(parameters.keySet())));
                }
                result = applyCondition(queries, result);
            }
            result = applyCondition(query, result);
            if (parameter.isMultiple()) {

                String multipleJoin = parameter.getMultiplejoin();
                BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
                if (multipleJoin.equals("AND")) {

                    for(String value : values){
                        boolQueryBuilder = boolQueryBuilder.must(new ReadableWrapperQueryBuilder(replaceCommonQueryVariables(getStatmentForValue(parameter,value)), parameter));
                    }
                } else {
                    for(String value : values){
                        boolQueryBuilder = boolQueryBuilder.should(new ReadableWrapperQueryBuilder(replaceCommonQueryVariables(getStatmentForValue(parameter,value)), parameter));
                    }
                }

                queryBuilderParam = boolQueryBuilder;
            }else{
                if(values.length>1){
                    throw new InvalidParameterException("Trying to search for multiple values of a non-multivalue field "+parameter.getName());
                }

                queryBuilderParam = new ReadableWrapperQueryBuilder(replaceCommonQueryVariables(getStatmentForValue(parameter,values[0])), parameter);
            }

            if(query.getJoin().equals("AND")){
                result = result.must(queryBuilderParam);
            }else{
                result = result.should(queryBuilderParam);
            }

        }

        if(asFilter == true
                && searchToken != null
                && searchToken.getSearchCriterias() != null
                && searchToken.getSearchCriterias().getContentkind() != null
                && searchToken.getSearchCriterias().getContentkind().length > 0){
            BoolQueryBuilder criteriasBool = new BoolQueryBuilder();
            Arrays.stream(searchToken.getSearchCriterias().getContentkind()).forEach((content) ->
                    criteriasBool.should(
                            new MatchQueryBuilder("type", CCConstants.getValidLocalName(content)))
            );
            result = result.filter(
                    criteriasBool
            );
        }

        return result;
    }

    private static BoolQueryBuilder applyCondition(MetadataQueryBase query, BoolQueryBuilder result) {
        for(MetadataQueryCondition condition : query.getConditions()){
            boolean conditionState = MetadataHelper.checkConditionTrue(condition.getCondition());
            if(conditionState && condition.getQueryTrue()!=null) {
                String conditionString = condition.getQueryTrue();
                conditionString = replaceCommonQueryVariables(conditionString);
                result.must(new ReadableWrapperQueryBuilder(conditionString));
            }
            if(!conditionState && condition.getQueryFalse()!=null) {
                String conditionString = condition.getQueryFalse();
                conditionString = replaceCommonQueryVariables(conditionString);
                result.must(new ReadableWrapperQueryBuilder(conditionString));
            }
        }
        return result;
    }
    private static String getStatmentForValue(MetadataQueryParameter parameter, String value) {
        if (value == null && parameter.isMandatory()) {
            throw new java.lang.IllegalArgumentException("null value for mandatory parameter " + parameter.getName() + " given, null values are not allowed if mandatory is set to true");
        }
        if (value == null)
            return "";

        // invoke any preprocessors for this value0
        try {
            value = MetadataQueryPreprocessor.run(parameter, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        if (value.startsWith("\"") && value.endsWith("\"") || parameter.isExactMatching()) {
            // clear value's '"'
            if(value.startsWith("\"") && value.endsWith("\"")) {
                value = value.substring(1, value.length() - 1);
            }
            //String statement = parameter.getStatement(value).replace("${value}", QueryParser.escape(value));
            String statement = QueryUtils.replacerFromSyntax(parameter.getSyntax()).replaceString(
                    parameter.getStatement(value),
                    "${value}", value);
            statement = QueryUtils.replacerFromSyntax(parameter.getSyntax(),true).replaceString(
                    statement,
                    "${valueRaw}", value);
            return statement;
        }

        String[] words = value.split(" ");
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        for (String word : words) {
            //String statement = parameter.getStatement(value).replace("${value}", QueryParser.escape(word));
            String statement = QueryUtils.replacerFromSyntax(parameter.getSyntax()).replaceString(
                    parameter.getStatement(value),
                    "${value}", word);
            statement = QueryUtils.replacerFromSyntax(parameter.getSyntax(),true).replaceString(
                    statement,
                    "${valueRaw}", value);
            boolQuery = boolQuery.must(new ReadableWrapperQueryBuilder(statement));

        }
        return boolQuery.toString();
    }

    public static Set<MetadataQueryParameter> getExcludeOwnFacets(MetadataQuery query, Map<String,String[]> parameters, List<String> facets){
        Set<MetadataQueryParameter> excludeOwn = new HashSet<>();
        for (String name : facets) {
            MetadataQueryParameter parameter = query.findParameterByName(name);
            if(parameter == null) continue;
            if((parameter.getMultiplejoin() != null && parameter.getMultiplejoin().equals("OR"))) excludeOwn.add(parameter);
        }
        return excludeOwn;
    }

    /**
     * returns FilterAggregations to be used in a separate call
     * @param mds
     * @param query
     * @param parameters
     * @param facets
     * @param excludeOwn
     * @param globalConditions
     * @param searchToken
     * @return
     * @throws IllegalArgumentException
     */
    public static List<AggregationBuilder> getAggregations(MetadataSet mds, MetadataQuery query, Map<String,String[]> parameters, List<String> facets, Set<MetadataQueryParameter> excludeOwn, QueryBuilder globalConditions, SearchToken searchToken) throws IllegalArgumentException {
        MetadataQueries queries = mds.getQueries(MetadataReader.QUERY_SYNTAX_DSL);
        List<AggregationBuilder> result = new ArrayList<>();
        String currentLocale = new AuthenticationToolAPI().getCurrentLocale();
        for (String facet : facets) {

            Map<String, String[]> tmp = new HashMap<>(parameters);
            if (excludeOwn.stream().anyMatch(mdqp -> mdqp.getName().equals(facet))) {
                tmp.remove(facet);
            }

            QueryBuilder qbFilter = getElasticSearchQuery(searchToken, queries, query, tmp, true);
            QueryBuilder qbNoFilter = getElasticSearchQuery(searchToken, queries, query, tmp, false);
            BoolQueryBuilder bqb = QueryBuilders.boolQuery();
            bqb = bqb.must(qbFilter).must(qbNoFilter).must(globalConditions);
            String fieldName = "properties." + facet+".keyword";
            MetadataQueryParameter parameter = query.findParameterByName(facet);
            if(parameter != null && parameter.getFacets() != null) {
                if(parameter.getFacets().size() != 1) {
                    throw new IllegalArgumentException("DSL Queries only support exactly ONE facet parameter");
                }
                fieldName = parameter.getFacets().get(0);
            }
            if(searchToken.getQueryString() != null && !searchToken.getQueryString().trim().isEmpty()){

                boolean isi18nProp = false;
                MetadataWidget mdw = mds.findWidget(facet);
                if(mdw != null && new MdsWidget(mdw).isHasValues()){
                    isi18nProp = true;
                }

                QueryBuilder mmqb = null;
                if(parameter != null && parameter.getFacets() != null) {
                    mmqb = getFacetFilter(searchToken.getQueryString(), parameter.getFacets().get(0));
                } else if(isi18nProp){
                    mmqb = getFacetFilter(searchToken.getQueryString(),"i18n."+currentLocale+"."+facet, "collections.i18n."+currentLocale+"."+facet);
                }else{
                    mmqb = getFacetFilter(searchToken.getQueryString(),"properties."+facet, "properties."+facet+".keyword");
                }
                bqb.must(mmqb);
            }

            result.add(AggregationBuilders.filter(facet, bqb).subAggregation(AggregationBuilders.terms(facet)
                    .size(searchToken.getFacetLimit())
                    .minDocCount(searchToken.getFacetsMinCount())
                    .field(fieldName)));

            if(parameters.get(facet) != null && parameters.get(facet).length > 0) {
                result.add(AggregationBuilders.filter(facet + "_selected", bqb).subAggregation(AggregationBuilders.terms(facet)
                        .size(parameters.get(facet).length)
                        .minDocCount(1)
                        .field("properties." + facet + ".keyword")
                        .includeExclude(new IncludeExclude(parameters.get(facet), null))));
            }


        }

        return result;
    }

    private static QueryBuilder getFacetFilter(String queryString, String... fieldName) throws IllegalArgumentException {
        BoolQueryBuilder bool = QueryBuilders.boolQuery();
        bool.minimumShouldMatch(1);
        Arrays.stream(fieldName).forEach(
                field -> {
                    bool.should(QueryBuilders.wildcardQuery(field, "*" + queryString + "*").caseInsensitive(true));
                }
        );
        return bool;
    }
}