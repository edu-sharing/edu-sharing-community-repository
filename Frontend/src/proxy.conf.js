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

const PROXY_CONFIG = [
    {
        context: [
            '/edu-sharing/rest',
            '/edu-sharing/graphql',
            '/edu-sharing/eduservlet',
            '/edu-sharing/preview',
            '/edu-sharing/themes',
            '/edu-sharing/ccimages',
            '/edu-sharing/oauth2',
            '/edu-sharing/shibboleth',
            '/edu-sharing/services',
        ],
        target: process.env.BACKEND_URL,
        secure: false,
        changeOrigin: true,
        onProxyRes: function (proxyRes, req, res) {
            proxyRes.headers['X-Edu-Sharing-Proxy-Target'] = process.env.BACKEND_URL;
            const cookies = proxyRes.headers['set-cookie'];
            if (cookies) {
                proxyRes.headers['set-cookie'] = cookies.map((cookie) =>
                    cookie
                        .replace('; Path=/edu-sharing', '; Path=/')
                        // We serve on a non-HTTPS connection, so 'Secure' cookies won't work.
                        .replace('; Secure', '')
                        .replace('; Partitioned', '')
                        // 'SameSite=None' is only allowed on 'Secure' cookies.
                        .replace('; SameSite=None', ''),
                );
            }
        },
    },
    {
        context: ['/rest', '/eduservlet', '/preview', '/themes'],
        target: process.env.BACKEND_URL + '/edu-sharing',
        secure: false,
        changeOrigin: true,
        onProxyRes: function (proxyRes, req, res) {
            proxyRes.headers['X-Edu-Sharing-Proxy-Target'] = process.env.BACKEND_URL;
            const cookies = proxyRes.headers['set-cookie'];
            if (cookies) {
                proxyRes.headers['set-cookie'] = cookies.map((cookie) =>
                    cookie
                        .replace('; Path=/edu-sharing', '; Path=/')
                        // We serve on a non-HTTPS connection, so 'Secure' cookies won't work.
                        .replace('; Secure', '')
                        // 'SameSite=None' is only allowed on 'Secure' cookies.
                        .replace('; SameSite=None', ''),
                );
            }
        },
    },
    {
        context: ['/rendering2'],
        target: process.env.RS2_URL || 'http://127.0.0.1.nip.io:8080',
        secure: false,
        changeOrigin: true,
        pathRewrite: { '^/rendering2': '/' },
        configure(proxy) {
            proxy.on('proxyReq', (proxyReq) => {
                proxyReq.setHeader('Origin', process.env.BACKEND_URL);
                // only receive non-gzip results for patching
                proxyReq.setHeader('Accept-Encoding', 'deflate');
            });
            proxy.on('proxyRes', (proxyRes, req, res) => {
                proxyRes.headers['X-Edu-Sharing-Proxy-Target'] = process.env.RS2_URL;
                const cookies = proxyRes.headers['set-cookie'];
                if (cookies) {
                    proxyRes.headers['set-cookie'] = cookies.map((cookie) =>
                        cookie
                            // We serve on a non-HTTPS connection, so 'Secure' cookies won't work.
                            .replace('; Secure', '')
                            .replace(/;\s*Domain=[^;]+/gi, '')
                            .replace(/;\s*Path=[^;]+/gi, '')
                            // 'SameSite=None' is only allowed on 'Secure' cookies.
                            .replace('; SameSite=None', ''),
                    );
                }
                const contentType = proxyRes.headers['content-type'] || '';
                if (!contentType.includes('application/json') && !contentType.includes('text')) {
                    return;
                }
                const chunks = [];
                proxyRes.on('data', (chunk) => {
                    chunks.push(chunk);
                });
                proxyRes.on('end', () => {
                    let body = Buffer.concat(chunks).toString('utf8');
                    // replace all rs2 uris to local for cookie auth
                    const rs2 = new URL(process.env.RS2_URL);
                    const escapedHost = rs2.host.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
                    const escapedPath = rs2.pathname.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
                    const regex = new RegExp(
                        `${rs2.protocol}//${escapedHost.replace(/:\\d+$/, '')}:\\d+${escapedPath}`,
                        'g',
                    );
                    body = body.replace(regex, 'http://localhost:4200/rendering2');
                    res.setHeader('content-length', Buffer.byteLength(body));
                    res.end(body);
                });
                // Prevent default piping
                proxyRes.pipe = () => {};
            });
        },
    },
];

module.exports = PROXY_CONFIG;
