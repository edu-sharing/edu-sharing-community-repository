import Fastify, { type FastifyInstance } from 'fastify';

/**
 * The management server: `GET /metrics` (prometheus) and the kubernetes probes on
 * MANAGEMENT_PORT, a separate Fastify instance in the same process - never
 * reachable through the public app port or the ingress. Fastify recommends several
 * instances in one process for exactly this:
 * https://fastify.dev/docs/latest/Guides/Recommendations/#multiple
 *
 * The same split the repository makes with its jmx exporter on 9090.
 */
export function buildManagementServer(app: FastifyInstance, options: { backendUrl: string }): FastifyInstance {
    const management = Fastify({ logger: false });
    const { register } = app.metrics.client;

    management.get('/metrics', async (_request, reply) => reply.type(register.contentType).send(await register.metrics()));

    // same split as the repository (rest/_about/health/liveness, .../readiness)

    // alive = the process answers; never asks the backend - a failing tomcat must not
    // make kubernetes restart every frontend server
    management.get('/health/liveness', async () => ({ status: 'ok' }));
    // we will later serve a custom error handler when we can't reach the backend
    management.get('/health/readiness', async () => ({ status: 'ok' }));

    return management;
}
