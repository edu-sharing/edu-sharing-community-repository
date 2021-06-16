package org.edu_sharing.restservices.connector.v1.model;

import org.edu_sharing.repository.client.tools.CCConstants;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.annotations.ApiModel;

@ApiModel(description = "")
public class ConnectorFileType {
	private String ccressourceversion,ccressourcetype,ccresourcesubtype,editorType;
	@JsonProperty("ccressourceversion")
	public String getCcressourceversion() {
		return ccressourceversion;
	}
	public void setCcressourceversion(String ccressourceversion) {
		this.ccressourceversion = ccressourceversion;
	}
	@JsonProperty("editorType")
	public String getEditorType() {
		return editorType;
	}
	public void setEditorType(String editorType) {
		this.editorType = editorType;
	}
	@JsonProperty("ccressourcetype")
	public String getCcressourcetype() {
		return ccressourcetype;
	}
	public void setCcressourcetype(String ccressourcetype) {
		this.ccressourcetype = ccressourcetype;
	}
	@JsonProperty("ccresourcesubtype")
	public String getCcresourcesubtype() {
		return ccresourcesubtype;
	}
	public void setCcresourcesubtype(String ccresourcesubtype) {
		this.ccresourcesubtype = ccresourcesubtype;
	}
	private String mimetype,filetype;
	@JsonProperty("mimetype")
	public String getMimetype() {
		return mimetype;
	}
	public void setMimetype(String mimetype) {
		this.mimetype = mimetype;
	}
	@JsonProperty("filetype")
	public String getFiletype() {
		return filetype;
	}
	public void setFiletype(String filetype) {
		this.filetype = filetype;
	}
	@JsonProperty("creatable")
	public boolean isCreatable() {
		return creatable;
	}
	public void setCreatable(boolean creatable) {
		this.creatable = creatable;
	}
	@JsonProperty("editable")
	public boolean isEditable() {
		return editable;
	}
	public void setEditable(boolean editable) {
		this.editable = editable;
	}
	private boolean creatable,editable;
}
