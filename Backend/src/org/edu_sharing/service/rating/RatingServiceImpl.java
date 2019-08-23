package org.edu_sharing.service.rating;

import com.google.gdata.util.common.base.Pair;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.apache.commons.collections.KeyValue;
import org.apache.log4j.Logger;
import org.edu_sharing.alfresco.policy.GuestCagePolicy;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.tools.cache.EduSharingRatingCache;
import org.edu_sharing.service.InsufficientPermissionException;
import org.edu_sharing.service.authority.AuthorityService;
import org.edu_sharing.service.authority.AuthorityServiceFactory;
import org.edu_sharing.service.nodeservice.NodeService;
import org.edu_sharing.service.nodeservice.NodeServiceFactory;
import org.edu_sharing.service.nodeservice.NodeServiceHelper;
import org.edu_sharing.service.permission.PermissionService;
import org.edu_sharing.service.permission.PermissionServiceFactory;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RatingServiceImpl implements RatingService {

    private Logger logger= Logger.getLogger(RatingServiceImpl.class);

    private PermissionService permissionService;
    private AuthorityService authorityService;
    private NodeService nodeService;

    public RatingServiceImpl() {
        this.nodeService=NodeServiceFactory.getLocalService();
        this.authorityService=AuthorityServiceFactory.getLocalService();
        this.permissionService=PermissionServiceFactory.getLocalService();
    }

    @Override
    public void addOrUpdateRating(String nodeId, Double rating, String text) throws Exception {
        checkPreconditions(nodeId);
        AuthenticationUtil.runAsSystem(()->{
            Rating currentRating = getRatingForUser(nodeId);
            HashMap<String, Object> props=new HashMap<>();
            props.put(CCConstants.CCM_PROP_RATING_VALUE,rating);
            props.put(CCConstants.CCM_PROP_RATING_TEXT,text);
            if(currentRating==null){
                nodeService.createNodeBasic(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE,nodeId,CCConstants.CCM_TYPE_RATING,CCConstants.CCM_ASSOC_RATING, props);
            }
            else{
                nodeService.updateNodeNative(currentRating.getRef().getId(),props);
            }
            invalidateCache(nodeId);
            return null;
        });
    }

    private void invalidateCache(String nodeId) {
        EduSharingRatingCache.delete(new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE,nodeId));
    }

    @Override
    public List<Rating> getRatings(String nodeId) {
        return this.nodeService.getChildrenChildAssociationRefType(nodeId,CCConstants.CCM_TYPE_RATING).stream().
                map((ref)->{
                    Rating rating = new Rating();
                    rating.setRef(ref.getChildRef());
                    rating.setRating(Double.parseDouble(NodeServiceHelper.getProperty(ref.getChildRef(),CCConstants.CCM_PROP_RATING_VALUE)));
                    rating.setText(NodeServiceHelper.getProperty(ref.getChildRef(),CCConstants.CCM_PROP_RATING_TEXT));
                    rating.setAuthority(NodeServiceHelper.getProperty(ref.getChildRef(),CCConstants.CM_PROP_C_CREATOR));
                    return rating;
                }).collect(Collectors.toList());
    }
    private Rating getRatingForUser(String nodeId){
       return getRatings(nodeId).stream().filter((r) -> r.getAuthority().equals(AuthenticationUtil.getFullyAuthenticatedUser())).findFirst().orElse(null);
    }
    @Override
    public void deleteRating(String nodeId) throws Exception {
        checkPreconditions(nodeId);
        AuthenticationUtil.runAsSystem(()-> {
            Rating rating = getRatingForUser(nodeId);
            if (rating != null) {
                nodeService.removeNode(rating.getRef().getId(), nodeId, false);
            } else {
                throw new IllegalArgumentException("No rating for current user found for the given node");
            }
            invalidateCache(nodeId);
            return null;
        });
    }

    @Override
    public AccumulatedRatings getAccumulatedRatings(String nodeId){
        try {
            AccumulatedRatings accumulated = EduSharingRatingCache.get(new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, nodeId));
            if (accumulated != null) {
                logger.info("using rating cache for node " + nodeId);
                return accumulated;
            }
        }catch(Exception e){
            logger.warn("Failed to resolve rating cache for node "+nodeId+": "+e.getMessage());
        }

        List<Rating> ratings = this.getRatings(nodeId);
        //@TODO: Duplicated call for getRatings
        Rating userRating = this.getRatingForUser(nodeId);

        AccumulatedRatings accumulated = new AccumulatedRatings();
        accumulated.setOverall(new AccumulatedRatings.RatingData(ratings.stream().map(Rating::getRating).reduce((a, b)->a+b).orElse(0.),ratings.size()));
        accumulated.setUser(userRating==null ? 0 : userRating.getRating());
        HashMap<Object, AccumulatedRatings.RatingData> affiliation = new HashMap<>();
        // collect counts for each affiliation group
        ratings.forEach((r)->{
            String authorityAffiliation= (String)authorityService.getAuthorityProperty(r.getAuthority(), CCConstants.CM_PROP_PERSON_EDU_SCHOOL_PRIMARY_AFFILIATION);
            if(authorityAffiliation==null)
                authorityAffiliation="none";
            AccumulatedRatings.RatingData entry = affiliation.getOrDefault(authorityAffiliation, new AccumulatedRatings.RatingData(0,0));
            entry.setCount(entry.getCount()+1);
            entry.setSum(entry.getSum()+r.getRating());
            affiliation.put(authorityAffiliation,entry);
        });
        accumulated.setAffiliation(affiliation);
        EduSharingRatingCache.put(new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, nodeId),accumulated);
        return accumulated;
    }

    private void checkPreconditions(String nodeId) throws Exception {
        if(authorityService.isGuest()){
            throw new GuestCagePolicy.GuestPermissionDeniedException("guests can not use ratings");
        }
        if(!NodeServiceHelper.getType(new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE,nodeId)).equals(CCConstants.CCM_TYPE_IO)){
            throw new IllegalArgumentException("Ratings only supported for nodes of type "+CCConstants.CCM_TYPE_IO);
        }
        List<String> permissions = permissionService.getPermissionsForAuthority(nodeId, AuthenticationUtil.getFullyAuthenticatedUser());
        if (!permissions.contains(CCConstants.PERMISSION_RATE)) {
            throw new InsufficientPermissionException("No permission '" + CCConstants.PERMISSION_RATE + "' to add ratings to node " + nodeId);
        }
    }
}
