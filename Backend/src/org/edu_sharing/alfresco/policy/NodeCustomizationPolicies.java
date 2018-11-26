package org.edu_sharing.alfresco.policy;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.content.ContentServicePolicies.OnContentUpdatePolicy;
import org.alfresco.repo.node.NodeServicePolicies.OnCreateNodePolicy;
import org.alfresco.repo.node.NodeServicePolicies.OnUpdatePropertiesPolicy;
import org.alfresco.repo.policy.BehaviourFilter;
import org.alfresco.repo.policy.JavaBehaviour;
import org.alfresco.repo.policy.PolicyComponent;
import org.alfresco.repo.version.VersionModel;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ActionService;
import org.alfresco.service.cmr.lock.LockService;
import org.alfresco.service.cmr.lock.LockStatus;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.ContentIOException;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.ContentStreamListener;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.cmr.thumbnail.ThumbnailService;
import org.alfresco.service.cmr.version.VersionService;
import org.alfresco.service.cmr.version.VersionType;
import org.alfresco.service.namespace.QName;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.log4j.Logger;
import org.edu_sharing.alfresco.jobs.PreviewJob;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.client.tools.forms.VCardTool;
import org.edu_sharing.repository.server.tools.ActionObserver;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.repository.server.tools.PropertiesHelper;
import org.edu_sharing.repository.server.tools.cache.RepositoryCache;
import org.quartz.JobDetail;
import org.quartz.ObjectAlreadyExistsException;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleTrigger;
import org.springframework.security.crypto.codec.Base64;

import com.coremedia.iso.IsoFile;
import com.coremedia.iso.boxes.Box;
import com.coremedia.iso.boxes.MovieBox;
import com.coremedia.iso.boxes.TrackBox;
import com.coremedia.iso.boxes.TrackHeaderBox;

/**
 * 
 * @author rudi
 *	
 *important only for IO's
 *
 * onCreateNode:
 * - content -> IO
 * - folder -> Map
 * - LOM metadata:
 * * - title
 * * - technical location
 * * - contributer
 * 
 * onContentUpdate:
 * 
 * - if IO
 * - set LOM technical size
 * - create preview only if new content
 * - fetch resourcinfo action
 * - Create version history and version
 * * - only if create_version value is true (default = true)
 */
public class NodeCustomizationPolicies implements OnContentUpdatePolicy, OnCreateNodePolicy, OnUpdatePropertiesPolicy{
	
	private static final String[] IO_REFERENCE_COPY_PROPERTIES = new String[]{
			CCConstants.CCM_PROP_IO_COMMONLICENSE_KEY,
			CCConstants.CCM_PROP_IO_COMMONLICENSE_CC_LOCALE,
			CCConstants.CCM_PROP_IO_COMMONLICENSE_CC_VERSION,
			CCConstants.CCM_PROP_IO_COMMONLICENSE_QUESTIONSALLOWED,
			CCConstants.CCM_PROP_IO_LICENSE,
			CCConstants.CCM_PROP_IO_LICENSE_DESCRIPTION,
			CCConstants.CCM_PROP_IO_LICENSE_FROM,
			CCConstants.CCM_PROP_IO_LICENSE_PROFILE_URL,
			CCConstants.CCM_PROP_IO_LICENSE_SOURCE_URL,
			CCConstants.CCM_PROP_IO_LICENSE_TITLE_OF_WORK,
			CCConstants.CCM_PROP_IO_LICENSE_TO,
			CCConstants.CCM_PROP_IO_LICENSE_VALID,

	};

	static Logger logger = Logger.getLogger(NodeCustomizationPolicies.class);

	ActionService actionService;
	
	NodeService nodeService;
	
	VersionService versionService;
	
	PersonService personService;
	
	PolicyComponent policyComponent;
	
	ContentService contentService;
	
	PermissionService permissionService;
	
	LockService lockService;
	
	ThumbnailService thumbnailService;
	
	BehaviourFilter policyBehaviourFilter;

	private SearchService searchService;

	private ResultSet result;
	
	Scheduler scheduler;
	
