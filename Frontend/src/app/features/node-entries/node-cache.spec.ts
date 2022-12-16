import { NodeCache } from './node-cache';

const ARRAY_0_5 = [0, 1, 2, 3, 4];
const ARRAY_5_10 = [5, 6, 7, 8, 9];
const ARRAY_0_10 = [0, 1, 2, 3, 4, 5, 6, 7, 8, 9];

describe('NodeCache', () => {
    let nodeCache: NodeCache<number>;

    beforeEach(() => {
        nodeCache = new NodeCache();
    });

    it('should find an exact match from 0', () => {
        nodeCache.add({
            startIndex: 0,
            endIndex: 10,
            data: ARRAY_0_10,
        });
        expect(nodeCache.get({ startIndex: 0, endIndex: 10 })).toEqual(ARRAY_0_10);
    });

    it('should find an exact match from 10', () => {
        nodeCache.add({
            startIndex: 10,
            endIndex: 20,
            data: ARRAY_0_10,
        });
        expect(nodeCache.get({ startIndex: 10, endIndex: 20 })).toEqual(ARRAY_0_10);
    });

    it('should find an exact match among multiple slices', () => {
        nodeCache.add({
            startIndex: 0,
            endIndex: 10,
            data: ARRAY_0_10,
        });
        nodeCache.add({
            startIndex: 20,
            endIndex: 30,
            data: ARRAY_0_10,
        });
        expect(nodeCache.get({ startIndex: 20, endIndex: 30 })).toEqual(ARRAY_0_10);
    });

    it('should find a partial match', () => {
        nodeCache.add({
            startIndex: 0,
            endIndex: 10,
            data: ARRAY_0_10,
        });
        expect(nodeCache.get({ startIndex: 3, endIndex: 7 })).toEqual([3, 4, 5, 6]);
    });

    it('should merge two connected slices', () => {
        nodeCache.add({
            startIndex: 0,
            endIndex: 5,
            data: ARRAY_0_5,
        });
        nodeCache.add({
            startIndex: 5,
            endIndex: 10,
            data: ARRAY_5_10,
        });
        expect(nodeCache.get({ startIndex: 0, endIndex: 10 })).toEqual(ARRAY_0_10);
    });

    it('should merge two reverse-connected slices', () => {
        nodeCache.add({
            startIndex: 5,
            endIndex: 10,
            data: ARRAY_5_10,
        });
        nodeCache.add({
            startIndex: 0,
            endIndex: 5,
            data: ARRAY_0_5,
        });
        expect(nodeCache.get({ startIndex: 0, endIndex: 10 })).toEqual(ARRAY_0_10);
    });

    it('should merge two overlapping slices', () => {
        nodeCache.add({
            startIndex: 0,
            endIndex: 7,
            data: [0, 1, 2, 3, 4, 5, 6],
        });
        nodeCache.add({
            startIndex: 3,
            endIndex: 8,
            data: [3, 4, 5, 6, 7],
        });
        expect(nodeCache.get({ startIndex: 0, endIndex: 8 })).toEqual([0, 1, 2, 3, 4, 5, 6, 7]);
    });

    it('should fill a gap', () => {
        nodeCache.add({
            startIndex: 0,
            endIndex: 3,
            data: [0, 1, 2],
        });
        nodeCache.add({
            startIndex: 6,
            endIndex: 9,
            data: [6, 7, 8],
        });
        nodeCache.add({
            startIndex: 3,
            endIndex: 6,
            data: [3, 4, 5],
        });
        expect(nodeCache.get({ startIndex: 0, endIndex: 9 })).toEqual([0, 1, 2, 3, 4, 5, 6, 7, 8]);
    });

    it('should find an exact missing range at the beginning', () => {
        nodeCache.add({
            startIndex: 6,
            endIndex: 9,
            data: [6, 7, 8],
        });
        expect(nodeCache.getMissingRange({ startIndex: 0, endIndex: 9 })).toEqual({
            startIndex: 0,
            endIndex: 6,
        });
    });

    it('should find an exact missing range at the end', () => {
        nodeCache.add({
            startIndex: 0,
            endIndex: 3,
            data: [0, 1, 2],
        });
        expect(nodeCache.getMissingRange({ startIndex: 0, endIndex: 9 })).toEqual({
            startIndex: 3,
            endIndex: 9,
        });
    });

    it('should find an exact missing range in the middle', () => {
        nodeCache.add({
            startIndex: 0,
            endIndex: 3,
            data: [0, 1, 2],
        });
        nodeCache.add({
            startIndex: 6,
            endIndex: 9,
            data: [6, 7, 8],
        });
        expect(nodeCache.getMissingRange({ startIndex: 0, endIndex: 9 })).toEqual({
            startIndex: 3,
            endIndex: 6,
        });
    });

    it('should find an overlapped missing range at the beginning', () => {
        nodeCache.add({
            startIndex: 6,
            endIndex: 9,
            data: [6, 7, 8],
        });
        expect(nodeCache.getMissingRange({ startIndex: 0, endIndex: 7 })).toEqual({
            startIndex: 0,
            endIndex: 6,
        });
    });

    it('should find an overlapped missing range in the middle', () => {
        nodeCache.add({
            startIndex: 0,
            endIndex: 3,
            data: [0, 1, 2],
        });
        nodeCache.add({
            startIndex: 6,
            endIndex: 9,
            data: [6, 7, 8],
        });
        expect(nodeCache.getMissingRange({ startIndex: 2, endIndex: 7 })).toEqual({
            startIndex: 3,
            endIndex: 6,
        });
    });
});
