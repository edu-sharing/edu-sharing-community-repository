import { UIConstants } from 'ngx-edu-sharing-ui';

/**
 * Route of the page shown while the backend is unreachable, relative to the base href.
 * The Frontend-Server sends browsers here (Frontend-Server/src/lib/error-response.ts,
 * `SERVICE_UNAVAILABLE_PATH`) - the page must work without any backend call.
 */
export const SERVICE_UNAVAILABLE_ROUTE = UIConstants.ROUTER_PREFIX + 'error/503';

/** Query parameter with the url to return to, set by the Frontend-Server. */
export const SERVICE_UNAVAILABLE_NEXT_PARAM = 'next';

/**
 * @param route the current path relative to the base href, e.g. `components/error/503`
 */
export function isServiceUnavailableRoute(route: string): boolean {
    return route.replace(/\/+$/, '') === SERVICE_UNAVAILABLE_ROUTE;
}

/**
 * The url to return to once the backend answers again: `next` if it is a path of this
 * app on this origin, otherwise the start page. Never the unavailable page itself.
 *
 * @param next the raw query parameter
 * @param baseUrl absolute url of the app, i.e. `document.baseURI`
 */
export function returnUrl(next: string | null | undefined, baseUrl: string): string {
    const base = new URL(baseUrl);
    if (!next || !next.startsWith('/') || next.startsWith('//') || /[\\\u0000-\u001f]/.test(next)) {
        return base.pathname;
    }
    let target: URL;
    try {
        target = new URL(next, base);
    } catch {
        return base.pathname;
    }
    if (
        target.origin !== base.origin ||
        !target.pathname.startsWith(base.pathname) ||
        isServiceUnavailableRoute(target.pathname.substring(base.pathname.length))
    ) {
        return base.pathname;
    }
    return target.pathname + target.search + target.hash;
}
