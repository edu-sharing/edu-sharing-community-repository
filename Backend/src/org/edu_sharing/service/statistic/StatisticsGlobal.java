package org.edu_sharing.service.statistic;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

public class StatisticsGlobal{
    public static abstract class TranslatableKey{
        @JsonProperty public String key;
        @JsonProperty public String displayName;
        TranslatableKey(){}
        TranslatableKey(String key){
            this.key=key;
            this.displayName=key;
        }
        TranslatableKey(String key, String displayName){
            this(key);
            this.displayName=displayName;
        }
    }
	public static class Repository{
		@JsonProperty public String name;
		@JsonProperty public String domain;
		@JsonProperty public Long queryTime;
	}
	public static class Group{
		public static class SubGroup{
            public static class SubGroupItem extends TranslatableKey{
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
		@JsonProperty public List<SubGroup> subGroups;
	}
    public static class KeyGroup extends TranslatableKey{
        @JsonProperty public int count;
        @JsonProperty public List<Group.SubGroup> subGroups;
    }
	public static class User{
		@JsonProperty public int count;
	}
    @JsonProperty
    private Group overall;
	@JsonProperty
	private List<KeyGroup> groups;
	@JsonProperty
	private User user;

    public Group getOverall() {
        return overall;
    }

    public void setOverall(Group overall) {
        this.overall = overall;
    }

    public List<KeyGroup> getGroups() {
        return groups;
    }

    public void setGroups(List<KeyGroup> groups) {
        this.groups = groups;
    }

    public User getUser() {
		return user;
	}
	public void setUser(User user) {
		this.user = user;
	}

}
