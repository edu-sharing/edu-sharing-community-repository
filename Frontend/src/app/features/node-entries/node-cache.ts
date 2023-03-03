export interface NodeCacheRange {
    startIndex: number;
    endIndex: number;
}

export interface NodeCacheSlice<T> extends NodeCacheRange {
    data: T[];
}

export class NodeCache<T> {
    private _slices: NodeCacheSlice<T>[] = [];

    add(slice: NodeCacheSlice<T>): void {
        if (slice.endIndex - slice.startIndex !== slice.data.length) {
            throw new Error('Tried to add invalid slice to cache: ' + JSON.stringify(slice));
        }
        this._slices.push(slice);
        this._normalizeSlices();
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

    private _normalizeSlices(): void {
        this._slices.sort((lhs, rhs) => lhs.startIndex - rhs.startIndex);
        for (let i = 0; i < this._slices.length - 1; i++) {
            for (let j = i + 1; j < this._slices.length; j++) {
                if (this._canMerge(this._slices[i], this._slices[j])) {
                    this._slices[i] = this._merge(this._slices[i], this._slices[j]);
                }
            }
        }
    }

    private _canMerge(lhs: NodeCacheSlice<T>, rhs: NodeCacheSlice<T>): boolean {
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
