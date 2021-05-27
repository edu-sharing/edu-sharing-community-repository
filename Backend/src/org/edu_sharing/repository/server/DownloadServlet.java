package org.edu_sharing.repository.server;

import java.io.*;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.util.TempFileProvider;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.metadataset.v2.tools.MetadataHelper;
import org.edu_sharing.metadataset.v2.tools.MetadataTemplateRenderer;
import org.edu_sharing.repository.client.rpc.Share;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.authentication.ContextManagementFilter;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.repository.server.tools.HttpQueryTool;
import org.edu_sharing.repository.server.tools.URLTool;
import org.edu_sharing.repository.server.tracking.TrackingTool;
import org.edu_sharing.restservices.NodeDao;
import org.edu_sharing.restservices.RepositoryDao;
import org.edu_sharing.restservices.RestConstants;
import org.edu_sharing.restservices.shared.Node;
import org.edu_sharing.service.nodeservice.NodeService;
import org.edu_sharing.service.nodeservice.NodeServiceFactory;
import org.edu_sharing.service.nodeservice.NodeServiceHelper;
import org.edu_sharing.service.permission.*;
import org.edu_sharing.service.provider.ProviderHelper;
import org.edu_sharing.service.share.ShareService;
import org.edu_sharing.service.share.ShareServiceImpl;
import org.edu_sharing.service.tracking.TrackingService;
import org.edu_sharing.service.tracking.TrackingServiceFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.util.StreamUtils;


public class DownloadServlet extends HttpServlet{


	static Logger logger = Logger.getLogger(DownloadServlet.class);
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {

		String nodeId = req.getParameter("nodeId");
		String repositoryId = req.getParameter("repositoryId");
		if(repositoryId != null && ApplicationInfoList.isLocalRepository(repositoryId)){
			repositoryId = null;
		}
		String nodeIds = req.getParameter("nodeIds");
		String fileName = req.getParameter("fileName");
		Mode mode = req.getParameter("mode")!=null ? Mode.valueOf(req.getParameter("mode")) : Mode.redirect;
		if(nodeIds!=null) {
			downloadZip(resp, nodeIds.split(","), null, null, null, fileName);
			return;
		}
		downloadNode(nodeId, repositoryId,req,resp,fileName,mode);

	}

