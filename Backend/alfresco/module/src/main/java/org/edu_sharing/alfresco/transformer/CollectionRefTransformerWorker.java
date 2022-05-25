package org.edu_sharing.alfresco.transformer;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.content.transform.ContentTransformerHelper;
import org.alfresco.repo.content.transform.ContentTransformerWorker;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.CopyService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.repository.TransformationOptions;
import org.alfresco.service.namespace.QName;
import org.apache.log4j.Logger;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;

public class CollectionRefTransformerWorker extends ContentTransformerHelper implements ContentTransformerWorker  {
	
	Logger logger = Logger.getLogger(CollectionRefTransformerWorker.class);
	
	NodeService nodeService = null;
	ContentService contentService = null;
	
	@Override
	public String getComments(boolean available) {
		return "supports transforming io ref objects previews";
	}
	
	@Override
	public String getVersionString() {
		return "1.0";
	}
	
	@Override
	public boolean isAvailable() {
		return true;
	}
	
	@Override
	public boolean isTransformable(String sourceMimetype, String targetMimetype, TransformationOptions options) {
		logger.debug("is transformable sourceMimetype:" + sourceMimetype + " targetMimetype:" + targetMimetype);
		
		AuthenticationUtil.RunAsWork<Boolean> isTransformableWorker = new AuthenticationUtil.RunAsWork<Boolean>() {
			@Override
			public Boolean doWork() throws Exception {
				
				NodeRef nodeRef = options.getSourceNodeRef();
				if(nodeRef == null || nodeRef.toString().equals("")){
					logger.debug("noderef is null or id is empty");
					return false;
				}
				Set<QName> aspects = nodeService.getAspects(nodeRef);
				
				if(aspects.contains(QName.createQName(CCConstants.CCM_ASPECT_COLLECTION_IO_REFERENCE))){
					
					if(targetMimetype.contains("image")){
						return true;
					}else{
						return false;
					}
					
					
				}
				return false;
			}
		};

		return AuthenticationUtil.runAs(isTransformableWorker, ApplicationInfoList.getHomeRepository().getUsername());
		
	}
	
	@Override
	public void transform(ContentReader reader, ContentWriter writer, TransformationOptions options) throws Exception {
		NodeRef nodeRef = options.getSourceNodeRef();
		logger.debug("will transform:" + nodeRef);
		
		String nodeId = (String)nodeService.getProperty(nodeRef, QName.createQName(CCConstants.CCM_PROP_IO_ORIGINAL));
		NodeRef nodeRefOriginal = new NodeRef(new StoreRef(StoreRef.PROTOCOL_WORKSPACE, "SpacesStore"),nodeId);
		
		Set<QName> set = new HashSet<QName>();
		set.add(QName.createQName(CCConstants.CM_TYPE_THUMBNAIL));
		
		List<ChildAssociationRef> refThumbnails =  nodeService.getChildAssocs(nodeRef, set);
		if(refThumbnails.size() == 0){
			List<ChildAssociationRef> orgThumbnails =  nodeService.getChildAssocs(nodeRefOriginal, set);
			if(orgThumbnails.size() > 0){
				ContentReader creader = contentService.getReader(orgThumbnails.get(0).getChildRef(), ContentModel.PROP_CONTENT);
				writer.putContent(creader);
			}
		}else{
			logger.debug("a thumbnail already exsists");
			
		}
		
	}
	
	public void setNodeService(NodeService nodeService) {
		this.nodeService = nodeService;
	}
	
	public void setContentService(ContentService contentService) {
		this.contentService = contentService;
	}
}
