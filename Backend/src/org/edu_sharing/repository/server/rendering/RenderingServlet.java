package org.edu_sharing.repository.server.rendering;

import org.apache.log4j.Logger;
import org.edu_sharing.repository.server.tools.URLTool;
import org.edu_sharing.service.rendering.RenderingService;
import org.edu_sharing.service.rendering.RenderingServiceFactory;
import org.edu_sharing.service.rendering.RenderingTool;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class RenderingServlet extends HttpServlet {
    private static final String DEFAULT_DISPLAY_MODE = RenderingTool.DISPLAY_EMBED;
    Logger logger = Logger.getLogger(RenderingServlet.class);

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp)
                throws ServletException, IOException {
            String node_id = req.getParameter("node_id");
            String version = req.getParameter("version");
            RenderingService renderingService = RenderingServiceFactory.getLocalService();
            Map<String, String> params=new HashMap<>();
            for(Object key:  req.getParameterMap().keySet()){
                params.put((String)key,req.getParameter((String)key));
            }

            resp.getWriter().write("<html>");
            resp.getWriter().write("<head>");
            resp.getWriter().write("<style>");
            resp.getWriter().write("body,html{margin:0; padding:0;}");
            resp.getWriter().write("</style>");
            resp.getWriter().write("</head>");
            resp.getWriter().write("<body>");
            String response;
            try {
                response = renderingService.getDetails(node_id, version,DEFAULT_DISPLAY_MODE, params);
                response = response.replace("{{{LMS_INLINE_HELPER_SCRIPT}}}",URLTool.getNgRenderNodeUrl(node_id,version)+"?");
            } catch (Throwable t) {
                response = RenderingErrorServlet.errorToHTML(req,
                        new RenderingException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, t.getMessage(), RenderingException.I18N.unknown, t));
            }
            resp.setContentType("text/html");
            resp.getWriter().write(response);
            resp.getWriter().write("</body>");
            resp.getWriter().write("</html>");
        }
}
