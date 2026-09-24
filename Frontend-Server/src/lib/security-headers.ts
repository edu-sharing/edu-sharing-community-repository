/**
 * Port of `SecurityHeadersFilter.getConfiguredHeaders`
 * (Backend/services/core/.../repository/server/SecurityHeadersFilter.java): the
 * headers of every shell page, built from lightbend `angular.headers`.
 *
 * Like the filter, only these headers are taken from the config - anything else
 * configured below `angular.headers` is ignored.
 */

const NONCE_PLACEHOLDER = '{{ngCspNonce}}';
const CSP = 'Content-Security-Policy';
const PLAIN_HEADERS = ['X-XSS-Protection', 'X-Frame-Options', 'Referrer-Policy'] as const;

/**
 * @param config `angular.headers`, as delivered in the internal config
 * @param nonce the nonce of this response, the same one substituted into index.html
 */
export function securityHeaders(config: Readonly<Record<string, unknown>>, nonce: string): Record<string, string> {
    const headers: Record<string, string> = {};
    for (const name of PLAIN_HEADERS) {
        const value = config[name];
        if (typeof value === 'string') {
            headers[name] = value;
        }
    }
    const policy = config[CSP];
    if (typeof policy === 'object' && policy !== null && !Array.isArray(policy)) {
        // "directive value; " per entry, trailing separator included - byte identical to the filter
        headers[CSP] = Object.entries(policy)
            .map(([directive, value]) => `${directive} ${String(value).replaceAll(NONCE_PLACEHOLDER, nonce)}; `)
            .join('');
    }
    return headers;
}
