package org.edu_sharing.restservices.iam.v1.model;

import java.util.List;

import org.edu_sharing.restservices.shared.Group;
import org.edu_sharing.restservices.shared.Pagination;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema;;


@Schema(description = "")
public class GroupEntries  {
  
  private List<Group> list = null;
  private Pagination pagination = null;

  
  /**
   **/
  @Schema(required = true, description = "")
  @JsonProperty("groups")
  public List<Group> getList() {
    return list;
  }
  /**
   **/
  @Schema(required = true, description = "")
  @JsonProperty("pagination")
  public Pagination getPagination() {
    return pagination;
  }
  public void setList(List<Group> list) {
    this.list = list;
  }

  

  @Override
  public String toString()  {
    StringBuilder sb = new StringBuilder();
    sb.append("class GroupEntries {\n");
    
    sb.append("  list: ").append(list).append("\n");
    sb.append("}\n");
    return sb.toString();
  }
	public void setPagination(Pagination pagination) {
		this.pagination=pagination;	
	}
	
}
