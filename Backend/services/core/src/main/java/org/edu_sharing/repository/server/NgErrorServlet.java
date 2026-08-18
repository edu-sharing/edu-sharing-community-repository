package org.edu_sharing.repository.server;

import org.apache.log4j.Logger;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

public class NgErrorServlet extends HttpServlet {
	private static final Logger logger = Logger.getLogger(NgErrorServlet.class);

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		handleRequest(req, resp);
	}

	@Override
	protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		handleRequest(req, resp);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		handleRequest(req, resp);
	}

	@Override
	protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		handleRequest(req, resp);
	}

	/**
	 * edu-sharing fix: true when the request that failed was addressed to the WebDAV servlet, which
	 * web.xml maps to /webdav/*. During an error dispatch the original URI is only reachable as a
	 * request attribute.
	 */
	private static boolean isWebDAVRequest(HttpServletRequest req) {
		Object errorUri = req.getAttribute("jakarta.servlet.error.request_uri");
		String uri = (errorUri != null) ? errorUri.toString() : req.getRequestURI();
		if (uri == null) {
			return false;
		}
		String webdavPath = req.getContextPath() + "/webdav";
		return uri.equals(webdavPath) || uri.startsWith(webdavPath + "/");
	}

	private static void handleRequest(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		if (isWebDAVRequest(req)) {
			// edu-sharing fix: a WebDAV client can do nothing with an HTML or JSON error page, and
			// producing one actively destroys the answer the WebDAV servlet already assembled:
			// ErrorFilter.handleError() calls resp.reset() for every client that does not ask for
			// text/html, which drops the status code and every response header - among them the
			// X-MSDAVEXT_ERROR that carries the reason a rejected upload failed. Returning right
			// here leaves that response untouched. The forward itself is harmless, it only resets
			// the body buffer, and Tomcat marks the error as reported once we return normally, so
			// no ErrorReportValve output is appended either.
			return;
		}
		try {
			Object errorMessage= req.getAttribute("jakarta.servlet.error.message");
			Object errorCode= req.getAttribute("jakarta.servlet.error.status_code");
			Object ex = req.getAttribute("jakarta.servlet.error.exception");
			if(ex instanceof Throwable) {
				logger.info("Internal exception: " + ((Throwable) ex).getMessage(), (Throwable) ex);
			}
			ErrorFilter.handleError(req, resp, new Throwable(
							errorMessage.toString()),
					Integer.parseInt(errorCode.toString())
			);
		}catch(NullPointerException e) {
			try {
				Throwable t = (Throwable) req.getAttribute("jakarta.servlet.error.exception");
				logger.error(t);
			} catch(Throwable t){
				resp.sendError(500, "Fatal error preparing error.html: "+t.getMessage());
			}
		}catch(Throwable t) {
			logger.error(t);
			resp.sendError(500, "Fatal error preparing error.html: "+t.getMessage());
		}
	}

	@Override
	protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		String method = req.getMethod();
		//prevent webdav methods 404 is transformed to 501
		if ("PROPFIND".equals(method)
				|| "PROPPATCH".equals(method)
				|| "COPY".equals(method)
				|| "LOCK".equals(method)
				|| "MKCOL".equals(method)
				|| "MOVE".equals(method)
				|| "UNLOCK".equals(method)) {
			handleRequest(req, resp);
		}else {
			super.service(req, resp);
		}

	}
}
