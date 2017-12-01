package org.edu_sharing.service.suggest;

import org.apache.log4j.Logger;
import org.edu_sharing.repository.client.rpc.metadataset.MetadataSetBaseProperty;

public class SuggestDAOFactory {

	
	static Logger logger = Logger.getLogger(SuggestDAOFactory.class);
	
	public static SuggestDAO getSuggestDAO(MetadataSetBaseProperty mdsbp) throws Exception{
		
		String suggestBoxDAO = mdsbp.getParam(MetadataSetBaseProperty.PARAM_SUGGESTBOX_DAO);
		
		if(suggestBoxDAO == null || suggestBoxDAO.trim().equals("")){
			
			if(mdsbp.getValuespace() != null && mdsbp.getValuespace().size() > 0){
				suggestBoxDAO = "org.edu_sharing.service.suggest.SuggestDAOImpl";
			}else{
				suggestBoxDAO = "org.edu_sharing.service.suggest.SuggestDAOSearchImpl";
			}
				
			
			
			logger.warn("no suggestBoxDAO using default:"+suggestBoxDAO);
		}
		
		Class clazz = Class.forName(suggestBoxDAO);
		Object obj = clazz.getConstructor(new Class[] { }).newInstance(new Object[] {});
		
		SuggestDAO suggestDao = (SuggestDAO)obj;
		suggestDao.setMetadataProperty(mdsbp);
		
		return suggestDao;
	}
	
}
