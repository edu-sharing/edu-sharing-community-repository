import { Helper } from '../core-module/rest/helper';

/**
 * Query parameter carrying the target of an SSO login. The backend keeps it at the
 * SSO start and redirects there once the login is done - see
 * Backend/.../authentication/LoginRedirect.java.
 */
export const LOGIN_REDIRECT_PARAM = 'redirect';

/**
 * Appends the login target to an SSO start url, so the user lands on `next` (the deep
 * link the login was opened for) instead of the start page.
 *
 * Only a path of this app is passed - the backend rejects anything else - and only
 * to urls of this edu-sharing: `/shibboleth`, `/sso?id=...` or a configured
 * `loginUrl` on the same origin. External urls are returned unchanged.
 *
 * @param url the SSO start url, relative or absolute
 * @param next the target, relative or absolute; nothing is appended without one
 * @param serverUrl absolute url of this edu-sharing, if it differs from the page origin
 */
export function withLoginRedirect(
    url: string,
    next: string | null | undefined,
    serverUrl?: string,
): string {
    if (!url || !next) {
        return url;
    }
    let start: URL;
    let target: URL;
    try {
        start = new URL(url, window.location.href);
        target = new URL(next, window.location.href);
    } catch {
        return url;
    }
    const ownOrigins = [window.location.origin];
    if (serverUrl) {
        try {
            ownOrigins.push(new URL(serverUrl).origin);
        } catch {
            // ignore an unparsable server url
        }
    }
    if (!ownOrigins.includes(start.origin) || !ownOrigins.includes(target.origin)) {
        return url;
    }
    if (start.searchParams.has(LOGIN_REDIRECT_PARAM)) {
        return url;
    }
    return Helper.addGetParameter(LOGIN_REDIRECT_PARAM, target.pathname + target.search, url);
}
