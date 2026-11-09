import type { FastifyInstance, FastifyReply } from 'fastify';
import { Type, type TypeBoxTypeProvider } from '@fastify/type-provider-typebox';
import { negotiateEncoding } from '../lib/static-cache.ts';
import { UnknownContextError, type ResolvedOverrides } from '../lib/translation-overrides.ts';
import type { TranslationBundle } from '../lib/translations.ts';

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

/** ids of contexts: node ids of the dynamic ones, free strings of the client.config.xml */
export const CONTEXT_ID_PATTERN = '^[^/\\\\\\s]{1,200}$';

export interface FrontendApiOptions {
    readonly translations: { get(language: string, domain: string, contextId?: string): Promise<TranslationBundle | undefined> };
    readonly overrides: { resolve(language: string, domain: string, contextId?: string): Promise<ResolvedOverrides> };
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
        description:
            'Nested dictionary of translations: the top level keys of every scope below assets/i18n merged, ' +
            'the overrides of the client config applied',
    },
);

const TranslationOverrides = Type.Record(Type.String(), Type.String(), {
    $id: 'TranslationOverrides',
    description: 'Flat translation overrides of the client config, dotted keys (WORKSPACE.TOAST.SAVED)',
});

const LanguageParams = Type.Object({
    language: Type.String({ pattern: LANGUAGE_PATTERN, description: 'e.g. de, en, de-informal' }),
});

const ContextQuery = Type.Object({
    context: Type.Optional(
        Type.String({
            pattern: CONTEXT_ID_PATTERN,
            description:
                'id of the config context to use instead of the one of the domain - Config.contextId of ' +
                'GET /rest/config/v1/values, which also reflects a context an admin enforced for the session',
        }),
    ),
});

function notFound(reply: FastifyReply, message: string): FastifyReply {
    // not even the browser keeps a 404: the context or language may exist a minute later
    return reply.code(404).header('Cache-Control', 'no-store').send({ error: 'Not Found', message });
}

/** the result of `load`, or the error of a context id the backend does not know - answered with a 404 */
async function catchUnknownContext<T>(load: () => Promise<T>): Promise<T | UnknownContextError> {
    try {
        return await load();
    } catch (err) {
        if (err instanceof UnknownContextError) {
            return err;
        }
        throw err;
    }
}

/**
 * Registered with the prefix `${CONTEXT_PATH}${FRONTEND_API_PATH}`. Called by the app and by
 * the web components on foreign pages - nothing here depends on a session or cookie, so
 * every answer allows any origin.
 */
export async function frontendApi(app: FastifyInstance, options: FrontendApiOptions): Promise<void> {
    const api = app.withTypeProvider<TypeBoxTypeProvider>();
    // the root context streams every body through to tomcat (no parsers, see server.ts) -
    // this plugin's context is encapsulated, so its own routes get a json parser again
    api.removeAllContentTypeParsers();
    api.addContentTypeParser('application/json', { parseAs: 'string' }, api.getDefaultJsonParser('error', 'error'));
    api.addSchema(ErrorBody);
    api.addSchema(Translations);
    api.addSchema(TranslationOverrides);

    api.addHook('onRequest', async (_request, reply) => {
        reply.header('Access-Control-Allow-Origin', '*');
    });

    api.get(
        '/v1/i18n/:language',
        {
            schema: {
                tags: [TAG_TRANSLATIONS],
                operationId: 'getTranslations',
                summary: 'Translations of the app for one language, with the overrides of the client config',
                description:
                    'The translations of the frontend build (formerly GET /rest/config/v1/language/defaults) with the ' +
                    'overrides of the client config on top (formerly GET /rest/config/v1/language): the ones of the ' +
                    'context given by `context` (404 if unknown), else of the context of the domain, else the global ones. ' +
                    'Overrides are kept per base language - de-informal gets those of de.',
                params: LanguageParams,
                querystring: ContextQuery,
                response: {
                    200: Type.Ref('Translations'),
                    304: Type.Null({ description: 'Not modified (If-None-Match)' }),
                    404: Type.Ref('FrontendApiError', { description: 'No translations for this language, or an unknown context' }),
                },
            },
        },
        async (request, reply) => {
            const { language } = request.params;
            const bundle = await catchUnknownContext(() =>
                options.translations.get(language, request.hostname, request.query.context),
            );
            if (bundle instanceof UnknownContextError) {
                // the app got the id from the backend - unknown here means the backend and this
                // server disagree (a context deleted, an id changed, made up by a caller)
                request.log.info({ contextId: bundle.contextId, language }, 'translations: context not found - 404');
                return notFound(reply, bundle.message);
            }
            if (bundle === undefined) {
                request.log.info({ language }, 'translations: no translations for this language - 404');
                return notFound(reply, `no translations for language ${language}`);
            }
            // the language and context are part of the url, the domain is the host: cacheable.
            // Short, as the overrides change with the config; revalidating costs a 304. Built
            // without the overrides (backend unreachable): not at all
            reply
                .header('Cache-Control', bundle.complete ? 'public, max-age=60' : 'no-store')
                .header('ETag', bundle.etag)
                .header('Vary', 'Accept-Encoding');
            const ifNoneMatch = request.headers['if-none-match'];
            if (ifNoneMatch !== undefined && ifNoneMatch.split(',').some((tag) => tag.trim().replace(/^W\//, '') === bundle.etag)) {
                return reply.code(304).send(null);
            }
            const encoding = negotiateEncoding(request.headers['accept-encoding'], bundle);
            if (encoding !== 'identity') {
                reply.header('Content-Encoding', encoding);
            }
            return reply.type('application/json').send(bundle[encoding]);
        },
    );

    api.get(
        '/v1/i18n/:language/overrides',
        {
            schema: {
                tags: [TAG_TRANSLATIONS],
                operationId: 'getTranslationOverrides',
                summary: 'Only the overrides of the client config for one language',
                description:
                    'For clients that load the default translations themselves (dev builds read assets/i18n directly). ' +
                    'The same overrides getTranslations applies.',
                params: LanguageParams,
                querystring: ContextQuery,
                response: {
                    200: Type.Ref('TranslationOverrides'),
                    404: Type.Ref('FrontendApiError', { description: 'Unknown context' }),
                },
            },
        },
        async (request, reply) => {
            const resolved = await catchUnknownContext(() =>
                options.overrides.resolve(request.params.language, request.hostname, request.query.context),
            );
            if (resolved instanceof UnknownContextError) {
                request.log.warn(
                    { contextId: resolved.contextId, language: request.params.language },
                    'translation overrides: context not found - 404',
                );
                return notFound(reply, resolved.message);
            }
            return reply.header('Cache-Control', resolved.complete ? 'public, max-age=60' : 'no-store').send(resolved.overrides);
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
        request.log.info({ method: request.method, url: request.url }, 'frontend api: no such route - 404');
        return notFound(reply, `${request.method} ${request.url} does not exist`);
    });
}
