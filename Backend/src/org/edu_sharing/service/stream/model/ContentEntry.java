package org.edu_sharing.service.stream.model;

import java.util.List;

public class ContentEntry{
	public static class Audience{
		public String authority;
		public STATUS status;
		
		public enum STATUS{
			OPEN,
			PROGRESS,
			DONE
		};
	}
	public String id;
	public Number priority=0;
	public List<String> nodeId;
	public List<String> category;
	public Number created;
	public Number modified;
	public String author;
	public List<Audience> audience;
	public String description;
	
}