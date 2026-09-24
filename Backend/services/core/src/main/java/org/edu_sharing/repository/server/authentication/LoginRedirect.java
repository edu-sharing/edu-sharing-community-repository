package org.edu_sharing.repository.server.authentication;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.apache.commons.lang3.StringUtils;

/**
 * Where to go after an SSO login.
 * <p>
 * The angular app passes the target explicitly when it starts an SSO login
 * ({@code /shibboleth?redirect=...}, {@code /sso?id=...&redirect=...}).
 * {@link LoginRedirectFilter} keeps it in the session on that first request, since
 * the round trip over the IdP loses the query; {@link ShibbolethServlet}, where every
 * SSO flow ends, resolves it.
 * <p>
 * Only paths below the context path are accepted - anything else would turn the
 * parameter into an open redirect.
 */
public final class LoginRedirect {
    public static final String PARAM = "redirect";
    public static final String SESSION_ATTRIBUTE = "LOGIN_REDIRECT_TARGET";
    private static final int MAX_LENGTH = 4096;

    private LoginRedirect() {
    }

    /**
     * @return the target when it is a path below the context path, otherwise null
     */
    public static String validate(HttpServletRequest request, String target) {
        if (StringUtils.isBlank(target) || target.length() > MAX_LENGTH) {
            return null;
        }
        // a relative path of this app only: no scheme, no protocol relative "//host",
        // no backslash (browsers read "/\host" as "//host"), no header injection
        if (!target.startsWith(request.getContextPath() + "/")
                || target.startsWith("//")
                || target.contains("\\")
                || target.contains("../")
                || target.chars().anyMatch(c -> c < 0x20 || c == 0x7f)) {
            return null;
        }
        return target;
    }

    /** Keeps a valid {@link #PARAM} of the request in the session, see {@link LoginRedirectFilter}. */
    public static void remember(HttpServletRequest request) {
        String target = validate(request, request.getParameter(PARAM));
        if (target != null) {
            request.getSession(true).setAttribute(SESSION_ATTRIBUTE, target);
        }
    }

    /**
     * The target after a login, in this order: the parameter of the request, the one
     * remembered at the start of the SSO flow, the one of a silent login
     * ({@link AuthenticationFilter#LOGIN_SUCCESS_REDIRECT_URL}).
     * The remembered target is used once.
     *
     * @return null when there is none
     */
    public static String resolve(HttpServletRequest request) {
        String target = validate(request, request.getParameter(PARAM));
        HttpSession session = request.getSession(false);
        if (session == null) {
            return target;
        }
        if (target == null) {
            target = (String) session.getAttribute(SESSION_ATTRIBUTE);
        }
        session.removeAttribute(SESSION_ATTRIBUTE);
        if (target == null) {
            target = (String) session.getAttribute(AuthenticationFilter.LOGIN_SUCCESS_REDIRECT_URL);
        }
        return target;
    }
}
