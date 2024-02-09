import { notNull } from '../../util/functions';

export interface NodeCacheRange {
    startIndex: number;
    endIndex: number;
}

export interface NodeCacheSlice<T> extends NodeCacheRange {
    data: readonly T[];
}

export class NodeCache<T> {
    private _slices: readonly NodeCacheSlice<T>[] = [];

    add(slice: NodeCacheSlice<T>): void {
        if (slice.endIndex - slice.startIndex !== slice.data.length) {
            throw new Error('Tried to add invalid slice to cache: ' + JSON.stringify(slice));
        }
        this._slices = this._normalizeSlices([...this._slices, slice]);
    }

    clear() {
        this._slices = [];
    }

    get(range: NodeCacheRange): T[] | null {
        for (const slice of this._slices) {
            if (slice.startIndex <= range.startIndex && slice.endIndex >= range.endIndex) {
                return slice.data.slice(
                    range.startIndex - slice.startIndex,
                    range.endIndex - slice.startIndex,
                );
            }
        }
        // for the first request
        console.warn(
            'Requested range was not found in the slices cache, falling back to a smaller slice (the backend resolved too less data)',
            range,
            this._slices,
        );
        for (const slice of this._slices) {
            if (slice.startIndex <= range.startIndex && slice.endIndex >= range.startIndex) {
                console.log(slice);
                return slice.data.slice(
                    range.startIndex - slice.startIndex,
                    range.endIndex - slice.startIndex,
                );
            }
        }
        console.error('Could not find any slice for the range', range, this._slices);
        return null;
    }

    getMissingRange(requestedRange: NodeCacheRange): NodeCacheRange | null {
        let fromIndex = requestedRange.startIndex;
        let toIndex = requestedRange.endIndex;
        for (const slice of this._slices) {
            if (slice.startIndex <= fromIndex && slice.endIndex > fromIndex) {
                fromIndex = slice.endIndex;
            } else if (slice.startIndex < toIndex && slice.endIndex >= toIndex) {
                toIndex = slice.startIndex;
            }
            if (fromIndex >= toIndex) {
                return null;
            }
        }
        return { startIndex: fromIndex, endIndex: toIndex };
    }

    private _normalizeSlices(slices: NodeCacheSlice<T>[]): NodeCacheSlice<T>[] {
        slices.sort((lhs, rhs) => lhs.startIndex - rhs.startIndex);
        for (let i = 0; i < slices.length - 1; i++) {
            for (let j = i + 1; j < slices.length; j++) {
                if (this._canMerge(slices[i], slices[j])) {
                    slices[i] = this._merge(slices[i], slices[j]);
                    slices[j] = null; // Mark for deletion
                }
            }
        }
        return slices.filter(notNull);
    }

    private _canMerge(lhs: NodeCacheSlice<T>, rhs: NodeCacheSlice<T>): boolean {
        if (!lhs || !rhs) {
            return false;
        }
        return lhs.endIndex >= rhs.startIndex;
    }

    private _merge(lhs: NodeCacheSlice<T>, rhs: NodeCacheSlice<T>): NodeCacheSlice<T> {
        return {
            startIndex: lhs.startIndex,
            endIndex: rhs.endIndex,
            data: [...lhs.data, ...rhs.data.slice(lhs.endIndex - rhs.startIndex)],
        };
    }
}
