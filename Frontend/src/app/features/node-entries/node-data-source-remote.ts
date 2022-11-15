/**
 * Some code from https://github.com/angular/components/blob/15.0.x/src/material/table/table-data-source.ts
 */

import { DataSource } from '@angular/cdk/collections';
import { MatSort, Sort } from '@angular/material/sort';
import { MatTableDataSourcePageEvent, MatTableDataSourcePaginator } from '@angular/material/table';
import { SearchResults } from 'ngx-edu-sharing-api';
import * as rxjs from 'rxjs';
import { BehaviorSubject, Observable, Subscription } from 'rxjs';
import { map, switchMap, tap } from 'rxjs/operators';
import { GenericAuthority, Node } from 'src/app/core-module/core.module';
import { InfiniteScrollPaginator } from './infinite-scroll-paginator';
import { ItemsCap } from './items-cap';
import { NodeCache, NodeCacheRange, NodeCacheSlice } from './node-cache';

type Range = NodeCacheRange;

export interface NodeRequestParams {
    range: Range;
    sort?: Sort;
}

export interface NodeResponse<T> {
    data: T[];
    total: number;
}

// TODO: Rename to something like "fetch implementation" or "request handler"
export type NodeRemote<T> = (params: NodeRequestParams) => Observable<NodeResponse<T>>;

export class NodeDataSourceRemote<
    T extends Node | GenericAuthority,
    P extends MatTableDataSourcePaginator = MatTableDataSourcePaginator,
> extends DataSource<T> {
    get paginator(): P | InfiniteScrollPaginator | null {
        return this._paginator;
    }
    set paginator(value: P | InfiniteScrollPaginator | null) {
        this._paginator = value;
        if (this._remote) {
            this._updateRemoteSubscription();
        }
    }
    private _paginator: P | InfiniteScrollPaginator;

    get sort(): MatSort | null {
        return this._sort;
    }
    set sort(sort: MatSort | null) {
        this._sort = sort;
        this._updateRemoteSubscription();
    }
    private _sort: MatSort | null;

    private _remote: NodeRemote<T>;
    private _renderChangesSubscription: Subscription | null = null;
    private _cache = new NodeCache<T>();
    private dataStream = new BehaviorSubject<T[]>([]);
    isLoading: boolean;
    private _itemsCap: ItemsCap<T> | null;
    get itemsCap(): ItemsCap<T> | null {
        return this._itemsCap;
    }
    set itemsCap(value: ItemsCap<T> | null) {
        this._itemsCap = value;
        this._connectRenderData();
    }
    private renderData = new BehaviorSubject<T[]>([]);
    private renderDataSubscription: Subscription | null;

    constructor() {
        super();
        this.paginator = new InfiniteScrollPaginator();
    }

    connect(): Observable<T[]> {
        if (!this.renderDataSubscription) {
            this._connectRenderData();
        }
        return this.renderData;
    }

    disconnect() {}

    setRemote(remote: NodeRemote<T>): void {
        this._remote = remote;
        this._cache.clear();
        this._updateRemoteSubscription();
    }

    getData() {
        return this.renderData.value;
    }

    isEmpty(): boolean {
        return this.dataStream.value?.length === 0;
    }

    hasMore() {
        if (!this.paginator) {
            return undefined;
        }
        return this.paginator.length > (this.paginator.pageIndex + 1) * this.paginator.pageSize;
    }

    getTotal() {
        return this.paginator.length;
    }

    appendData(appendData: T[], location: 'before' | 'after' = 'after') {
        // TODO: handle pagination
        let data = this.dataStream.value;
        if (location === 'after') {
            data = data.concat(appendData);
        } else {
            data = appendData.concat(data);
        }
        this.dataStream.next(data);
    }

    removeData(data: T[]): void {
        throw new Error('not implemented');
    }

    private _connectRenderData(): void {
        this.renderDataSubscription?.unsubscribe();
        if (this.itemsCap) {
            this.renderDataSubscription = this.itemsCap
                .connect(this.dataStream)
                .subscribe((data) => this.renderData.next(data));
        } else {
            this.renderDataSubscription = this.dataStream.subscribe((data) =>
                this.renderData.next(data),
            );
        }
    }

    private _updateRemoteSubscription(): void {
        if (!this.paginator) {
            // We won't fetch remote data without a paginator in place. Wait for the paginator to be
            // connected.
            return;
        }
        const sortChange: Observable<Sort | null | void> = this._sort
            ? (
                  rxjs.merge(
                      this._sort.sortChange,
                      this._sort.initialized,
                  ) as Observable<Sort | void>
              ).pipe(tap(() => this._cache.clear()))
            : rxjs.of(null);
        const pageChange: Observable<MatTableDataSourcePageEvent | void> = rxjs.merge(
            this._paginator.page,
            //   this._internalPageChanges,
            this._paginator.initialized,
        ) as Observable<MatTableDataSourcePageEvent | void>;

        this._renderChangesSubscription?.unsubscribe();
        this._renderChangesSubscription = rxjs
            .combineLatest([sortChange, pageChange])
            .pipe(
                map(() => this._cache.getMissingRange(this._getRequestRange())),
                switchMap((missingRange) => this._downloadAndCache(missingRange)),
                map(() => this._cache.get(this._getDisplayRange())),
            )
            .subscribe((data) => this.dataStream.next(data));
    }

    private _getRequestRange(): Range {
        return {
            startIndex: this._paginator.pageIndex * this.paginator.pageSize,
            endIndex: (this._paginator.pageIndex + 1) * this._paginator.pageSize,
        };
    }

    private _getDisplayRange(): Range {
        const requestRange = this._getRequestRange();
        return {
            startIndex: requestRange.startIndex,
            endIndex: Math.min(requestRange.endIndex, this.paginator.length),
        };
    }

    private _downloadAndCache(missingRange: Range): Observable<void> {
        if (missingRange) {
            this.isLoading = true;
            return this._remote({ range: missingRange, sort: this._sort }).pipe(
                tap((response) => (this.paginator.length = response.total)),
                tap((response) =>
                    this._cache.add(this._getCacheSlice(missingRange, response.data)),
                ),
                tap({
                    next: () => (this.isLoading = false),
                    error: () => (this.isLoading = false),
                }),
                map(() => void 0),
            );
        } else {
            return rxjs.of(null);
        }
    }

    private _getCacheSlice(range: Range, data: T[]): NodeCacheSlice<T> {
        return {
            startIndex: range.startIndex,
            endIndex: Math.min(range.endIndex, range.startIndex + data.length),
            data,
        };
    }
}

export function fromSearchResults(searchResults: SearchResults): NodeResponse<Node> {
    return {
        data: searchResults.nodes,
        total: searchResults.pagination.total,
    };
}
