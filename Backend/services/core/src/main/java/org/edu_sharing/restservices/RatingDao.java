package org.edu_sharing.restservices;

import org.edu_sharing.service.model.NodeRef;
import org.edu_sharing.service.model.NodeRefImpl;
import org.edu_sharing.service.rating.*;

import java.util.Date;
import java.util.List;

public class RatingDao {

	private RatingService ratingService;
	private RepositoryDao repoDao;
	public RatingDao(RepositoryDao repoDao) {
		this.repoDao = repoDao;
		this.ratingService = RatingServiceFactory.getRatingService(repoDao.getId());
	}
	public void addOrUpdateRating(String nodeId,Double rating,String text) throws DAOException{
		try{
			this.ratingService.addOrUpdateRating(nodeId, rating, text);
		}catch(Exception e){
			throw DAOException.mapping(e);
		}
	}
	public void deleteRating(String nodeId) throws DAOException {
		try{
			this.ratingService.deleteRating(nodeId);
		}catch(Exception e){
			throw DAOException.mapping(e);
		}
	}

	public List<RatingHistory> getAccumulatedRatingHistory(String nodeId, Date after) throws DAOException {
		try{
			return this.ratingService.getAccumulatedRatingHistory(new NodeRefImpl(repoDao.getId(), null, null, nodeId), after);
		}catch(Exception e) {
			throw DAOException.mapping(e);
		}
	}

	public List<String> getAlteredNodes(Date after) throws DAOException {
		try{
			return this.ratingService.getAlteredNodeIds(after);
		}catch(Exception e) {
			throw DAOException.mapping(e);
		}
	}
}
