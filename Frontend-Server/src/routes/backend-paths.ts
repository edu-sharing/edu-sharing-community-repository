/**
 * Paths that belong to the Tomcat webapp, relative to CONTEXT_PATH.
 *
 * Derived from Backend/services/webapp/src/main/webapp/WEB-INF/web.xml: every
 * <servlet-mapping> that is not the `ng-index` servlet (NgServlet).
 *
 * Unlisted paths still reach the backend through the not-found fallback, they are
 * only slower and logged - so a gap here degrades performance, never correctness.
 *
 * That fallback is TRANSITIONAL. This list is meant to become exhaustive, after
 * which PROXY_UNMATCHED and the fallback go away and an unlisted path is a 404.
 * Every `unmatched path` warning in the logs is an entry that belongs here.
 */
export const BACKEND_PATHS: readonly string[] = [
    '/.well-known',
    '/appactivation',
    '/classic.html',
    '/content',
    '/eduservlet',
    '/graphql',
    '/index.jsp',
    '/jsp',
    '/login',
    '/logout',
    '/metadata',
    '/monitoring',
    '/node',
    '/oauth2',
    '/oauth2server',
    '/preview',
    '/rendering-error',
    '/renderingproxy',
    '/rest',
    '/restservice',
    '/robots.txt',
    '/saml2',
    '/schoolcontext.jsp',
    '/screenreader',
    '/search',
    '/services',
    '/servlet',
    '/share',
    '/shibboleth',
    '/sso',
    '/webdav',
];

/**
 * Methods beyond the standard ones, used by WebDAV clients on `/webdav` (see
 * `NgErrorServlet.service`, which answers them itself on errors). Registered with
 * fastify so the backend routes accept them.
 */
export const WEBDAV_METHODS: readonly string[] = ['PROPFIND', 'PROPPATCH', 'MKCOL', 'COPY', 'MOVE', 'LOCK', 'UNLOCK'];

/**
 * Paths the Angular router owns. Mirrors the `ng-index` servlet mappings in
 * web.xml (/index.html, /components/*, /v/*); these are the only paths that get
 * the SPA shell. Everything else that is not a file falls through to the backend,
 * exactly as it does today.
 */
export const ANGULAR_ROUTE_PREFIXES: readonly string[] = ['/components', '/v'];
