package org.edu_sharing.spring.security.oauth2;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.HttpMethod;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.edu_sharing.alfresco.lightbend.LightbendConfigLoader;
import org.edu_sharing.alfresco.policy.NodeCustomizationPolicies;
import org.edu_sharing.alfresco.service.config.model.Config;
import org.edu_sharing.alfresco.service.config.model.LoginSilentMode;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.authentication.AuthenticationFilter;
import org.edu_sharing.repository.server.authentication.ContextManagementFilter;
import org.edu_sharing.service.config.ConfigServiceFactory;
import org.edu_sharing.spring.ApplicationContextFactory;
import org.edu_sharing.spring.security.oauth2.config.OAuth2ClientProperties;
import org.edu_sharing.spring.security.oauth2.config.OAuth2ConfigProvider;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;


@RequiredArgsConstructor
@Slf4j
@Service
public class SilentLoginModeRedirect {
    public static String SESS_ATT_SILENT_LOGIN_TARGET = "SILENT_LOGIN_TARGET";
    public static String SESS_ATT_SILENT_LOGIN_RESULT = "SILENT_LOGIN_RESULT";

    private static final Pattern CRAWLER_USER_AGENT = Pattern.compile(
            "\\bbot\\b|[a-z]bot\\b|crawl|spider|slurp|mediapartners|facebookexternalhit|embedly",
            Pattern.CASE_INSENSITIVE);

    private static final String CONFIG_SKIP_USER_AGENTS = "security.sso.silentLoginSkipUserAgents";
    private static final int MIN_NAME_LENGTH = 3;
    private static final Set<String> IGNORED_NAMES_LOGGED = ConcurrentHashMap.newKeySet();