	public void init() {
		
		policyComponent.bindClassBehaviour(OnCreateNodePolicy.QNAME, ContentModel.TYPE_CONTENT, new JavaBehaviour(this, "onCreateNode"));
		policyComponent.bindClassBehaviour(OnCreateNodePolicy.QNAME, ContentModel.TYPE_FOLDER, new JavaBehaviour(this, "onCreateNode"));
		policyComponent.bindClassBehaviour(OnCreateNodePolicy.QNAME, QName.createQName(CCConstants.CCM_TYPE_IO), new JavaBehaviour(this, "onCreateNode"));
		policyComponent.bindClassBehaviour(OnCreateNodePolicy.QNAME, QName.createQName(CCConstants.CCM_TYPE_MAP), new JavaBehaviour(this, "onCreateNode"));
		
		policyComponent.bindClassBehaviour(OnContentUpdatePolicy.QNAME, QName.createQName(CCConstants.CCM_TYPE_IO), new JavaBehaviour(this, "onContentUpdate"));
		
		//for async changed properties refresh node in cache
		policyComponent.bindClassBehaviour(OnUpdatePropertiesPolicy.QNAME, QName.createQName(CCConstants.CCM_TYPE_IO), new JavaBehaviour(this, "onUpdateProperties"));
		policyComponent.bindClassBehaviour(OnUpdatePropertiesPolicy.QNAME, QName.createQName(CCConstants.CCM_TYPE_MAP), new JavaBehaviour(this, "onUpdateProperties"));
	}
	
	@Override
	public void onContentUpdate(NodeRef nodeRef, boolean newContent) {
		
		logger.debug("nodeRef:" +  nodeRef.getId());
		
		if(QName.createQName(CCConstants.CCM_TYPE_IO).equals(nodeService.getType(nodeRef))){
			
			
			ContentReader reader = contentService.getReader(nodeRef, ContentModel.PROP_CONTENT);
			
			LockStatus lockStatus = lockService.getLockStatus(nodeRef);
			long contentSize =  0l;
			if ((reader!=null) && (reader.getContentData()!=null)) contentSize = reader.getContentData().getSize();
			String mimetype = null;
			if (reader!=null) mimetype = reader.getMimetype();
			logger.debug(" reader.getContentData().getSize():"+ contentSize +" newContent:"+newContent+" LockStatus:"+lockStatus+" mimetype:"+mimetype);
			
			if(reader != null){
				nodeService.setProperty(nodeRef, QName.createQName(CCConstants.LOM_PROP_TECHNICAL_SIZE), reader.getContentData().getSize());	
			}
			if(contentSize > 0 && mimetype != null && !nodeService.hasAspect(nodeRef,QName.createQName(CCConstants.CCM_ASPECT_COLLECTION_IO_REFERENCE))){
				nodeService.setProperty(nodeRef, QName.createQName(CCConstants.LOM_PROP_TECHNICAL_FORMAT), mimetype);
			}
			
			logger.debug("lockStatus:"+lockStatus);
			if(newContent 
					&& (LockStatus.NO_LOCK.equals(lockStatus) || LockStatus.LOCK_EXPIRED.equals(lockStatus))
					&& (reader!=null) && (reader.getContentData()!=null) && reader.getContentData().getSize() > 0){
			
				logger.debug("will do the thumbnail");
				
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
				ContentReader thumbnail = contentService.getReader(nodeRef, QName.createQName(CCConstants.CCM_PROP_IO_USERDEFINED_PREVIEW));
				if(thumbnail!=null && thumbnail.getContentData()!=null && thumbnail.getContentData().getSize()>0){
					// this node has already a custom thumbnail, we do not need to create one
					// this prevents especially the import from slowing down when using a Binary Handler
				}
				else {
					Action thumbnailAction = actionService.createAction(CCConstants.ACTION_NAME_CREATE_THUMBNAIL);
					thumbnailAction.setTrackStatus(true);
					thumbnailAction.setExecuteAsynchronously(true);
					thumbnailAction.setParameterValue("thumbnail-name", CCConstants.CM_VALUE_THUMBNAIL_NAME_imgpreview_png);
					thumbnailAction.setParameterValue(ActionObserver.ACTION_OBSERVER_ADD_DATE, new Date());

					ActionObserver.getInstance().addAction(nodeRef, thumbnailAction);

					SimpleTrigger st = new SimpleTrigger();
					st.setName("ImmediateTrigger");
					st.setRepeatCount(0);
					st.setStartTime(new Date(System.currentTimeMillis() + 500));
					JobDetail jd = new JobDetail();
					jd.setJobClass(PreviewJob.class);
					jd.setName(PreviewJob.class.getName() + " Immediate " + System.currentTimeMillis());

					try {
						scheduler.scheduleJob(jd, st);
					} catch (ObjectAlreadyExistsException e) {
						//only when debug, the job should only be executed as a singelton, so this exception is fine
						logger.debug(e.getMessage());
					} catch (SchedulerException e) {
						logger.error(e.getMessage(), e);
					}
				}
			}
			
			logger.debug("will do the resourceinfo. noderef:"+nodeRef);
			Action resourceInfoAction = actionService.createAction(CCConstants.ACTION_NAME_RESOURCEINFO);
			actionService.executeAction(resourceInfoAction, nodeRef, true, false);
			
			
			Action extractMetadataAction = actionService.createAction("extract-metadata");
			//dont do async cause it conflicts with preview creation when webdav is used
			actionService.executeAction(extractMetadataAction, nodeRef, true, false);
			
			Boolean createVersion = (Boolean)nodeService.getProperty(nodeRef, QName.createQName(CCConstants.CCM_PROP_IO_CREATE_VERSION));
			//only create version when content is there, cause else in webdav for one file it goes twice here
			//when metadata changes the servlet does version creation
			if (createVersion && (reader!=null) && (reader.getContentData()!=null) && reader.getContentData().getSize() > 0) {
				if(versionService.getVersionHistory(nodeRef) == null) {
                	Map<String, Serializable> transFormedProps = transformQNameKeyToString(nodeService.getProperties(nodeRef));
        			
                	//see https://issues.alfresco.com/jira/browse/ALF-12815
        			//alfresco-4.0.d fix version should start with 1.0 not with 0.1
                	transFormedProps.put(VersionModel.PROP_VERSION_TYPE, VersionType.MAJOR);
        			versionService.createVersion(nodeRef,transFormedProps);
                }else{
                	versionService.createVersion(nodeRef,transformQNameKeyToString(nodeService.getProperties(nodeRef)));
            	}
			}
			new RepositoryCache().remove(nodeRef.getId());
		}
	
	}
	
