package org.edu_sharing.restservices.node.v1.model;

import org.edu_sharing.restservices.shared.Node;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(description = "")
public class NodeText  {
  
  private String text,html;

  @JsonProperty("text")
  public String getText() {
	return text;
}
public void setText(String text) {
	this.text = text;
}
@JsonProperty("html")
public String getHtml() {
	return html;
}
public void setHtml(String html) {
	this.html = html;
}

}
