/**
 * Some code from https://github.com/angular/components/blob/15.0.x/src/material/table/table-data-source.ts
 */

import { DataSource } from '@angular/cdk/collections';
import { MatSort, Sort } from '@angular/material/sort';
import { MatTableDataSourcePageEvent, MatTableDataSourcePaginator } from '@angular/material/table';
import { SearchResults } from 'ngx-edu-sharing-api';
import * as rxjs from 'rxjs';
import { BehaviorSubject, Observable, Subscription } from 'rxjs';
import { filter, map, switchMap, tap } from 'rxjs/operators';
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

export type LoadingState =
    // The data source is loading data for the first time.
    | 'initial'
    // Loading data after change of parameters, i.e., it will replace the current data when done.
    | 'reset'
    // Loading another page with unchanged parameters.
    | 'page'
    // Loading done.
    | false;

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
        if (this._remote) {
            this._updateRemoteSubscription();
        }
    }
    private _sort: MatSort | null;

    private _remote: NodeRemote<T>;
    private _renderChangesSubscription: Subscription | null = null;
    private _cache = new NodeCache<T>();
    private dataStream = new BehaviorSubject<T[]>([]);
    isLoading: LoadingState;
    private _itemsCap: ItemsCap<T> | null;
    get itemsCap(): ItemsCap<T> | null {
        return this._itemsCap;
    }
    set itemsCap(value: ItemsCap<T> | null) {
        this._itemsCap = value;
        // Only reconnect if already connected.
        if (this.renderDataSubscription) {
            this._connectRenderData();
        }
    }
    private renderData = new BehaviorSubject<T[]>([]);
    private renderDataSubscription: Subscription | null;
    private _isLoading = new BehaviorSubject<boolean>(false);
    private _initDone = false;
    private _resetDone = false;

    constructor() {
        super();
        this.paginator = new InfiniteScrollPaginator();
        this._registerLoadingState();
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

    private _registerLoadingState(): void {
        this._isLoading.subscribe((isLoading) => {
            if (!isLoading) {
                this.isLoading = false;
            } else if (!this._initDone) {
                this.isLoading = 'initial';
            } else if (!this._resetDone) {
                this.isLoading = 'reset';
            } else {
                this.isLoading = 'page';
            }
        });
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
        if (!this.paginator || !this.sort) {
            // We won't fetch remote data without a paginator or sorting in place. Wait for the
            // components to be connected to be connected.
            //
            // TODO: Use an approach were pagination and sorting is handled independently from UI
            // components and UI components are merely controlled by that logic. This way, we can do
            // our request earlier and don't get stuck, when UI components are disabled with ngIf.
            //
            // Concept:
            // - Provide configuration like page size and default sorting to the module handling
            //   pagination and sorting
            // - Register UI components with that module when they become available
            // - This module is the source of truth. For query params etc., we listen for events by
            //   this module.
            return;
        }
        this._resetDone = false;
        const sortChange: Observable<Sort | null | void> = this._sort
            ? (rxjs.merge(
                  this._sort.sortChange.pipe(
                      tap(() => {
                          this._resetDone = false;
                          this._cache.clear();
                          this.paginator.pageIndex = 0;
                          this.paginator.page.next({
                              pageIndex: 0,
                              pageSize: this.paginator.pageSize,
                              length: this.paginator.length,
                          });
                      }),
                      // Stop propagation and instead rely on the page event to trigger the request.
                      filter(() => false),
                  ),
                  this._sort.initialized,
              ) as Observable<Sort | void>)
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
                tap(() => {
                    this._initDone = true;
                    this._resetDone = true;
                }),
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
            this._isLoading.next(true);
            return this._remote({ range: missingRange, sort: this._sort }).pipe(
                tap((response) => (this.paginator.length = response.total)),
                tap((response) =>
                    this._cache.add(this._getCacheSlice(missingRange, response.data)),
                ),
                tap({
                    next: () => this._isLoading.next(false),
                    error: () => this._isLoading.next(false),
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
