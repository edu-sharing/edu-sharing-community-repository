package org.edu_sharing.restservices.node.v1.model;

import org.edu_sharing.restservices.shared.Node;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema;;

@Schema(description = "")
public class NodeText  {
  
  private String text,html,raw;

  @JsonProperty
  public String getText() {
	return text;
}
public void setText(String text) {
	this.text = text;
}
@JsonProperty
public String getHtml() {
	return html;
}
public void setHtml(String html) {
	this.html = html;
}
@JsonProperty
public String getRaw() {
	return raw;
}
public void setRaw(String raw) {
	this.raw = raw;
}


}
