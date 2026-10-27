package org.edu_sharing.repository.server.sitemap;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.edu_sharing.alfresco.lightbend.LightbendConfigLoader;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.edu_sharing.spring.servlet.SpringHttpServlet;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;


@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
public class RobotsTXTServlet extends SpringHttpServlet {

    private static Logger logger = Logger.getLogger(RobotsTXTServlet.class);

    @Autowired
    private LightbendConfigLoader lightbendConfigLoader;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        try {

            String robots = StringUtils.join(lightbendConfigLoader.getConfig().getStringList("angular.robots"), "\n");
            robots = robots.replace("{{sitemap}}",req.getRequestURL().toString().replace("robots.txt","eduservlet/sitemap"));
            resp.getOutputStream().print(robots);
        } catch (Throwable t) {
            t.printStackTrace();
            resp.sendError(500, t.toString());
        }
    }

}
