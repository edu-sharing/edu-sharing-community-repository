package org.edu_sharing.repository.server;

import java.awt.Graphics;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.awt.image.FilteredImageSource;
import java.awt.image.ImageFilter;
import java.awt.image.ImageProducer;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.plugins.jpeg.JPEGImageWriteParam;
import javax.imageio.stream.MemoryCacheImageInputStream;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.SingleThreadModel;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.swing.GrayFilter;

import org.alfresco.repo.content.ContentStore;
import org.alfresco.repo.rule.ReorderRules;
import org.alfresco.repo.security.permissions.AccessDeniedException;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.InvalidNodeRefException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.version.VersionService;
import org.alfresco.service.namespace.QName;import org.apache.james.mime4j.io.MaxHeaderLengthLimitException;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.rpc.GetPreviewResult;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.client.tools.MimeTypes;
import org.edu_sharing.repository.server.tools.cache.PreviewCache;
import org.edu_sharing.repository.server.tools.security.SignatureVerifier;
import org.edu_sharing.service.mime.MimeTypesV2;
import org.edu_sharing.service.nodeservice.NodeServiceFactory;
import org.edu_sharing.service.permission.PermissionServiceFactory;
import org.springframework.context.ApplicationContext;

import com.google.gwt.widgetideas.graphics.client.Color;

public class PreviewServlet extends HttpServlet implements SingleThreadModel {

	public static final String RESULT_TYPE_MIME_ICON = "mime_type";

	private static final int MAX_IMAGE_SIZE = 2000;

