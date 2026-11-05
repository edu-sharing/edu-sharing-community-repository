import type { OutgoingHttpHeaders } from 'node:http';
import type { FastifyBaseLogger } from 'fastify';
import type { components } from '../shared/api-types.ts';
import { LRUCache } from 'lru-cache';
import { isConnectionError, normalizeDomain, type JsonGetter } from './backend-client.ts';

/** REST models, generated from openapi.json by scripts/generate-api-types.mjs */
type ConfigResponse = components['schemas']['Config'];
type ValuesBackendInternal = components['schemas']['ValuesBackendInternal'];
type AngularConfig = components['schemas']['AngularConfig'];
type Values = components['schemas']['Values'];
type GuestConfig = components['schemas']['GuestConfig'];

/** `exporter.lrmi` - the mapping `LRMITool` works with, both maps always present, `enrichment` defaults to true */
export type LrmiMapping = Readonly<Required<components['schemas']['LrmiConfig']>>;

/** the fields `K` of `T` with the default applied - always present, although the spec has them optional */
type Defaulted<T, K extends keyof T> = Readonly<Required<Pick<T, K>>>;

/**
 * The slice of `GET /rest/config/v1/values` this server works with, normalized.
 * Everything but `current` is part of `ValuesBackendInternal`, which the backend
 * only returns on its internal port - the cache must be fed by a client on that port.
 *
 * Picked from the generated schemas, keeping their names and nesting, with the
 * defaults already applied - so no caller has to walk optional chains or repeat a
 * default, and a spec change shows up here as a compile error.
 *
 * From `current`: `branding` appends " – edu-sharing" to page titles,
 * `defaultLocation` is where the bare entry point goes (default-location.ts).
 */
export interface RepositoryConfig
    extends Defaulted<Values, 'branding'>,
        Readonly<Pick<Values, 'defaultLocation'>> {
    /** `exporter.lrmi` */
    readonly lrmi: LrmiMapping;
    /**
     * `angular` - `headers` are the security headers of the shell pages (security-headers.ts).
     * The spec types their values as objects (Content-Security-Policy is one), the other
     * entries are strings.
     */
    readonly angular: Defaulted<AngularConfig, 'headers' | 'customHtmlHeaders'>;
    /**
     * `repository.guest`, `enabled` defaults to false as in the lightbend reference config.
     * With guest access off the repository hands an anonymous caller nothing at all, so
     * the shell must not be enriched either - see the enrichment in server.ts.
     */
    readonly guest: Defaulted<GuestConfig, 'enabled'>;
}

interface Entry {
    readonly config: RepositoryConfig;
    readonly fetchedAt: number;
    /** the backend reloaded its config since - still handed out until the refetch is done */
    readonly invalidated?: boolean;
}

export interface ConfigCacheOptions {
    readonly contextPath: string;
    readonly pollIntervalMs: number;
    readonly maxAgeMs: number;
    /** most domains kept at once (least recently used first out) - any Host header makes a key; default 100 */
    readonly maxDomains?: number;
    /** while the backend is unreachable (or a poll failed), `_about` is polled this often instead of every pollIntervalMs */
    readonly unreachableRetryMs?: number;
    /**
     * client for the `_about` poll, with its own (longer) timeout than the config fetches -
     * defaults to the config client
     */
    readonly probe?: JsonGetter;
    /**
     * failed polls in a row without a clear verdict (timeout, garbage) until the backend
     * counts as unreachable - a refused connection or a 5xx counts at once
     */
    readonly failedPollsUntilUnreachable?: number;
    readonly log: FastifyBaseLogger;
    readonly now?: () => number;
}

const DEFAULT_UNREACHABLE_RETRY_MS = 5000;
const DEFAULT_FAILED_POLLS_UNTIL_UNREACHABLE = 3;

/** what found the backend unreachable */
export type UnreachableSource = 'poll' | 'config' | 'proxy';

