package org.edu_sharing.service.search;

import org.apache.log4j.Logger;
import org.edu_sharing.metadataset.v2.MetadataQueryParameter;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.index.query.WrapperQueryBuilder;
import org.elasticsearch.search.aggregations.*;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.aggregations.support.AggregationContext;
import org.elasticsearch.xcontent.NamedXContentRegistry;
import org.elasticsearch.xcontent.ParseField;
import org.elasticsearch.xcontent.XContentBuilder;
import org.elasticsearch.xcontent.XContentParser;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

/**
 * Wrapper for Elastic RestHighLevelClient
 * to include required patches
 */
public class ESRestHighLevelClient extends RestHighLevelClient {
    static Logger logger = Logger.getLogger(ESRestHighLevelClient.class);

    /**
     * public <T> Entry(Class<T> categoryClass, ParseField name, CheckedFunction<XContentParser, ? extends T, IOException> parser) {
     *             this.categoryClass = Objects.requireNonNull(categoryClass);
     *             this.name = Objects.requireNonNull(name);
     *             this.parser = Objects.requireNonNull((p, c) -> parser.apply(p));
     *         }
     * @param builder
     */
    public ESRestHighLevelClient(RestClientBuilder builder) {
        super(builder, Collections.singletonList(
                new NamedXContentRegistry.Entry(
                        Aggregation.class,
                        new ParseField("multi_terms"),
                        ESRestHighLevelClient::getMultiTerms
                )
        ));
    }

    private static Aggregation getMultiTerms(XContentParser p, Object c) throws IOException {
        return ParsedStringTerms.fromXContent(p, (String) c);
    }

    /**
     * this feature is missing in the java rest client
     * https://github.com/elastic/elasticsearch/issues/75030
     */
    public static class MultiTermsAggregationBuilder extends AbstractAggregationBuilder {

        private final Collection<String> termFields;
        private final int minDocCount;
        private final int size;

        public MultiTermsAggregationBuilder(String name, Collection<String> termFields, int minDocCount, int size) {
            super(name);
            this.termFields = termFields;
            this.minDocCount = minDocCount;
            this.size = size;
        }

        @Override
        protected void doWriteTo(StreamOutput out) throws IOException {
        }

        @Override
        protected AggregatorFactory doBuild(AggregationContext context, AggregatorFactory parent, AggregatorFactories.Builder subfactoriesBuilder) throws IOException {
            return null;
        }

        @Override
        protected XContentBuilder internalXContent(XContentBuilder builder, Params params) throws IOException {
            builder.startObject();
            builder.field("min_doc_count", minDocCount);
            builder.field("size", size);
            builder.startArray("terms");
            for (String termField : termFields) {
                builder.startObject();
                builder.field("field", termField);
                // missing elements should still behave and not be filtered
                // otherwise, the query will only match if both files have values
                builder.field("missing", "");
                builder.endObject();
            }
            builder.endArray();
            builder.endObject();
            return builder;
        }

        @Override
        protected AggregationBuilder shallowCopy(AggregatorFactories.Builder factoriesBuilder, Map<String, Object> metadata) {
            return null;
        }

        @Override
        public BucketCardinality bucketCardinality() {
            return null;
        }

        @Override
        public String getType() {
            return "multi_terms";
        }
    }

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
}
