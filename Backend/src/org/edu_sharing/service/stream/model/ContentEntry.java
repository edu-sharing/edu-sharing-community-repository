package org.edu_sharing.service.stream.model;

import java.util.List;
import java.util.Map;

public class ContentEntry{
	public static class Audience{
		public String authority;
		public STATUS status;
		
		public enum STATUS{
			OPEN,
			READ,
			PROGRESS,
			DONE
		};
	}
	public String id;
	public Number priority=0;
	public List<String> nodeId;
	public Number created;
	public Number modified;
	public String author;
	public List<Audience> audience;
	public String description;
	public String title;
	public Map<String,Object> properties;
	
}