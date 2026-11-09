import { loadConfig, loadDotEnv } from './config.ts';
import { buildManagementServer } from './lib/management-server.ts';
import { buildServer } from './server.ts';

loadDotEnv();
const config = loadConfig();
const app = await buildServer(config);
// /metrics + /health on a separate instance, never reachable through the public port
const managementServer = config.managementPort > 0 ? buildManagementServer(app, { backendUrl: config.backendUrl }) : undefined;

for (const signal of ['SIGINT', 'SIGTERM'] as const) {
    process.once(signal, () => {
        app.log.info({ signal }, 'shutting down');
        // app.close() waits for every running request - a long proxied one (a download, a hanging
        // backend call) kept the process alive, refusing new requests, until it was killed
        // (node --watch in dev waits forever)
        setTimeout(() => {
            app.log.warn({ timeoutMs: config.shutdownTimeoutMs }, 'requests still running after the shutdown timeout, exiting anyway');
            process.exit(0);
        }, config.shutdownTimeoutMs).unref();
        void Promise.all([app.close(), managementServer?.close()]).then(() => process.exit(0));
    });
}

await app.listen({ host: config.host, port: config.port });
if (managementServer !== undefined) {
    await managementServer.listen({ host: config.host, port: config.managementPort });
}
app.log.info(
    {
        contextPath: config.contextPath,
        backendUrl: config.backendUrl,
        backendInternalUrl: config.backendInternalUrl,
        distDir: config.distDir,
        managementPort: config.managementPort,
    },
    'frontend-server ready',
);
