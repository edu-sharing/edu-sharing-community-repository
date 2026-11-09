import type { FastifyBaseLogger } from 'fastify';
import type { components } from '../shared/api-types.ts';
import { normalizeDomain, type JsonGetter } from './backend-client.ts';

type ConfigLanguage = components['schemas']['Language'];
type Context = components['schemas']['Context'];
type ValuesBackendInternal = components['schemas']['ValuesBackendInternal'];

/** a context id the backend does not know (not even after a refetch) */
export class UnknownContextError extends Error {
    // no parameter property: node runs the sources with type stripping only
    readonly contextId: string;

    constructor(contextId: string) {
        super(`unknown context ${contextId}`);
        this.name = 'UnknownContextError';
        this.contextId = contextId;
    }
}

/**
 * An unknown context id refetches the snapshot - the context may just have been created -
 * but at most this often, so made up ids cannot keep the backend busy.
 */
const REFETCH_ON_UNKNOWN_CONTEXT_MS = 10000;

/** flat: `WORKSPACE.TOAST.SAVED` -> text, as the client config writes them */
export type Overrides = Readonly<Record<string, string>>;

export interface ResolvedOverrides {
    readonly overrides: Overrides;
    /**
     * changes whenever the overrides may have changed (config reload) - part of the cache
     * key of the merged translations and of their ETag
     */
    readonly version: string;
    /** the context the overrides came from, undefined: the global config */
    readonly contextId?: string;
    /** false: the backend could not be asked, the answer holds no overrides and must not be cached long */
    readonly complete: boolean;
}

export interface TranslationOverridesOptions {
    readonly contextPath: string;
    /** `lastCacheUpdate` of the backend, moves with every config reload (ConfigCache) */
    readonly configVersion: () => number;
    /** refetch at the latest after this long, should a reload be missed */
    readonly maxAgeMs: number;
    readonly log: FastifyBaseLogger;
    readonly now?: () => number;
}

interface Snapshot {
    /** `<language>` of the client.config.xml */
    readonly global: readonly ConfigLanguage[];
    readonly byId: ReadonlyMap<string, Context>;
    readonly byDomain: ReadonlyMap<string, Context>;
    readonly configVersion: number;
    readonly fetchedAt: number;
    /** counts the snapshots - a new one may hold other overrides than the one before */
    readonly serial: number;
}

/**
 * Level 2 of the translations: the overrides of the client config, the port of
 * `ConfigApi.getLanguage` / `ConfigServiceImpl.getConfigByContext`.
 *
 * Domain independent data, so one snapshot for all domains: the global `<language>` list
 * (`ValuesBackendInternal.language` of `GET /config/v1/values`, internal port only) and every context
 * (`GET /config/v1/context/?includeStatic=true`). The context of a request is the one
 * given by id (`?context=`, what the backend enforced for the session - the app reads it
 * from `Config.contextId`), else the one of its domain.
 *
 * Unlike `ConfigServiceImpl.overrideLanguage`, a language only the context overrides
 * counts as well - the backend drops those silently.
 */
export class TranslationOverrides {
    readonly #client: JsonGetter;
    readonly #options: TranslationOverridesOptions;
    readonly #now: () => number;
    #snapshot: Snapshot | undefined;
    #loading: Promise<Snapshot | undefined> | undefined;
    #serial = 0;

    constructor(client: JsonGetter, options: TranslationOverridesOptions) {
        this.#client = client;
        this.#options = options;
        this.#now = options.now ?? Date.now;
    }

