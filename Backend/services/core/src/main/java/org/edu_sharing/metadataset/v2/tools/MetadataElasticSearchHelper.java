package org.edu_sharing.metadataset.v2.tools;

import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.aggregations.AggregationBuilders;
import co.elastic.clients.elasticsearch._types.aggregations.MultiTermLookup;
import co.elastic.clients.elasticsearch._types.aggregations.TermsAggregation;
import co.elastic.clients.elasticsearch._types.mapping.RuntimeField;
import co.elastic.clients.elasticsearch._types.mapping.RuntimeFieldType;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.json.JsonData;
import co.elastic.clients.json.JsonpUtils;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.edu_sharing.metadataset.v2.*;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.AuthenticationToolAPI;
import org.edu_sharing.restservices.mds.v1.model.MdsWidget;
import org.edu_sharing.restservices.search.v1.model.SearchFacet;
import org.edu_sharing.service.search.ReadableWrapperQueryBuilder;
import org.edu_sharing.service.search.SearchServiceElastic;
import org.edu_sharing.service.search.model.SearchToken;
import org.edu_sharing.service.search.model.SharedToMeType;
import org.jetbrains.annotations.Nullable;

import java.security.InvalidParameterException;
import java.util.*;
import java.util.stream.Collectors;

public class MetadataElasticSearchHelper extends MetadataSearchHelper {
    /**
     * the given count will be multiplied by this value since facets are filtered for the containing string afterwards and we need some overhead
     */
    public static final int FACET_LIMIT_MULTIPLIER = 5;
    public static final String FACET_SELECTED_POSTFIX = "_selected";
    static final String GEOPOINT_RUNTIME_FIELD = "geo_point_runtime";
    /** Terms-agg value source merging indexed values + <em>all</em> nested suggestions for the property. Param: {@code property}. */
    public static final String COMBINED_SUGGESTION_FACET_SCRIPT = SearchServiceElastic.loadScript("suggestion-combined-facet.painless");
    /** Like {@link #COMBINED_SUGGESTION_FACET_SCRIPT} but nested suggestions are restricted to {@code createdBy == authority}. Params: {@code property}, {@code authority}. */
    public static final String COMBINED_SUGGESTION_FACET_CURRENT_USER_SCRIPT = SearchServiceElastic.loadScript("suggestion-combined-facet-current-user.painless");
    /** Marker in the aggregation meta data for facets which are split into a plain and a suggestion branch */
    public static final String COMBINED_SUGGESTION_META_KIND = "esKind";
    public static final String COMBINED_SUGGESTION_META_KIND_VALUE = "combinedSuggestionFacet";
    public static final String COMBINED_SUGGESTION_META_SIZE = "size";
    public static final String COMBINED_SUGGESTION_META_MIN_DOC_COUNT = "minDocCount";
    /** Branch without own suggestions of the current user: plain doc_values terms aggregation */
    public static final String COMBINED_SUGGESTION_BRANCH_PLAIN = "plain";
    /** Branch with own suggestions of the current user: painless script reading _source */
    public static final String COMBINED_SUGGESTION_BRANCH_WITH_SUGGESTIONS = "withSuggestions";
    public static final String COMBINED_SUGGESTION_BRANCH_VALUES = "values";
    /** The suggestion branch is usually tiny, so we request more buckets there to keep the merge accurate */
    static final int COMBINED_SUGGESTION_MIN_BRANCH_SIZE = 100;
    static Logger logger = Logger.getLogger(MetadataElasticSearchHelper.class);
    private static MetadataQueryPreprocessor preprocessor = new MetadataQueryPreprocessor(MetadataReader.QUERY_SYNTAX_DSL);

    //@TODO make more generic or fix in elastic model
    public static final List<String> nonKeywordFacets = List.of("cclom:format");

    public static BoolQuery.Builder getElasticSearchQuery(SearchToken searchToken, MetadataQueries queries, MetadataQuery query, Map<String, String[]> parameters) throws IllegalArgumentException {
        return getElasticSearchQuery(searchToken, queries, query, parameters, true);
    }