/**
 * Backend config, cached per domain - the backend resolves the config context from
 * the request domain (`ConfigServiceFactory.getCurrentDomain`), so one entry per
 * domain.
 *
 * Invalidation follows `GET /rest/_about` -> `lastCacheUpdate`
 * (`RepoFactory.getLastRefreshed()`), which moves whenever the backend reloads its
 * configuration. In a cluster every pod has its own value and a load balancer hands
 * them out in turn, so only an *increase* over the highest value seen counts -
 * anything else would invalidate the cache on every poll. `maxAgeMs` bounds an entry
 * should a refresh be missed.
 *
 * Stale-while-revalidate: an outdated or invalidated entry is handed out at once and
 * refetched in the background - a page only waits for the config when its domain has
 * none yet. Never throws: a failing backend keeps the old entry, or yields null when
 * there is none, and callers render without the config dependent parts.
 */
export class ConfigCache {
    readonly #client: JsonGetter;
    readonly #options: ConfigCacheOptions;
    readonly #now: () => number;
    readonly #entries: LRUCache<string, Entry>;
    readonly #pending = new Map<string, Promise<RepositoryConfig | null>>();
    #lastCacheUpdate = Number.NEGATIVE_INFINITY;
    #timer: NodeJS.Timeout | undefined;
    #retryTimer: NodeJS.Timeout | undefined;
    #reachable = true;
    #failedPolls = 0;
    readonly #unreachableListeners: ((source: UnreachableSource) => void)[] = [];

