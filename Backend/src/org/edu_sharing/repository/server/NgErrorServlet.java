package org.edu_sharing.repository.server;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.edu_sharing.repository.client.tools.UrlTool;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.repository.server.tools.LRMITool;
import org.edu_sharing.repository.server.tools.URLTool;
import org.json.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.net.URL;

public class NgErrorServlet extends HttpServlet {
	private static Logger logger = Logger.getLogger(NgErrorServlet.class);

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		try {
			Object errorCode=req.getAttribute("javax.servlet.error.status_code");
			resp.sendRedirect(URLTool.getNgErrorUrl(errorCode.toString()));
		}catch(Throwable t) {
			t.printStackTrace();
			resp.sendError(500, "Fatal error preparing error.html: "+t.getMessage());
		}
	}
}