	@Override
	public void onCreateNode(ChildAssociationRef childAssocRef) {
		
		NodeRef eduNodeRef = childAssocRef.getChildRef();
		QName type = nodeService.getType(eduNodeRef);
		
		/**
		 * it seams it is not possible to get an order to policy execution
		 * so sometimes NodeCustomization.onCreateNode leads to ScopePolicies.beforeUpdateNode
		 * which leads to ScopeNodeWrongScopeException: trying to modify unscoped node from within a scope"
		 * 
		 * disable policies for this node to prevent that beforeUpdateNode 
		 * checks the scope which will be there after update
		 */
		policyBehaviourFilter.disableBehaviour(eduNodeRef);
		
		logger.debug("nodeRef:" +  eduNodeRef.getId());
		
		try{
	
			/**
			 * set content types to io type
			 */
			if(ContentModel.TYPE_CONTENT.equals(type)){
				logger.debug("its a content node will transform to IO");
				// type
				QName ioType = QName.createQName(CCConstants.CCM_TYPE_IO);
	        	nodeService.setType(eduNodeRef,ioType);
	        	type = ioType;
			}
			
			/**
			 * generate metadata
			 */
			if(QName.createQName(CCConstants.CCM_TYPE_IO).equals(type)){
				logger.debug("will generate lom metadata");
	        	// props
	        	Map<QName, Serializable> props = nodeService.getProperties(eduNodeRef);
	        	String name = (String) props.get(ContentModel.PROP_NAME);
	        	
	        	//sometimes when this method is called the prop is already set so check if null i.e. lom importer
	        	if(nodeService.getProperty(eduNodeRef, QName.createQName(CCConstants.LOM_PROP_GENERAL_TITLE)) == null){
					// removed on 2017-04-20
	        		//nodeService.setProperty(eduNodeRef, QName.createQName(CCConstants.LOM_PROP_GENERAL_TITLE), name);
	        	}
	        	
	        	//for collections and solr set originalid (will be overwritten by collectionservice if a reference io is created)
				// the id will be written on copy, so may it already exists -> then keep it
				if(nodeService.getProperty(eduNodeRef,QName.createQName(CCConstants.CCM_PROP_IO_ORIGINAL))==null)
	        		nodeService.setProperty(eduNodeRef, QName.createQName(CCConstants.CCM_PROP_IO_ORIGINAL),eduNodeRef.getId());
	        	
				NodeRef personRef = personService.getPerson((String) props.get(ContentModel.PROP_CREATOR));
				Map<QName, Serializable> userInfo = nodeService.getProperties(personRef);
				
				String givenName = (String)userInfo.get(ContentModel.PROP_FIRSTNAME);
				String surename = (String)userInfo.get(ContentModel.PROP_LASTNAME);
				String email = (String)userInfo.get(CCConstants.PROP_USER_EMAIL);
				
				if (surename == null || surename.isEmpty()) {
					surename = (String)userInfo.get(CCConstants.PROP_USERNAME);
				}
				
				String replicationSourceId = (String)nodeService.getProperty(eduNodeRef, QName.createQName(CCConstants.CCM_PROP_IO_REPLICATIONSOURCEID));
				
				/**
				 * only do this with local created objects. contributer info is delivered by importer
				 */
				if(replicationSourceId == null || replicationSourceId.trim().equals("")){
					HashMap<String,String> vcardMap = new HashMap<String,String>();
					vcardMap.put(CCConstants.VCARD_GIVENNAME, givenName);
					vcardMap.put(CCConstants.VCARD_SURNAME, surename);
					vcardMap.put(CCConstants.VCARD_EMAIL, email);
					String vcardString = VCardTool.hashMap2VCard(vcardMap);
					
					//sometimes when this method is called the prop is already set so check if null i.e. lom importer
					if(nodeService.getProperty(eduNodeRef,  QName.createQName(CCConstants.CCM_PROP_IO_REPL_METADATACONTRIBUTER_CREATOR)) == null){
						nodeService.setProperty(eduNodeRef, QName.createQName(CCConstants.CCM_PROP_IO_REPL_METADATACONTRIBUTER_CREATOR), vcardString);
					}
					if(nodeService.getProperty(eduNodeRef,  QName.createQName(CCConstants.CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_AUTHOR)) == null){
						// Changed for 4.0: DESREPO-897 do not autofill author
						// nodeService.setProperty(eduNodeRef, QName.createQName(CCConstants.CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_AUTHOR), vcardString);
					}
					
					String techLocValue = "ccrep://" + ApplicationInfoList.getHomeRepository().getAppId() + "/" + eduNodeRef.getId();
					if(nodeService.getProperty(eduNodeRef,  QName.createQName(CCConstants.LOM_PROP_TECHNICAL_LOCATION)) == null){
						nodeService.setProperty(eduNodeRef, QName.createQName(CCConstants.LOM_PROP_TECHNICAL_LOCATION), techLocValue);
					}
				}
				
				// inherit the mds from the parent folder
				Serializable mdsForceSer = nodeService.getProperty(childAssocRef.getParentRef(), QName.createQName(CCConstants.CM_PROP_METADATASET_EDU_FORCEMETADATASET));
				boolean mdsForce=(mdsForceSer == null) ? false : (boolean)mdsForceSer;
				if(mdsForce){
					String mdsName=(String)nodeService.getProperty(childAssocRef.getParentRef(), QName.createQName(CCConstants.CM_PROP_METADATASET_EDU_METADATASET));
					nodeService.setProperty(eduNodeRef, QName.createQName(CCConstants.CM_PROP_METADATASET_EDU_METADATASET),mdsName);
				}
	            
			}
			
			if(ContentModel.TYPE_FOLDER.equals(type)){
				// type
				logger.debug("its a folder node will transform to map");
	        	nodeService.setType(eduNodeRef, QName.createQName(CCConstants.CCM_TYPE_MAP));
			}
		}finally{
			policyBehaviourFilter.enableBehaviour(eduNodeRef);
		}
	}
	
