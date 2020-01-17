package org.edu_sharing.repository.server;

import java.io.*;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.mail.Store;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.cnri.util.StreamUtil;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.namespace.QName;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.lang.StringUtils;
import org.apache.http.client.methods.HttpGet;
import org.apache.log4j.Logger;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.rpc.Share;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.repository.server.tools.HttpQueryTool;
import org.edu_sharing.repository.server.tools.URLTool;
import org.edu_sharing.repository.server.tracking.TrackingTool;
import org.edu_sharing.restservices.shared.Node;
import org.edu_sharing.service.nodeservice.NodeService;
import org.edu_sharing.service.nodeservice.NodeServiceFactory;
import org.edu_sharing.service.nodeservice.NodeServiceHelper;
import org.edu_sharing.service.permission.PermissionService;
import org.edu_sharing.service.permission.PermissionServiceFactory;
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
		String nodeIds = req.getParameter("nodeIds");
		String fileName = req.getParameter("fileName");
		Mode mode = req.getParameter("mode")!=null ? Mode.valueOf(req.getParameter("mode")) : Mode.redirect;
		if(nodeIds!=null) {
			downloadZip(resp, nodeIds.split(","), null, null, null, fileName);
		}
		downloadNode(nodeId,req,resp,fileName,mode);

	}

	private void downloadNode(String nodeId, HttpServletRequest req, HttpServletResponse resp, String fileName, Mode mode) throws IOException {
		try {
			if (!NodeServiceHelper.downloadAllowed(nodeId)) {
				resp.sendRedirect(URLTool.getNgErrorUrl(""+HttpServletResponse.SC_FORBIDDEN));
				return;
			}
			String version=req.getParameter("version");
			if(version!=null && version.isEmpty())
			    version=null;
			NodeService nodeService = NodeServiceFactory.getLocalService();
			OutputStream bufferOut = resp.getOutputStream();
			TrackingServiceFactory.getTrackingService().trackActivityOnNode(new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE,nodeId),null,TrackingService.EventType.DOWNLOAD_MATERIAL);
			InputStream is=null;
			try {
				is = nodeService.getContent(StoreRef.PROTOCOL_WORKSPACE, StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(), nodeId, version, ContentModel.PROP_CONTENT.toString());
			}catch(Throwable t){

			}
			if(is==null || is.available()==0){
				if(mode.equals(Mode.passthrough)) {
					is = getStreamFromLocation(nodeId);
				}
				else if(mode.equals(Mode.redirect)){
					resp.sendRedirect(NodeServiceHelper.getProperty(new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE,nodeId),CCConstants.LOM_PROP_TECHNICAL_LOCATION));
					return;
				}
			}
			setHeaders(resp,
					fileName!=null ? fileName : NodeServiceHelper.getProperty(new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, nodeId)
					, CCConstants.CM_NAME));
			//resp.setHeader("Content-Length",""+is.available());
			StreamUtils.copy(is,
					bufferOut);

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

		ByteArrayOutputStream bufferOut = new ByteArrayOutputStream();
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
						if(nodeService.hasAspect(StoreRef.PROTOCOL_WORKSPACE,StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(),nodeId,CCConstants.CCM_ASPECT_COLLECTION_IO_REFERENCE)){
							String refNodeId = (String)nodeService.getProperty(StoreRef.PROTOCOL_WORKSPACE,StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(),nodeId, CCConstants.CCM_PROP_IO_ORIGINAL);
							nodeId = refNodeId;

							// check if PERMISSION_READ_ALL (read content) is present
							if(!permissionService.hasPermission(StoreRef.PROTOCOL_WORKSPACE,StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(),nodeId,CCConstants.PERMISSION_READ_ALL)){
								throw new SecurityException();
							}
							isCollectionRef = true;
						}
						String finalNodeId = nodeId;

						TrackingTool.trackActivityOnNode(nodeId,null,TrackingService.EventType.DOWNLOAD_MATERIAL);
                        AuthenticationUtil.RunAsWork work= () ->{
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
				outputData(resp, zipName, bufferOut);
			}
		}
		catch(Throwable t){
			t.printStackTrace();
		}
	}

	private static void outputData(HttpServletResponse resp, String filename, ByteArrayOutputStream bufferOut) throws IOException {
		setHeaders(resp, filename);
		resp.setHeader("Content-Length",""+bufferOut.size());
		resp.getOutputStream().write(bufferOut.toByteArray());
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
