package org.edu_sharing.alfresco.policy;

import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.Date;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import org.alfresco.model.ContentModel;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ActionService;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.apache.log4j.Logger;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.tools.ActionObserver;

import com.coremedia.iso.IsoFile;
import com.coremedia.iso.boxes.Box;
import com.coremedia.iso.boxes.MovieBox;
import com.coremedia.iso.boxes.TrackBox;
import com.coremedia.iso.boxes.TrackHeaderBox;

public class ThumbnailHandling {
	
	ServiceRegistry serviceRegistry = (ServiceRegistry) AlfAppContextGate.getApplicationContext()
			.getBean(ServiceRegistry.SERVICE_REGISTRY);
	
	ContentService contentService = serviceRegistry.getContentService();
	NodeService nodeService = serviceRegistry.getNodeService();
	ActionService actionService = serviceRegistry.getActionService();
	
	Logger logger = Logger.getLogger(ThumbnailHandling.class);
	
	
	
	public void thumbnailHandling(NodeRef nodeRef) {
		
		Action thumbnailAction = actionService.createAction(CCConstants.ACTION_NAME_CREATE_THUMBNAIL);
		thumbnailAction.setTrackStatus(true);
		thumbnailAction.setExecuteAsynchronously(true);
		thumbnailAction.setParameterValue("thumbnail-name", CCConstants.CM_VALUE_THUMBNAIL_NAME_imgpreview_png);
		thumbnailAction.setParameterValue(ActionObserver.ACTION_OBSERVER_ADD_DATE, new Date());
	
		ActionObserver.getInstance().addAction(nodeRef, thumbnailAction);
	}
}
