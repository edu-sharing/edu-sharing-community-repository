package org.edu_sharing.restservices.mds.v1.model;

import java.util.List;

import org.edu_sharing.metadataset.v2.MetadataReaderV2;
import org.edu_sharing.metadataset.v2.MetadataTemplate;
import org.edu_sharing.metadataset.v2.MetadataWidget;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.annotations.ApiModel;
@ApiModel(description = "")
public class ViewV2 {
	private String id,caption,icon,html;
	private String rel;
	private boolean hideIfEmpty;
	private boolean isExtended;

	public ViewV2(){}
	public ViewV2(MetadataTemplate template) {
		this.id=template.getId();
		this.caption=template.getCaption();
		this.hideIfEmpty=template.getHideIfEmpty();
		this.isExtended=template.isExtended();
		this.icon=template.getIcon();
		this.html=template.getHtml();
		this.rel=template.getRel();
	}

	@JsonProperty("caption")
	public String getCaption() {
		return caption;
	}
	public void setCaption(String caption) {
		this.caption = caption;
	}
	@JsonProperty("id")
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	@JsonProperty("html")
	public String getHtml() {
		return html;
	}
	public void setHtml(String html) {
		this.html = html;
	}
	@JsonProperty("icon")
	public String getIcon() {
		return icon;
	}
	public void setIcon(String icon) {
		this.icon = icon;
	}
	@JsonProperty("rel")
	public String getRel() {
		return rel;
	}
	public void setRel(String rel) {
		this.rel = rel;
	}
	@JsonProperty("hideIfEmpty")
	public boolean isHideIfEmpty() {
		return hideIfEmpty;
	}
	public void setHideIfEmpty(boolean hideIfEmpty) {
		this.hideIfEmpty = hideIfEmpty;
	}
	@JsonProperty("isExtended")
	public boolean isExtended() {
		return isExtended;
	}

	public void setExtended(boolean extended) {
		isExtended = extended;
	}
}

