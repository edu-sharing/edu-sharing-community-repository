package org.edu_sharing.service.monitoring;

import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

public class MonitoringServlet extends HttpServlet {

	Logger logger = Logger.getLogger(MonitoringServlet.class);
	/**
	 * 
	 */
	private static final long serialVersionUID = 8475506246212000449L;

	int defaultTimeoutSeconds = 30;
	
	
	
	
	@Override
	public void init(ServletConfig config) throws ServletException {
		// TODO Auto-generated method stub
		super.init(config);
	}
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		
		int timeout = defaultTimeoutSeconds;
		String paramTimeout = req.getParameter("timeout");
		if(paramTimeout != null 
				&& !paramTimeout.trim().equals("")) {
			try {
				timeout = Integer.parseInt(paramTimeout);
			}catch(NumberFormatException e) {
				logger.warn(paramTimeout + " is not an int");
			}
		}
		
		String paramMode = req.getParameter("mode");
		if(paramMode == null || 
				(!paramMode.equals(Monitoring.Modes.SEARCH.name()) && !paramMode.equals(Monitoring.Modes.SERVICE.name()))) {
			paramMode = Monitoring.Modes.SERVICE.name();
		}
		try {
			if(Monitoring.Modes.SERVICE.name().equals(paramMode)) {
				new Monitoring().alfrescoServicesCheckTimeout(timeout);
			}else {
				new Monitoring().alfrescoSearchEngineCheckTimeout(timeout);
			}
		} catch (Throwable e) {
			logger.error(e.getMessage(), e);
			resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
		}
	}
}
