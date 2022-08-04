package org.edu_sharing.service.rating;

import org.apache.commons.lang.NotImplementedException;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.Date;
import java.util.List;

public class RatingServiceAdapter implements RatingService {

    private final Logger logger= Logger.getLogger(RatingServiceAdapter.class);

    public RatingServiceAdapter(String appId) {

    }

    @Override
    public void addOrUpdateRating(String nodeId, Double rating, String text) throws Exception {
        throw new NotImplementedException();
    }

    private void invalidateCache(String nodeId) {
        throw new NotImplementedException();
    }

    @Override
    public List<Rating> getRatings(String nodeId, Date after) {
        return null;
    }
    private Rating getRatingForUser(String nodeId){
       return null;
    }
    @Override
    public void deleteRating(String nodeId) throws Exception {
        throw new NotImplementedException();
    }

    @Override
    public RatingDetails getAccumulatedRatings(String nodeId, Date after){
        return null;
    }

    @Override
    public List<String> getAlteredNodeIds(@NotNull Date after) {
        return null;
    }

    @Override
    public List<RatingHistory> getAccumulatedRatingHistory(String nodeId, Date after) {
        return null;
    }

    @Override
    public void changeUserData(@NotNull String oldAuthority, @NotNull String newAuthority) {

    }

    @Override
    public void deleteUserData(@NotNull String authority) {

    }
}
