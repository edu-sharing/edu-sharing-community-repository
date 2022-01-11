package org.edu_sharing.service.statistic;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class StatisticsGlobal{
    public static abstract class StatisticsTranslatableKey {
        @JsonProperty public String key;
        @JsonProperty public String displayName;
        StatisticsTranslatableKey(){}
        StatisticsTranslatableKey(String key){
            this.key=key;
            this.displayName=key;
        }
        StatisticsTranslatableKey(String key, String displayName){
            this(key);
            this.displayName=displayName;
        }
    }
	public static class Repository{
		@JsonProperty public String name;
		@JsonProperty public String domain;
		@JsonProperty public Long queryTime;
	}
	public static class StatisticsGroup {
		public static class StatisticsSubGroup {
            public static class SubGroupItem extends StatisticsTranslatableKey {
                @JsonProperty public int count;

                public SubGroupItem(String key, Integer count) {
                    super(key);
                    this.count=count;
                }
                public SubGroupItem(String key, String displayName, Integer count) {
                    super(key,displayName);
                    this.count=count;
                }
            }
            @JsonProperty public String id;
            @JsonProperty public List<SubGroupItem> count;
		}
		@JsonProperty public int count;
		@JsonProperty public List<StatisticsSubGroup> subGroups;
	}
    public static class StatisticsKeyGroup extends StatisticsTranslatableKey {
        @JsonProperty public int count;
        @JsonProperty public List<StatisticsGroup.StatisticsSubGroup> subGroups;
    }
	public static class StatisticsUser {
		@JsonProperty public int count;
	}
    @JsonProperty
    private StatisticsGroup overall;
	@JsonProperty
	private List<StatisticsKeyGroup> groups;
	@JsonProperty
	private StatisticsUser user;

    public StatisticsGroup getOverall() {
        return overall;
    }

    public void setOverall(StatisticsGroup overall) {
        this.overall = overall;
    }

    public List<StatisticsKeyGroup> getGroups() {
        return groups;
    }

    public void setGroups(List<StatisticsKeyGroup> groups) {
        this.groups = groups;
    }

    public StatisticsUser getUser() {
		return user;
	}
	public void setUser(StatisticsUser user) {
		this.user = user;
	}

}
