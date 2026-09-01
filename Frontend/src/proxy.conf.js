require('dotenv').config();

if (!process.env.BACKEND_URL) {
    throw new Error(
        'Missing environment variable `BACKEND_URL`.' +
            '\n\nTo get started, run' +
            '\n\n    cp .env.example .env' +
            '\n\nand edit `.env`.' +
            '\n',
    );
}

console.log('Starting proxy: ', process.env.BACKEND_URL, process.env.RS2_URL);

const LOCAL_URL = 'http://localhost:4200';

function mapCookies(proxyRes, extraReplacements = false) {
    const cookies = proxyRes.headers['set-cookie'];
    if (cookies) {
        proxyRes.headers['set-cookie'] = cookies.map((cookie) => {
            cookie = cookie
                // We serve on a non-HTTPS connection, so 'Secure' cookies won't work.
                .replace('; Secure', '')
                .replace('; Partitioned', '')
                // 'SameSite=None' is only allowed on 'Secure' cookies.
                .replace('; SameSite=None', '');
            if (extraReplacements) {
                cookie = cookie.replace(/;\s*Domain=[^;]+/gi, '').replace(/;\s*Path=[^;]+/gi, '');
            } else {
                cookie = cookie.replace('; Path=/edu-sharing', '; Path=/');
            }
            return cookie;
        });
    }
}

/**
 * Buffers a text/json response body, hands it to `patch` and sends the result.
 * Returns false if the response is not patchable (binary) and was left untouched.
 */
function patchBody(proxyRes, res, patch) {
    const contentType = proxyRes.headers['content-type'] || '';
    if (!contentType.includes('application/json') && !contentType.includes('text')) {
        return false;
    }
    const chunks = [];
    proxyRes.on('data', (chunk) => {
        chunks.push(chunk);
    });
    proxyRes.on('end', () => {
        const body = patch(Buffer.concat(chunks).toString('utf8'));
        res.setHeader('content-length', Buffer.byteLength(body));
        res.end(body);
    });
    // Prevent default piping
    proxyRes.pipe = () => {};
    return true;
}

// absolute preview urls of the configured backend host (with any/no port)
const PREVIEW_URL_REGEX = (() => {
    const backend = new URL(process.env.BACKEND_URL);
    const escapedHost = backend.hostname.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
    // the path may continue with a sub-path or a query (`/preview?nodeId=...`), or end there
    return new RegExp(
        `https?://${escapedHost}(?::\\d+)?/edu-sharing/preview(?=[/?"'\\s\\\\]|$)`,
        'g',
    );
})();

function patchPreviewUrls(body) {
    return body.replace(PREVIEW_URL_REGEX, `${LOCAL_URL}/edu-sharing/preview`);
}

function repoConfigure(proxy) {
    proxy.on('proxyReq', (proxyReq) => {
        // only receive non-gzip results for patching
        proxyReq.setHeader('Accept-Encoding', 'deflate');
    });
    proxy.on('proxyRes', (proxyRes, req, res) => {
        proxyRes.headers['X-Edu-Sharing-Proxy-Target'] = process.env.BACKEND_URL;
        mapCookies(proxyRes);
        patchBody(proxyRes, res, patchPreviewUrls);
    });
}

const PROXY_CONFIG = [
    {
        context: [
            '/edu-sharing/rest',
            '/edu-sharing/graphql',
            '/edu-sharing/eduservlet',
            '/edu-sharing/preview',
            '/edu-sharing/themes',
            '/edu-sharing/share',
            '/edu-sharing/ccimages',
            '/edu-sharing/oauth2/',
            '/edu-sharing/oauth2server/',
            '/edu-sharing/shibboleth',
            '/edu-sharing/sso',
            '/edu-sharing/services',
            '/edu-sharing/login/google',
        ],
        target: process.env.BACKEND_URL,
        secure: false,
        ws: true,
        changeOrigin: true,
        bypass(req, res, proxyOptions) {
            if (req.method !== 'POST' && req.url.startsWith('/edu-sharing/share')) {
                // proxy only the post request to the backend, not any other paths
                return req.url;
            }
        },
        configure: repoConfigure,
    },
    {
        context: ['/rest', '/eduservlet', '/preview', '/themes'],
        target: process.env.BACKEND_URL + '/edu-sharing',
        secure: false,
        changeOrigin: true,
        configure: repoConfigure,
    },
    {
        context: ['/rendering2'],
        target: process.env.RS2_URL || 'http://127.0.0.1.nip.io:8080',
        secure: false,
        changeOrigin: true,
        pathRewrite: { '^/rendering2': '' },
        configure(proxy) {
            proxy.on('proxyReq', (proxyReq) => {
                proxyReq.removeHeader('Origin');
                // only receive non-gzip results for patching
                proxyReq.setHeader('Accept-Encoding', 'deflate');
            });
            proxy.on('proxyRes', (proxyRes, req, res) => {
                proxyRes.headers['X-Edu-Sharing-Proxy-Target'] = process.env.RS2_URL;
                mapCookies(proxyRes, true);
                patchBody(proxyRes, res, (body) => {
                    // replace all rs2 uris to local for cookie auth
                    const rs2 = new URL(process.env.RS2_URL);
                    const escapedHost = rs2.host.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
                    // drop a trailing slash from the path so it stays in the body after
                    // replacement (pathname is at least "/", which the match would otherwise
                    // swallow and glue "rendering2" onto the following segment)
                    const escapedPath = rs2.pathname
                        .replace(/\/$/, '')
                        .replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
                    const regex = new RegExp(
                        `${rs2.protocol}//${escapedHost.replace(/:\\d+$/, '')}:\\d+${escapedPath}`,
                        'g',
                    );
                    body = body.replace(regex, `${LOCAL_URL}/rendering2`);
                    // rewrite absolute repo preview urls to the local proxy path
                    return patchPreviewUrls(body);
                });
            });
        },
    },
];

module.exports = PROXY_CONFIG;
