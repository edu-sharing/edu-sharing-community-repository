import type { FastifyInstance } from 'fastify';
import swagger from '@fastify/swagger';
import { FRONTEND_API_PATH } from '../routes/frontend-api.ts';

/**
 * Collects the openapi spec of the frontend api - before the routes are registered, the
 * plugin listens on onRoute. Only tagged routes are in it (proxy, shell and static files
 * have none). Paths are relative to the server url, as in the backend's spec, so the
 * generated client takes a root url ending in `/frontend-api` like the other ends in `/rest`.
 *
 * Used by the dev server (/frontend-api/openapi.json, /frontend-api/docs) and by
 * scripts/generate-openapi.mjs, which writes the checked in openapi.json.
 */
export async function registerOpenApi(app: FastifyInstance, contextPath: string): Promise<void> {
    const prefix = `${contextPath}${FRONTEND_API_PATH}`;
    await app.register(swagger, {
        openapi: {
            openapi: '3.0.3',
            info: {
                title: 'edu-sharing Frontend-Server API',
                description: 'Endpoints the frontend server answers itself, without the repository.',
                version: '1.0',
            },
            servers: [{ url: prefix }],
        },
        hideUntagged: true,
        transform: ({ schema, url }) => ({
            schema,
            url: url.startsWith(prefix) ? url.slice(prefix.length) : url,
        }),
        // keep the $id schemas as components instead of def-0, def-1 ...
        refResolver: {
            buildLocalReference: (json, _baseUri, _fragment, i) => (typeof json.$id === 'string' ? json.$id : `def-${i}`),
        },
    });
}
