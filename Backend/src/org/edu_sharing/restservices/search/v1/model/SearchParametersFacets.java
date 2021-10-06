package org.edu_sharing.restservices.search.v1.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiParam;

import javax.ws.rs.QueryParam;
import java.util.List;

@ApiModel(description = "")
public class SearchParametersFacets {
    private List<String> facettes;
    Integer facetMinCount;
    Integer facetLimit;
    String facetSuggest;

    @ApiModelProperty(required = true, value = "")
    @JsonProperty("facettes")
    public List<String> getFacettes() { return facettes;}
    public void setFacettes(List<String> facettes) { this.facettes = facettes; }

    @ApiModelProperty(required = true, value = "5")
    @JsonProperty("facetMinCount")
    public Integer getFacetMinCount() { return facetMinCount; }
    public void setFacetMinCount(Integer facetMinCount) { this.facetMinCount = facetMinCount;}

    @ApiModelProperty(required = true, value = "10")
    @JsonProperty("facetLimit")
    public Integer getFacetLimit() { return facetLimit; }
    public void setFacetLimit(Integer facetLimit) { this.facetLimit = facetLimit; }

    @ApiModelProperty(required = false)
    @JsonProperty("facetSuggest")
    public String getFacetSuggest() { return facetSuggest; }
    public void setFacetSuggest(String facetSuggest) { this.facetSuggest = facetSuggest; }
}
