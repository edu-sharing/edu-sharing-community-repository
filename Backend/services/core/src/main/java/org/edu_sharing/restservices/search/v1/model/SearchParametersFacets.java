package org.edu_sharing.restservices.search.v1.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import org.edu_sharing.restservices.shared.MdsQueryCriteria;

import java.util.List;

@Schema(description = "")
public class SearchParametersFacets {
    private List<String> facets;
    Integer facetMinCount = 5;
    Integer facetLimit = 10;
    String facetSuggest;
    private List<MdsQueryCriteria> criteria;

    @Schema(required = true, description = "")
    @JsonProperty("facets")
    public List<String> getFacets() { return facets;}
    public void setFacets(List<String> facets) { this.facets = facets; }

    @Schema(required = false, defaultValue = "5")
    @JsonProperty("facetMinCount")
    public Integer getFacetMinCount() { return facetMinCount; }
    public void setFacetMinCount(Integer facetMinCount) { this.facetMinCount = facetMinCount;}

    @Schema(required = false, defaultValue = "10")
    @JsonProperty("facetLimit")
    public Integer getFacetLimit() { return facetLimit; }
    public void setFacetLimit(Integer facetLimit) { this.facetLimit = facetLimit; }

    @Schema(required = false)
    @JsonProperty("facetSuggest")
    public String getFacetSuggest() { return facetSuggest; }
    public void setFacetSuggest(String facetSuggest) { this.facetSuggest = facetSuggest; }

    @Schema(required = true, description = "")
    @JsonProperty("criteria")
    public List<MdsQueryCriteria> getCriteria() {
        return criteria;
    }

    public void setCriteria(List<MdsQueryCriteria> criteria) {
        this.criteria = criteria;
    }
}
