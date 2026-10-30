import {
    COLLECTION_TREE_PRIMARY_PROPERTY,
    getCollectionTreeSearchProperty,
    readCollectionTreeValues,
} from './collection-tree-config';

const WIDGET_ID = 'virtual:collection_id_tree';

describe('getCollectionTreeSearchProperty', () => {
    it('uses the widget property while sub-collections are included', () => {
        expect(getCollectionTreeSearchProperty(WIDGET_ID, true)).toBe(WIDGET_ID);
    });

    it('uses the primary property when sub-collections are left out', () => {
        expect(getCollectionTreeSearchProperty(WIDGET_ID, false)).toBe(
            COLLECTION_TREE_PRIMARY_PROPERTY,
        );
    });
});

describe('readCollectionTreeValues', () => {
    it('reads the widget property as including sub-collections', () => {
        expect(readCollectionTreeValues(WIDGET_ID, { [WIDGET_ID]: ['a'] }, false)).toEqual({
            value: ['a'],
            includeSubcollections: true,
        });
    });

    it('reads the primary property as leaving out sub-collections', () => {
        expect(
            readCollectionTreeValues(
                WIDGET_ID,
                { [COLLECTION_TREE_PRIMARY_PROPERTY]: ['a'] },
                false,
            ),
        ).toEqual({ value: ['a'], includeSubcollections: false });
    });

    it('prefers the widget property when both are given', () => {
        expect(
            readCollectionTreeValues(
                WIDGET_ID,
                {
                    [WIDGET_ID]: ['a'],
                    [COLLECTION_TREE_PRIMARY_PROPERTY]: ['b'],
                },
                false,
            ),
        ).toEqual({ value: ['a'], includeSubcollections: true });
    });

    it('includes sub-collections without any values', () => {
        expect(readCollectionTreeValues(WIDGET_ID, undefined, false)).toEqual({
            value: undefined,
            includeSubcollections: true,
        });
    });

    it('leaves the primary property to a widget of its own', () => {
        expect(
            readCollectionTreeValues(
                WIDGET_ID,
                { [COLLECTION_TREE_PRIMARY_PROPERTY]: ['a'] },
                true,
            ),
        ).toEqual({ value: undefined, includeSubcollections: true });
    });

    it('never includes sub-collections for a widget holding the primary property', () => {
        expect(
            readCollectionTreeValues(
                COLLECTION_TREE_PRIMARY_PROPERTY,
                { [COLLECTION_TREE_PRIMARY_PROPERTY]: ['a'] },
                false,
            ),
        ).toEqual({ value: ['a'], includeSubcollections: false });
    });
});
