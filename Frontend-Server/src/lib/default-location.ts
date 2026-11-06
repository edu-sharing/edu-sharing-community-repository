/**
 * `AuthenticationFilter.redirectToDefaultLocation`: the bare entry point (`CONTEXT_PATH/`)
 * goes to `components/<defaultLocation>`, or to `defaultLocation` itself when it is an
 * absolute url. Without a config - backend down, older backend - to the login, like the
 * filter's fallback. The angular root route relies on this in production builds
 * (`StartupComponent` does nothing there).
 *
 * @param defaultLocation `current.defaultLocation` of the backend config (default `login`)
 */
export function defaultLocationUrl(contextPath: string, defaultLocation: string | undefined): string {
    const location = defaultLocation?.trim() || 'login';
    if (location.includes('://')) {
        return location;
    }
    return `${contextPath}/components/${location.replace(/^\/+/, '')}`;
}
