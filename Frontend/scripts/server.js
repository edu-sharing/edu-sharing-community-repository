require('dotenv').config();
const express = require('express');
const { createProxyMiddleware } = require('http-proxy-middleware');
const app = express();

const distDir = __dirname + '/../dist';

// Serve `dist` directory
app.use('/edu-sharing', express.static(distDir));

// Proxy REST requests to `BACKEND_URL` as set in .env
app.use(
    '/edu-sharing/rest',
    createProxyMiddleware({
        changeOrigin: true,
        target: process.env.BACKEND_URL,
        secure: false,

        onProxyRes: function (proxyRes, req, res) {
            // const exchange = `[${req.method}] [${proxyRes.statusCode}] ${req.path} -> ${proxyRes.req.protocol}//${proxyRes.req.host}${proxyRes.req.path}`;
            // console.log(exchange);
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
    }),
);

// Fallback to `index.html` to serve page requests into Angular's path hierarchy
app.get('/edu-sharing/*', function (req, res) {
    res.sendFile('index.html', { root: distDir });
});

app.listen(4200);

console.log('');
console.log('Serving contents of the directory "dist" on http://localhost:4200/edu-sharing.');
console.log('');
console.log('Please run your desired build (if not done already), e.g.,');
console.log('    npm run build');
