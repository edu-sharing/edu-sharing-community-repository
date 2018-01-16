package org.edu_sharing.service.statistic;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class StatisticsGlobal {
	public static class Materials{
		@JsonProperty public int overall;
		@JsonProperty public int image;
		@JsonProperty public int video;
		@JsonProperty public int text;
		@JsonProperty public int spreadsheet;
		@JsonProperty public int presentation;		
		@JsonProperty public int link;		
	}
	public static class Licenses{
		@JsonProperty public int CC_0;
		@JsonProperty public int CC_BY;
		@JsonProperty public int PDM;
		@JsonProperty public int unknown;
	}
	public static class History{
		@JsonProperty public String date;
		@JsonProperty public int created;
		@JsonProperty public int modified;
	}
	public static class User{
		@JsonProperty public int overall;
	}
	@JsonProperty
	private Materials materials;
	@JsonProperty
	private Licenses licenses;
	@JsonProperty
	private User user;
	@JsonProperty
	private List<History> history_daily;
	@JsonProperty
	private List<History> history_monthly;
	
	public Materials getMaterials() {
		return materials;
	}
	public void setMaterials(Materials materials) {
		this.materials = materials;
	}
	public Licenses getLicenses() {
		return licenses;
	}
	public void setLicenses(Licenses licenses) {
		this.licenses = licenses;
	}
	public User getUser() {
		return user;
	}
	public void setUser(User user) {
		this.user = user;
	}
	public List<History> getHistory_daily() {
		return history_daily;
	}
	public void setHistory_daily(List<History> history_daily) {
		this.history_daily = history_daily;
	}
	public List<History> getHistory_monthly() {
		return history_monthly;
	}
	public void setHistory_monthly(List<History> history_monthly) {
		this.history_monthly = history_monthly;
	}
	
	
}
