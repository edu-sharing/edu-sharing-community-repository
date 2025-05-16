require('dotenv').config();

const { createProxyMiddleware } = require('http-proxy-middleware');

module.exports = function expressMiddleware(router) {
    router.use(
        '/api',
        createProxyMiddleware({
            target: process.env.BACKEND_URL + '/edu-sharing/rest',
            changeOrigin: true,
            pathRewrite: {
                '^/api': '', // Remove `/api` from the request URL before forwarding
            },
        }),
    );
};
