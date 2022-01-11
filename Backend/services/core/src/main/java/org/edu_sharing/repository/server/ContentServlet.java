package org.edu_sharing.repository.server;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.alfresco.model.ContentModel;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.version.Version;
import org.alfresco.service.cmr.version.VersionHistory;
import org.alfresco.service.namespace.QName;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.repository.server.tools.security.SignatureVerifier;
import org.edu_sharing.repository.server.tools.security.Signing;
import org.edu_sharing.service.nodeservice.NodeServiceFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.util.StreamUtils;


public class ContentServlet extends HttpServlet{

	
	Logger logger = Logger.getLogger(ContentServlet.class);
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {

		// the app requesting data (e.g. esrender)
		String appId = req.getParameter("appId");
		// the app where the content is stored
		String repId = req.getParameter("repId");
		String nodeId = req.getParameter("nodeId");
		String version = req.getParameter("version");
		String timestamp = req.getParameter("timeStamp");
		
		//signature(nodeId+timestamp)=authToken
		String authToken = req.getParameter("authToken");
		
		
		if(appId == null || appId.trim().equals("")){
			String message = "missing appId";
			logger.error(message);
			resp.sendError(HttpServletResponse.SC_PRECONDITION_FAILED,message);
			return;
		}
		
		
		if(nodeId == null || nodeId.trim().equals("")){
			String message = "missing nodeId";
			logger.error(message);
			resp.sendError(HttpServletResponse.SC_PRECONDITION_FAILED,message);
			return;
		}
		
		
		SignatureVerifier.Result result = new SignatureVerifier().verify(appId, authToken,  nodeId+timestamp, timestamp);
		if(result.getStatuscode() != HttpServletResponse.SC_OK){
			resp.sendError(result.getStatuscode(),result.getMessage());
			return;
		}
	
		
		ServletOutputStream op = resp.getOutputStream();
		
		ApplicationContext appContext = AlfAppContextGate.getApplicationContext();

		ServiceRegistry serviceRegistry = (ServiceRegistry) appContext.getBean(ServiceRegistry.SERVICE_REGISTRY);
		NodeRef nodeRef = new NodeRef(MCAlfrescoAPIClient.storeRef,nodeId);
		
		
		
		try{
			ApplicationInfo  homeAppInfo = ApplicationInfoList.getHomeRepository();
			serviceRegistry.getAuthenticationService().authenticate(homeAppInfo.getUsername(), homeAppInfo.getPassword().toCharArray());
			// if remote repository, fetch the content via the implemented node service
			if(repId!=null && !homeAppInfo.getAppId().equals(repId)){
				String mimetype=NodeServiceFactory.getNodeService(repId).getContentMimetype(null,null,nodeId);
				InputStream is = NodeServiceFactory.getNodeService(repId).getContent(null, null, nodeId, null, ContentModel.PROP_CONTENT.toString());
				resp.setContentType((mimetype != null) ? mimetype : "application/octet-stream");
				ByteArrayOutputStream bos = new ByteArrayOutputStream();
				if(is!=null) {
					StreamUtils.copy(is, bos);
				}
				resp.setContentLength(bos.size());
				StreamUtils.copy(bos.toByteArray(),resp.getOutputStream());
				if(is!=null) {
					is.close();
				}
			}
			else {
				/**
				 * Collection change nodeRef to original
				 */
				boolean isCollectionRef = false;
				if (serviceRegistry.getNodeService().hasAspect(nodeRef, QName.createQName(CCConstants.CCM_ASPECT_COLLECTION_IO_REFERENCE))) {
					String refNodeId = (String) serviceRegistry.getNodeService().getProperty(nodeRef, QName.createQName(CCConstants.CCM_PROP_IO_ORIGINAL));
					nodeRef = new NodeRef(MCAlfrescoAPIClient.storeRef, refNodeId);
					isCollectionRef = true;
				}

				// we only fetch a specific version if it's not a ref
				// and it's not a remote node
				if (!isCollectionRef && version != null && !version.trim().equals("") && !homeAppInfo.getAppId().equals(repId)) {
					VersionHistory versionHistory = serviceRegistry.getVersionService().getVersionHistory(nodeRef);
					Version versionObj = null;
					if(versionHistory != null) {
						versionObj = versionHistory.getVersion(version);
					}

					if (versionObj == null) {
						String message = "unknown version";
						logger.error(message);
						resp.sendError(HttpServletResponse.SC_PRECONDITION_FAILED, message);
						return;
					}
					if (!versionObj.getFrozenModifiedDate().equals(versionHistory.getHeadVersion().getFrozenModifiedDate()))
						nodeRef = versionObj.getFrozenStateNodeRef();
				}


				if (nodeRef != null) {
					ContentReader reader = serviceRegistry.getContentService().getReader(nodeRef, ContentModel.PROP_CONTENT);
					if (reader == null) {
						return;
					}

					String mimetype = reader.getMimetype();

					resp.setContentType((mimetype != null) ? mimetype : "application/octet-stream");
					resp.setContentLength((int) reader.getContentData().getSize());

					int length = 0;
					//
					// Stream to the requester.
					//
					byte[] bbuf = new byte[1024];
					// DataInputStream in = new
					// DataInputStream(url.openStream());
					DataInputStream in = new DataInputStream(reader.getContentInputStream());
					while ((in != null) && ((length = in.read(bbuf)) != -1)) {
						op.write(bbuf, 0, length);
					}

					in.close();
					op.flush();
					op.close();
				}
			}
		}catch(Throwable t) {
			throw new ServletException(t);
		}finally{
			serviceRegistry.getAuthenticationService().invalidateTicket(serviceRegistry.getAuthenticationService().getCurrentTicket());
		}
		
		
	}
	
}
