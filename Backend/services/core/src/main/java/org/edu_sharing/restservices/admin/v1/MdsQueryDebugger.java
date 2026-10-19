package org.edu_sharing.restservices.admin.v1;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import org.apache.commons.lang3.SerializationUtils;
import org.edu_sharing.metadataset.v2.MetadataQueries;
import org.edu_sharing.metadataset.v2.MetadataQueryBase;
import org.edu_sharing.metadataset.v2.MetadataQuery;
import org.edu_sharing.metadataset.v2.MetadataQueryParameter;
import org.edu_sharing.metadataset.v2.MetadataReader;
import org.edu_sharing.metadataset.v2.MetadataSet;
import org.edu_sharing.metadataset.v2.tools.MetadataElasticSearchHelper;
import org.edu_sharing.restservices.admin.v1.model.MdsQueryDebugCriterion;
import org.edu_sharing.restservices.admin.v1.model.MdsQueryDebugParameters;
import org.edu_sharing.restservices.admin.v1.model.MdsQueryDebugResult;
import org.edu_sharing.restservices.admin.v1.model.MdsQueryDebugValueMapping;
import org.edu_sharing.restservices.shared.MdsQueryCriteria;
import org.edu_sharing.service.search.ElasticDslReadableHelper;
import org.edu_sharing.service.search.SearchServiceElastic;
import org.edu_sharing.service.search.model.SearchToken;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Builds the elastic dsl for a mds query (without executing it) and maps every given criterion
 * to its mds parameter definition, statement and resulting elastic query fragment
 */
public class MdsQueryDebugger {
    private static final String DEFAULT_STATEMENT_KEY = "default";

    private MdsQueryDebugger() {
    }

    public static MdsQueryDebugResult debug(SearchServiceElastic searchService, MetadataSet mds, String queryId, MdsQueryDebugParameters parameters, SearchToken searchToken) {
        boolean includeGlobalConditions = parameters.isIncludeGlobalConditions();
        List<MdsQueryCriteria> criteria = parameters.getCriteria();
        // always work on a copy, the (cached) mds must never be modified. Performance is not relevant for debugging
        mds = SerializationUtils.clone(mds);
        if (!parameters.isIncludeSharedBasequery()) {
            removeBasequery(mds.getQueries(MetadataReader.QUERY_SYNTAX_DSL));
        }
        if (!parameters.isIncludeQueryBasequery()) {
            removeBasequery(mds.findQuery(queryId, MetadataReader.QUERY_SYNTAX_DSL));
        }
        MetadataQuery query = mds.findQuery(queryId, MetadataReader.QUERY_SYNTAX_DSL);
        MetadataQueries queries = mds.getQueries(MetadataReader.QUERY_SYNTAX_DSL);

        MdsQueryDebugResult result = new MdsQueryDebugResult();
        result.setMetadataset(mds.getId());
        result.setQuery(query.getId());
        result.setJoin(query.getJoin());
        result.setIndex(SearchServiceElastic.WORKSPACE_INDEX);
        result.setGlobalConditionsIncluded(includeGlobalConditions);
        result.setAvailableParameters(query.getParameters().stream().map(MetadataQueryParameter::getName).sorted().toList());

        // only criteria which can be mapped are used for the dsl, so a single broken criterion does not break the whole debug output
        Map<String, String[]> appliedCriteria = new LinkedHashMap<>();
        if (criteria != null) {
            for (MdsQueryCriteria criterion : criteria) {
                String[] values = criterion.getValues() == null ? new String[0] : criterion.getValues().toArray(new String[0]);
                MdsQueryDebugCriterion mapping = mapCriterion(queries, query, criterion.getProperty(), values);
                if (mapping.isApplied()) {
                    appliedCriteria.put(criterion.getProperty(), values);
                }
                result.getCriteria().add(mapping);
            }
        }
        if (!includeGlobalConditions) {
            result.getWarnings().add("Global conditions (read permissions, store, scope and special filters) are not included, the real search will return less results");
        }
        if (!parameters.isIncludeQueryBasequery()) {
            result.getWarnings().add("Basequery and conditions of the query " + query.getId() + " are not included, the real search will differ");
        }
        if (!parameters.isIncludeSharedBasequery()) {
            result.getWarnings().add("Basequery and conditions shared by all queries (<queries>) are not included, the real search will differ");
        }
        if (result.getCriteria().stream().anyMatch(c -> c.getIgnorable() > 0)) {
            result.getWarnings().add("If the search has no results, the real search is repeated without the ignorable criteria");
        }

        SearchServiceElastic.ElasticSearchRequests requests = searchService.buildSearchRequests(mds, query, appliedCriteria, searchToken, includeGlobalConditions);
        result.setDsl(ElasticDslReadableHelper.toReadableJson(requests.getSearchRequest()));
        if (requests.getAggregationsRequest() != null) {
            result.setAggregationsDsl(ElasticDslReadableHelper.toReadableJson(requests.getAggregationsRequest()));
        }
        return result;
    }

