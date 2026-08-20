package org.edu_sharing.repository.server.rendering;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.permissions.AccessDeniedException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.edu_sharing.alfresco.RestrictedAccessException;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.SecurityHeadersFilter;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.repository.tools.URLHelper;
import org.edu_sharing.restservices.NodeDao;
import org.edu_sharing.restservices.RepositoryDao;
import org.edu_sharing.restservices.shared.Node;
import org.edu_sharing.restservices.shared.SignedNode;
import org.edu_sharing.service.config.ConfigServiceFactory;
import org.edu_sharing.service.nodeservice.NodeServiceHelper;
import org.edu_sharing.service.permission.PermissionServiceFactory;
import org.edu_sharing.service.rendering.RenderingService;
import org.edu_sharing.service.rendering.RenderingServiceFactory;
import org.edu_sharing.service.rendering.RenderingTool;
import org.edu_sharing.service.tracking.ActivityEventService;
import org.edu_sharing.service.tracking.ActivityOnNodeEventType;
import org.edu_sharing.spring.servlet.SpringHttpServlet;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
public class RenderingServlet extends SpringHttpServlet {
    private static final String DEFAULT_DISPLAY_MODE = RenderingTool.DISPLAY_EMBED;
    private static final Logger logger = Logger.getLogger(RenderingServlet.class);

    @Autowired
    private transient ActivityEventService activityEventService;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // new, preferred parameter
        String node_id = req.getParameter("nodeId");
        if (node_id == null) {
            // deprecated parameter
            node_id = req.getParameter("node_id");
        }
        String version = req.getParameter("version");

        ApplicationInfo rs2 = ApplicationInfoList.getRenderingService2();
        RenderingService renderingService = RenderingServiceFactory.getInstance().getLocalService();
        Map<String, String> params = new HashMap<>();
        for (Object key : req.getParameterMap().keySet()) {
            params.put((String) key, req.getParameter((String) key));
        }

        String nonce = SecurityHeadersFilter.ngCspNonce.get();
        resp.getWriter().write("<html>");
        resp.getWriter().write("<head>");
        try {
            resp.getWriter().write("<style nonce=\"" + nonce + "\">");
            String customCSS = ConfigServiceFactory.getCurrentConfig().values.customCSS;
            if (!StringUtils.isBlank(customCSS)) {
                resp.getWriter().write(customCSS);
            }
            resp.getWriter().write("</style>");
        } catch (Exception e) {
            logger.warn("Could not resolve config", e);
        }

        if (rs2 != null) {
            String webComponentBase = URLHelper.getBaseUrlFromRequest(req) + "/web-components/rendering-service/";
            String apiUrl = URLHelper.getBaseUrlFromRequest(req) + "/rest";
            resp.getWriter().write("<script nonce=\"" + nonce + "\">");
            resp.getWriter().write("window.__env={EDU_SHARING_API_URL:'" + apiUrl + "'};");
            resp.getWriter().write("window.__EDUSHARING_PUBLIC_PATH__='" + webComponentBase + "';");
            resp.getWriter().write("</script>");
            resp.getWriter().write("<script src=\"" + webComponentBase + "main.js\" type=\"module\"></script>");
            resp.getWriter().write("<link rel=\"stylesheet\" href=\"" + webComponentBase + "styles.css\"/>");
        } else {
            // hack for renderer
            resp.getWriter().write("<es-app ngCspNonce=\"" + nonce + "\"></es-app>");
            resp.getWriter().write("<style nonce=\"" + nonce + "\">");
            resp.getWriter().write("body,html{margin:0; padding:0;}");
            resp.getWriter().write("</style>");
        }
        resp.getWriter().write("</head>");
        resp.getWriter().write("<body class= \"eduservlet-render-body\">");
        /*
         * angular resolves the CSP_NONCE token via its default factory, which looks up
         * `document.body.querySelector('[ngCspNonce]')`. Without it, angular itself and the material cdk
         * (MediaMatcher) inject <style> elements without a nonce, which are blocked by our style-src policy.
         * The marker must be inside the body (not the head) and must be present regardless of whether the
         * rendering content or an error page follows below.
         */
        resp.getWriter().write("<div ngCspNonce=\"" + nonce + "\" hidden></div>");
        String response;
        try {
            NodeRef ref = new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, node_id);
            if (NodeServiceHelper.hasAspect(ref, CCConstants.CCM_ASPECT_COLLECTION_IO_REFERENCE)) {
                try {
                    NodeServiceHelper.validatePermissionRestrictedAccess(ref, CCConstants.PERMISSION_READ_ALL, CCConstants.PERMISSION_EMBED);
                } catch (RestrictedAccessException e) {
                    throw new AccessDeniedException(CCConstants.PERMISSION_EMBED);
                }
            } else {
                if (!PermissionServiceFactory.getInstance().getLocalService().hasPermission(ref.getStoreRef().getProtocol(),
                        ref.getStoreRef().getIdentifier(),
                        node_id,
                        CCConstants.PERMISSION_EMBED)) {
                    throw new AccessDeniedException(CCConstants.PERMISSION_EMBED);
                }
            }

            if (rs2 != null) {
                String webComponentBase = URLHelper.getBaseUrlFromRequest(req) + "/web-components/rendering-service/";
                NodeDao nodeDao = NodeDao.getNode(RepositoryDao.getHomeRepository(), node_id);
                // build the node only once, it is expensive
                Node nodeConverted = nodeDao.asNode();
                SignedNode signedNode = NodeDao.getSignedNode(nodeConverted);
                String encodedNode = Base64.getEncoder().encodeToString(signedNode.getNode().getBytes());
                String encodedSignature = Base64.getEncoder().encodeToString(signedNode.getSignature());
                String jwt = NodeDao.getJWT(nodeConverted);

                response = "<style nonce=\"" + nonce + "\">"
                        // max height: full viewport minus the static bar at the bottom
                        + "edu-sharing-render { --containerHeight: calc(100vh - 60px); }"
                        + "</style>"
                        + "<edu-sharing-render"
                        + " ngCspNonce=\"" + nonce + "\""
                        + " encoded_node=\"" + encodedNode + "\""
                        + " signature=\"" + encodedSignature + "\""
                        + " jwt=\"" + jwt + "\""
                        + " render_url=\"" + rs2.getContentUrl() + "\""
                        + " assets_url=\"" + webComponentBase + "/assets\""
                        + " signature_algorithm=\"" + signedNode.getSignatureAlgorithm() + "\""
                        + "></edu-sharing-render>";
            } else {
                response = renderingService.getDetails(ApplicationInfoList.getHomeRepository().getAppId(), node_id, version, DEFAULT_DISPLAY_MODE, params).getDetails();
                response = response.replace("{{{LMS_INLINE_HELPER_SCRIPT}}}", URLHelper.getNgRenderNodeUrl(node_id, version, true) + "?");
                // add nonce to render styles
                response = response.replace("<style", "<style nonce=\"" + nonce + "\"");
                // in rs2, tracking is done client-side!
                activityEventService.trackActivityOnNode(new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, node_id), null, ActivityOnNodeEventType.VIEW_MATERIAL_EMBEDDED, AuthenticationUtil.getFullyAuthenticatedUser());
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
}