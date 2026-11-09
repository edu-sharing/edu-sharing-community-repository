import type { FastifyInstance, FastifyReply } from 'fastify';
import { Type, type TypeBoxTypeProvider } from '@fastify/type-provider-typebox';
import type { Bundle } from '../lib/language-defaults.ts';
import { negotiateEncoding } from '../lib/static-cache.ts';

/**
 * The API this server answers itself, relative to CONTEXT_PATH. Outside of `/rest` on
 * purpose: `/rest/*` belongs to tomcat (the service chart routes it there directly), this
 * prefix falls into the catch-all of the frontend-server ingress without further routes.
 */
export const FRONTEND_API_PATH = '/frontend-api';

/** only routes with a tag end up in the openapi spec, see lib/openapi.ts */
export const TAG_TRANSLATIONS = 'TRANSLATIONS v1';

/** what the app sends as language: `de`, `en`, `de-informal`, `de-no-binnen-i` ... */
export const LANGUAGE_PATTERN = '^[a-z]{2}(-[a-z0-9]+)*$';

export interface FrontendApiOptions {
    readonly languageDefaults: { get(language: string): Promise<Bundle | undefined> };
}

const ErrorBody = Type.Object(
    {
        error: Type.String(),
        message: Type.String(),
    },
    { $id: 'FrontendApiError' },
);

const Translations = Type.Object(
    {},
    {
        $id: 'Translations',
        additionalProperties: true,
        description: 'Nested dictionary of translations, the top level keys of every scope below assets/i18n merged',
    },
);

function notFound(reply: FastifyReply, message: string): FastifyReply {
    return reply.code(404).send({ error: 'Not Found', message });
}

/**
 * Registered with the prefix `${CONTEXT_PATH}${FRONTEND_API_PATH}`. Called by the app and by
 * the web components on foreign pages - nothing here depends on a session or cookie, so
 * every answer allows any origin.
 */
export async function frontendApi(app: FastifyInstance, options: FrontendApiOptions): Promise<void> {
    const api = app.withTypeProvider<TypeBoxTypeProvider>();
    api.addSchema(ErrorBody);
    api.addSchema(Translations);

    api.addHook('onRequest', async (_request, reply) => {
        reply.header('Access-Control-Allow-Origin', '*');
    });

    api.get(
        '/v1/i18n/:language',
        {
            schema: {
                tags: [TAG_TRANSLATIONS],
                operationId: 'getTranslations',
                summary: 'Default translations of the app for one language',
                description:
                    'The translations of the frontend build, formerly GET /rest/config/v1/language/defaults of the repository ' +
                    '(without the overrides of the client config, see /rest/config/v1/language).',
                params: Type.Object({
                    language: Type.String({ pattern: LANGUAGE_PATTERN, description: 'e.g. de, en, de-informal' }),
                }),
                response: {
                    200: Type.Ref('Translations'),
                    404: Type.Ref('FrontendApiError', { description: 'No translations for this language' }),
                },
            },
        },
        async (request, reply) => {
            const bundle = await options.languageDefaults.get(request.params.language);
            if (bundle === undefined) {
                return notFound(reply, `no translations for language ${request.params.language}`);
            }
            const encoding = negotiateEncoding(request.headers['accept-encoding'], bundle);
            if (encoding !== 'identity') {
                reply.header('Content-Encoding', encoding);
            }
            // the language is part of the url, so the answer is cacheable. Not immutable:
            // the url stays the same across builds
            return reply
                .type('application/json')
                .header('Cache-Control', 'public, max-age=300')
                .header('Vary', 'Accept-Encoding')
                .send(bundle[encoding]);
        },
    );

    // everything else below the prefix: never the static files or the backend fallback.
    // OPTIONS answers the CORS preflight of cross origin callers
    api.all('/*', async (request, reply) => {
        if (request.method === 'OPTIONS') {
            return reply
                .code(204)
                .header('Access-Control-Allow-Methods', 'GET, HEAD, OPTIONS')
                .header('Access-Control-Allow-Headers', request.headers['access-control-request-headers'] ?? '*')
                .header('Access-Control-Max-Age', '86400')
                .send();
        }
        return notFound(reply, `${request.method} ${request.url} does not exist`);
    });
}