    /**
     * @param language as the app names it (`de-informal`) - overrides are kept per base
     *   language (`de`), as the backend reads them (`getCurrentLanguage`: two letters)
     * @param domain the browser facing host
     * @param contextId the context the backend enforced, wins over the domain
     * @throws UnknownContextError for a context id the backend does not know
     */
    async resolve(language: string, domain: string, contextId?: string): Promise<ResolvedOverrides> {
        let snapshot = await this.#current();
        if (snapshot === undefined) {
            // the backend could not be asked - an id cannot be checked, the defaults go out alone
            return { overrides: {}, version: 'none', complete: false };
        }
        let context: Context | undefined;
        if (contextId !== undefined) {
            context = snapshot.byId.get(contextId);
            if (context === undefined && this.#now() - snapshot.fetchedAt >= REFETCH_ON_UNKNOWN_CONTEXT_MS) {
                // created after the snapshot was taken? Ask once more
                snapshot = (await this.#refetch()) ?? snapshot;
                context = snapshot.byId.get(contextId);
            }
            if (context === undefined) {
                throw new UnknownContextError(contextId);
            }
        }
        context ??= snapshot.byDomain.get(normalizeDomain(domain));
        const base = language.slice(0, 2);
        return {
            overrides: { ...entriesOf(snapshot.global, base), ...entriesOf(context?.language, base) },
            version: `${snapshot.serial}`,
            ...(context?.id && { contextId: context.id }),
            complete: true,
        };
    }

    /** a new snapshot now, joining a fetch already running */
    #refetch(): Promise<Snapshot | undefined> {
        this.#loading ??= this.#load().finally(() => {
            this.#loading = undefined;
        });
        return this.#loading;
    }

    /** the snapshot, refetched when the backend reloaded its config or it got too old */
    async #current(): Promise<Snapshot | undefined> {
        const snapshot = this.#snapshot;
        const fresh =
            snapshot !== undefined &&
            snapshot.configVersion >= this.#options.configVersion() &&
            this.#now() - snapshot.fetchedAt < this.#options.maxAgeMs;
        if (fresh) {
            return snapshot;
        }
        const loading = this.#refetch();
        // stale while revalidate: an old snapshot right away, the fetch goes on
        return snapshot ?? loading;
    }

    async #load(): Promise<Snapshot | undefined> {
        // taken before the requests: a reload meanwhile must trigger the next fetch
        const configVersion = this.#options.configVersion();
        const base = `${this.#options.contextPath}/rest/config/v1`;
        try {
            const [values, contexts] = await Promise.all([
                this.#client.getJson(`${base}/values`),
                this.#client.getJson(`${base}/context/?includeStatic=true`),
            ]);
            if (values.body === undefined || contexts.body === undefined) {
                throw new Error(`config requests answered with status ${values.status} / ${contexts.status}`);
            }
            const list = Array.isArray(contexts.body) ? (contexts.body as (Context | null)[]) : [];
            const byId = new Map<string, Context>();
            const byDomain = new Map<string, Context>();
            for (const context of list) {
                if (context === null || typeof context !== 'object') {
                    continue;
                }
                if (context.id) {
                    byId.set(context.id, context);
                }
                for (const domain of context.domain ?? []) {
                    if (domain) {
                        byDomain.set(normalizeDomain(domain), context);
                    }
                }
            }
            const snapshot: Snapshot = {
                global: (values.body as { currentBackend?: ValuesBackendInternal }).currentBackend?.language ?? [],
                byId,
                byDomain,
                configVersion,
                fetchedAt: this.#now(),
                serial: ++this.#serial,
            };
            this.#options.log.debug({ contexts: byId.size, domains: byDomain.size }, 'translation overrides loaded');
            this.#snapshot = snapshot;
            return snapshot;
        } catch (error) {
            this.#options.log.warn({ err: error }, 'loading the translation overrides failed, using the old ones if any');
            return this.#snapshot;
        }
    }
}

/**
 * The entries of one language. Several `<language>` blocks for the same language: the
 * backend takes the first (`ConfigServiceFactory.getLanguageData`), so does this.
 */
function entriesOf(languages: readonly ConfigLanguage[] | undefined, language: string): Record<string, string> {
    const entry = languages?.find((candidate) => candidate.language?.toLowerCase() === language.toLowerCase());
    const result: Record<string, string> = {};
    for (const pair of entry?.string ?? []) {
        if (pair.key !== undefined && pair.value !== undefined) {
            result[pair.key] = pair.value;
        }
    }
    return result;
}

/**
 * The overrides on top of the default translations, as the app's TranslationLoader did
 * (`applyOverrides`): a dotted key addresses a nested entry, missing levels are created.
 * Copy on write - `translations` (level 1, shared) stays untouched. An override that would
 * turn a text into a subtree is skipped (the app threw).
 */
export function applyOverrides(
    translations: Readonly<Record<string, unknown>>,
    overrides: Overrides,
    log?: FastifyBaseLogger,
): Record<string, unknown> {
    const result: Record<string, unknown> = { ...translations };
    // the objects of `result` that are copies already, so they may be written
    const copied = new Set<object>([result]);
    for (const [key, value] of Object.entries(overrides)) {
        const path = key.split('.');
        const leaf = path.pop() as string;
        let node = result;
        let valid = true;
        for (const item of path) {
            const child = node[item];
            if (typeof child === 'string') {
                log?.warn({ key }, 'translation override turns a text into a subtree, skipped');
                valid = false;
                break;
            }
            let next: Record<string, unknown>;
            if (child !== null && typeof child === 'object' && !Array.isArray(child)) {
                next = copied.has(child) ? (child as Record<string, unknown>) : { ...(child as Record<string, unknown>) };
            } else {
                next = {};
            }
            copied.add(next);
            node[item] = next;
            node = next;
        }
        if (valid) {
            node[leaf] = value;
        }
    }
    return result;
}