    /**
     * the user agents of the common browsers, a name of the config must not match one of them
     */
    private static final List<String> BROWSER_USER_AGENTS = List.of(
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36 Edg/126.0.0.0",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:128.0) Gecko/20100101 Firefox/128.0",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.5 Safari/605.1.15",
            "Mozilla/5.0 (iPhone; CPU iPhone OS 17_5 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.5 Mobile/15E148 Safari/604.1",
            "Mozilla/5.0 (Linux; Android 14; SM-S918B) AppleWebKit/537.36 (KHTML, like Gecko) SamsungBrowser/25.0 Chrome/121.0.0.0 Mobile Safari/537.36",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36 OPR/112.0.0.0",
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36");

    private final OAuth2ConfigProvider configService;
    private final LightbendConfigLoader configLoader;

    public boolean process(HttpServletRequest request, HttpServletResponse response) throws Exception {

        if (!checkConditions(request, response)) {
            log.debug("conditions not given");
            return false;
        }

        //http://localhost:8080/edu-sharing/components/login?local=true
        if (isLocalLoginForced(request)) {
            return false;
        }

        // preview redirect is problematic since browser seem to load random images from cache on 302 responses
        if (request.getServletPath().equals("/rest") || request.getServletPath().equals("/preview")) {
            log.debug("servlet path {} is not allowed",request.getServletPath());
            return false;
        }

        // machine endpoints and crawlers keep no session, they would loop between the redirects and never get the content
        if (isMachineEndpoint(request.getServletPath(), request.getPathInfo())) {
            log.debug("machine endpoint {}{} is not allowed", request.getServletPath(), request.getPathInfo());
            return false;
        }
        if (isSkippedClient(request.getHeader("User-Agent"))) {
            log.debug("client {} is not allowed", request.getHeader("User-Agent"));
            return false;
        }

        // check other authentication methods
        if(request.getParameter("ticket") != null
                || request.getHeader("Authorization") != null
                || ContextManagementFilter.accessTool.get() != null
                || request.getParameter(CCConstants.REQUEST_PARAM_ACCESSTOKEN) != null){
            log.debug("another auth method forced");
            return false;
        }


        String result = (String) request.getSession().getAttribute(SESS_ATT_SILENT_LOGIN_RESULT);
        if (result != null) {
            log.debug(SESS_ATT_SILENT_LOGIN_RESULT + ": "+result);
            request.getSession().removeAttribute(SESS_ATT_SILENT_LOGIN_RESULT);
            return false;
        }

        String targetPath = request.getContextPath()
                + request.getServletPath()
                + (request.getPathInfo() != null ? request.getPathInfo() : "")
                + (request.getQueryString() != null ? ("?" + request.getQueryString()) : "");
        request.getSession().setAttribute(SESS_ATT_SILENT_LOGIN_TARGET, targetPath);

        if(targetPath.contains("components/error")){
            log.debug("target path "+targetPath);
            return false;
        }

        log.debug("SILENT_LOGIN_TARGET:"+request.getSession().getAttribute(SESS_ATT_SILENT_LOGIN_TARGET));
        //response.sendRedirect(request.getContextPath() + SilentLoginAuthorizationRequestResolver.DEFAULT_SILENT_LOGIN_PATH);

        String context = NodeCustomizationPolicies.getEduSharingContext();
        OAuth2ClientProperties config = configService.getConfig(context);
        String registrationKey = config.getDefaultRegistration();

        if (StringUtils.isBlank(registrationKey)) {
            return false;
        }

        response.sendRedirect("/edu-sharing/oauth2/authorization/"+config.getRegistrationId(registrationKey)+"?prompt=none");
        return true;
    }

    /**
     * Endpoints that are only consumed by machines, an interactive login makes no sense there.
     * The servlets are mapped to the exact path (e.g. /eduservlet/sitemap), then the whole path is the servlet path
     * and there is no path info. With a mapping to a path prefix it is split into both, so they are joined.
     */
    static boolean isMachineEndpoint(String servletPath, String pathInfo) {
        String path = (servletPath == null ? "" : servletPath) + (pathInfo == null ? "" : pathInfo);
        return path.equals("/eduservlet/sitemap")
                || path.equals("/eduservlet/oembed")
                || path.equals("/eduservlet/oai") || path.startsWith("/eduservlet/oai/")
                || path.equals("/robots.txt");
    }

    /**
     * common crawlers (Googlebot, bingbot, ...) and link preview bots
     */
    static boolean isCrawler(String userAgent) {
        return userAgent != null && CRAWLER_USER_AGENT.matcher(userAgent).find();
    }

    /**
     * A client that never gets the silent login redirect: a crawler of the common patterns, or one of the explicit
     * names of the config (security.sso.silentLoginSkipUserAgents). It keeps no session, so it would loop between the
     * redirects. Everything else, a browser in the first place, is not touched.
     */
    boolean isSkippedClient(String userAgent) {
        return isCrawler(userAgent) || matchesName(userAgent, getSkipUserAgents());
    }

    private List<String> getSkipUserAgents() {
        com.typesafe.config.Config config = configLoader.getConfig();
        return config.hasPath(CONFIG_SKIP_USER_AGENTS) ? config.getStringList(CONFIG_SKIP_USER_AGENTS) : List.of();
    }

    /**
     * Whether the user agent contains one of the names (not case sensitive).
     * A name that would match a browser is ignored (and logged once): a typo in the config like "Mozilla" or "Chrome"
     * would switch the silent login off for every browser. So are blank names and names of less than three characters.
     */
    static boolean matchesName(String userAgent, Collection<String> names) {
        if (userAgent == null || userAgent.isBlank() || names == null) {
            return false;
        }
        String lowerUserAgent = userAgent.toLowerCase(Locale.ROOT);
        for (String name : names) {
            String lowerName = name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
            if (lowerName.length() < MIN_NAME_LENGTH || matchesABrowser(lowerName)) {
                if (IGNORED_NAMES_LOGGED.add(lowerName)) {
                    log.warn("Ignoring '{}' in {}: it is blank, too short or part of the user agent of a browser", name, CONFIG_SKIP_USER_AGENTS);
                }
                continue;
            }
            if (lowerUserAgent.contains(lowerName)) {
                return true;
            }
        }
        return false;
    }

    private static boolean matchesABrowser(String lowerName) {
        return BROWSER_USER_AGENTS.stream().anyMatch(browser -> browser.toLowerCase(Locale.ROOT).contains(lowerName));
    }

    private boolean isLocalLoginForced(HttpServletRequest request) {
        return request.getRequestURI().equals(request.getContextPath() + AuthenticationFilter.PATH_LOGIN_ANGULAR)
                && "true".equalsIgnoreCase(request.getParameter("local"));
    }

    public boolean processError(HttpServletRequest request, HttpServletResponse response) throws Exception {
        if (!checkConditions(request, response)) {
            log.debug("processError: conditions not given");
            return false;
        }
        String target = (String) request.getSession().getAttribute(SESS_ATT_SILENT_LOGIN_TARGET);
        if (target == null) {
            log.debug("processError: missing {} in session attributes",SESS_ATT_SILENT_LOGIN_TARGET);
            return false;
        }
        request.getSession().setAttribute(SESS_ATT_SILENT_LOGIN_RESULT, "login_required");
        log.debug("processError: redirecting to "+target);
        response.sendRedirect(target);
        request.getSession().removeAttribute(SESS_ATT_SILENT_LOGIN_TARGET);
        return true;
    }

    public boolean processSuccess(HttpServletRequest request, HttpServletResponse response) throws Exception {
        if (!checkConditions(request, response)) {
            log.debug("processSuccess: conditions not given");
            return false;
        }
        String target = (String) request.getSession().getAttribute(SESS_ATT_SILENT_LOGIN_TARGET);
        if (target == null) {
            log.debug("processSuccess: missing {}", SESS_ATT_SILENT_LOGIN_TARGET);
            return false;
        }

        request.getSession().setAttribute(AuthenticationFilter.LOGIN_SUCCESS_REDIRECT_URL, target);
        log.debug("processSuccess: set LOGIN_SUCCESS_REDIRECT_URL to {} and redirecting to /shibboleth", target);
        response.sendRedirect(request.getContextPath() + "/shibboleth");
        return true;
    }

    private boolean checkConditions(HttpServletRequest request, HttpServletResponse response) throws Exception {
        if (!HttpMethod.GET.equals(request.getMethod())) {
            return false;
        }

        if (!ApplicationContextFactory.getApplicationContext().getEnvironment()
                .acceptsProfiles(Profiles.of(SecurityConfigurationOAuth2.PROFILE_ID))) {
            return false;
        }

        Config config = ConfigServiceFactory.getCurrentConfig();
        return config == null || LoginSilentMode.redirect.equals(config.values.loginSilentMode);
    }
}
