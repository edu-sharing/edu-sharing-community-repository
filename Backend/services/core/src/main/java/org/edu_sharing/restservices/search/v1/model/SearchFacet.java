package org.edu_sharing.restservices.search.v1.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Map;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class SearchFacet {
    @JsonProperty
    @JsonPropertyDescription("facet property name (as defined in the mds query)")
    private String property;
    @JsonProperty
    @JsonPropertyDescription("additional map of arguments to be given into the aggs builder, i.e. \"precision\" for geo aggregations")
    private Map<String, Serializable> args;
}
