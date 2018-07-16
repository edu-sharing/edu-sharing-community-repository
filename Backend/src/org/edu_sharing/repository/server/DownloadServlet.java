package org.edu_sharing.repository.server;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.permissions.AccessDeniedException;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.version.Version;
import org.alfresco.service.cmr.version.VersionHistory;
import org.alfresco.service.namespace.QName;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;
import org.codehaus.plexus.util.FileUtils;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.rpc.Share;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.client.tools.UrlTool;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.repository.server.tools.URLTool;
import org.edu_sharing.repository.server.tools.security.SignatureVerifier;
import org.edu_sharing.repository.server.tools.security.Signing;
import org.edu_sharing.service.share.ShareService;
import org.edu_sharing.service.share.ShareServiceImpl;
import org.springframework.context.ApplicationContext;


public class DownloadServlet extends HttpServlet{

	
	Logger logger = Logger.getLogger(DownloadServlet.class);
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		
		String appId = req.getParameter("appId");
		String nodeIds = req.getParameter("nodeIds");
		String parentNodeId = req.getParameter("parentNodeId");
		String token = req.getParameter("token"); // token for share link
		String password = req.getParameter("password"); // password for share link
		String zipName = req.getParameter("fileName");
		if(zipName==null || zipName.isEmpty())
			zipName="Download.zip";

		String[] nodeIdsSplit=nodeIds.split(",");

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
			}catch(Throwable t){
				resp.sendRedirect(URLTool.getNgMessageUrl("invalid_share"));
				return;
			}
			for(String node : nodeIdsSplit){
				if(!shareService.isNodeAccessibleViaShare(new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE,parentNodeId),node)){
					resp.sendRedirect(URLTool.getNgMessageUrl("security_error"));
					return;
				}

			}
		}
		
		
		if(appId == null || appId.trim().equals("")){
			String message = "missing appId";
			logger.error(message);
			resp.sendError(HttpServletResponse.SC_PRECONDITION_FAILED,message);
			return;
		}
		
		
		if(nodeIds == null || nodeIds.trim().equals("")){
			String message = "missing nodeIds";
			logger.error(message);
			resp.sendError(HttpServletResponse.SC_PRECONDITION_FAILED,message);
			return;
		}
		
		
		
		ServletOutputStream op = resp.getOutputStream();
		
		ApplicationContext appContext = AlfAppContextGate.getApplicationContext();

		ServiceRegistry serviceRegistry = (ServiceRegistry) appContext.getBean(ServiceRegistry.SERVICE_REGISTRY);
		ApplicationInfo  homeAppInfo = ApplicationInfoList.getHomeRepository();

		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		ZipOutputStream zos = new ZipOutputStream(buffer);
		zos.setMethod( ZipOutputStream.DEFLATED );
		


		try{
			AuthenticationUtil.RunAsWork<Boolean> work= () ->{
				String errors="";
				for(String nodeId : nodeIdsSplit){
					try{
						NodeRef nodeRef = new NodeRef(MCAlfrescoAPIClient.storeRef,nodeId);
						/**
						 * Collection change nodeRef to original
						 */
						boolean isCollectionRef=false;
						if(serviceRegistry.getNodeService().hasAspect(nodeRef, QName.createQName(CCConstants.CCM_ASPECT_COLLECTION_IO_REFERENCE))){
							String refNodeId = (String)serviceRegistry.getNodeService().getProperty(nodeRef, QName.createQName(CCConstants.CCM_PROP_IO_ORIGINAL));
							nodeRef = new NodeRef(MCAlfrescoAPIClient.storeRef, refNodeId);
							isCollectionRef = true;
						}
						String filename = (String)serviceRegistry.getNodeService().getProperty(nodeRef, QName.createQName(CCConstants.CM_NAME));
						String wwwurl = (String)serviceRegistry.getNodeService().getProperty(nodeRef, QName.createQName(CCConstants.CCM_PROP_IO_WWWURL));
						if(wwwurl!=null){
							errors += filename+": Is a link and can not be downloaded\r\n";
							continue;
						}
						ContentReader reader = serviceRegistry.getContentService().getReader(nodeRef, ContentModel.PROP_CONTENT);
						if(reader==null){
							errors += filename+": Has no content\r\n";
							continue;
						}
						InputStream is = reader.getContentInputStream();
						resp.setContentType("application/zip");

						DataInputStream in = new DataInputStream(is);

						ZipEntry entry = new ZipEntry(filename);
						zos.putNextEntry(entry);
						byte[] buf=new byte[1024];
						while(true){
							int l=in.read(buf);
							if(l<=0)
								break;
							zos.write(buf,0,l);
						}
						in.close();
					}catch(Throwable t){
						t.printStackTrace();
						resp.sendRedirect(URLTool.getNgMessageUrl("INVALID"));
						return false;
					}
				}
				if(errors.length()>0){
					ZipEntry entry = new ZipEntry("Info.txt");
					zos.putNextEntry(entry);
					zos.write(errors.getBytes());
				}
				zos.close();
				return true;
			};
			boolean result;
			if(share!=null){
				result=AuthenticationUtil.runAsSystem(work);
			}
			else{
				result=work.doWork();
			}
			if(result) {
				resp.setHeader("Content-Disposition","attachment; filename=\""+cleanName(zipName)+"\"");
				resp.getOutputStream().write(buffer.toByteArray());
			}
		}
		catch(Throwable t){
			t.printStackTrace();
		}
		
	}

	public static String cleanName(String name) {
		return name.replaceAll("[^a-zA-Z0-9-_\\.]", "_");
	}

}
