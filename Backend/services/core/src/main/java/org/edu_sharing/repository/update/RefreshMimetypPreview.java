package org.edu_sharing.repository.update;

import java.io.PrintWriter;

import org.alfresco.model.ContentModel;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ActionService;
import org.alfresco.service.cmr.lock.LockService;
import org.alfresco.service.cmr.lock.LockType;
import org.alfresco.service.cmr.repository.ContentIOException;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.ContentStreamListener;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.MimetypeService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchParameters;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.QName;
import org.apache.log4j.Logger;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.tools.ActionObserver;
import org.edu_sharing.repository.server.tools.PropertiesHelper;
import org.springframework.context.ApplicationContext;

public class RefreshMimetypPreview extends UpdateAbstract {
	
	public static final String ID = "RefreshMimetypPreview";

	public static final String description = "define a filter for files that will be refreshed for mimetype and preview";

	ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
	ServiceRegistry serviceRegistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);

	StoreRef spacesStoreStoreRef = new StoreRef(StoreRef.PROTOCOL_WORKSPACE, "SpacesStore");
	
	public RefreshMimetypPreview(PrintWriter out) {
		this.out = out;
		this.logger = Logger.getLogger(RefreshMimetypPreview.class);
	}
	
	@Override
	public void execute() {
		run(false);
	}

	@Override
	public void test() {
		run(true);
	}

	private void run(boolean test){
		
		NodeService nodeService = serviceRegistry.getNodeService();
		SearchService searchService = serviceRegistry.getSearchService();
		ContentService contentService = serviceRegistry.getContentService();
		MimetypeService mimetypeService = serviceRegistry.getMimetypeService();
		LockService lockService = serviceRegistry.getLockService();
		
		try{
			
			String propFile = "org/edu_sharing/repository/update/refresh_mimetypepreview.properties";
			java.util.Properties props = PropertiesHelper.getProperties(propFile, PropertiesHelper.TEXT);
			
			String filter =  (String)props.get("filter");
			this.logInfo("using filter:"+filter);
			
			if(filter == null || filter.trim().equals("")){
				logInfo("no filter defined. will stop processing!");
				return;
			}
			
			SearchParameters sp = new SearchParameters();
			sp.addStore(spacesStoreStoreRef);
			sp.setQuery(filter);
			sp.setLanguage(SearchService.LANGUAGE_LUCENE);
			ResultSet resultSet = searchService.query(sp);
			logInfo("found:" + resultSet.length());
			for(final NodeRef nodeRef : resultSet.getNodeRefs()){
				QName typeQName = nodeService.getType(nodeRef);
				String type = typeQName.getLocalName();
				String name = (String)nodeService.getProperty(nodeRef,ContentModel.PROP_NAME);
				
				logInfo("name:"+name + " type:"+type);
				
				if(typeQName.equals(QName.createQName(CCConstants.CCM_TYPE_IO))){
					
					ContentReader contentReader =  contentService.getReader(nodeRef, ContentModel.PROP_CONTENT);
					
					String oldMimeType = contentReader.getMimetype(); 
					
					//set to null so that guessMimetype() will not use old one
					contentReader.setEncoding(null);
					contentReader.setMimetype(null);
					
					String newMimetype = mimetypeService.guessMimetype(name,contentReader);
					logInfo("oldMimeType:"+ oldMimeType + " newMimeType:"+newMimetype);
					if(!test){
						
						try{
							
							lockService.lock(nodeRef, LockType.WRITE_LOCK);
							
							ContentWriter writer = contentService.getWriter(nodeRef, ContentModel.PROP_CONTENT, true);
							
							//call preview action cause call in policy only works for new content
							writer.addListener(new ContentStreamListener() {
								@Override
								public void contentStreamClosed() throws ContentIOException {
									logInfo("finished setting new mimetype");
									ActionService actionService = serviceRegistry.getActionService();
						            Action thumbnailAction = actionService.createAction(CCConstants.ACTION_NAME_CREATE_THUMBNAIL);
									thumbnailAction.setTrackStatus(true);
									
									thumbnailAction.setParameterValue("thumbnail-name", CCConstants.CM_VALUE_THUMBNAIL_NAME_imgpreview_png);
									ActionObserver.getInstance().addAction(nodeRef, thumbnailAction);
									
									//cause its already async set executeAsynchronously to false
									actionService.executeAction(thumbnailAction, nodeRef, true, false);
									logInfo("finished setting new preview");
								}
							});
							writer.setEncoding("UTF-8");
							writer.setMimetype(newMimetype);
							writer.putContent(contentReader.getContentInputStream());
							nodeService.setProperty(nodeRef,QName.createQName(CCConstants.LOM_PROP_TECHNICAL_FORMAT), newMimetype);
							
						} finally {
							lockService.unlock(nodeRef);
						}
					}
				}else{
					logInfo("type "+ typeQName+" not allowed!");
				}
			}
			
		} catch(Exception e) {
			this.logError(e.getMessage(), e);
		}
	}
	
	@Override
	public String getDescription() {
		return description;
	}
	
	@Override
	public String getId() {
		return ID;
	}
	
	@Override
	public void run() {
		this.logInfo("not implemented");
	}

}