    public static BoolQuery.Builder getElasticSearchQuery(SearchToken searchToken, MetadataQueries queries, MetadataQuery query, Map<String, String[]> parameters, Boolean asFilter) throws IllegalArgumentException {


        BoolQuery.Builder result = new BoolQuery.Builder();
        if (asFilter == null || (asFilter == query.getBasequeryAsFilter())) {
            String baseQuery = replaceCommonQueryVariables(query.getPrimaryBasequery());
            String baseQueryConditional = replaceCommonQueryVariables(query.findBasequery(parameters == null ? null : parameters.keySet()));

            if (Objects.equals(baseQuery, baseQueryConditional)) {
                if (baseQuery != null) {
                    result.must(must -> must.wrapper(new ReadableWrapperQueryBuilder(baseQuery).build()));
                }
            } else {
                result.must(must -> must.wrapper(new ReadableWrapperQueryBuilder(baseQuery).build()))
                        .must(must -> must.wrapper(new ReadableWrapperQueryBuilder(baseQueryConditional).build()));
            }
        }

        if (parameters != null && parameters.isEmpty()) {
            if (query.isApplyBasequery()) {
                String baseQueryParam = queries.findBasequery(parameters.keySet());
                if (!StringUtils.isEmpty(baseQueryParam)) {
                    result.must(must -> must.wrapper(new ReadableWrapperQueryBuilder(baseQueryParam).build()));
                }
                applyCondition(queries, result);
            }
            applyCondition(query, result);
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

            if (asFilter != null && parameter.isAsFilter() != asFilter) {
                continue;
            }

            Query queryBuilderParam = null;
            if (query.isApplyBasequery()) {
                String baseQueryParam = queries.findBasequery(parameters.keySet());
                if (!StringUtils.isEmpty(baseQueryParam)) {
                    result.must(must -> must.wrapper(new ReadableWrapperQueryBuilder(baseQueryParam).build()));
                }
                applyCondition(queries, result);
            }

            applyCondition(query, result);
            BoolQuery sharedFilesSearch;
            if (query.getId().equals("workspace") && parameter.getName().equals("parent") && parameter.getPreprocessor().equals("node_path")) {
                sharedFilesSearch = handleSharedFilesSearch(queries, values);
            } else {
                sharedFilesSearch = null;
            }
            if (sharedFilesSearch != null) {
                queryBuilderParam = Query.of(q -> q.bool(sharedFilesSearch));
            } else if (parameter.isMultiple()) {

                MetadataQueryParameter.ParameterJoinStrategy multipleJoin = parameter.getMultiplejoin();
                BoolQuery.Builder boolQueryBuilder = new BoolQuery.Builder();
                if (multipleJoin.equals(MetadataQueryParameter.ParameterJoinStrategy.AND)) {
                    for (String value : values) {
                        boolQueryBuilder.must(must -> must.wrapper(new ReadableWrapperQueryBuilder(replaceCommonQueryVariables(getStatementForValue(parameter, value)), parameter).build()));
                    }
                } else if (multipleJoin.equals(MetadataQueryParameter.ParameterJoinStrategy.OR)) {
                    for (String value : values) {
                        boolQueryBuilder.should(should -> should.wrapper(new ReadableWrapperQueryBuilder(replaceCommonQueryVariables(getStatementForValue(parameter, value)), parameter).build()));
                    }
                } else {
                    boolQueryBuilder.should(should -> should.wrapper(new ReadableWrapperQueryBuilder(replaceCommonQueryVariables(getStatementForValues(parameter, values)), parameter).build()));
                }

                queryBuilderParam = Query.of(q -> q.bool(boolQueryBuilder.build()));
            } else {
                if (values.length > 1) {
                    throw new InvalidParameterException("Trying to search for multiple values of a non-multivalue field " + parameter.getName());
                }

                queryBuilderParam = Query.of(q -> q.wrapper(new ReadableWrapperQueryBuilder(replaceCommonQueryVariables(getStatementForValue(parameter, values[0])), parameter).build()));
            }

            if (query.getJoin().equals("AND")) {
                result.must(queryBuilderParam);
            } else {
                result.should(queryBuilderParam);
            }

        }

        if (asFilter
                && searchToken != null
                && searchToken.getSearchCriterias() != null
                && searchToken.getSearchCriterias().getContentkind() != null
                && searchToken.getSearchCriterias().getContentkind().length > 0) {

            result.filter(filter -> filter.bool(criteriasBool -> {
                Arrays.stream(searchToken.getSearchCriterias().getContentkind())
                        .forEach((content) -> criteriasBool.should(should -> should
                                .match(match -> match
                                        .field("type")
                                        .query(CCConstants.getValidLocalName(content)))));
                return criteriasBool;
            }));
        }

        return result;
    }

