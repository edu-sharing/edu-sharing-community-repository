import type { OutgoingHttpHeaders } from 'node:http';
import type { FastifyBaseLogger } from 'fastify';
import type { components } from '../shared/api-types.ts';
import type { JsonGetter } from './backend-client.ts';

/** REST models, generated from openapi.json by scripts/generate-api-types.mjs */
type ConfigResponse = components['schemas']['Config'];
type ValuesBackendInternal = components['schemas']['ValuesBackendInternal'];
type AngularConfig = components['schemas']['AngularConfig'];

/** `exporter.lrmi` - the mapping `LRMITool` works with, both maps always present */
export type LrmiMapping = Readonly<Required<components['schemas']['LrmiConfig']>>;

/**
 * The slice of `GET /rest/config/v1/values` this server works with, normalized.
 * Everything but `branding` is part of `ValuesBackendInternal`, which the backend
 * only returns on its internal port - the cache must be fed by a client on that port.
 */
export interface RepositoryConfig {
    /** `current.branding` - appends " – edu-sharing" to page titles */
    readonly branding: NonNullable<components['schemas']['Values']['branding']>;
    readonly lrmi: LrmiMapping;
    /**
     * `angular.headers` - security headers of the shell pages, see security-headers.ts.
     * The spec types the values as objects (Content-Security-Policy is one), the other
     * entries are strings - hence unknown here.
     */
    readonly angularHeaders: Readonly<Record<string, unknown>>;
    readonly customHtmlHeaders: NonNullable<AngularConfig['customHtmlHeaders']>;
}

interface Entry {
    readonly config: RepositoryConfig;
    readonly fetchedAt: number;
}

export interface ConfigCacheOptions {
    readonly contextPath: string;
    readonly pollIntervalMs: number;
    readonly maxAgeMs: number;
    readonly log: FastifyBaseLogger;
    readonly now?: () => number;
}

/**
 * Backend config, cached per domain - the backend resolves the config context from
 * the request domain (`ConfigServiceFactory.getCurrentDomain`), so one entry per
 * domain.
 *
 * Invalidation follows `GET /rest/_about` -> `lastCacheUpdate`
 * (`RepoFactory.getLastRefreshed()`), which moves whenever the backend reloads its
 * configuration. In a cluster every pod has its own value and a load balancer hands
 * them out in turn, so only an *increase* over the highest value seen counts -
 * anything else would flush the cache on every poll. `maxAgeMs` bounds an entry
 * should a refresh be missed.
 *
 * Never blocks and never throws: a failing backend yields the stale entry, or null
 * when there is none yet, and callers render without the config dependent parts.
 */
export class ConfigCache {
    readonly #client: JsonGetter;
    readonly #options: ConfigCacheOptions;
    readonly #now: () => number;
    readonly #entries = new Map<string, Entry>();
    readonly #pending = new Map<string, Promise<RepositoryConfig | null>>();
    #lastCacheUpdate = Number.NEGATIVE_INFINITY;
    #timer: NodeJS.Timeout | undefined;

    constructor(client: JsonGetter, options: ConfigCacheOptions) {
        this.#client = client;
        this.#options = options;
        this.#now = options.now ?? Date.now;
    }

    start(): void {
        if (this.#options.pollIntervalMs <= 0 || this.#timer !== undefined) {
            return;
        }
        void this.poll();
        this.#timer = setInterval(() => void this.poll(), this.#options.pollIntervalMs);
        // a pending poll must not keep the process alive on shutdown
        this.#timer.unref();
    }

    /**
     * Readiness: the backend answered `_about` at least once (or a config is cached),
     * so the first shells do not go out without security headers. Stays true when the
     * backend fails later - the stale config keeps serving. Always true without polling.
     */
    isReady(): boolean {
        return this.#options.pollIntervalMs <= 0 || this.#lastCacheUpdate !== Number.NEGATIVE_INFINITY || this.#entries.size > 0;
    }

