import { Params } from '@angular/router';
import { Values } from 'ngx-edu-sharing-ui';
import {
    SEARCH_FILTERS_QUERY_PARAM,
    SEARCH_INPUT_QUERY_PARAM,
    WIDGET_TYPE,
    WIDGETS,
} from '../types/custom-definitions';
import { GridTile } from '../types/grid-tile';
import { Swimlane } from '../types/swimlane';

// widgets that stay visible while the page is searched, as they navigate rather than list results
const ALWAYS_MATCHING_WIDGETS: WIDGET_TYPE[] = [
    WIDGETS.COLLECTION_CHIPS,
    WIDGETS.TOPICS_COLUMN_BROWSER,
];

export interface SwimlaneHitMatching {
    // per swimlane ID, whether the swimlane stays visible for the current search
    bySwimlaneId: Map<string, boolean>;
    firstMatchingSwimlaneId: string | undefined;
    lastMatchingSwimlaneId: string | undefined;
}

/**
 * Decides which swimlanes match the current search: by their title, by a grid tile with hits, or
 * by a tile whose widget always stays visible.
 */
export const computeSwimlaneHitMatching = (
    swimlanes: Swimlane[],
    titleMatchesBySwimlaneId: Map<string, boolean>,
): SwimlaneHitMatching => {
    const bySwimlaneId: Map<string, boolean> = new Map<string, boolean>();
    swimlanes.forEach((swimlane: Swimlane): void => {
        const matches: boolean =
            (titleMatchesBySwimlaneId.get(swimlane.id) ?? false) ||
            (swimlane.grid ?? []).some(
                (tile: GridTile): boolean =>
                    tile.hasHits || ALWAYS_MATCHING_WIDGETS.includes(tile.item),
            );
        bySwimlaneId.set(swimlane.id, matches);
    });
    const matching: Swimlane[] = swimlanes.filter((swimlane: Swimlane): boolean =>
        bySwimlaneId.get(swimlane.id),
    );
    return {
        bySwimlaneId,
        firstMatchingSwimlaneId: matching[0]?.id,
        lastMatchingSwimlaneId: matching[matching.length - 1]?.id,
    };
};

/**
 * Reads a filter query param, which holds the values keyed by property as JSON.
 */
export const parseSearchFilters = (param: string): Values => {
    if (!param) {
        return {};
    }
    try {
        return JSON.parse(param) as Values;
    } catch {
        // the param is part of a URL and may hold anything
        return {};
    }
};

/**
 * Whether two sets of page filters select the same values, regardless of the order of their
 * properties and values.
 */
export const sameSearchFilters = (a: Values, b: Values): boolean => {
    const normalize = (filters: Values): string =>
        JSON.stringify(
            Object.keys(filters ?? {})
                .sort()
                .map((key: string) => [key, [...(filters[key] ?? [])].sort()]),
        );
    return normalize(a) === normalize(b);
};

/**
 * The query params holding a search input and page filters; empty ones are removed from the URL.
 */
export const searchStateToQueryParams = (searchInput: string, filters: Values): Params => {
    const hasFilters: boolean = Object.keys(filters ?? {}).length > 0;
    return {
        [SEARCH_INPUT_QUERY_PARAM]: searchInput || null,
        [SEARCH_FILTERS_QUERY_PARAM]: hasFilters ? JSON.stringify(filters) : null,
    };
};
