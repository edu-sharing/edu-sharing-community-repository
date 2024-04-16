package org.edu_sharing.service.rating;

import org.edu_sharing.service.model.NodeRef;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Date;
import java.util.List;

public interface RatingService {

	void addOrUpdateRating(String node, Double rating, String text) throws Exception;

	List<Rating> getRatings(String nodeId, Date after);

	void deleteRating(String nodeId) throws Exception;

	RatingDetails getAccumulatedRatings(NodeRef nodeId, Date after);

	List<String> getAlteredNodeIds(Date after);

	List<RatingHistory> getAccumulatedRatingHistory(NodeRef nodeId, Date after);

	void changeUserData(@NotNull String oldAuthority, @NotNull String newAuthority);

	void deleteUserData(@NotNull String authority);
}
