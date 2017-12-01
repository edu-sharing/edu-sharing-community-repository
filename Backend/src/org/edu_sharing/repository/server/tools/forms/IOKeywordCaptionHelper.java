package org.edu_sharing.repository.server.tools.forms;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.fileupload.FileItem;
import org.apache.log4j.Logger;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.client.tools.forms.CCForms;
import org.edu_sharing.repository.server.MCAlfrescoBaseClient;
import org.edu_sharing.repository.server.RepoFactory;
import org.edu_sharing.service.suggest.SuggestDAOKeywordSQLImpl;

public class IOKeywordCaptionHelper extends HelperAbstract {

	Logger logger = Logger.getLogger(IOKeywordCaptionHelper.class);
	
	@Override
	public HashMap<String, Object> execute(HashMap<String, Object> params,
			HashMap<String, String> authenticatioInfo) {
		
		String nodeId = (String) params.get(CCConstants.NODEID);
		String repositoryId = (String) params.get(CCConstants.REPOSITORY_ID);
		List items = (List) params.get("ITEMS");
		List<FileItem> fileItems = getFileItems(CCForms.getFormEleNameByProp(CCConstants.CCM_TYPE_IO, CCConstants.LOM_PROP_GENERAL_KEYWORD), items);
		
	

		if (fileItems != null) {
			try {
				MCAlfrescoBaseClient mcAlfrescoBaseClient = (MCAlfrescoBaseClient) RepoFactory.getInstance(repositoryId, authenticatioInfo);
				
				ArrayList<String> kwCap = new ArrayList<String>();
				
				for(FileItem fi : fileItems){
					String keywordId = fi.getString();
					if(keywordId != null && !keywordId.trim().equals("")){
						String keywordCap = new SuggestDAOKeywordSQLImpl().getValueNoCat(keywordId);
						if(keywordCap != null && !keywordCap.trim().equals("") && !kwCap.contains(keywordCap)){
							kwCap.add(keywordCap);
						}
					}
				}
				
				if(kwCap.size() > 0){
					HashMap<String,Object> props = new HashMap<String,Object>();
					props.put( CCConstants.CCM_PROP_IO_GENERALKEYWORD_CAPTION, kwCap);
					mcAlfrescoBaseClient.updateNode(nodeId, props);
				}
				
			
			}catch(Throwable e){
				logger.error(e.getMessage(),e);
			}
		}
		
		
		return null;
	}
	
}
