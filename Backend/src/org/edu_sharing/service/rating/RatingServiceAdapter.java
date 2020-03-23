package org.edu_sharing.service.rating;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.namespace.QName;
import org.apache.commons.lang.NotImplementedException;
import org.apache.log4j.Logger;
import org.edu_sharing.alfresco.policy.GuestCagePolicy;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
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
import org.edu_sharing.service.toolpermission.ToolPermissionHelper;
import org.springframework.context.ApplicationContext;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class RatingServiceAdapter implements RatingService {

    private Logger logger= Logger.getLogger(RatingServiceAdapter.class);

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
    public AccumulatedRatings getAccumulatedRatings(String nodeId, Date after){
        return null;
    }
}
