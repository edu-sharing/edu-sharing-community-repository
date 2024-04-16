package org.edu_sharing.service.search;

import co.elastic.clients.elasticsearch._types.query_dsl.WrapperQuery;
import lombok.extern.slf4j.Slf4j;
import org.edu_sharing.metadataset.v2.MetadataQueryParameter;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Base64;

/**
 * make the query builder readable if print to string to improve debugging
 */
@Slf4j
public class ReadableWrapperQueryBuilder extends WrapperQuery.Builder {
    private final MetadataQueryParameter parameter;
    private final String query;

    public ReadableWrapperQueryBuilder(String query) {
        this(query, null);
    }

    public ReadableWrapperQueryBuilder(String query, MetadataQueryParameter parameter) {
        query(Base64.getEncoder().encodeToString(query.getBytes()));
        this.query = query;
        this.parameter = parameter;
    }

    @Override
    public WrapperQuery build() {
        try {
            new JSONObject(this.query);
            return super.build();
        } catch (JSONException e) {
            log.warn("The given json is invalid: " + e.getMessage());
            log.warn("Query: " + (parameter != null ? (parameter.getName() + ": ") : "") + query);
            throw e;
        }
    }
}