    private static BoolQuery handleSharedFilesSearch(MetadataQueries queries, String[] values) {
        if (values[0].startsWith("-to_me_shared_files")) {
            try {
                if ("-to_me_shared_files_personal-".equals(values[0])) {
                    return SearchServiceElastic.getFilesSharedToMeQuery(queries, SharedToMeType.Private);
                } else if ("-to_me_shared_files-".equals(values[0])) {
                    return SearchServiceElastic.getFilesSharedToMeQuery(queries, SharedToMeType.All);
                } else {
                    logger.error("unknown SharedToMeType:" + values[0]);
                }
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        } else if (values[0].startsWith("-my_shared_files")) {
            try {
                return SearchServiceElastic.getFilesSharedByMeQuery(queries);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }
        return null;
    }

    private static BoolQuery.Builder applyCondition(MetadataQueryBase query, BoolQuery.Builder result) {
        for (MetadataQueryCondition condition : query.getConditions()) {
            boolean conditionState = MetadataHelper.checkConditionTrue(condition.getCondition());
            if (conditionState && condition.getQueryTrue() != null) {
                String conditionString = condition.getQueryTrue();
                conditionString = replaceCommonQueryVariables(conditionString);
                final String condString = conditionString;
                result.must(must -> must.wrapper(new ReadableWrapperQueryBuilder(condString).build()));
            }
            if (!conditionState && condition.getQueryFalse() != null) {
                String conditionString = condition.getQueryFalse();
                conditionString = replaceCommonQueryVariables(conditionString);
                final String condString = conditionString;
                result.must(must -> must.wrapper(new ReadableWrapperQueryBuilder(condString).build()));
            }
        }
        return result;
    }
    private static String getStatementForValues(MetadataQueryParameter parameter, String[] valuesIn) {
        if(valuesIn == null && parameter.isMandatory()) {
            throw new java.lang.IllegalArgumentException("null value for mandatory parameter "+parameter.getName()+" given, null values are not allowed if mandatory is set to true");
        }
        List<String> values = Arrays.asList(valuesIn);
        // invoke any preprocessors for this value
        values = values.stream().map(v -> {
            try {
                return preprocessor.run(parameter, v);
            } catch (Throwable e) {
                logger.error(e.getMessage(),e);
                throw new RuntimeException(e);
            }
        }).collect(Collectors.toList());

        int i = 0;
        final BoolQuery.Builder boolQuery = new BoolQuery.Builder();
        String statement = parameter.getStatement(null);
        for(String value : values) {
            statement = QueryUtils.replacerFromSyntax(parameter.getSyntax()).replaceString(statement, "${value[" + i + "]}", value);
            statement = QueryUtils.replacerFromSyntax(parameter.getSyntax(), true).replaceString(statement,"${valueRaw[" + i + "]}", value);
            i++;
        }
        final String finStatement = statement;
        boolQuery.must(must -> must.wrapper(new ReadableWrapperQueryBuilder(finStatement).build()));
        return JsonpUtils.toJsonString(Query.of(q -> q.bool(boolQuery.build())), new JacksonJsonpMapper());
    }
    private static String getStatementForValue(MetadataQueryParameter parameter, String value) {
        if (value == null && parameter.isMandatory()) {
            throw new java.lang.IllegalArgumentException("null value for mandatory parameter " + parameter.getName() + " given, null values are not allowed if mandatory is set to true");
        }
        if (value == null)
            return "";

        // invoke any preprocessors for this value
        try {
            value = preprocessor.run(parameter, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

       if (value.startsWith("\"") && value.endsWith("\"") || parameter.isExactMatching()) {
           String valueRaw = value;
           // clear value's '"'
           if (value.startsWith("\"") && value.endsWith("\"")) {
                value = value.substring(1, value.length() - 1);
            }
            //String statement = parameter.getStatement(value).replace("${value}", QueryParser.escape(value));
            String statement = QueryUtils.replacerFromSyntax(parameter.getSyntax()).replaceString(
                    parameter.getStatement(value),
                    "${value}", value);
           // use valueRaw with real "raw" value if isExactMatching==true to support simple_query_string PHRASE
            statement = QueryUtils.replacerFromSyntax(parameter.getSyntax(), true).replaceString(
                    statement,
                    "${valueRaw}", parameter.isExactMatching() ? valueRaw : value);
            return statement;
        }

        String[] words = value.split(" ");
        final BoolQuery.Builder boolQuery = new BoolQuery.Builder();
        for (String word : words) {
            if (word.length() == 1 && !Character.isLetterOrDigit(word.charAt(0))) {
                continue;
            }
            //String statement = parameter.getStatement(value).replace("${value}", QueryParser.escape(word));
            String statement = QueryUtils.replacerFromSyntax(parameter.getSyntax()).replaceString(
                    parameter.getStatement(word),
                    "${value}", word);
            statement = QueryUtils.replacerFromSyntax(parameter.getSyntax(), true).replaceString(
                    statement,
                    "${valueRaw}", word);

            final String finStatement = statement;
            boolQuery.must(must -> must.wrapper(new ReadableWrapperQueryBuilder(finStatement).build()));

        }
        return JsonpUtils.toJsonString(Query.of(q -> q.bool(boolQuery.build())), new JacksonJsonpMapper());
    }

    public static Set<MetadataQueryParameter> getExcludeOwnFacets(MetadataQuery query, Map<String, String[]> parameters, List<SearchFacet> facets) {
        Set<MetadataQueryParameter> excludeOwn = new HashSet<>();
        for (SearchFacet name : facets) {
            MetadataQueryParameter parameter = query.findParameterByName(name.getProperty());
            if (parameter == null) continue;
            if ((parameter.getMultiplejoin() != null && parameter.getMultiplejoin().equals(MetadataQueryParameter.ParameterJoinStrategy.OR)))
                excludeOwn.add(parameter);
        }
        return excludeOwn;
    }

    /**
     * Builds the facet aggregation for {@code combineWithSuggestions} facets.
     *
     * <p>Instead of running the painless script (which has to load and parse {@code _source}) for every matching
     * document, the documents are split into two disjoint branches:</p>
     * <ul>
     *     <li>{@link #COMBINED_SUGGESTION_BRANCH_PLAIN}: documents without own suggestions of the current user for this
     *     property, aggregated via doc_values on {@code field}</li>
     *     <li>{@link #COMBINED_SUGGESTION_BRANCH_WITH_SUGGESTIONS}: documents with own suggestions, aggregated via the
     *     existing painless script</li>
     * </ul>
     * <p>Since every document is part of exactly one branch, summing the bucket counts of both branches yields the
     * same result as the script-only aggregation. The merge happens in {@link #mergeCombinedSuggestionBuckets}.</p>
     */
    static Aggregation buildCombinedSuggestionFacetAggregation(String property, String field, String authority, int size, int minDocCount) {
        TermsAggregation plainTerms = AggregationBuilders.terms()
                .field(field)
                .size(size)
                .minDocCount(1)
                .build();
        if (StringUtils.isBlank(authority)) {
            // without an authority the script can never add suggestion values, so the plain aggregation is sufficient
            return new Aggregation.Builder()
                    .filter(f -> f.matchAll(m -> m))
                    .meta(combinedSuggestionMeta(size, minDocCount))
                    .aggregations(COMBINED_SUGGESTION_BRANCH_PLAIN, new Aggregation.Builder()
                            .filter(f -> f.matchAll(m -> m))
                            .aggregations(COMBINED_SUGGESTION_BRANCH_VALUES, plainTerms._toAggregation())
                            .build())
                    .build();
        }
        Query ownSuggestions = Query.of(q -> q.nested(n -> n
                .path("suggestions")
                .query(nq -> nq.bool(b -> b
                        .filter(f -> f.term(t -> t.field("suggestions.propertyId").value(property)))
                        .filter(f -> f.term(t -> t.field("suggestions.createdBy").value(authority)))
                ))
        ));
        Aggregation plain = new Aggregation.Builder()
                .filter(f -> f.bool(b -> b.mustNot(ownSuggestions)))
                .aggregations(COMBINED_SUGGESTION_BRANCH_VALUES, plainTerms._toAggregation())
                .build();
        Aggregation withSuggestions = new Aggregation.Builder()
                .filter(ownSuggestions)
                .aggregations(COMBINED_SUGGESTION_BRANCH_VALUES,
                        buildCombinedSuggestionScriptTerms(property, authority, Math.max(size, COMBINED_SUGGESTION_MIN_BRANCH_SIZE), 1))
                .build();
        Map<String, Aggregation> branches = new LinkedHashMap<>();
        branches.put(COMBINED_SUGGESTION_BRANCH_PLAIN, plain);
        branches.put(COMBINED_SUGGESTION_BRANCH_WITH_SUGGESTIONS, withSuggestions);
        return new Aggregation.Builder()
                .filter(f -> f.matchAll(m -> m))
                .meta(combinedSuggestionMeta(size, minDocCount))
                .aggregations(branches)
                .build();
    }

    static Aggregation buildCombinedSuggestionScriptTerms(String property, String authority, int size, int minDocCount) {
        Map<String, JsonData> params = new HashMap<>();
        params.put("property", JsonData.of(property));
        params.put("authority", authority == null ? JsonData.of("") : JsonData.of(authority));
        return AggregationBuilders.terms()
                .script(s -> s
                        .source(COMBINED_SUGGESTION_FACET_CURRENT_USER_SCRIPT)
                        .lang("painless")
                        .params(params))
                .size(size)
                .minDocCount(minDocCount)
                .build()._toAggregation();
    }

    private static Map<String, JsonData> combinedSuggestionMeta(int size, int minDocCount) {
        Map<String, JsonData> meta = new HashMap<>();
        meta.put(COMBINED_SUGGESTION_META_KIND, JsonData.of(COMBINED_SUGGESTION_META_KIND_VALUE));
        meta.put(COMBINED_SUGGESTION_META_SIZE, JsonData.of(size));
        meta.put(COMBINED_SUGGESTION_META_MIN_DOC_COUNT, JsonData.of(minDocCount));
        return meta;
    }

    /** Result of {@link #mergeCombinedSuggestionBuckets}: merged buckets (sorted, cut to size) and the remaining doc count */
    public record MergedFacetBuckets(List<Map.Entry<String, Long>> buckets, long sumOtherDocCount) {
    }

    /**
     * Merges the term buckets of the branches of a combined suggestion facet.
     * Counts of equal keys are summed up, then {@code minDocCount} is applied, the buckets are sorted like the
     * default terms aggregation (count desc, key asc) and cut to {@code size}. Cut buckets are added to
     * {@code sumOtherDocCount}.
     */
    public static MergedFacetBuckets mergeCombinedSuggestionBuckets(List<Map<String, Long>> branchBuckets, List<Long> branchSumOtherDocCounts, int size, long minDocCount) {
        Map<String, Long> counts = new HashMap<>();
        for (Map<String, Long> branch : branchBuckets) {
            branch.forEach((key, count) -> counts.merge(key, count, Long::sum));
        }
        long sumOther = branchSumOtherDocCounts.stream().filter(Objects::nonNull).mapToLong(Long::longValue).sum();
        List<Map.Entry<String, Long>> sorted = counts.entrySet().stream()
                .filter(e -> e.getValue() >= minDocCount)
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed().thenComparing(Map.Entry.<String, Long>comparingByKey()))
                .map(e -> Map.entry(e.getKey(), e.getValue()))
                .collect(Collectors.toList());
        if (sorted.size() > size) {
            sumOther += sorted.subList(size, sorted.size()).stream().mapToLong(Map.Entry::getValue).sum();
            sorted = new ArrayList<>(sorted.subList(0, size));
        }
        return new MergedFacetBuckets(sorted, sumOther);
    }

    /**
     * returns FilterAggregations to be used in a separate call
     *
     * @param mds
     * @param query
     * @param parameters
     * @param facets
     * @param excludeOwn
     * @param globalConditions conditions that apply to every facet alike (permissions, store, ...).
     *                         When given, they are applied <b>once</b> as the request's top level query - together
     *                         with the whole facet independent part of the matching tree (base query, search term,
     *                         all criteria that are not subject to {@code excludeOwn}) - instead of being repeated
     *                         inside every facet's filter sub-aggregation. Each facet's filter then only carries its
     *                         own difference. Pass {@code null} when the caller sets its own top level query which
     *                         already contains all of that; in that case the complete tree is built per facet.
     * @param searchToken
     * @return
     * @throws IllegalArgumentException
     */
    public static Map<String, Aggregation> applyAggregations(SearchRequest.Builder searchRequestBuilder, MetadataSet mds, MetadataQuery query, Map<String, String[]> parameters, List<SearchFacet> facets, Set<MetadataQueryParameter> excludeOwn, @Nullable Query globalConditions, SearchToken searchToken) throws IllegalArgumentException {
        MetadataQueries queries = mds.getQueries(MetadataReader.QUERY_SYNTAX_DSL);
        Map<String, Aggregation> result = new HashMap<>();
        String currentLocale = AuthenticationToolAPI.getInstance().getCurrentLocale();

        Set<String> excludeOwnNames = excludeOwn.stream()
                .map(MetadataQueryParameter::getName)
                .collect(Collectors.toSet());
        /*
         * Only an "AND" joined query may be split up: tree(A + B) == tree(A) AND tree(B) does not hold when the
         * parameters are joined via "should".
         */
        boolean hoistSharedQuery = globalConditions != null && "AND".equals(query.getJoin());
        if (hoistSharedQuery) {
            // everything that is identical for every facet is evaluated a single time as the top level query
            Map<String, String[]> sharedParameters = new HashMap<>(parameters == null ? Collections.emptyMap() : parameters);
            sharedParameters.keySet().removeAll(excludeOwnNames);
            BoolQuery sharedFilter = getElasticSearchQuery(searchToken, queries, query, sharedParameters, true).build();
            BoolQuery sharedNoFilter = getElasticSearchQuery(searchToken, queries, query, sharedParameters, false).build();
            searchRequestBuilder.query(q -> q.bool(shared -> shared
                    .must(must -> must.bool(sharedFilter))
                    .must(must -> must.bool(sharedNoFilter))
                    .must(globalConditions)));
        } else if (globalConditions != null) {
            searchRequestBuilder.query(globalConditions);
        }

        for (SearchFacet facet : facets) {

            Map<String, String[]> tmp = new HashMap<>(parameters == null ? Collections.emptyMap() : parameters);
            if (hoistSharedQuery) {
                // the shared part is already applied as top level query, keep only this facet's own difference
                tmp.keySet().retainAll(excludeOwnNames);
            }

            if (excludeOwn.stream().anyMatch(mdqp -> mdqp.getName().equals(facet.getProperty()))) {
                tmp.remove(facet.getProperty());
            }

            /*
             * The filter sub-aggregation only carries what is specific to this facet. Everything shared
             * (base query, search term, globalConditions, ...) is already applied as the top level query.
             */
            BoolQuery.Builder fullFilterQuery = new BoolQuery.Builder();
            if (hoistSharedQuery && tmp.isEmpty()) {
                // nothing facet specific left, but the response parsing expects a filter aggregation
                fullFilterQuery.must(must -> must.matchAll(matchAll -> matchAll));
            } else {
                BoolQuery.Builder qbFilter = getElasticSearchQuery(searchToken, queries, query, tmp, true);
                BoolQuery.Builder qbNoFilter = getElasticSearchQuery(searchToken, queries, query, tmp, false);
                fullFilterQuery
                        .must(must -> must.bool(qbFilter.build()))
                        .must(must -> must.bool(qbNoFilter.build()));
            }

            List<MetadataQueryParameter.MetadataQueryFacetItem> fieldName = Collections.singletonList(
                    new MetadataQueryParameter.MetadataQueryFacetItem(
                            (nonKeywordFacets.contains(facet.getProperty())
                                    ? "properties." + facet.getProperty()
                                    : "properties." + facet.getProperty()+".keyword"), null)
            );

            MetadataQueryParameter parameter = query.findParameterByName(facet.getProperty());

            Optional<MetadataQueryParameter.MetadataQueryFacet> metadataQueryFacet = Optional.ofNullable(parameter)
                    .map(MetadataQueryParameter::getFacet);

            if (metadataQueryFacet.isPresent() && !metadataQueryFacet.get().getItems().isEmpty()) {
                if (metadataQueryFacet.get().getItems().size() > 1) {
                    logger.warn("Using more than one facet parameter is not recommended when using elasticsearch");
                }
                fieldName = metadataQueryFacet.get().getItems();
            }

            Query facetsSearchFilter = null;
            if (searchToken.getQueryString() != null && !searchToken.getQueryString().trim().isEmpty()) {

                boolean isi18nProp = false;
                MetadataWidget mdw = mds.findWidget(facet.getProperty());
                if (mdw != null && new MdsWidget(mdw).isHasValues()) {
                    isi18nProp = true;
                }

                if (metadataQueryFacet.isPresent()) {
                    if (metadataQueryFacet.get().getItems().size() > 1) {
                        BoolQuery.Builder facetQuery = new BoolQuery.Builder();
                        for (MetadataQueryParameter.MetadataQueryFacetItem parameterFacetItem : metadataQueryFacet.get().getItems()) {
                            facetQuery.should(getFacetFilter(searchToken.getQueryString(), parameterFacetItem.getValue()));
                        }
                        facetsSearchFilter = Query.of(q -> q.bool(facetQuery.build()));
                    } else {
                        String[] facetName = new String[]{
                                "properties." + facet.getProperty() + ".keyword",
                                "i18n." + currentLocale + "." + facet.getProperty(),
                                "collections.i18n." + currentLocale + "." + facet.getProperty()};
                        if(!metadataQueryFacet.get().getItems().isEmpty()) {
                            facetName = new String[]{metadataQueryFacet.get().getItems().get(0).getValue()};
                        }
                        facetsSearchFilter = getFacetFilter(searchToken.getQueryString(), facetName);
                    }
                } else if (isi18nProp) {
                    facetsSearchFilter = getFacetFilter(searchToken.getQueryString(), "i18n." + currentLocale + "." + facet.getProperty(), "collections.i18n." + currentLocale + "." + facet.getProperty());
                } else {
                    facetsSearchFilter = getFacetFilter(searchToken.getQueryString(), "properties." + facet.getProperty(), "properties." + facet.getProperty() + ".keyword");
                }
            }

            // https://discuss.elastic.co/t/sub-aggregation-in-new-java-api-client/313447
            Query bqbQuery = null;
            if (fieldName.size() == 1) {
                Aggregation innerAggregation;
                if(metadataQueryFacet.isPresent() && metadataQueryFacet.get().getType().equals(MetadataQueryParameter.MetadataQueryFacet.Type.geo_grid)) {
                    if(facet.getArgs() == null || !facet.getArgs().containsKey("precision")) {
                        throw new IllegalArgumentException("Parameter for a field of type geo requires args field precision");
                    }
                    searchRequestBuilder.runtimeMappings(GEOPOINT_RUNTIME_FIELD, RuntimeField.of(r -> r
                            .type(RuntimeFieldType.GeoPoint)
                            .script(s -> s.source(
                                            "if (doc.containsKey('properties.cm:latitude.number') && doc['properties.cm:latitude.number'].size() > 0 && " +
                                                    "doc.containsKey('properties.cm:longitude.number') && doc['properties.cm:longitude.number'].size() > 0) { " +
                                                    "emit(doc['properties.cm:latitude.number'].value, doc['properties.cm:longitude.number'].value); }"
                                    )
                            )
                    ));
                    innerAggregation = AggregationBuilders.geotileGrid()
                            .field(GEOPOINT_RUNTIME_FIELD)
                            .precision((int) facet.getArgs().get("precision"))
                            .build()._toAggregation();
                } else if (metadataQueryFacet.isPresent()
                        && metadataQueryFacet.get().isCombineWithSuggestions()
                        && (metadataQueryFacet.get().getItems() == null || metadataQueryFacet.get().getItems().size() <= 1)) {
                    // only show suggestions created by the current user in addition to indexed property values
                    String authority = org.alfresco.repo.security.authentication.AuthenticationUtil.getFullyAuthenticatedUser();
                    int size = metadataQueryFacet.map(MetadataQueryParameter.MetadataQueryFacet::getMaxBucketSize).orElse(searchToken.getFacetLimit() * FACET_LIMIT_MULTIPLIER);
                    if (fieldName.get(0).getNested() == null) {
                        innerAggregation = buildCombinedSuggestionFacetAggregation(
                                facet.getProperty(), fieldName.get(0).getValue(), authority, size, searchToken.getFacetsMinCount());
                    } else {
                        // nested facet fields keep the legacy script based behaviour
                        innerAggregation = buildCombinedSuggestionScriptTerms(facet.getProperty(), authority, size, searchToken.getFacetsMinCount());
                    }
                } else {
                    TermsAggregation.Builder builder = AggregationBuilders.terms()
                            .field(fieldName.get(0).getValue())
                            .size(metadataQueryFacet.map(MetadataQueryParameter.MetadataQueryFacet::getMaxBucketSize).orElse(searchToken.getFacetLimit() * FACET_LIMIT_MULTIPLIER))
                            .minDocCount(searchToken.getFacetsMinCount());
                    if(metadataQueryFacet.isPresent() && StringUtils.isNotBlank(metadataQueryFacet.get().getMissing())) {
                        builder.missing(metadataQueryFacet.get().getMissing());
                    }
                    innerAggregation = builder.build()._toAggregation();

                }
                if (fieldName.get(0).getNested() != null) {
                    bqbQuery = fullFilterQuery.build()._toQuery();
                    String nestedName = fieldName.get(0).getNested();
                    Query finalFacetsSearchFilter = facetsSearchFilter;
                    Query finalBqbQuery = bqbQuery;
                    result.put(facet.getProperty(), new Aggregation.Builder().filter(
                                    f -> f.bool(b -> b.must(finalBqbQuery).must(
                                            m -> m.nested(n -> {
                                                n.path(nestedName);
                                                if (finalFacetsSearchFilter != null) {
                                                    n.query(finalFacetsSearchFilter);
                                                }
                                                return n;
                                            }))
                                    ))
                            .aggregations(facet.getProperty(), new Aggregation.Builder().nested(n -> n.path(nestedName)).aggregations(
                                            facet.getProperty() + "_nested", innerAggregation
                                    ).build()
                            ).build()
                    );
                } else {
                    if (facetsSearchFilter != null) {
                        fullFilterQuery.must(facetsSearchFilter);
                    }
                    bqbQuery = fullFilterQuery.build()._toQuery();
                    result.put(
                            facet.getProperty(),
                            new Aggregation.Builder().filter(bqbQuery)
                                    .aggregations(facet.getProperty(), innerAggregation
                                    ).build()
                    );
                }
            } else {
                if (facetsSearchFilter != null) {
                    fullFilterQuery.must(facetsSearchFilter);
                }
                bqbQuery = fullFilterQuery.build()._toQuery();
                result.put(
                        facet.getProperty(),
                        new Aggregation.Builder().filter(
                                        bqbQuery
                                ).aggregations(facet.getProperty(), AggregationBuilders.multiTerms()
                                        .terms(fieldName.stream().map(f -> MultiTermLookup.of(t -> t.field(f.getValue()).missing(""))).collect(Collectors.toList()))
                                        .size(metadataQueryFacet.map(MetadataQueryParameter.MetadataQueryFacet::getMaxBucketSize).orElse(searchToken.getFacetLimit() * FACET_LIMIT_MULTIPLIER))
                                        .minDocCount((long) searchToken.getFacetsMinCount())
                                        .build()
                                        ._toAggregation()
                                )
                                // @TODO: Check if this is really necessary
                                .meta("type", JsonData.of("multi_terms")).build()
                );
            }


            if (parameters != null && parameters.get(facet.getProperty()) != null && parameters.get(facet.getProperty()).length > 0) {
                List<MetadataQueryParameter.MetadataQueryFacetItem> facetDetails = metadataQueryFacet.map(MetadataQueryParameter.MetadataQueryFacet::getItems).orElse(null);
                result.put(
                        facet.getProperty() + FACET_SELECTED_POSTFIX,
                        new Aggregation.Builder().filter(
                                bqbQuery
                        ).aggregations(facet.getProperty(), agg -> agg
                                .terms(term -> term
                                        .field(facetDetails == null || facetDetails.isEmpty() ? "properties." + facet.getProperty() + ".keyword" : facetDetails.get(0).getValue())
                                        .size(parameters.get(facet.getProperty()).length)
                                        .minDocCount(1)
                                        .include(ti -> ti.terms(Arrays.asList(parameters.get(facet.getProperty())))))
                        ).build()
                );

            }

        }
        searchRequestBuilder.aggregations(result);

        return result;
    }

    private static Query getFacetFilter(String queryString, String... fieldName) throws IllegalArgumentException {
        return Query.of(q -> q.bool(bool -> {
            bool.minimumShouldMatch("1");
            Arrays.stream(fieldName).forEach(
                    field -> {
                        bool.should(should -> should.wildcard(wc -> wc.field(field).value("*" + queryString + "*").caseInsensitive(true)));
                    }
            );
            return bool;
        }));
    }
}
