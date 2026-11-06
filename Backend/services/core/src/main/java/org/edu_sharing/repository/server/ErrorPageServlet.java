package org.edu_sharing.repository.server;

import org.apache.log4j.Logger;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.edu_sharing.spring.servlet.SpringHttpServlet;

/**
 * Target of the web.xml {@code <error-page>}s: every {@code sendError} and every exception that
 * did not pass the {@link ErrorFilter} ends up here (ERROR dispatch) and is answered like the
 * ErrorFilter does - status kept, browsers sent to the error page of the app, json / plain text
 * for everything else.
 */
public class ErrorPageServlet extends SpringHttpServlet {
	private static final Logger logger = Logger.getLogger(ErrorPageServlet.class);

	/**
	 * Every method, the error dispatch keeps the one of the failed request. Besides the standard
	 * methods this covers the WebDAV ones (PROPFIND, LOCK, ...): HttpServlet would answer them with
	 * 501 instead of the original status, which breaks the windows WebDAV client on a 404.
	 */
	@Override
	protected void service(HttpServletRequest req, HttpServletResponse resp) {
		handleRequest(req, resp);
	}

	/**
	 * edu-sharing fix: true when the request that failed was addressed to the WebDAV servlet, which
	 * web.xml maps to /webdav/*. During an error dispatch the original URI is only reachable as a
	 * request attribute.
	 */
	static boolean isWebDAVRequest(HttpServletRequest req) {
		Object errorUri = req.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
		String uri = (errorUri != null) ? errorUri.toString() : req.getRequestURI();
		if (uri == null) {
			return false;
		}
		String webdavPath = req.getContextPath() + "/webdav";
		return uri.equals(webdavPath) || uri.startsWith(webdavPath + "/");
	}

	static void handleRequest(HttpServletRequest req, HttpServletResponse resp) {
		if (isWebDAVRequest(req)) {
			// edu-sharing fix: a WebDAV client can do nothing with an HTML or JSON error page, and
			// producing one actively destroys the answer the WebDAV servlet already assembled:
			// ErrorFilter.handleError() calls resp.reset() for every client that does not ask for
			// text/html, which drops the status code and every response header - among them the
			// X-MSDAVEXT_ERROR that carries the reason a rejected upload failed. Returning right
			// here leaves that response untouched.
			return;
		}
		try {
			Throwable exception = req.getAttribute(RequestDispatcher.ERROR_EXCEPTION) instanceof Throwable t ? t : null;
			if (exception != null) {
				logger.info("Internal exception: " + exception.getMessage(), exception);
			}
			Object message = req.getAttribute(RequestDispatcher.ERROR_MESSAGE);
			ErrorFilter.handleError(req, resp,
					new Throwable(message != null ? message.toString() : exception != null ? exception.getMessage() : null),
					statusCode(req.getAttribute(RequestDispatcher.ERROR_STATUS_CODE), exception)
			);
		} catch (RuntimeException e) {
			logger.error("Fatal error preparing the error page", e);
			if (!resp.isCommitted()) {
				resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			}
		}
	}

	/**
	 * the status of the error dispatch, else the one the exception maps to, else 500 - a direct
	 * request (no error dispatch) has neither
	 */
	static int statusCode(Object statusCode, Throwable exception) {
		if (statusCode instanceof Integer code) {
			return code;
		}
		if (statusCode != null) {
			try {
				return Integer.parseInt(statusCode.toString());
			} catch (NumberFormatException ignored) {
				// fall through
			}
		}
		if (exception instanceof ErrorFilter.ErrorFilterException e) {
			return e.getStatusCode();
		}
		return HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
	}
}
