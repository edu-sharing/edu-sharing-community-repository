package org.edu_sharing.repository.server.sitemap;

import org.apache.log4j.Logger;
import org.springframework.util.StreamUtils;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.Charset;


public class RobotsTXTServlet extends HttpServlet{

    Logger logger = Logger.getLogger(RobotsTXTServlet.class);
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        try {

            String robots=StreamUtils.copyToString(getClass().getResourceAsStream("robots.txt.properties"), Charset.forName("UTF-8"));
            robots=robots.replace("{{sitemap}}",req.getRequestURL().toString().replace("robots.txt","eduservlet/sitemap"));
            resp.getOutputStream().print(robots);
        } catch (Throwable t) {
            t.printStackTrace();
            resp.sendError(500, t.toString());
        }
    }

}