	@Override
	public void onUpdateProperties(NodeRef nodeRef, Map<QName, Serializable> before, Map<QName, Serializable> after) {
		
		//System.out.println("********** onUpdateProperties node("+nodeRef.getId()+")");
		
		// make the title like the name(when webdav rename is done), @TODO mybe just show the name in the gui
		String nameBefore = (String)before.get(ContentModel.PROP_NAME);
		String nameAfter =  (String)after.get(ContentModel.PROP_NAME);
		
		QName type = nodeService.getType(nodeRef);
		
		logger.debug("nodeRef:" + nodeRef +" nodeRef:" +  nodeRef.getId());
		
		
		if(type.equals(QName.createQName(CCConstants.CCM_TYPE_IO))){
			if(nameAfter != null && !nameAfter.equals(nameBefore)){
				// removed on 2017-04-20
				//nodeService.setProperty(nodeRef, QName.createQName(CCConstants.LOM_PROP_GENERAL_TITLE), nameAfter);
			}
			// refresh all collection io's metadata
			String query="ASPECT:\""+CCConstants.CCM_ASPECT_COLLECTION_IO_REFERENCE+"\" AND @ccm\\:original:\""+nodeRef.getId()+"\"";
			result=searchService.query(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE,SearchService.LANGUAGE_LUCENE,query);
			Map<QName, Serializable> properties = nodeService.getProperties(nodeRef);
			for(NodeRef ref : result.getNodeRefs()){
				Map<QName, Serializable> originalProperties = nodeService.getProperties(ref);
				for(QName prop : properties.keySet()){
					if(Arrays.asList(IO_REFERENCE_COPY_PROPERTIES).contains(prop.toString())){
						originalProperties.put(prop, properties.get(prop));
					}
				}
				nodeService.setProperties(ref, originalProperties);
				new RepositoryCache().remove(ref.getId());
			}
		}
		
		// refresh Titel for Maps changed in webdav
		if(type.equals(QName.createQName(CCConstants.CCM_TYPE_MAP))){
			if(nameAfter != null && !nameAfter.equals(nameBefore)){
				nodeService.setProperty(nodeRef, ContentModel.PROP_TITLE, nameAfter);
			}
		}
		
		// for async prozessed properties like exif: remove from cache
		new RepositoryCache().remove(nodeRef.getId());
		
		// URL link update
		String beforeURL = null;
		String afterURL = null;
		for (QName qName : before.keySet()) if ("wwwurl".equals(qName.getLocalName())) beforeURL = ""+before.get(qName);
		for (QName qName : after.keySet()) if ("wwwurl".equals(qName.getLocalName())) afterURL = ""+after.get(qName);
		if ((afterURL!=null) && (!afterURL.equals(beforeURL))) {
			
			logger.info("---> UPDATE/CREATE THUMBNAIL FOR LINK("+afterURL+") ON NODE("+nodeRef.getId()+")");
			
			String linktype = (String)after.get(QName.createQName(CCConstants.CCM_PROP_LINKTYPE));
			String previewImageBase64 = (linktype != null && linktype.equals(CCConstants.CCM_VALUE_LINK_LINKTYPE_USER_GENERATED)) ? getPreviewFromURL(afterURL) : null;
			writeBase64Image(nodeRef,previewImageBase64);
			
		}
		
	}
	
	
	
