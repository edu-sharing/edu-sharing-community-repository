import { Client, errors, type estypes } from '@elastic/elasticsearch';

/** the package exposes its generated request/response types under `estypes` */
export type SearchRequest = estypes.SearchRequest;

/**
 * What callers need - lets tests hand in a fake instead of a running cluster.
 * `_source` of the first hit, or undefined when nothing matched.
 */
export interface ElasticSearcher {
    firstSource(request: SearchRequest): Promise<Record<string, unknown> | undefined>;
}

/**
 * The official client, pinned to the 8.x line: the repository runs Elasticsearch 8
 * (`SearchServiceElastic` uses the Java client 8.x), and the JS client only supports
 * its own major. An Elasticsearch upgrade has to bump this dependency with it.
 *
 * Connection settings mirror `elasticsearch.servers` of the backend
 * (`SearchServiceElastic.getConfiguredHosts`), which is a plain host list without
 * authentication.
 */
export class ElasticClient implements ElasticSearcher {
    readonly #client: Client;
    readonly #timeoutMs: number;

    constructor(node: string, timeoutMs: number) {
        this.#timeoutMs = timeoutMs;
        this.#client = new Client({
            node,
            // the shell request waits for this - a retry would double the page's latency
            maxRetries: 0,
        });
    }

    async firstSource(request: SearchRequest): Promise<Record<string, unknown> | undefined> {
        // requestTimeout belongs on the REQUEST, not on the Client: the constructor
        // option is not applied (verified against 8.19.2 - a 1 ms client timeout let a
        // 13 ms query through), while the per request one aborts a hanging cluster on
        // time and raises errors.TimeoutError.
        const response = await this.#client.search<Record<string, unknown>>(request, {
            requestTimeout: this.#timeoutMs,
        });
        return response.hits.hits[0]?._source;
    }

    close(): Promise<void> {
        return this.#client.close();
    }
}

/**
 * True for an index that does not exist - almost always the release bumped the index
 * name (`ELASTIC_INDEX`, see config.ts) and nobody adjusted the configuration.
 */
export function isMissingIndex(error: unknown): boolean {
    return error instanceof errors.ResponseError && error.body?.error?.type === 'index_not_found_exception';
}

/** The cluster could not be reached at all, as opposed to answering with an error. */
export function isElasticUnreachable(error: unknown): boolean {
    return error instanceof errors.ConnectionError || error instanceof errors.TimeoutError;
}