    /** removes the basequery and all conditions, only used on a copy of the mds */
    private static void removeBasequery(MetadataQueryBase query) {
        query.setBasequery(new HashMap<>());
        for (Iterator<?> conditions = query.getConditions().iterator(); conditions.hasNext(); ) {
            conditions.next();
            conditions.remove();
        }
    }

    static MdsQueryDebugCriterion mapCriterion(MetadataQueries queries, MetadataQuery query, String name, String[] values) {
        MdsQueryDebugCriterion mapping = new MdsQueryDebugCriterion();
        mapping.setProperty(name);
        mapping.setValues(List.of(values));
        MetadataQueryParameter parameter = query.findParameterByName(name);
        if (parameter == null) {
            mapping.getWarnings().add("Parameter is not defined in query " + query.getId() + ", the real search will fail");
            return mapping;
        }
        mapping.setKnown(true);
        mapping.setAsFilter(parameter.isAsFilter());
        mapping.setMultiple(parameter.isMultiple());
        mapping.setMultipleJoin(Objects.toString(parameter.getMultiplejoin(), null));
        mapping.setExactMatching(parameter.isExactMatching());
        mapping.setMandatory(parameter.isMandatory());
        mapping.setIgnorable(parameter.getIgnorable());
        mapping.setPreprocessor(parameter.getPreprocessor());
        if (parameter.getStatements() != null) {
            Map<String, String> statements = new LinkedHashMap<>();
            parameter.getStatements().forEach((key, statement) -> statements.put(key == null ? DEFAULT_STATEMENT_KEY : key, statement));
            mapping.setStatements(statements);
        }

        if (values.length == 0) {
            mapping.getWarnings().add("No values given, the criterion is ignored");
            mapping.setApplied(true);
            return mapping;
        }
        if (parameter.getIgnorable() > 0) {
            mapping.getWarnings().add("Ignorable: removed if the search has no results");
        }
        try {
            mapping.setElasticQuery(ElasticDslReadableHelper.toReadableJson(
                    MetadataElasticSearchHelper.getParameterQuery(queries, query, parameter, values)));
            mapping.setApplied(true);
        } catch (Exception e) {
            mapping.getWarnings().add("Error building query, the criterion is not part of the dsl: " + e.getMessage());
        }
        // a combined statement (no multiple join) uses ${value[i]} placeholders, so single values can't be mapped separately
        boolean mapSingleValues = !parameter.isMultiple()
                || MetadataQueryParameter.ParameterJoinStrategy.AND.equals(parameter.getMultiplejoin())
                || MetadataQueryParameter.ParameterJoinStrategy.OR.equals(parameter.getMultiplejoin());
        for (String value : values) {
            mapping.getValueMappings().add(mapValue(queries, query, parameter, value, mapSingleValues));
        }
        return mapping;
    }

    private static MdsQueryDebugValueMapping mapValue(MetadataQueries queries, MetadataQuery query, MetadataQueryParameter parameter, String value, boolean buildQuery) {
        MdsQueryDebugValueMapping valueMapping = new MdsQueryDebugValueMapping();
        valueMapping.setValue(value);
        boolean valueSpecific = parameter.getStatements() != null && parameter.getStatements().containsKey(value);
        valueMapping.setStatementKey(valueSpecific ? value : null);
        try {
            valueMapping.setStatement(parameter.getStatement(value));
            if (buildQuery) {
                valueMapping.setElasticQuery(ElasticDslReadableHelper.toReadableJson(
                        MetadataElasticSearchHelper.getParameterQuery(queries, query, parameter, new String[]{value})));
            }
        } catch (Exception e) {
            valueMapping.setElasticQuery("Error: " + e.getMessage());
        }
        return valueMapping;
    }
}