	private void writeBase64Image(NodeRef nodeRef, String previewImageBase64) {
		if (previewImageBase64!=null) {

			logger.info("---> GOT PREVIEW IMAGE BASE64: "+previewImageBase64.substring(21, 256)+" ...");
			final ContentWriter contentWriter = contentService.getWriter(nodeRef, QName.createQName("{http://www.campuscontent.de/model/1.0}userdefined_preview"), true);
			contentWriter.addListener(new ContentStreamListener() {
				@Override
				public void contentStreamClosed() throws ContentIOException {
					logger.info("Content Stream of preview Image was closed");
					logger.info(" ContentData size:" + contentWriter.getContentData().getSize());
					logger.info(" ContentData URL:" + contentWriter.getContentData().getContentUrl());
					logger.info(" ContentData MimeTyp:" + contentWriter.getContentData().getMimetype());
					logger.info(" ContentData ToString:" + contentWriter.getContentData().toString());
				}
			});
			contentWriter.setMimetype("image/png");
			byte[] imageData = Base64.decode(previewImageBase64.getBytes());
			if (imageData.length==0) logger.warn("LENGTH OF IMAGE BYTE DATA IS 0 !! ");
			try {
				ByteArrayInputStream is = new ByteArrayInputStream(imageData);
				contentWriter.putContent(is);
			} catch (Exception e) {
				logger.error("EXCEPTION:");
				e.printStackTrace();
			}
			logger.info("---> OK IMAGE WRITTEN");
			
		} else {
			logger.warn("---> NO PREVIEW IMAGE");
		}
	}
	
