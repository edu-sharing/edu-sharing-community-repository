package org.edu_sharing.repository.server;

import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.edu_sharing.repository.client.SuggestService;
import org.edu_sharing.repository.client.rpc.metadataset.MetadataSetBaseProperty;
import org.edu_sharing.repository.server.tools.metadataset.MetadataCache;
import org.edu_sharing.service.suggest.SuggestDAO;
import org.edu_sharing.service.suggest.SuggestDAOFactory;

import com.google.gwt.user.client.ui.SuggestOracle;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;

public class SuggestServiceImpl extends RemoteServiceServlet implements SuggestService{

	Logger logger = Logger.getLogger(SuggestServiceImpl.class);
	
	
	@Override
	public ArrayList<? extends  SuggestOracle.Suggestion> request(String query, Integer mdsPropertyId) {
		
		ArrayList<? extends SuggestOracle.Suggestion> result = null;
		
		try{
			MetadataSetBaseProperty mdsbp = MetadataCache.getMetadataSetProperty(mdsPropertyId);
			
			SuggestDAO suggestDao = SuggestDAOFactory.getSuggestDAO(mdsbp);
			result = (ArrayList<? extends SuggestOracle.Suggestion>)suggestDao.query(query);
			

		}catch(Exception e){
			logger.error(e.getMessage(), e);
		}
		
		return result;
		
	}
	
	@Override
	public String getValue(String key, Integer mdsPropertyId) {
		try{
			MetadataSetBaseProperty mdsbp = MetadataCache.getMetadataSetProperty(mdsPropertyId);
			SuggestDAO suggestDao = SuggestDAOFactory.getSuggestDAO(mdsbp);
			return suggestDao.getValue(key);
		}catch(Exception e){
			logger.error(e.getMessage(), e);
		}
		return null;
	}
	
	
	
	
	@Override
	protected void doUnexpectedFailure(Throwable e) {
		logger.error(e.getMessage(), e);
		super.doUnexpectedFailure(e);
	}
}
