package org.edu_sharing.repository.server;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;

public class NgServlet extends HttpServlet {
	private static Logger logger = Logger.getLogger(NgServlet.class);

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		try {
			ApplicationInfo home = ApplicationInfoList.getHomeRepository();
			String head=home.getCustomHtmlHeaders();
			File index=new File(req.getSession().getServletContext().getRealPath("index.html"));
			String html=FileUtils.readFileToString(index);
			int pos=html.indexOf("</head>");
			if(head!=null) {
				html=html.substring(0,pos)+head+html.substring(pos);
			}
			if(req.getHeader("User-Agent")!=null) {
				if (req.getHeader("User-Agent").contains("cordova / edu-sharing-app")) {
					String platform = "";
					if (req.getHeader("User-Agent").contains("ios"))
						platform = "ios";
					if (req.getHeader("User-Agent").contains("android"))
						platform = "android";
					html = html.substring(0, pos) +
							"<script type=\"text/javascript\" src=\"assets/cordova/" + platform + "/cordova.js\"></script>"
							+ html.substring(pos);
					logger.info("cordova app, add cordova.js to header");
				}
				if (req.getHeader("User-Agent").contains("ionic / edu-sharing-app")) {
					// when using ionic, a local webserver is running on 8080 which will serve the bridge files!
					html = html.substring(0, pos) +
							"<script type=\"text/javascript\" src=\"http://localhost:8080/cordova.js\"></script>"
							+ html.substring(pos);
					logger.info("ionic app, add cordova.js to header");
				}
			}
			resp.setHeader("Content-Type","text/html");
			resp.getOutputStream().print(html);
		}catch(Throwable t) {
			t.printStackTrace();
			resp.sendError(500, "Fatal error preparing index.html: "+t.getMessage());
		}
	}
}
