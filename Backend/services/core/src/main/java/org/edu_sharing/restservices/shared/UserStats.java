package org.edu_sharing.restservices.shared;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.Data;

@Data
public class UserStats  {
	@JsonPropertyDescription("All elements from this user (the current user has access to)")
	private UserStatsGroup allStats;

	@JsonPropertyDescription("All elements from this user shared publicly")
	private UserStatsGroup publicStats;

	@Data
	public static class UserStatsGroup {
		private int nodeCount = 0;
		private int nodeCountCC = 0;
		private int collectionCount = 0;
	}
}
