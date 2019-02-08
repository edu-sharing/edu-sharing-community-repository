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
		ContentReader reader = contentService.getReader(nodeRef, ContentModel.PROP_CONTENT);
		if(reader.getMimetype().contains("video")){
			
			ReadableByteChannel rbc = null;
			try{
				
				rbc = Channels.newChannel(reader.getContentInputStream());
				IsoFile isoFile = new IsoFile(rbc);
				MovieBox moov = isoFile.getMovieBox();
				if(moov != null && moov.getBoxes() != null){
					for(Box b : moov.getBoxes()) {
					   
					    
					    if(b instanceof TrackBox){
					    	TrackHeaderBox thb = ((TrackBox)b).getTrackHeaderBox();
					    	
					    	if(thb.getWidth() > 0 && thb.getHeight() > 0){
					    		nodeService.setProperty(nodeRef, QName.createQName(CCConstants.CCM_PROP_IO_WIDTH), thb.getWidth());
					    		nodeService.setProperty(nodeRef, QName.createQName(CCConstants.CCM_PROP_IO_HEIGHT), thb.getHeight());
					    	}
					 
					    }
					    
					}
				}
				
			}catch(Exception e){
				logger.error(e.getMessage(), e);
			}finally{
				
				if(rbc != null){
					try{
					
						rbc.close();
					}catch(IOException e){
						logger.error(e.getMessage(), e);
					}
				}
			}
		}
		// alfresco does not read image size for all images, so we try to fix it
		// trying to load not the whole image but just the bounding rect, see also:
		// http://stackoverflow.com/questions/1559253/java-imageio-getting-image-dimensions-without-reading-the-entire-file
		if(reader.getMimetype().contains("image")){
			try{
				try(ImageInputStream in = ImageIO.createImageInputStream(reader.getContentInputStream())){
				    final Iterator<ImageReader> readers = ImageIO.getImageReaders(in);
				    if (readers.hasNext()) {
				        ImageReader imageReader = readers.next();
				        try {
				        	imageReader.setInput(in);
				        	nodeService.setProperty(nodeRef, QName.createQName(CCConstants.EXIF_PROP_PIXELXDIMENSION), imageReader.getWidth(0));
							nodeService.setProperty(nodeRef, QName.createQName(CCConstants.EXIF_PROP_PIXELYDIMENSION), imageReader.getHeight(0));
				        } finally {
				        	imageReader.dispose();
				        }
				    }
				} 
			}catch(Throwable t){}
		}
		
		Action thumbnailAction = actionService.createAction(CCConstants.ACTION_NAME_CREATE_THUMBNAIL);
		thumbnailAction.setTrackStatus(true);
		thumbnailAction.setExecuteAsynchronously(true);
		thumbnailAction.setParameterValue("thumbnail-name", CCConstants.CM_VALUE_THUMBNAIL_NAME_imgpreview_png);
		thumbnailAction.setParameterValue(ActionObserver.ACTION_OBSERVER_ADD_DATE, new Date());
	
		ActionObserver.getInstance().addAction(nodeRef, thumbnailAction);
	}
}
