import { interpolate } from './interpolate-util';

const scope = {
    item: { nodeId: 'abc-123', title: 'Optik' },
    index: 2,
    page: { collectionId: 'root-1' },
};

describe('interpolate', () => {
    it('resolves a dotted path inside a string', () => {
        void expect(interpolate('Neu in ${item.title}', scope)).toBe('Neu in Optik');
    });

    it('resolves placeholders in nested objects and arrays', () => {
        const config = {
            headline: 'Neu in ${item.title}',
            propertyFilters: { 'virtual:collection_id_tree': ['${item.nodeId}'] },
            sort: { active: 'cm:modified', direction: 'desc' },
        };
        void expect(interpolate(config, scope)).toEqual({
            headline: 'Neu in Optik',
            propertyFilters: { 'virtual:collection_id_tree': ['abc-123'] },
            sort: { active: 'cm:modified', direction: 'desc' },
        });
    });

    it('leaves the source untouched', () => {
        const config = { headline: '${item.title}' };
        interpolate(config, scope);
        void expect(config.headline).toBe('${item.title}');
    });

    it('keeps the double-brace tags of AI-generated texts', () => {
        void expect(interpolate('{{node(cm:title)|-}} in ${item.title}', scope)).toBe(
            '{{node(cm:title)|-}} in Optik',
        );
    });

    it('drops a placeholder the scope does not cover', () => {
        void expect(interpolate('a${item.unknown}b', scope)).toBe('ab');
        void expect(interpolate('${nothing.at.all}', scope)).toBe('');
    });

    it('stringifies non-string values', () => {
        void expect(interpolate('position ${index}', scope)).toBe('position 2');
    });

    it('passes values that are not strings, objects or arrays through', () => {
        void expect(interpolate(7, scope)).toBe(7);
        void expect(interpolate(true, scope)).toBe(true);
        void expect(interpolate(null, scope)).toBeNull();
        void expect(interpolate(undefined, scope)).toBeUndefined();
    });
});
