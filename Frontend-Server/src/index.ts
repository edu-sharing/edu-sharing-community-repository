import { loadConfig } from './config.ts';
import { buildServer } from './server.ts';

const config = loadConfig();
const app = await buildServer(config);

for (const signal of ['SIGINT', 'SIGTERM'] as const) {
    process.on(signal, () => {
        app.log.info({ signal }, 'shutting down');
        void app.close().then(() => process.exit(0));
    });
}

await app.listen({ host: config.host, port: config.port });
app.log.info(
    { contextPath: config.contextPath, backendUrl: config.backendUrl, distDir: config.distDir },
    'frontend-server ready',
);
