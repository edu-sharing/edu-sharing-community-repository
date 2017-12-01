package org.edu_sharing.restservices.shared;


import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;


@ApiModel(description = "")
public class Pagination  {
  
  private Integer total = null;
  private Integer from = null;
  private Integer count = null;

  public Pagination(){}
  public Pagination(org.edu_sharing.service.search.model.SearchResult result) {
	  from=result.getSkipCount();
	  total=result.getTotalCount();
	  count=result.getCount();
  }
/**
   **/
  @ApiModelProperty(required = true, value = "")
  @JsonProperty("total")
  public Integer getTotal() {
    return total;
  }
  public void setTotal(Integer total) {
    this.total = total;
  }

  
  /**
   **/
  @ApiModelProperty(required = true, value = "")
  @JsonProperty("from")
  public Integer getFrom() {
    return from;
  }
  public void setFrom(Integer from) {
    this.from = from;
  }

  
  /**
   **/
  @ApiModelProperty(required = true, value = "")
  @JsonProperty("count")
  public Integer getCount() {
    return count;
  }
  public void setCount(Integer count) {
    this.count = count;
  }

}
