package org.edu_sharing.restservices.shared;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.edu_sharing.repository.server.SearchResultNodeRef;
import org.springframework.data.domain.Page;

@Data
public class Pagination  {

  @JsonProperty(required = true)
  private Integer total = null;
  @JsonProperty(required = true)
  private Integer from = null;
  @JsonProperty(required = true)
  private Integer count = null;

  public Pagination(){}
  public Pagination(org.edu_sharing.service.search.model.SearchResult<?> result) {
	  from=result.getSkipCount();
	  total=result.getTotalCount();
	  count=result.getCount();
  }
  public Pagination(SearchResultNodeRef result) {
    from=result.getStartIDX();
    total=result.getNodeCount();
    count=result.getData().size();
  }

  public Pagination(List<?> result) {
    from=0;
    total=result.size();
    count=result.size();
  }

  public Pagination(int from, int total, int count) {
      this.from = from;
      this.total = total;
      this.count = count;
  }

  public Pagination(Page<?> page) {
    this.from = (int)page.getPageable().getOffset();
    this.total = (int)page.getTotalElements();
    this.count = page.getContent().size();
  }



}
