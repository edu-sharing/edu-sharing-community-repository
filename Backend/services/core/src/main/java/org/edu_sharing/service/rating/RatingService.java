package org.edu_sharing.service.rating;

import java.util.Date;
import java.util.List;

public interface RatingService {

	void addOrUpdateRating(String node, Double rating, String text) throws Exception;

	List<Rating> getRatings(String nodeId, Date after);

	void deleteRating(String nodeId) throws Exception;

	RatingDetails getAccumulatedRatings(String nodeId, Date after);
}
