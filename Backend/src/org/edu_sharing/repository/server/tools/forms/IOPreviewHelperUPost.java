/**
 *
 *  
 * 
 * 
 *	
 *
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 *
 */
package org.edu_sharing.repository.server.tools.forms;


import java.io.File;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItem;
import org.apache.log4j.Logger;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.MCAlfrescoAPIClient;
import org.edu_sharing.repository.server.MCAlfrescoBaseClient;
import org.edu_sharing.repository.server.RepoFactory;
import org.springframework.context.ApplicationContext;

/**
 * Userdefined Preview
 * check if userdefined preview is present. if usedefined preview safe this before
 * content cause onContentUpdate Policy with async preview generation for content prop 
 *  does run in a ConcurrencyFailureException when userdefined preview is safed.
 *  
 * 
 *  
 *  userdefined preview processing is not async							 *  
 *  
 */
public class IOPreviewHelperUPost extends HelperAbstract {

	Logger logger = Logger.getLogger(IOPreviewHelperUPost.class);

	MCAlfrescoBaseClient mcBaseClient = null;
	
	ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();

	/**
	 * 1 Helper für C + U wenn IO content != null lösche alle previews und
	 * erzeuge neue Preview
	 * 
	 * @TODO wenn sich content nicht geändert hat nicht ausführen
	 */
	public HashMap<String, Object> execute(HashMap<String, Object> params, final HashMap<String, String> authenticatioInfo) {
		logger.info("starting");
		if (params != null) {
			final String nodeId = (String) params.get(CCConstants.NODEID);
			String type = (String) params.get(CCConstants.NODETYPE);
			final String repositoryId = (String) params.get(CCConstants.REPOSITORY_ID);
			List items = (List) params.get("ITEMS");
			if (type == null || !type.equals(CCConstants.CCM_TYPE_IO) || nodeId == null || repositoryId == null) {
				return null;
			} else {
				
				try{
					mcBaseClient = (MCAlfrescoBaseClient)RepoFactory.getInstance(repositoryId, authenticatioInfo);
				}catch(Throwable e){
					logger.error(e.getMessage(), e);
					return null;
				}
				
				
				
				try {

					FileItem itemPreviewContent = getFileItem(CCConstants.CCM_PROP_IO_USERDEFINED_PREVIEW, items);
				
					if (itemPreviewContent != null && itemPreviewContent.getSize() > 0){
						
						if(itemPreviewContent instanceof DiskFileItem){
							DiskFileItem dfi = (DiskFileItem)itemPreviewContent;
							String fileName = dfi.getName(); 
							
							
							File f = dfi.getStoreLocation();
						
							//to small files will not be cached on harddisk so do f.exsists check
							if(mcBaseClient instanceof MCAlfrescoAPIClient && f.exists()){
								((MCAlfrescoAPIClient)mcBaseClient).setUserDefinedPreview(nodeId, f, fileName);
							}else{
								mcBaseClient.setUserDefinedPreview(nodeId, dfi.get(), fileName);
							}
						}
								
					}

				}catch (Throwable e) {
					logger.error("Helper failed for Preview", e);
				}
				
				
			}
		}

		logger.info("returning");
		return null;
	}

}
