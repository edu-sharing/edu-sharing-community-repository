package org.edu_sharing.service.stream.model;

import java.util.List;
import java.util.Map;

public class StreamSearchRequest {
	public List<String> authority;
	public ContentEntry.Audience.STATUS status;
	public Map<String,String> properties;
	public String search;
	public int offset=0;
	public int size=0;
	public StreamSearchRequest(List<String> authority,ContentEntry.Audience.STATUS status) {
		this.authority=authority;
		this.status=status;
	}
}
