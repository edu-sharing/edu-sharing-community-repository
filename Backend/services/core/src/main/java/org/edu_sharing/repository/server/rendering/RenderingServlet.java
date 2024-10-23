package org.edu_sharing.repository.server.rendering;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.permissions.AccessDeniedException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.SecurityHeadersFilter;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.repository.tools.URLHelper;
import org.edu_sharing.service.InsufficientPermissionException;
import org.edu_sharing.service.config.ConfigServiceFactory;
import org.edu_sharing.service.nodeservice.NodeServiceHelper;
import org.edu_sharing.service.permission.PermissionChecking;
import org.edu_sharing.service.permission.PermissionServiceFactory;
import org.edu_sharing.service.rendering.RenderingService;
import org.edu_sharing.service.rendering.RenderingServiceFactory;
import org.edu_sharing.service.rendering.RenderingTool;
import org.edu_sharing.service.tracking.TrackingService;
import org.edu_sharing.service.tracking.TrackingServiceFactory;
import org.springframework.web.servlet.support.RequestContextUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class RenderingServlet extends HttpServlet {
    private static final String DEFAULT_DISPLAY_MODE = RenderingTool.DISPLAY_EMBED;
    private static Logger logger = Logger.getLogger(RenderingServlet.class);
    private static PermissionChecking permissionChecking;

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp)
                throws ServletException, IOException {

            if(permissionChecking == null) {
                // @TODO: Is there a more streamlined way?
                permissionChecking = RequestContextUtils.findWebApplicationContext(req).getBean("permissionChecking", PermissionChecking.class);
            }

            // new, preferred parameter
            String node_id = req.getParameter("nodeId");
            if(node_id == null) {
                // deprecated parameter
                node_id = req.getParameter("node_id");
            }
            String version = req.getParameter("version");

            RenderingService renderingService = RenderingServiceFactory.getLocalService();
            Map<String, String> params=new HashMap<>();
            for(Object key:  req.getParameterMap().keySet()){
                params.put((String)key,req.getParameter((String)key));
            }

            resp.getWriter().write("<html>");
            resp.getWriter().write("<head>");
            // hack for renderer
            resp.getWriter().write("<es-app ngCspNonce=\"" + SecurityHeadersFilter.ngCspNonce.get() + "\"></es-app>");
            resp.getWriter().write("<style nonce=\"" +SecurityHeadersFilter.ngCspNonce.get() + "\">");
            resp.getWriter().write("body,html{margin:0; padding:0;}");
            try {
                String customCSS = ConfigServiceFactory.getCurrentConfig().values.customCSS;
                if(!StringUtils.isBlank(customCSS)) {
                    resp.getWriter().write(customCSS);
                }
            } catch (Exception e) {
                logger.warn("Could not resolve config", e);
            }

            resp.getWriter().write("</style>");
            resp.getWriter().write("</head>");
            resp.getWriter().write("<body class= \"eduservlet-render-body\">");
            String response;
            try {
                /*if(!PermissionServiceFactory.getLocalService().hasPermission(StoreRef.PROTOCOL_WORKSPACE,
                        StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(),
                        node_id,
                        CCConstants.PERMISSION_EMBED)){
                     throw new AccessDeniedException(CCConstants.PERMISSION_EMBED);
                }*/
                String original = checkAndGetCollectionRef(node_id);
                if(original != null) {
                    response = AuthenticationUtil.runAsSystem(() -> renderInternal(renderingService,original,version,params));
                }else {
                    response = renderInternal(renderingService,original,version,params);
                }


            } catch (Throwable t) {
                RenderingException exception = RenderingException.fromThrowable(t);
                response = RenderingErrorServlet.errorToHTML(req,
                        exception);
                resp.setStatus(exception.getStatusCode());
            }
            resp.setContentType("text/html");
            resp.getWriter().write(response);
            resp.getWriter().write("</body>");
            resp.getWriter().write("</html>");
        }

        private String renderInternal(RenderingService renderingService, String node_id, String version, Map<String, String> params) throws Exception {
            String response = renderingService.getDetails(ApplicationInfoList.getHomeRepository().getAppId(), node_id, version,DEFAULT_DISPLAY_MODE, params).getDetails();
            response = response.replace("{{{LMS_INLINE_HELPER_SCRIPT}}}", URLHelper.getNgRenderNodeUrl(node_id,version)+"?");
            // add nonce to render styles
            response = response.replace("<style", "<style nonce=\"" +SecurityHeadersFilter.ngCspNonce.get() + "\"");
            TrackingServiceFactory.getTrackingService().trackActivityOnNode(new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, node_id), null, TrackingService.EventType.VIEW_MATERIAL_EMBEDDED);
            return response;
        }


    private static String checkAndGetCollectionRef(String nodeId) throws InsufficientPermissionException {
        NodeRef ref = new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, nodeId);
        if(NodeServiceHelper.hasAspect(ref,CCConstants.CCM_ASPECT_COLLECTION_IO_REFERENCE)){
            NodeServiceHelper.validatePermissionRestrictedAccess(ref, CCConstants.PERMISSION_READ_ALL, CCConstants.PERMISSION_EMBED);
            return NodeServiceHelper.getProperty(ref, CCConstants.CCM_PROP_IO_ORIGINAL);
        } else {
            permissionChecking.checkNodePermissions(nodeId, new String[]{CCConstants.PERMISSION_READ_ALL, CCConstants.PERMISSION_EMBED});
        }
        return null;
    }
}
