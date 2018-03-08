package org.edu_sharing.service.statistic;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

public class StatisticsGlobal {
	public static class Materials{
		@JsonProperty
		public List<License> licenses;
	}
	public static class License{
		public static class Facette{
			@JsonProperty public String name;
			@JsonProperty public Map<String,Integer> count;
		}
		@JsonProperty public String name;
		@JsonProperty public String type;
		@JsonProperty public int count;
		@JsonProperty public List<Facette> facettes;
	}
	public static class User{
		@JsonProperty public int count;
	}
	@JsonProperty
	private Materials materials;
	@JsonProperty
	private User user;
	
	public Materials getMaterials() {
		return materials;
	}
	public void setMaterials(Materials materials) {
		this.materials = materials;
	}
	public User getUser() {
		return user;
	}
	public void setUser(User user) {
		this.user = user;
	}
	
	
	
	
}
