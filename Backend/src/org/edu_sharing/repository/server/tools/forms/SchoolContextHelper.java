package org.edu_sharing.repository.server.tools.forms;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.fileupload.FileItem;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.client.tools.forms.CCForms;
import org.edu_sharing.repository.server.MCAlfrescoBaseClient;
import org.edu_sharing.repository.server.RepoFactory;

/**
 * @TODO validate if it are noderefs when not empty
 * 
 * fill Full Text Search prop
 * 
 * @author rudi
 *
 */
public class SchoolContextHelper extends HelperAbstract {

	
	@Override
	public HashMap<String, Object> execute(HashMap<String, Object> params, HashMap<String, String> authenticatioInfo) {


		String nodeId = (String) params.get(CCConstants.NODEID);
		List items = (List) params.get("ITEMS");
		List<FileItem> fileItems = getFileItems(CCForms.getFormEleNameByProp(CCConstants.CCM_TYPE_IO, CCConstants.CCM_PROP_IO_SCHOOLCONTEXT), items);

		String repositoryId = (String) params.get(CCConstants.REPOSITORY_ID);
		
		ArrayList<String> topics = new ArrayList<String>();
		for(FileItem fileItem:fileItems){
			if(fileItem != null && fileItem.getString() != null){
				String path = fileItem.getString();
				if(!path.trim().equals("")){
					
					//workspace://SpacesStore/5dc92ec9-52f0-4863-8cf1-f4eac67966af#workspace://SpacesStore/51c8ca88-f978-419a-a6b8-135c0c3694fb#workspace://SpacesStore/cdbfbcc6-b24b-452d-99e2-9a916678871c#workspace://SpacesStore/f7594881-a171-44fd-8861-7f85cef5698f#asdfsadfsf
					String[] splitted = path.split("#");
					if(splitted.length > 4){
						String topic = splitted[4];
						if(topic != null && !topic.trim().equals("")){
							topics.add(topic);

						}
					}
					
				}
			}
		}
		
		if(topics.size() > 0){
			try{
				MCAlfrescoBaseClient repoClient = (MCAlfrescoBaseClient) RepoFactory.getInstance(repositoryId, authenticatioInfo);
				HashMap<String,Object> _props = new HashMap<String,Object>();
				_props.put(CCConstants.CCM_PROP_IO_SCHOOLTOPIC, topics);
				repoClient.updateNode(nodeId, _props);
			}catch(Throwable e){
				
			}
		}
		
		
		return null;
	}
}