    /** per cached domain: when its config was fetched - for the metrics */
    entries(): { domain: string; fetchedAt: number }[] {
        return [...this.#entries].map(([domain, entry]) => ({ domain, fetchedAt: entry.fetchedAt }));
    }

    stop(): void {
        clearInterval(this.#timer);
        this.#timer = undefined;
    }

    /** One `_about` round trip; flushes every entry when `lastCacheUpdate` increased. */
    async poll(): Promise<void> {
        try {
            const response = await this.#client.getJson(`${this.#options.contextPath}/rest/_about`);
            const lastCacheUpdate = (response.body as { lastCacheUpdate?: unknown } | undefined)?.lastCacheUpdate;
            if (typeof lastCacheUpdate !== 'number' || lastCacheUpdate <= this.#lastCacheUpdate) {
                return;
            }
            if (this.#lastCacheUpdate !== Number.NEGATIVE_INFINITY) {
                this.#options.log.info({ lastCacheUpdate }, 'backend config refreshed, flushing config cache');
            }
            this.#lastCacheUpdate = lastCacheUpdate;
            this.#entries.clear();
        } catch (error) {
            this.#options.log.debug({ err: error }, 'polling backend _about failed');
        }
    }

    /**
     * @param domain the browser facing host, the cache key
     * @param headers identity-free headers carrying that domain, see `anonymousFor`
     */
    get(domain: string, headers: OutgoingHttpHeaders): Promise<RepositoryConfig | null> {
        const entry = this.#entries.get(domain);
        if (entry !== undefined && this.#now() - entry.fetchedAt < this.#options.maxAgeMs) {
            return Promise.resolve(entry.config);
        }
        const pending = this.#pending.get(domain);
        if (pending !== undefined) {
            return pending;
        }
        const load = this.#load(domain, headers, entry).finally(() => this.#pending.delete(domain));
        this.#pending.set(domain, load);
        return load;
    }

    async #load(domain: string, headers: OutgoingHttpHeaders, stale: Entry | undefined): Promise<RepositoryConfig | null> {
        try {
            const response = await this.#client.getJson(`${this.#options.contextPath}/rest/config/v1/values`, headers);
            if (response.body === undefined) {
                throw new Error(`config request answered with status ${response.status}`);
            }
            if (!isInternalConfig(response.body)) {
                this.#options.log.warn(
                    { domain },
                    'backend config lacks the internal values - BACKEND_INTERNAL_URL must point to the internal tomcat port',
                );
            }
            const config = parseRepositoryConfig(response.body);
            this.#entries.set(domain, { config, fetchedAt: this.#now() });
            return config;
        } catch (error) {
            this.#options.log.warn({ err: error, domain }, 'loading backend config failed, using the stale config if any');
            return stale?.config ?? null;
        }
    }
}

/**
 * Tolerant: an older backend, or the public `ValuesBackend` (wrong port), yields empty
 * values instead of an error.
 */
export function parseRepositoryConfig(body: unknown): RepositoryConfig {
    const response = asConfigResponse(body);
    // the public ValuesBackend simply lacks the optional internal fields
    const backend = response.currentBackend as ValuesBackendInternal | undefined;
    return {
        branding: response.current?.branding === true,
        lrmi: {
            properties: backend?.exporter?.lrmi?.properties ?? {},
            vcard: backend?.exporter?.lrmi?.vcard ?? {},
        },
        angularHeaders: backend?.angular?.headers ?? {},
        customHtmlHeaders: backend?.angular?.customHtmlHeaders ?? '',
    };
}

/** Whether the config came from the internal port - the public one carries no `angular` block. */
export function isInternalConfig(body: unknown): boolean {
    const backend = asConfigResponse(body).currentBackend;
    return backend !== undefined && 'angular' in backend;
}

function asConfigResponse(body: unknown): ConfigResponse {
    return typeof body === 'object' && body !== null && !Array.isArray(body) ? (body as ConfigResponse) : {};
}
