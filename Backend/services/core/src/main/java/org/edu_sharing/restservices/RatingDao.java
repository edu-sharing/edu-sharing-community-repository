package org.edu_sharing.restservices;

import org.edu_sharing.service.rating.RatingService;
import org.edu_sharing.service.rating.RatingServiceFactory;

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
}
