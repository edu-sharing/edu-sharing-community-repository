import { createProxyMiddleware } from 'http-proxy-middleware';

require('dotenv').config();

export default function expressMiddleware(router) {
    router.use(
        '/api',
        createProxyMiddleware({
            target: process.env.BACKEND_URL + '/edu-sharing/rest',
            changeOrigin: true,
            pathRewrite: {
                '^/api': '',
            },
        }),
    );
}
