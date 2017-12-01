package org.edu_sharing.restservices.iam.v1.model;

import java.util.List;

import org.edu_sharing.restservices.shared.Pagination;
import org.edu_sharing.restservices.shared.User;
import org.edu_sharing.restservices.shared.UserSimple;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;


@ApiModel(description = "")
public class UserEntries  {
  
	  private List<UserSimple> list = null;
	  private Pagination pagination = null;

  
  /**
   **/
  @ApiModelProperty(required = true, value = "")
  @JsonProperty("users")
  public List<UserSimple> getList() {
    return list;
  }
  public void setList(List<UserSimple> list) {
    this.list = list;
  }

  

  @Override
  public String toString()  {
    StringBuilder sb = new StringBuilder();
    sb.append("class UserEntries {\n");
    
    sb.append("  list: ").append(list).append("\n");
    sb.append("}\n");
    return sb.toString();
  }
  @ApiModelProperty(required = true, value = "")
  @JsonProperty("pagination")
	public Pagination getPagination() {
		return pagination;
	}
	public void setPagination(Pagination pagination) {
		this.pagination = pagination;
	}
}