	public  void generateWebsitePreview(NodeRef nodeRef, String url) {
		if(nodeRef == null || url == null) {
			return;
		}
		String previewImageBase64 = getPreviewFromURL(url);
		if(previewImageBase64 != null) {
			writeBase64Image(nodeRef, previewImageBase64);
		}
	}
	
	  /**
	 * edu-sharing for setting version props
	 * @param props
	 * @return
	 */
	Map<String,Serializable> transformQNameKeyToString(Map<QName, Serializable> props){
		Map<String,Serializable> result = new HashMap<String,Serializable>();
		for(Map.Entry<QName,Serializable> entry : props.entrySet()){
			result.put(entry.getKey().toString(), entry.getValue());
		}
		return result;
	}
	
	public void setActionService(ActionService actionService) {
		this.actionService = actionService;
	}
	
	public void setNodeService(NodeService nodeService) {
		this.nodeService = nodeService;
	}
	public void setSearchService(SearchService searchService) {
		this.searchService = searchService;
	}
	public void setVersionService(VersionService versionService) {
		this.versionService = versionService;
	}
	
	public void setPolicyComponent(PolicyComponent policyComponent) {
		this.policyComponent = policyComponent;
	}
	
	public void setPersonService(PersonService personService) {
		this.personService = personService;
	}
	
	public void setContentService(ContentService contentService) {
		this.contentService = contentService;
	}
	
	public void setPermissionService(PermissionService permissionService) {
		this.permissionService = permissionService;
	}
	
	public void setLockService(LockService lockService) {
		this.lockService = lockService;
	}
	
	public void setThumbnailService(ThumbnailService thumbnailService) {
		this.thumbnailService = thumbnailService;
	}
	
	/*
	 * handle NULL when not possible or deactivated
	 */
	public static String getPreviewFromURL(String httpURL) {
		
		String websitePreviewRenderService = "";
		try {
			websitePreviewRenderService = ApplicationInfoList.getHomeRepository().getWebsitepreviewrenderservice();
		} catch (Exception e) {
			logger.error(CCConstants.REPOSITORY_FILE_HOME+": Was not able to find or unvalid value '"+ApplicationInfo.WEBSITEPREVIEWRENDERSERVICE+"' - set at least to empty string or 'false' to deactivate",e);
		}
		if ((websitePreviewRenderService==null) || (websitePreviewRenderService.trim().length()==0) || (!websitePreviewRenderService.trim().startsWith("http"))) {
			logger.info("No preview Image of Link - websitepreviewrenderservice on "+CCConstants.REPOSITORY_FILE_HOME+" is deactivated");
			return null;
		} else {
			logger.info("OK got websitepreviewrenderservice ...");
		}
		
		// setting the scale factor from the 1024 default width
		String scale = "0.25";
		
		// basic result setup
		String result = null;
		if (httpURL==null) return null;

		// try to get a preview image from local nodeJS server running
		// the following service: https://github.com/rootzoll/web-screenshot
		// --> IF NOT AVAILABLE WILL JUST WARN
		try {
			final String localServiceUrl = websitePreviewRenderService+"/?url="+java.net.URLEncoder.encode(httpURL, "ISO-8859-1")+"&scale="+scale+"&base64=1"; 
			//System.out.println("Calling external Service: "+localServiceUrl);
		    HttpClient client = new HttpClient();
		    GetMethod method = new GetMethod(localServiceUrl);
		    int statusCode = client.executeMethod(method);
		    if (statusCode == HttpStatus.SC_OK) {
			      result = new String(method.getResponseBody());
			      logger.info("OK on on Preview Image Service");
		    } else {
				logger.error("HTTP Error "+statusCode+" on Preview Image Service: "+localServiceUrl);
		    }
		} 
		catch (java.net.ConnectException ce) {
			logger.warn("!WARN! No Preview Image Service running at '"+websitePreviewRenderService+"' ...");
		}
		catch (Exception e) {
			logger.error("EXCEPTION on Preview Image Service: "+e.getMessage(), e);
		}
	
		return result;
	}
	
	public void setPolicyBehaviourFilter(BehaviourFilter policyBehaviourFilter) {
		this.policyBehaviourFilter = policyBehaviourFilter;
	}

	public void setScheduler(Scheduler scheduler) {
		this.scheduler = scheduler;
	}
}