	ServiceRegistry serviceRegistry;

	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		ApplicationContext appContext = AlfAppContextGate.getApplicationContext();
		serviceRegistry = (ServiceRegistry) appContext.getBean(ServiceRegistry.SERVICE_REGISTRY);
	}

	private boolean isCacheable(int width,int height, int maxWidth, int maxHeight){
		if(maxWidth>0 || maxHeight>0)
			return false;
		for(int i=0;i<PreviewCache.CACHE_SIZES_WIDTH.length;i++){
			if(PreviewCache.CACHE_SIZES_WIDTH[i]==width && PreviewCache.CACHE_SIZES_HEIGHT[i]==height)
				return true;
		}
		return false;
	}
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		String nodeId = req.getParameter("nodeId");
		
		// Auth by usage, allow a specific node to render if the user has a current usage signature for it
		SignatureVerifier.runAsAuthByUsage(nodeId,req.getSession(),new RunAsWork<Void>() {
					@Override
					public Void doWork() throws Exception {
						fetchNodeData(req,resp);
						return null;
					}	
		});
	}
	
	private void fetchNodeData(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		ServletOutputStream op = resp.getOutputStream();
		String nodeId=null;

		MCAlfrescoAPIClient repoClient = new MCAlfrescoAPIClient();
		
		boolean isCollection=false;
		try {
			
			nodeId = req.getParameter("nodeId");
			String type = req.getParameter("type");
			String version = req.getParameter("version");
			
			StoreRef storeRef = MCAlfrescoAPIClient.storeRef;
			String storeProtocol =  req.getParameter("storeProtocol");
			String storeId = req.getParameter("storeId");
			if(storeProtocol != null && storeId != null){
				storeRef = new StoreRef(storeProtocol,storeId);
			}
			
			NodeRef nodeRef = new NodeRef(storeRef, nodeId);
			QName nodeType = serviceRegistry.getNodeService().getType(nodeRef);
			
			// check nodetype for security reasons
			String inNodeId=nodeId;
			if (nodeId != null) {
				try {
					HashMap<String,Object> props = new MCAlfrescoAPIClient().getProperties(nodeId);
					
					// For collections: Fetch the original object for preview
					if(props.containsKey(CCConstants.CCM_PROP_IO_ORIGINAL)){
						
						String original = (String) props.get(CCConstants.CCM_PROP_IO_ORIGINAL);
						
						if(!nodeId.equals(original)){
							nodeId=(String) props.get(CCConstants.CCM_PROP_IO_ORIGINAL);
							isCollection=true;
						}
					}
					
					validateScope(req, props);
					// we need to check permissions and allow or deny access by using the READ_PREVIEW permission
					validatePermissions(storeRef,inNodeId);
					
					if (nodeType.equals(QName.createQName(CCConstants.CCM_TYPE_REMOTEOBJECT))) {
						
						props=NodeServiceFactory.getNodeService((String) props.get(CCConstants.CCM_PROP_REMOTEOBJECT_REPOSITORYID))
							.getProperties(storeProtocol, storeId, (String) props.get(CCConstants.CCM_PROP_REMOTEOBJECT_NODEID));
						if(props != null){
							String thumbnail = (String)props.get(CCConstants.CM_ASSOC_THUMBNAILS);
							if(thumbnail != null && !thumbnail.trim().equals("")){
								resp.sendRedirect(thumbnail);
								return;
							}
						}
						deliverContentAsSystem(nodeRef, CCConstants.CM_PROP_CONTENT, req, resp);
						return;
						//resp.sendRedirect(defaultImage);
						//throw new Exception();
					}
					
					if (!nodeType.equals(QName.createQName(CCConstants.CCM_TYPE_IO)) 
							&& !nodeType.equals(QName.createQName(CCConstants.CCM_TYPE_MAP))
							&& !nodeType.equals(QName.createQName(CCConstants.CCM_TYPE_SAVED_SEARCH))) {
						//resp.sendError(HttpServletResponse.SC_NOT_ACCEPTABLE, "type is not an io and no map!");
						//return;
						throw new Exception();
					}
				} catch (InvalidNodeRefException e) {
					resp.sendError(HttpServletResponse.SC_NOT_ACCEPTABLE, e.getMessage());
					return;
				}
			} else {
				resp.sendError(HttpServletResponse.SC_NOT_ACCEPTABLE, "nodeId is null!");
				return;
			}
			
			if(QName.createQName(CCConstants.CCM_TYPE_MAP).equals(nodeType)){
				if(deliverContentAsSystem(nodeRef, CCConstants.CCM_PROP_MAP_ICON, req, resp))
					return;
			}

			if (type != null && type.equals(RESULT_TYPE_MIME_ICON)) {

				ContentReader reader = serviceRegistry.getContentService().getReader(nodeRef, QName.createQName(CCConstants.CM_PROP_CONTENT));
				String mimetype = reader.getMimetype();

				String iconUrl = new MimeTypes(req.getContextPath()).getIconUrl(mimetype, null);
				resp.sendRedirect(iconUrl);
				return;

			} else {

				//get previewurl to find out type (generated/userdefined)
				//Attention the url of GetPreviewResult of generated/userdefined previews points on this servlet so don't use it
				
				GetPreviewResult getPrevResult = null;
				if(version != null && !version.trim().equals("") && !isCollection){
					HashMap<String, HashMap<String,Object>> versionHistory = repoClient.getVersionHistory(nodeId);
					if(versionHistory != null){
						for(Map.Entry<String, HashMap<String,Object>> entry : versionHistory.entrySet()){
							String tmpVers = (String)entry.getValue().get(CCConstants.LOM_PROP_LIFECYCLE_VERSION);
							if(version.equals(tmpVers)){
								
								String vNodeId = (String)entry.getValue().get(CCConstants.SYS_PROP_NODE_UID);
								System.out.println("vNodeId:"+vNodeId+ " entry key:"+entry.getKey());
								
								StoreRef sr = new StoreRef(VersionService.VERSION_STORE_PROTOCOL, "version2Store");
								getPrevResult = repoClient.getPreviewUrl(sr, entry.getKey());
								
							}
							
						}
					}
				}
				final String nodeIdFinal=nodeId;

				if(getPrevResult == null){
					if(isCollection){
						// we need to access the actual object as admin
						getPrevResult = AuthenticationUtil.runAsSystem(new RunAsWork<GetPreviewResult>() {
							@Override
							public GetPreviewResult doWork() throws Exception {
								return repoClient.getPreviewUrl(MCAlfrescoAPIClient.storeRef, nodeIdFinal);
							}
						});
					}
					else{
						getPrevResult = repoClient.getPreviewUrl(MCAlfrescoAPIClient.storeRef, nodeId);
					}
				}
				
				if(isCollection){
					final GetPreviewResult getPrevResultFinal=getPrevResult;
					if(AuthenticationUtil.runAsSystem(new RunAsWork<Boolean>() {
						@Override
						public Boolean doWork() throws IOException  {
							return loadPreview(req, resp, op, nodeIdFinal, repoClient, getPrevResultFinal);
						}
					})){
						return;
					}
				}
				else{
					if(loadPreview(req, resp, op, nodeId, repoClient, getPrevResult)){
						return;
					}
				}
				
				/**
				 * generated or userdefined
				 */
				if (getPrevResult != null && (getPrevResult.getType().equals(GetPreviewResult.TYPE_USERDEFINED) || getPrevResult.getType().equals(GetPreviewResult.TYPE_GENERATED))) {
					NodeRef prevNodeRef = null;
					String property = CCConstants.CM_PROP_CONTENT;
					if (getPrevResult.getType().equals(GetPreviewResult.TYPE_USERDEFINED)) {
						prevNodeRef = new NodeRef(MCAlfrescoAPIClient.storeRef, nodeId);
						property = CCConstants.CCM_PROP_IO_USERDEFINED_PREVIEW;
					}
					if (getPrevResult.getType().equals(GetPreviewResult.TYPE_GENERATED)) {
						
						
						HashMap<String, Object> previewProps = null;
						final String fnodeId = nodeId;
						if(isCollection) {
							previewProps = AuthenticationUtil.runAsSystem(new RunAsWork<HashMap<String, Object>>() {
								@Override
								public HashMap<String, Object> doWork() throws Exception {
									return repoClient.getChild(fnodeId, CCConstants.CM_TYPE_THUMBNAIL, CCConstants.CM_NAME,
											CCConstants.CM_VALUE_THUMBNAIL_NAME_imgpreview_png);
								}
							});
						}else {
							previewProps = repoClient.getChild(nodeId, CCConstants.CM_TYPE_THUMBNAIL, CCConstants.CM_NAME,
									CCConstants.CM_VALUE_THUMBNAIL_NAME_imgpreview_png);
						}
						
						 
						String previewNodeId = (String) previewProps.get(CCConstants.SYS_PROP_NODE_UID);
						prevNodeRef = new NodeRef(MCAlfrescoAPIClient.storeRef, previewNodeId);
					}
					if (prevNodeRef != null) {
						
						if(isCollection) {
							NodeRef fprevNodeRef = prevNodeRef;
							String fproperty = property;
							boolean result = deliverContentAsSystem(fprevNodeRef, fproperty, req, resp);
							if(result) {
								return;
							}
							op.close();
						}else {
							if(deliverContentAsSystem(prevNodeRef, property, req, resp)) {
								
								return;
							}
							op.close();
								
							
						}
						
					}
				}
				
			}

		} catch (org.alfresco.repo.security.permissions.AccessDeniedException e) {
			
			MimeTypesV2 mime=new MimeTypesV2(repoClient.appInfo);
			String noPermImage=mime.getNoPermissionsPreview();
			resp.sendRedirect(noPermImage);
			return;
		} catch (Throwable e) {
			e.printStackTrace();
		}
		/**
		 * fallback to mime first, then default
		 */
		MimeTypesV2 mime=new MimeTypesV2(repoClient.appInfo);
		try{
			HashMap<String,Object> props;
			String[] aspects=new String[]{};
			String type=null;
			if(isCollection){
				final String nodeIdFinal=nodeId;
				props=AuthenticationUtil.runAsSystem(new RunAsWork<HashMap<String,Object>>() {
					@Override
					public HashMap<String, Object> doWork() throws Exception {
						try{
							return repoClient.getProperties(nodeIdFinal);
						}catch(Throwable t){
							throw new Exception(t);
						}
					}
				});
			}
			else{
				props=repoClient.getProperties(nodeId);
				aspects=repoClient.getAspects(nodeId);
				type = repoClient.getNodeType(nodeId);
			}
			resp.sendRedirect(mime.getPreview(type,props,Arrays.asList(aspects)));
			return;
		}
		catch(Throwable t){
			resp.sendRedirect(mime.getDefaultPreview());
		}
	}

	private void validatePermissions(StoreRef storeRef, String nodeId) {
		HashMap<String, Boolean> result = PermissionServiceFactory.getLocalService().hasAllPermissions(storeRef.getProtocol(),storeRef.getIdentifier(),nodeId,new String[]{CCConstants.PERMISSION_READ_PREVIEW});
		if(!result.get(CCConstants.PERMISSION_READ_PREVIEW))
			throw new AccessDeniedException("No "+CCConstants.PERMISSION_READ_PREVIEW+" on "+nodeId);
	}

	private void validateScope(HttpServletRequest req, HashMap<String, Object> props) {
		String scope=(String) req.getSession().getAttribute(CCConstants.AUTH_SCOPE);
		// Allow only valid scope
		if(props.containsKey(CCConstants.CCM_PROP_EDUSCOPE_NAME)){
			String nodeScope=(String) props.get(CCConstants.CCM_PROP_EDUSCOPE_NAME);
			if(!nodeScope.equals(scope)){
				throw new AccessDeniedException("Node has an other scope");
			}
		}
		// This happens if the user tries to access a non-scoped node from a scope
		else if(scope!=null){
			throw new AccessDeniedException("Node does not have a scope");
		}
	}

	/**
	 * returns true wenn a redirect was done
	 * @param req
	 * @param resp
	 * @param op
	 * @param nodeId
	 * @param repoClient
	 * @param getPrevResult
	 * @return
	 * @throws IOException
	 */
	private boolean loadPreview(HttpServletRequest req, HttpServletResponse resp, ServletOutputStream op, String nodeId,
			MCAlfrescoAPIClient repoClient, GetPreviewResult getPrevResult) throws IOException {
		
		if(getPrevResult == null){
			return false;
		}
		
		/**
		 * external URL
		 */
		if (getPrevResult.getType().equals(GetPreviewResult.TYPE_EXTERNAL)) {
			String extThumbUrl = getPrevResult.getUrl();
			if (extThumbUrl != null && !extThumbUrl.trim().equals("")) {
				resp.sendRedirect(extThumbUrl);
				return true;
			}
		}

		/**
		 * generated or user defined
		 */
		if (getPrevResult.getType().equals(GetPreviewResult.TYPE_USERDEFINED) || getPrevResult.getType().equals(GetPreviewResult.TYPE_GENERATED)) {
			NodeRef prevNodeRef = null;
			String property = CCConstants.CM_PROP_CONTENT;
			if (getPrevResult.getType().equals(GetPreviewResult.TYPE_USERDEFINED)) {
				prevNodeRef = new NodeRef(MCAlfrescoAPIClient.storeRef, nodeId);
				property = CCConstants.CCM_PROP_IO_USERDEFINED_PREVIEW;
			}
			if (getPrevResult.getType().equals(GetPreviewResult.TYPE_GENERATED)) {
				HashMap<String, Object> previewProps = repoClient.getChild(nodeId, CCConstants.CM_TYPE_THUMBNAIL, CCConstants.CM_NAME,
						CCConstants.CM_VALUE_THUMBNAIL_NAME_imgpreview_png);
				String previewNodeId = (String) previewProps.get(CCConstants.SYS_PROP_NODE_UID);
				prevNodeRef = new NodeRef(MCAlfrescoAPIClient.storeRef, previewNodeId);
			}
			if (prevNodeRef != null) {
				if(deliverContentAsSystem(prevNodeRef, property, req, resp))
					return true;
				op.close();
			}
		}
		if (getPrevResult.getType().equals(GetPreviewResult.TYPE_DEFAULT)) {
			NodeRef nodeRef = new NodeRef(MCAlfrescoAPIClient.storeRef, nodeId);
			ContentReader reader = serviceRegistry.getContentService().getReader(nodeRef, QName.createQName(CCConstants.CM_PROP_CONTENT));
			if(reader!=null && reader.getMimetype().startsWith("image")) {
				if(deliverContentAsSystem(nodeRef,  CCConstants.CM_PROP_CONTENT, req, resp))
					return true;
			}
		}
		return false;
	}
	private DataInputStream postProcessImage(String nodeId,DataInputStream in,HttpServletRequest req){
		float quality=0.7f;
		boolean grayscale=false;
		
		int width=0,height=0,maxHeight=0,maxWidth=0;
		boolean crop=false;
		boolean hasAnyValue=false;
		try{
			quality=Integer.parseInt(req.getParameter("quality"))/100.f;
			hasAnyValue=true;
		}catch(Throwable t){}
		try{
			grayscale=req.getParameter("grayscale").equals("true");
			hasAnyValue=true;
		}catch(Throwable t){}
		
		try{
			crop=req.getParameter("crop").equals("true");
			hasAnyValue=true;
		}catch(Throwable t){}
		try{
			width=Integer.parseInt(req.getParameter("width"));
		}catch(Throwable t){}
		try{
			height=Integer.parseInt(req.getParameter("height"));
		}catch(Throwable t){}
		try{
			maxWidth=Integer.parseInt(req.getParameter("maxWidth"));
		}catch(Throwable t){}
		try{
			maxHeight=Integer.parseInt(req.getParameter("maxHeight"));
		}catch(Throwable t){}
		
		if(!hasAnyValue)
			return null;
		
		boolean fromCache=false;
		
		if(isCacheable(width, height,maxWidth,maxHeight)){
			File file=PreviewCache.getFileForNode(nodeId,width,height);
			if(file.exists()){
				try{
					in.close();
					in=new DataInputStream(new FileInputStream(file));
					fromCache=true;
				}catch(Throwable t){}
			}
		}
		// if the image is cached, allow only 70% quality because it's useless to compress in higher ratio
		quality=Math.min(Math.max(quality, 0), fromCache ? 0.7f : 1);
		width=Math.min(Math.max(width, 0), MAX_IMAGE_SIZE);
		height=Math.min(Math.max(height, 0), MAX_IMAGE_SIZE);
		maxWidth=Math.min(Math.max(maxWidth, 0), MAX_IMAGE_SIZE);
		maxHeight=Math.min(Math.max(maxHeight, 0), MAX_IMAGE_SIZE);
		
		try{
			// cache optimization, if no other tasks, just return the cached preview
			if(fromCache && !grayscale && Math.abs(quality-0.7f)<0.1){
				return in;
			}
			
			BufferedImage img=ImageIO.read(in);
			
			try{
				float aspect=Float.parseFloat(req.getParameter("aspect"));
				if(aspect>1){
					width=img.getWidth();
					height=(int) (width/aspect);
				}
				else{
					height=img.getHeight();
					width=(int) (height*aspect);
				}
			}
			catch(Throwable t){}
			
			if(crop && !fromCache){
				float aspectOriginal=(float)img.getWidth()/(float)img.getHeight();
				if(maxWidth>0){
					width=(int) Math.min(height*aspectOriginal,maxWidth);
				}
				if(maxHeight>0){
					height=(int) Math.min(width/aspectOriginal,maxHeight);
				}
				BufferedImage cropped=new BufferedImage(width,height, BufferedImage.TYPE_INT_ARGB); // getType() sometimes return 0
				float aspectCrop=(float)width/(float)height;
				Graphics g=cropped.getGraphics();
				if(aspectCrop>aspectOriginal){
					float scaledHeight=cropped.getWidth()/aspectOriginal;
					Image scaled=img.getScaledInstance((int)(scaledHeight*aspectOriginal),(int)scaledHeight, BufferedImage.SCALE_SMOOTH);
					g.drawImage(scaled, 0,(int)( -(scaledHeight-cropped.getHeight())/2), cropped.getWidth(),(int)scaledHeight, null);
				}
				else{
					float scaledWidth=cropped.getHeight()*aspectOriginal;
					Image scaled=img.getScaledInstance((int)scaledWidth,(int)(scaledWidth/aspectOriginal), BufferedImage.SCALE_SMOOTH);
					g.drawImage(scaled, (int)( -(scaledWidth-cropped.getWidth())/2),0,(int)scaledWidth, cropped.getHeight(), null);
				}
				img=cropped;
			}
			BufferedImage imgOut=new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
			imgOut.getGraphics().setColor(java.awt.Color.WHITE);
			imgOut.getGraphics().fillRect(0, 0,imgOut.getWidth(),imgOut.getHeight());
			imgOut.getGraphics().drawImage(img, 0, 0, null);
		
			if(!fromCache && isCacheable(width, height,maxWidth,maxHeight)){
				// Drop alpha (weird colors in jpg otherwise)
				ImageIO.write(imgOut, "JPG",PreviewCache.getFileForNode(nodeId, width, height));
			}
				
			if(grayscale){
				ImageFilter filter = new GrayFilter(true, 0);  
				ImageProducer producer = new FilteredImageSource(imgOut.getSource(), filter);  
				Image gray = Toolkit.getDefaultToolkit().createImage(producer);
				
				imgOut.getGraphics().drawImage(gray, 0, 0, null);
				
			}
				
			// Drop alpha (weird colors in jpg otherwise)
			
			JPEGImageWriteParam jpegParams = new JPEGImageWriteParam(null);
			jpegParams.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
			jpegParams.setCompressionQuality(quality);
			ImageWriter writer = ImageIO.getImageWritersByFormatName("jpg").next();
			ByteArrayOutputStream os=new ByteArrayOutputStream();
			MemoryCacheImageOutputStream imgOutStream = new MemoryCacheImageOutputStream(os);
		
			writer.setOutput(imgOutStream);
			writer.write(null,new IIOImage(imgOut,null,null),jpegParams);
			imgOutStream.close();
			in.close();
			return new DataInputStream(new ByteArrayInputStream(os.toByteArray()));
			
		}
		catch(Throwable t){
			try {
				in.close();
			} catch (IOException e) {}
			return null;
		}
		
	}
	private boolean deliverContentAsSystem(NodeRef nodeRef, String contentProp,HttpServletRequest req, HttpServletResponse resp) throws IOException{
		return AuthenticationUtil.runAsSystem(new RunAsWork<Boolean>() {

			@Override
			public Boolean doWork() throws Exception {
				ServletOutputStream op = resp.getOutputStream();
				ContentReader reader = serviceRegistry.getContentService().getReader(nodeRef,
						QName.createQName(contentProp));
				
				if(reader == null){
					return false;
				}
				
				String mimetype = reader.getMimetype();

				
				//resp.setContentLength((int) reader.getContentData().getSize());
				// resp.setHeader("Content-Disposition",
				// "attachment; filename=\" preview.png \"");

				int length = 0;
				//
				// Stream to the requester.
				//
				byte[] bbuf = new byte[1024];
				// DataInputStream in = new
				// DataInputStream(url.openStream());
				InputStream is=reader.getContentInputStream();
				DataInputStream in = new DataInputStream(is);
				if(mimetype.startsWith("image")){
					DataInputStream tmp = postProcessImage(nodeRef.getId(),in,req);
					if(tmp != null){
						in = tmp;
						mimetype = "image/jpeg";
					}
					else {
						// image was broken but stream is consumed, open a new one
						reader = serviceRegistry.getContentService().getReader(nodeRef,
								QName.createQName(contentProp));
						is=reader.getContentInputStream();
						in = new DataInputStream(is);
					}
					
				}
				
				resp.setContentType((mimetype != null) ? mimetype : "application/octet-stream");
				
				resp.setContentLength((int) in.available());

					
				while ((in != null) && ((length = in.read(bbuf)) != -1)) {
					op.write(bbuf, 0, length);
				}
				
				in.close();

				op.flush();
				op.close();
				return true;
			}
		});
		
	}
}