	private void downloadNode(String nodeId, String repositoryId, HttpServletRequest req, HttpServletResponse resp, String fileName, Mode mode) throws IOException {
		try {
			// allow signature based auth from connector to bypass the download/content access
			NodeService nodeService = repositoryId == null ? NodeServiceFactory.getLocalService() : NodeServiceFactory.getNodeService(repositoryId);
			logger.debug("Access tool: " + ContextManagementFilter.accessToolType.get());
			if (repositoryId == null &&
					!NodeServiceHelper.downloadAllowed(nodeId) &&
					!ApplicationInfo.TYPE_CONNECTOR.equals(ContextManagementFilter.accessToolType.get())) {
				logger.info("Download forbidden for node " + nodeId);
				resp.sendRedirect(URLTool.getNgErrorUrl(""+HttpServletResponse.SC_FORBIDDEN));
				return;
			}
			String version=req.getParameter("version");
			if(version!=null && version.isEmpty())
			    version=null;
			OutputStream bufferOut = resp.getOutputStream();
			NodeRef nodeRef = new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, nodeId);
			String name = fileName!=null ? fileName : nodeService.getProperty(nodeRef.getStoreRef().getProtocol(), nodeRef.getStoreRef().getIdentifier(), nodeRef.getId(), CCConstants.CM_NAME);
			if("true".equalsIgnoreCase(req.getParameter("metadata"))){
				String metadata = getMetadataRenderer(nodeRef).render("io_text");
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				out.write(metadata.getBytes());
				outputData(resp,name + ".txt", out);
				return;
			}
			String originalNodeId;
			if(repositoryId == null) {
				TrackingServiceFactory.getTrackingService().trackActivityOnNode(new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, nodeId), null, TrackingService.EventType.DOWNLOAD_MATERIAL);
				originalNodeId = checkAndGetCollectionRef(nodeId);
			} else {
				originalNodeId = nodeId;
			}
			InputStream is=null;
			try {
				if(originalNodeId != null){
					String finalVersion = version;
					is = AuthenticationUtil.runAsSystem(() -> {
						try {
							return nodeService.getContent(
									StoreRef.PROTOCOL_WORKSPACE,
									StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(),
									originalNodeId,
									finalVersion,
									ContentModel.PROP_CONTENT.toString());
						} catch (Throwable ignored) {
						}
						return null;
					});
				} else {
					is = nodeService.getContent(
							StoreRef.PROTOCOL_WORKSPACE,
							StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(),
							nodeId,
							version,
							ContentModel.PROP_CONTENT.toString());
				}
			} catch(Throwable ignored){

			}
			if(is==null || is.available()==0){
				if(mode.equals(Mode.passthrough)) {
					is = getStreamFromLocation(nodeId);
				}
				else if(mode.equals(Mode.redirect)){
					resp.sendRedirect(nodeService.getProperty(nodeRef.getStoreRef().getProtocol(), nodeRef.getStoreRef().getIdentifier(), nodeRef.getId(),CCConstants.LOM_PROP_TECHNICAL_LOCATION));
					return;
				}
			}
			setHeaders(resp, name);
			//resp.setHeader("Content-Length",""+is.available());
			StreamUtils.copy(is, bufferOut);

		}catch(Throwable t){
			logger.error(t);
			resp.sendRedirect(URLTool.getNgErrorUrl(""+HttpServletResponse.SC_INTERNAL_SERVER_ERROR));
		}
	}

	/**
	 * tries to fetch the stream from the node's technical location, if available
	 * @param nodeId
	 * @return
	 */
	private InputStream getStreamFromLocation(String nodeId) {
		String location = NodeServiceHelper.getProperty(new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, nodeId), CCConstants.LOM_PROP_TECHNICAL_LOCATION);
		if(location==null)
			return null;
		Map<String, String> headers=new HashMap<>();
		return new HttpQueryTool().getStream(new GetMethod(location));
	}
	private static String checkAndGetCollectionRef(String nodeId){
		NodeRef ref = new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, nodeId);
		if(NodeServiceHelper.hasAspect(ref,CCConstants.CCM_ASPECT_COLLECTION_IO_REFERENCE)){
			NodeServiceHelper.validatePermissionRestrictedAccess(ref, CCConstants.PERMISSION_READ_ALL);
			return NodeServiceHelper.getProperty(ref, CCConstants.CCM_PROP_IO_ORIGINAL);
		}
		return null;
	}
	public static void downloadZip(HttpServletResponse resp, String[] nodeIds, String parentNodeId, String token, String password, String zipName) throws IOException {
		if(zipName==null || zipName.isEmpty())
			zipName="Download.zip";

		if(nodeIds == null || nodeIds.length==0){
			String message = "missing nodeIds";
			resp.sendError(HttpServletResponse.SC_PRECONDITION_FAILED,message);
			return;
		}

		Share share=null;
		ShareService shareService=new ShareServiceImpl();
		if(parentNodeId!=null && token!=null){
			try {
				share = shareService.getShare(parentNodeId, token);
				if (share == null)
					throw new Exception();
				if (share.getPassword()!=null && (!share.getPassword().equals(ShareServiceImpl.encryptPassword(password)))) {
					throw new Exception();
				}
				if (share.getExpiryDate() != ShareService.EXPIRY_DATE_UNLIMITED) {
					if (new Date(System.currentTimeMillis()).after(new Date(share.getExpiryDate()))) {
						resp.sendRedirect(URLTool.getNgMessageUrl("share_expired"));
						return;
					}
				}
			}catch(Throwable t){
				t.printStackTrace();
				resp.sendRedirect(URLTool.getNgMessageUrl("invalid_share"));
				return;
			}
			for(String node : nodeIds){
				if(!shareService.isNodeAccessibleViaShare(new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE,parentNodeId),node)){
					resp.sendRedirect(URLTool.getNgErrorUrl(""+HttpServletResponse.SC_FORBIDDEN));
					return;
				}

			}
		}


		/*
		if(appId == null || appId.trim().equals("")){
			String message = "missing appId";
			logger.error(message);
			resp.sendError(HttpServletResponse.SC_PRECONDITION_FAILED,message);
			return;
		}
		*/



		ServletOutputStream op = resp.getOutputStream();

		ApplicationContext appContext = AlfAppContextGate.getApplicationContext();

		ApplicationInfo homeAppInfo = ApplicationInfoList.getHomeRepository();

		NodeService nodeService = NodeServiceFactory.getLocalService();
		PermissionService permissionService = PermissionServiceFactory.getLocalService();

		File file = TempFileProvider.createTempFile("edu.",".zip");
		FileOutputStream bufferOut = new FileOutputStream(file);
		ZipOutputStream zos = new ZipOutputStream(bufferOut);
		zos.setMethod( ZipOutputStream.DEFLATED );

        List<String> errors=new ArrayList<>();
		try{
			AuthenticationUtil.RunAsWork<Boolean> runAll= () ->{
				for(String nodeId : nodeIds){
					try{
						/**
						 * Collection change nodeRef to original
						 */
						boolean isCollectionRef=false;
						String originalNodeId = checkAndGetCollectionRef(nodeId);
						TrackingTool.trackActivityOnNode(nodeId,null,TrackingService.EventType.DOWNLOAD_MATERIAL);
						if(originalNodeId != null){
							nodeId = originalNodeId;
							isCollectionRef = true;
						}
						String finalNodeId = nodeId;

                        AuthenticationUtil.RunAsWork work= () ->{
							try {
								addMetadataFile(finalNodeId, zos);
							} catch (Throwable t) {
								logger.warn("Could not export metadata for node "+finalNodeId, t);
							}
							String filename = nodeService.getProperty(StoreRef.PROTOCOL_WORKSPACE,StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(),finalNodeId,CCConstants.CM_NAME);
                            String wwwurl = nodeService.getProperty(StoreRef.PROTOCOL_WORKSPACE,StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(),finalNodeId,CCConstants.CCM_PROP_IO_WWWURL);
                            if(wwwurl!=null){
                                errors.add( filename+": Is a link and can not be downloaded" );
                                return null;
                            }
							InputStream reader = null;
							try {
								reader = nodeService.getContent(StoreRef.PROTOCOL_WORKSPACE,StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(),finalNodeId,null, ContentModel.PROP_CONTENT.toString());
							} catch (Throwable t) {
							}
							if(reader==null){
                                errors.add( filename+": Has no content" );
                                return null;
                            }
							if(!NodeServiceHelper.downloadAllowed(finalNodeId)){
								errors.add(filename+": Download not allowed");
								return null;
							}
                            resp.setContentType("application/zip");

                            DataInputStream in = new DataInputStream(reader);

                            ZipEntry entry = new ZipEntry(filename);
                            zos.putNextEntry(entry);
                            byte[] buffer=new byte[1024];
                            while(true){
                                int l=in.read(buffer);
                                if(l<=0)
                                    break;
                                zos.write(buffer,0,l);
                            }
                            in.close();
                            return null;
                        };
                        if(isCollectionRef)
                            AuthenticationUtil.runAsSystem(work);
                        else
                            work.doWork();
					}catch(Throwable t){
                        logger.warn(t.getMessage(),t);
						resp.sendRedirect(URLTool.getNgErrorUrl(""+HttpServletResponse.SC_INTERNAL_SERVER_ERROR));
						return false;
					}
				}
				if(errors.size()>0){
					ZipEntry entry = new ZipEntry("Info.txt");
					zos.putNextEntry(entry);
                    zos.write(StringUtils.join(errors,"\r\n").getBytes());
				}
				zos.close();
				return true;
			};
			boolean result;
			if(share!=null){
				result=AuthenticationUtil.runAsSystem(runAll);
			}
			else{
				result=runAll.doWork();
			}
			if(result) {
				outputData(resp, zipName, file);
			}
		}
		catch(Throwable t){
			t.printStackTrace();
		}
	}

	private static void addMetadataFile(String nodeId,ZipOutputStream zos) throws Throwable {
		NodeRef nodeRef = new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, nodeId);
		String filename = NodeServiceHelper.getProperty(nodeRef, CCConstants.CM_NAME);
		filename += ".txt";
		MetadataTemplateRenderer render = getMetadataRenderer(nodeRef);
		ZipEntry entry = new ZipEntry("metadata/" + filename);
		zos.putNextEntry(entry);
		zos.write(render.render("io_text").getBytes());
	}

	private static MetadataTemplateRenderer getMetadataRenderer(NodeRef nodeRef) throws Throwable {
		// inherit the props so childobjects will get all properties
		// only local nodes are supported right now
		NodeDao nodeDao = NodeDao.getNode(RepositoryDao.getHomeRepository(), nodeRef.getId());
		HashMap<String, Object> props = nodeDao.getInheritedPropertiesFromParent();
		MetadataTemplateRenderer render = new MetadataTemplateRenderer(
				MetadataHelper.getMetadataset(nodeRef),
				nodeRef
				, AuthenticationUtil.getFullyAuthenticatedUser(),
				props
		);
		render.setRenderingMode(MetadataTemplateRenderer.RenderingMode.TEXT);
		return render;
	}

	private static void outputData(HttpServletResponse resp, String filename, ByteArrayOutputStream bufferOut) throws IOException {
		setHeaders(resp, filename);
		resp.setHeader("Content-Length",""+bufferOut.size());
		resp.getOutputStream().write(bufferOut.toByteArray());
	}

	private static void outputData(HttpServletResponse resp, String filename, File file) throws IOException {
		setHeaders(resp, filename);
		resp.setHeader("Content-Length",""+file.length());
		IOUtils.copy(new FileInputStream(file),resp.getOutputStream());
	}


	private static void setHeaders(HttpServletResponse resp, String filename) {
		resp.setHeader("Content-type","application/octet-stream");
		resp.setHeader("Content-Transfer-Encoding","binary");
		resp.setHeader("Content-Disposition","attachment; filename=\""+cleanName(filename)+"\"");
	}

	public static String cleanName(String name) {
		return name.replaceAll("[^a-zA-Z0-9-_\\.]", "_");
	}

	/**
	 * The mode, only relevant if content is not stored localy but using the TECHNICAL_LOCATION
	 * Default is redirect
	 */
	enum Mode {
		redirect, // redirect the request to the TECHNICAL_LOCATION
		passthrough, // Fetch the stream from the TECHNICAL_LOCATION, and pass it to the client (like a proxy)

	}
}
