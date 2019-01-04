package org.edu_sharing.repository.server;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.repository.server.tools.LRMITool;
import org.json.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.net.URL;

public class NgServlet extends HttpServlet {
	private static Logger logger = Logger.getLogger(NgServlet.class);

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		try {
			ApplicationInfo home = ApplicationInfoList.getHomeRepository();
			String head=home.getCustomHtmlHeaders();
			File index=new File(req.getSession().getServletContext().getRealPath("index.html"));
			String html=FileUtils.readFileToString(index);
			if(head!=null) {
				html = addToHead(head, html);
			}
			URL url = new URL(req.getRequestURL().toString());
			if(url.getPath().contains("components/render/")){
				html = addLRMI(html,url);
			}
			if(req.getHeader("User-Agent")!=null){
				if(req.getHeader("User-Agent").contains("cordova / edu-sharing-app"){
                    String platform="";
                    if(req.getHeader("User-Agent").contains("ios"))
                        platform="ios";
                    if(req.getHeader("User-Agent").contains("android"))
                        platform="android";
                    html=addToHead("<script type=\"text/javascript\" src=\"assets/cordova/"+platform+"/cordova.js\"></script>",html);
                    logger.info("cordova app, add cordova.js to header");
                }
				if (req.getHeader("User-Agent").contains("ionic / edu-sharing-app")) {
					// when using ionic, a local webserver is running on 54361 which will serve the bridge files!
					html =addToHead("<script type=\"text/javascript\" src=\"http://localhost:54361/cordova.js\"></script>",html);
					logger.info("ionic app, add cordova.js to header");
				}
			}
			resp.setHeader("Content-Type","text/html");
			resp.getOutputStream().write(html.getBytes("UTF-8"));
		}catch(Throwable t) {
			t.printStackTrace();
			resp.sendError(500, "Fatal error preparing index.html: "+t.getMessage());
		}
	}

	private String addLRMI(String html, URL url) {
		try {
			String[] path = url.getPath().split("/");
			String nodeId = path[path.length - 1];
			JSONObject lrmi = LRMITool.getLRMIJson(nodeId);
			String data = "<script type=\"application/ld+json\">";
			data += lrmi.toString(2);
			data += "</script>";
			return addToHead(data, html);
		}catch(Throwable t){
			logger.error("Failed to load node properties for attaching LRMI data:");
			t.printStackTrace();
		}
		return html;
	}

	private String addToHead(String head, String html) {
		int pos=html.indexOf("</head>");
		html=html.substring(0,pos)+head+html.substring(pos);
		return html;
	}
}
