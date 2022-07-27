package org.edu_sharing.metadataset.v2.tools;

import com.sun.star.lang.IllegalArgumentException;
import org.apache.lucene.queryParser.QueryParser;
import org.edu_sharing.metadataset.v2.*;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.WrapperQueryBuilder;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MetadataElasticSearchHelper extends MetadataSearchHelper {
    public static QueryBuilder getElasticSearchQuery(MetadataQueries queries,MetadataQuery query, Map<String,String[]> parameters) throws IllegalArgumentException {

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
        result.must(baseQueryBuilder);
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

            QueryBuilder queryBuilderParam = null;
            if(query.isApplyBasequery()){
                if(queries.findBasequery(parameters.keySet())!=null &&
                        !queries.findBasequery(parameters.keySet()).isEmpty()) {
                    result.must(QueryBuilders.wrapperQuery(queries.findBasequery(parameters.keySet())));
                }
                result = applyCondition(queries, result);
            }
            result = applyCondition(query, result);
            if (parameter.isMultiple()) {

                String multipleJoin = parameter.getMultiplejoin();
                BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
                if (multipleJoin.equals("AND")) {

                    for(String value : values){
                        boolQueryBuilder = boolQueryBuilder.must(QueryBuilders.wrapperQuery(replaceCommonQueryVariables(getStatmentForValue(parameter,value))));
                    }
                } else {
                    for(String value : values){
                        boolQueryBuilder = boolQueryBuilder.should(QueryBuilders.wrapperQuery(replaceCommonQueryVariables(getStatmentForValue(parameter,value))));
                    }
                }

                queryBuilderParam = boolQueryBuilder;
            }else{
                if(values.length>1){
                    throw new InvalidParameterException("Trying to search for multiple values of a non-multivalue field "+parameter.getName());
                }

                queryBuilderParam = QueryBuilders.wrapperQuery(replaceCommonQueryVariables(getStatmentForValue(parameter,values[0])));
            }

            if(query.getJoin().equals("AND")){
                result = result.must(queryBuilderParam);
            }else{
                result = result.should(queryBuilderParam);
            }

        }
        return result;
    }

    private static BoolQueryBuilder applyCondition(MetadataQueryBase query, BoolQueryBuilder result) {
        for(MetadataQueryCondition condition : query.getConditions()){
            boolean conditionState = MetadataHelper.checkConditionTrue(condition.getCondition());
            if(conditionState && condition.getQueryTrue()!=null) {
                String conditionString = condition.getQueryTrue();
                conditionString = replaceCommonQueryVariables(conditionString);
                result.must(QueryBuilders.wrapperQuery(conditionString));
            }
            if(!conditionState && condition.getQueryFalse()!=null) {
                String conditionString = condition.getQueryFalse();
                conditionString = replaceCommonQueryVariables(conditionString);
                result.must(QueryBuilders.wrapperQuery(conditionString));
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
                    "${valueRaw}", word);
            boolQuery = boolQuery.must(QueryBuilders.wrapperQuery(statement));

        }
        return boolQuery.toString();
    }
}