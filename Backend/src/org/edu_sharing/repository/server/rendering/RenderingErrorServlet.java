package org.edu_sharing.repository.server.rendering;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.edu_sharing.repository.server.tools.I18nServer;
import org.springframework.extensions.webscripts.Runtime;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;

public class RenderingErrorServlet extends HttpServlet {
    private static Logger logger = Logger.getLogger(RenderingErrorServlet.class);

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        RenderingException exception = (RenderingException) req
                .getAttribute("javax.servlet.error.exception");
        if(exception.getNested()!=null){
            logger.warn(exception.getNested());
        }
        String html = errorToHTML(req.getSession().getServletContext(), exception);

        resp.setHeader("Content-Type","text/html");
        resp.setStatus(exception.getStatusCode());
        resp.getOutputStream().write(html.getBytes("UTF-8"));
    }

    public static String errorToHTML(ServletContext context, RenderingException exception) {
        try {
            String exceptionName="";
            if(exception.getNested()!=null){
                exceptionName=exception.getNested().getClass().getSimpleName();
            }
            File index = new File(context.getRealPath("rendering-error.html"));
            String html = FileUtils.readFileToString(index);
            html = html.replace("{{statusCode}}", exception.getStatusCode() + "");
            html = html.replace("{{message}}", I18nServer.getTranslationDefaultResourcebundleNoException("rendering_error_" + exception.getI18nName()));
            html = html.replace("{{technicalMessage}}", exception.getTechnicalDetail());
            html = html.replace("{{exception}}", exceptionName);
            return html;
        }catch(IOException e){
            throw new RuntimeException(e);
        }
    }
}
