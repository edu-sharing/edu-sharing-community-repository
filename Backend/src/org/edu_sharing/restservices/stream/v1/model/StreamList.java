package org.edu_sharing.restservices.stream.v1.model;

import java.util.List;

import org.edu_sharing.restservices.shared.Pagination;

import com.fasterxml.jackson.annotation.JsonProperty;

public class StreamList {
	@JsonProperty private List<StreamEntry> stream;
	@JsonProperty private Pagination pagination;
	public List<StreamEntry> getStream() {
		return stream;
	}
	public void setStream(List<StreamEntry> stream) {
		this.stream = stream;
	}
	public Pagination getPagination() {
		return pagination;
	}
	public void setPagination(Pagination pagination) {
		this.pagination = pagination;
	}	
}
