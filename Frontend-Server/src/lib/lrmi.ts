import { VCard } from '../shared/VCard.ts';
import type { LrmiMapping } from './config-cache.ts';
import type { LoadedNode, NodeProperties } from './node-metadata.ts';

/**
 * Port of `LRMITool.getLRMIJson` (Backend/services/core/.../tools/LRMITool.java).
 * The mapping is the same lightbend config (`exporter.lrmi`) LRMITool reads,
 * delivered through `currentBackend.exporter.lrmi`.
 */

const OR_SEPARATOR = ',';
const AND_SEPARATOR = '+';

/**
 * LRMITool reads `AlfrescoMimeType` from the raw properties; the index has it as
 * `cclom:format`, which `loadNode` lifts to a field of its own.
 */
const NODE_FIELD_FALLBACKS: Readonly<Record<string, (loaded: LoadedNode) => string | null | undefined>> = {
    AlfrescoMimeType: (loaded) => loaded.mimetype,
};

export type LrmiJson = Record<string, unknown>;

/**
 * @param renderUrl `URLHelper.getNgRenderNodeUrl(id, version)`
 * @param properties the properties of the requested version (the node's own for the latest)
 */
export function buildLrmi(loaded: LoadedNode, mapping: LrmiMapping, renderUrl: string): LrmiJson {
    const { properties } = loaded;
    const lrmi: LrmiJson = {
        '@context': 'http://schema.org/',
        '@type': ['CreativeWork', 'MediaObject'],
    };
    for (const [field, property] of Object.entries(mapping.vcard)) {
        put(lrmi, field, fromVCards(properties[property]));
    }
    put(lrmi, 'url', renderUrl);
    put(lrmi, 'thumbnailUrl', loaded.previewUrl);
    put(lrmi, 'dateCreated', isoDate(first(properties['cm:created_LONG'])));
    put(lrmi, 'dateModified', isoDate(first(properties['cm:modified_LONG'])));
    put(lrmi, 'datePublished', isoDate(first(properties['ccm:published_date_LONG'])));
    put(lrmi, 'license', loaded.licenseUrl);
    for (const [field, expression] of Object.entries(mapping.properties)) {
        // split by "," (or) first, then by "+" (and)
        const alternatives = expression.split(OR_SEPARATOR).map((alternative) => alternative.split(AND_SEPARATOR));
        put(lrmi, field, combined(loaded, alternatives));
    }
    return lrmi;
}

/** org.json drops a key put with null - so does this. */
function put(target: LrmiJson, key: string, value: unknown): void {
    if (value === null || value === undefined) {
        delete target[key];
    } else {
        target[key] = value;
    }
}

/**
 * `LRMITool.getPropertyCombined`: the first alternative with any filled property
 * wins; within an alternative all values are concatenated. One value -> scalar,
 * several -> array.
 */
function combined(loaded: LoadedNode, alternatives: string[][]): unknown {
    for (const keys of alternatives) {
        const result: string[] = [];
        for (const key of keys) {
            const values = valuesOf(loaded, key);
            if (values.length === 0 || (values.length === 1 && values[0] === '')) {
                continue;
            }
            result.push(...values);
        }
        if (result.length === 1) {
            return result[0];
        }
        if (result.length > 1) {
            return result;
        }
    }
    return null;
}

function valuesOf(loaded: LoadedNode, key: string): string[] {
    const values = loaded.properties[key];
    if (values !== undefined && values !== null) {
        return values;
    }
    const fallback = NODE_FIELD_FALLBACKS[key]?.(loaded);
    return fallback === undefined || fallback === null ? [] : [fallback];
}

/**
 * `LRMITool.getFromVCard`: one schema.org Person / Organization per vcard, nameless
 * vcards skipped, a single entry unwrapped. Parsing is the Angular app's own
 * VCard class, so the frontend and this server agree on what a vcard contains.
 */
function fromVCards(vcards: string[] | null | undefined): unknown {
    if (vcards === undefined || vcards === null) {
        return null;
    }
    const result: Record<string, unknown>[] = [];
    for (const raw of vcards) {
        const vcard = new VCard(raw);
        if (vcard.getDisplayName().trim() === '') {
            continue;
        }
        result.push(
            vcard.getType() === VCard.TYPE_PERSON
                ? { '@type': 'Person', givenName: vcard.givenname ?? '', familyName: vcard.surname ?? '' }
                : { '@type': 'Organization', legalName: vcard.org ?? '' },
        );
    }
    if (result.length > 1) {
        return result;
    }
    return result[0] ?? null;
}

function first(values: string[] | null | undefined): string | undefined {
    return values?.[0];
}

/**
 * `DateFormatUtils.ISO_DATETIME_TIME_ZONE_FORMAT` (`yyyy-MM-dd'T'HH:mm:ssZZ`), always
 * UTC. A non numeric value is passed through, as LRMITool does.
 */
function isoDate(value: string | undefined): string | null {
    if (value === undefined || value === '') {
        return null;
    }
    const millis = Number(value);
    if (!Number.isFinite(millis)) {
        return value;
    }
    return `${new Date(millis).toISOString().slice(0, 19)}+00:00`;
}