    constructor(client: JsonGetter, options: ConfigCacheOptions) {
        this.#client = client;
        this.#options = options;
        this.#now = options.now ?? Date.now;
        this.#entries = new LRUCache<string, Entry>({ max: options.maxDomains ?? 100 });
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

    /** the domains a config is cached for */
    domains(): string[] {
        return [...this.#entries.keys()];
    }

    stop(): void {
        clearInterval(this.#timer);
        clearTimeout(this.#retryTimer);
        this.#timer = undefined;
        this.#retryTimer = undefined;
    }

    /**
     * False from a failed `_about` poll (no answer, or 5xx - e.g. a starting Tomcat) or a
     * failed connection of a proxied request until the next successful poll. Unlike
     * readiness this flips both ways: the app pages send the browser to the
     * "service unavailable" page meanwhile.
     */
    isBackendReachable(): boolean {
        return this.#reachable;
    }

    /** called whenever the backend turns unreachable (not again while it stays so) - for the metrics */
    onUnreachable(listener: (source: UnreachableSource) => void): void {
        this.#unreachableListeners.push(listener);
    }

    /** The backend could not be reached - also reported by the proxy. Polls faster until it answers again. */
    markUnreachable(source: UnreachableSource, reason: string): void {
        if (this.#reachable) {
            this.#options.log.warn({ source, reason }, 'backend unreachable');
            this.#reachable = false;
            for (const listener of this.#unreachableListeners) {
                listener(source);
            }
        }
        this.#scheduleRetry();
    }

    /** a quicker next poll - only as part of the background polling, a manual poll() schedules nothing */
    #scheduleRetry(): void {
        if (this.#timer === undefined || this.#retryTimer !== undefined) {
            return;
        }
        this.#retryTimer = setTimeout(() => {
            this.#retryTimer = undefined;
            void this.poll();
        }, this.#options.unreachableRetryMs ?? DEFAULT_UNREACHABLE_RETRY_MS);
        this.#retryTimer.unref();
    }

    /**
     * One `_about` round trip; invalidates every entry when `lastCacheUpdate` increased.
     *
     * A refused connection (or DNS, connect timeout) and a 5xx mark the backend unreachable
     * at once. Anything else failing - mostly a timeout of a busy tomcat - only after
     * `failedPollsUntilUnreachable` polls in a row, so a single slow answer does not send
     * every browser to the "service unavailable" page.
     */
    async poll(): Promise<void> {
        let response;
        try {
            response = await (this.#options.probe ?? this.#client).getJson(`${this.#options.contextPath}/rest/_about`);
        } catch (error) {
            const reason = error instanceof Error ? error.message : String(error);
            this.#options.log.debug({ err: error }, 'polling backend _about failed');
            this.#failedPolls++;
            if (isConnectionError(error) || this.#failedPolls >= (this.#options.failedPollsUntilUnreachable ?? DEFAULT_FAILED_POLLS_UNTIL_UNREACHABLE)) {
                this.markUnreachable('poll', reason);
            } else {
                this.#scheduleRetry();
            }
            return;
        }
        if (response.status >= 500) {
            this.#failedPolls++;
            this.markUnreachable('poll', `_about answered ${response.status}`);
            return;
        }
        this.#failedPolls = 0;
        if (!this.#reachable) {
            this.#options.log.info('backend reachable again');
            this.#reachable = true;
        }
        const lastCacheUpdate = (response.body as { lastCacheUpdate?: unknown } | undefined)?.lastCacheUpdate;
        if (typeof lastCacheUpdate !== 'number' || lastCacheUpdate <= this.#lastCacheUpdate) {
            return;
        }
        if (this.#lastCacheUpdate !== Number.NEGATIVE_INFINITY) {
            this.#options.log.info({ lastCacheUpdate }, 'backend config refreshed, refetching the cached configs');
        }
        this.#lastCacheUpdate = lastCacheUpdate;
        for (const [domain, entry] of [...this.#entries.entries()]) {
            this.#entries.set(domain, { ...entry, invalidated: true });
        }
    }

    /** the cached config of a domain, however old, without asking the backend */
    peek(domain: string): RepositoryConfig | null {
        return this.#entries.get(normalizeDomain(domain))?.config ?? null;
    }

    /**
     * @param domain the browser facing host, the cache key
     * @param headers identity-free headers carrying that domain, see `anonymousFor`
     */
    get(requestedDomain: string, headers: OutgoingHttpHeaders): Promise<RepositoryConfig | null> {
        // host names are case insensitive - one entry for Repo.Example.org and repo.example.org
        const domain = normalizeDomain(requestedDomain);
        const entry = this.#entries.get(domain);
        if (entry !== undefined && !entry.invalidated && this.#now() - entry.fetchedAt < this.#options.maxAgeMs) {
            return Promise.resolve(entry.config);
        }
        // one fetch per domain at a time, however many requests ask
        let load = this.#pending.get(domain);
        if (load === undefined) {
            load = this.#load(domain, headers, entry).finally(() => this.#pending.delete(domain));
            this.#pending.set(domain, load);
        }
        // an old entry right away, the fetch goes on in the background
        return entry !== undefined ? Promise.resolve(entry.config) : load;
    }

    /** settles once no config fetch is running any more - for tests */
    async idle(): Promise<void> {
        await Promise.all(this.#pending.values());
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
            if (isConnectionError(error)) {
                this.markUnreachable('config', error instanceof Error ? error.message : String(error));
            }
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
        ...(response.current?.defaultLocation !== undefined && { defaultLocation: response.current.defaultLocation }),
        lrmi: {
            // absent means on, as in the lightbend reference config
            enrichment: backend?.exporter?.lrmi?.enrichment !== false,
            properties: backend?.exporter?.lrmi?.properties ?? {},
            vcard: backend?.exporter?.lrmi?.vcard ?? {},
        },
        angular: {
            headers: backend?.angular?.headers ?? {},
            customHtmlHeaders: backend?.angular?.customHtmlHeaders ?? '',
        },
        guest: {
            // absent means off: the reference config defaults repository.guest.enabled to
            // false, and an older backend that does not report it must not make us enrich
            enabled: response.currentBackend?.repository?.guest?.enabled === true,
        },
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
