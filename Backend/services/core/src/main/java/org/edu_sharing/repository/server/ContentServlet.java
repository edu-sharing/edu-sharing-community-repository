package org.edu_sharing.repository.server;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.version.Version;
import org.alfresco.service.cmr.version.VersionHistory;
import org.alfresco.service.namespace.QName;
import org.apache.log4j.Logger;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.repository.server.tools.security.SignatureVerifier;
import org.edu_sharing.service.nodeservice.NodeServiceFactory;
import org.edu_sharing.spring.servlet.SpringHttpServlet;
import org.springframework.context.ApplicationContext;
import org.springframework.util.StreamUtils;


public class ContentServlet extends SpringHttpServlet {

	
	static Logger logger = Logger.getLogger(ContentServlet.class);
	
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
        String signedAlg = req.getParameter("signedAlg");
		
		
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
		
		
		SignatureVerifier.Result result = new SignatureVerifier().verify(appId, authToken,  nodeId+timestamp, timestamp, signedAlg);
		if(result.getStatuscode() != HttpServletResponse.SC_OK){
			resp.sendError(result.getStatuscode(),result.getMessage());
			return;
		}
	
		
		ServletOutputStream op = resp.getOutputStream();
		
		ApplicationContext appContext = AlfAppContextGate.getApplicationContext();

		ServiceRegistry serviceRegistry = (ServiceRegistry) appContext.getBean(ServiceRegistry.SERVICE_REGISTRY);

		
		try {
			ApplicationInfo  homeAppInfo = ApplicationInfoList.getHomeRepository();
			AuthenticationUtil.runAsSystem(() -> {
				try {
					NodeRef nodeRef = new NodeRef(MCAlfrescoAPIClient.storeRef,nodeId);
					// if remote repository, fetch the content via the implemented node service
					if (repId != null && !homeAppInfo.getAppId().equals(repId)) {
						String mimetype = NodeServiceFactory.getInstance().getService(repId).getContentMimetype(null, null, nodeId);
						InputStream is = NodeServiceFactory.getInstance().getService(repId).getContent(null, null, nodeId, null, ContentModel.PROP_CONTENT.toString());
						resp.setContentType((mimetype != null) ? mimetype : "application/octet-stream");
						ByteArrayOutputStream bos = new ByteArrayOutputStream();
						if (is != null) {
							StreamUtils.copy(is, bos);
						}
						resp.setContentLength(bos.size());
						StreamUtils.copy(bos.toByteArray(), resp.getOutputStream());
						if (is != null) {
							is.close();
						}
					} else {
						/**
						 * Collection change nodeRef to original
						 */
						boolean isCollectionRef = false;
						if (serviceRegistry.getNodeService().hasAspect(nodeRef, QName.createQName(CCConstants.CCM_ASPECT_COLLECTION_IO_REFERENCE))) {
							String refNodeId = (String) serviceRegistry.getNodeService().getProperty(nodeRef, QName.createQName(CCConstants.CCM_PROP_IO_ORIGINAL));
							nodeRef = new NodeRef(MCAlfrescoAPIClient.storeRef, refNodeId);
							isCollectionRef = true;
						}

						boolean isPublishedMaterial = serviceRegistry.getNodeService().getProperty(nodeRef, QName.createQName(CCConstants.CCM_PROP_IO_PUBLISHED_ORIGINAL)) != null;
						// we only fetch a specific version if it's not a ref
						// and it's not a remote node
						if (!isCollectionRef && !isPublishedMaterial && version != null && !version.trim().equals("") && homeAppInfo.getAppId().equals(repId)) {
							VersionHistory versionHistory = serviceRegistry.getVersionService().getVersionHistory(nodeRef);
							Version versionObj = null;
							if (versionHistory != null) {
								versionObj = versionHistory.getVersion(version);
							}

							if (versionObj == null) {
								String message = "unknown version";
								logger.error(message);
								resp.sendError(HttpServletResponse.SC_PRECONDITION_FAILED, message);
								return null;
							}
							if (!versionObj.getFrozenModifiedDate().equals(versionHistory.getHeadVersion().getFrozenModifiedDate()))
								nodeRef = versionObj.getFrozenStateNodeRef();
						}


						if (nodeRef != null) {
							ContentReader reader = serviceRegistry.getContentService().getReader(nodeRef, ContentModel.PROP_CONTENT);
							if (reader == null) {
								return null;
							}

							String mimetype = reader.getMimetype();
							long expectedLength = reader.getContentData().getSize();

							resp.setContentType((mimetype != null) ? mimetype : "application/octet-stream");
							// important: use the addHeader method here since the length method only accepts int which overflows for long values (> 4 GB)
							resp.addHeader("Content-Length", Long.toString(expectedLength));

							//
							// Stream to the requester.
							//
							long bytesWritten = 0;
							// try-with-resources: the previous code left the reader's InputStream open
							// whenever read()/write() threw below, leaking it (and whatever it holds
							// onto in the underlying content store) for the life of the request.
							try (DataInputStream in = new DataInputStream(reader.getContentInputStream())) {
								int length;
								byte[] bbuf = new byte[1024];
								while ((length = in.read(bbuf)) != -1) {
									op.write(bbuf, 0, length);
									bytesWritten += length;
								}
								op.flush();
							} catch (IOException e) {
								// The Content-Length header above is already committed to the client by
								// this point, so this failure surfaces to the caller as an abrupt
								// connection close (a "PrematureCloseException" or a content-length
								// mismatch downstream), not as a clean error status - this log is the
								// only place the actual cause is visible.
								logger.error("Failed to stream content for nodeId=" + nodeId + " after writing "
										+ bytesWritten + " of " + expectedLength + " expected bytes", e);
								throw e;
							}
							if (bytesWritten != expectedLength) {
								logger.warn("Streamed content size mismatch for nodeId=" + nodeId + ": declared="
										+ expectedLength + ", actual=" + bytesWritten);
							}
							op.close();
						}
					}
				}catch (Throwable t) {
					logger.error("Error delivering content for appId=" + appId + ", repId=" + repId
							+ ", nodeId=" + nodeId, t);
					throw new RuntimeException(t);
				}
				return null;
			});
		}catch(Throwable t) {
			throw new ServletException(t.getCause());
		}
		
		
	}
	
}
