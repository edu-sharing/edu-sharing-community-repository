import {
    SEARCH_FILTERS_QUERY_PARAM,
    SEARCH_INPUT_QUERY_PARAM,
    WIDGETS,
} from '../types/custom-definitions';
import { GridTile } from '../types/grid-tile';
import { Swimlane } from '../types/swimlane';
import {
    computeSwimlaneHitMatching,
    parseSearchFilters,
    sameSearchFilters,
    searchStateToQueryParams,
} from './search-hit-util';

const swimlane = (id: string, grid: Partial<GridTile>[] = []): Swimlane =>
    ({ id, grid } as unknown as Swimlane);

describe('search-hit-util', () => {
    describe('computeSwimlaneHitMatching', () => {
        it('matches swimlanes by title, by tile hits and by always visible widgets', () => {
            const swimlanes: Swimlane[] = [
                swimlane('none', [{ hasHits: false }]),
                swimlane('title'),
                swimlane('hits', [{ hasHits: false }, { hasHits: true }]),
                swimlane('chips', [{ item: WIDGETS.COLLECTION_CHIPS }]),
                swimlane('empty'),
            ];
            const result = computeSwimlaneHitMatching(swimlanes, new Map([['title', true]]));
            void expect([...result.bySwimlaneId]).toEqual([
                ['none', false],
                ['title', true],
                ['hits', true],
                ['chips', true],
                ['empty', false],
            ]);
            void expect(result.firstMatchingSwimlaneId).toBe('title');
            void expect(result.lastMatchingSwimlaneId).toBe('chips');
        });

        it('has no first or last swimlane without any match', () => {
            const result = computeSwimlaneHitMatching([swimlane('a')], new Map());
            void expect(result.firstMatchingSwimlaneId).toBeUndefined();
            void expect(result.lastMatchingSwimlaneId).toBeUndefined();
        });
    });

    describe('parseSearchFilters', () => {
        it('reads the JSON of the param and ignores anything else', () => {
            void expect(parseSearchFilters('{"ccm:taxonid":["a"]}')).toEqual({
                'ccm:taxonid': ['a'],
            });
            void expect(parseSearchFilters('not json')).toEqual({});
            void expect(parseSearchFilters(undefined)).toEqual({});
        });
    });

    describe('sameSearchFilters', () => {
        it('ignores the order of properties and values', () => {
            void expect(
                sameSearchFilters({ a: ['1', '2'], b: ['3'] }, { b: ['3'], a: ['2', '1'] }),
            ).toBeTrue();
            void expect(sameSearchFilters({}, {})).toBeTrue();
        });

        it('tells apart differing selections', () => {
            void expect(sameSearchFilters({ a: ['1'] }, { a: ['2'] })).toBeFalse();
            void expect(sameSearchFilters({ a: ['1'] }, { a: ['1'], b: ['2'] })).toBeFalse();
            void expect(sameSearchFilters({ a: ['1'] }, {})).toBeFalse();
        });
    });

    describe('searchStateToQueryParams', () => {
        it('removes empty values from the URL', () => {
            void expect(searchStateToQueryParams('', {})).toEqual({
                [SEARCH_INPUT_QUERY_PARAM]: null,
                [SEARCH_FILTERS_QUERY_PARAM]: null,
            });
            void expect(searchStateToQueryParams('math', { a: ['b'] })).toEqual({
                [SEARCH_INPUT_QUERY_PARAM]: 'math',
                [SEARCH_FILTERS_QUERY_PARAM]: '{"a":["b"]}',
            });
        });
    });
});
