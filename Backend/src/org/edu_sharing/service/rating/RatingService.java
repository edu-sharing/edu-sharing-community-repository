package org.edu_sharing.service.rating;

import org.alfresco.service.cmr.repository.ChildAssociationRef;

import java.util.List;

public interface RatingService {

	void addOrUpdateRating(String node, Double rating, String text) throws Exception;

	List<Rating> getRatings(String node);

	void deleteRating(String nodeId) throws Exception;

	AccumulatedRatings getAccumulatedRatings(String nodeId);
}
