/**
 * Some code from https://github.com/angular/components/blob/15.0.x/src/material/table/table-data-source.ts
 */

import { DataSource } from '@angular/cdk/collections';
import { Sort } from '@angular/material/sort';
import { MatTableDataSourcePageEvent, MatTableDataSourcePaginator } from '@angular/material/table';
import { ActivatedRoute, Router } from '@angular/router';
import { SearchResults } from 'ngx-edu-sharing-api';
import * as rxjs from 'rxjs';
import { BehaviorSubject, Observable, ReplaySubject, Subject, Subscription } from 'rxjs';
import { debounceTime, filter, map, switchMap, takeUntil, tap } from 'rxjs/operators';
import { GenericAuthority, Node } from 'src/app/core-module/core.module';
import { ItemsCap } from './items-cap';
import { NodeCache, NodeCacheRange, NodeCacheSlice } from './node-cache';
import { PaginationStrategy } from './node-entries-global.service';

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

export interface PaginationConfig {
    defaultPageSize: number;
    strategy: PaginationStrategy;
}

export class NodeDataSourceRemote<
    T extends Node | GenericAuthority,
    P extends MatTableDataSourcePaginator = MatTableDataSourcePaginator,
> extends DataSource<T> {
    get paginator(): P | null {
        return this._paginationHandler.paginator;
    }
    set paginator(value: P | null) {
        this._paginationHandler.paginator = value;
    }
    private _paginationHandler = new PaginationHandler<P>();
    private _sortHandler = new SortHandler();

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
        this._registerLoadingState();
    }

    connect(): Observable<T[]> {
        if (!this.renderDataSubscription) {
            this._connectRenderData();
        }
        return this.renderData;
    }

    disconnect() {}

    init({
        paginationConfig,
        initialSort,
    }: {
        paginationConfig: PaginationConfig;
        initialSort: Sort;
    }): void {
        this._paginationHandler.init(paginationConfig);
        this._sortHandler.init(initialSort);
    }

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
        return this._paginationHandler.hasMore();
    }

    loadMore() {
        return this._paginationHandler.loadMore();
    }

    getTotal() {
        return this._paginationHandler.length;
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

    registerQueryParameters(route: ActivatedRoute, router: Router): void {
        this._paginationHandler.registerQueryParameters(route, router);
        this._sortHandler.registerQueryParameters(route, router);
    }

    changeSort(sort: Sort): void {
        this._sortHandler.changeSort(sort);
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
        this._resetDone = false;
        const sortChange = rxjs.merge(
            this._sortHandler.sortChange.pipe(
                tap(() => {
                    this._resetDone = false;
                    this._cache.clear();
                    this._paginationHandler.firstPage();
                }),
            ),
            this._sortHandler.initialized,
        );
        const pageChange: Observable<MatTableDataSourcePageEvent | void> = rxjs.merge(
            this._paginationHandler.pageChange,
            //   this._internalPageChanges,
            this._paginationHandler.initialized,
        );

        this._renderChangesSubscription?.unsubscribe();
        this._renderChangesSubscription = rxjs
            .combineLatest([sortChange, pageChange])
            .pipe(
                // Don't send multiple requests in case a sort change triggers a page change.
                debounceTime(0),
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
            startIndex: this._paginationHandler.pageIndex * this._paginationHandler.pageSize,
            endIndex: (this._paginationHandler.pageIndex + 1) * this._paginationHandler.pageSize,
        };
    }

    private _getDisplayRange(): Range {
        const requestRange = this._getRequestRange();
        return {
            startIndex: requestRange.startIndex,
            endIndex: Math.min(requestRange.endIndex, this._paginationHandler.length),
        };
    }

    private _downloadAndCache(missingRange: Range): Observable<void> {
        if (missingRange) {
            this._isLoading.next(true);
            return this._remote({ range: missingRange, sort: this._sortHandler.currentSort }).pipe(
                tap((response) => (this._paginationHandler.length = response.total)),
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

/**
 * A layer between the data source and a paginator component or an infinite-scroll paginator.
 *
 * Without this, the data source would need to wait for a paginator component to be added to the DOM
 * to send its first request. Also, the data source would not be able to function without the
 * paginator component or directive constantly available in the DOM.
 */
class PaginationHandler<P extends MatTableDataSourcePaginator = MatTableDataSourcePaginator> {
    private _paginator: P;
    get paginator(): P {
        return this._paginator;
    }
    set paginator(value: P) {
        this._paginator = value;
        this._initPaginator(value);
    }
    private _config: PaginationConfig;
    private _pageIndex = 0;
    get pageIndex() {
        return this._pageIndex;
    }
    set pageIndex(value) {
        this._pageIndex = value;
        if (this.paginator) {
            this.paginator.pageIndex = value;
        }
    }
    private _pageSize: number;
    get pageSize(): number {
        return this._pageSize;
    }
    set pageSize(value: number) {
        this._pageSize = value;
        if (this.paginator) {
            this.paginator.pageSize = value;
        }
    }
    private _length = 0;
    get length() {
        return this._length;
    }
    set length(value) {
        this._length = value;
        if (this.paginator) {
            this.paginator.length = value;
        }
    }

    private readonly _initialized = new ReplaySubject<void>();
    initialized = this._initialized.asObservable();
    private _isInitialized = false;
    private readonly _pageChange = new Subject<MatTableDataSourcePageEvent>();
    pageChange = this._pageChange.asObservable();
    private readonly _paginatorReset = new Subject<void>();

    init(config: PaginationConfig): void {
        this._config = config;
        this.pageSize = config.defaultPageSize;
        this._isInitialized = true;
        this._initialized.next();
        this._initialized.complete();
    }

    firstPage(): void {
        const previousPageIndex = this.pageIndex;
        const previousPageSize = this.pageSize;
        this.pageIndex = 0;
        if (this._config.strategy === 'infinite-scroll') {
            this.pageSize = this._config.defaultPageSize;
        }
        if (previousPageIndex !== this.pageIndex || previousPageSize !== this.pageSize) {
            this._emitPageEvent();
        }
    }

    hasMore(): boolean {
        if (!this._isInitialized) {
            return undefined;
        }
        return this.length > (this.pageIndex + 1) * this.pageSize;
    }

    /**
     * @returns Whether there is more data to load
     */
    loadMore(): boolean {
        if (this._config.strategy !== 'infinite-scroll') {
            console.warn(
                `Called loadMore with pagination strategy ${this._config.strategy}.`,
                `The only supported strategy for loadMore is 'infinite-scroll'.`,
            );
            return false;
        }
        if (this.hasMore()) {
            this.pageSize = Math.min(this.pageSize + this._config.defaultPageSize, this.length);
            this._emitPageEvent();
            return true;
        } else {
            return false;
        }
    }

    registerQueryParameters(route: ActivatedRoute, router: Router): void {
        const defaultPageSize = this._config.defaultPageSize;
        let currentPageParam = 0;
        let currentPageSizeParam = this.pageSize;
        route.queryParams
            .pipe(
                map((params) => ({
                    page: params.page ? parseInt(params.page) - 1 : 0,
                    pageSize: params.pageSize ? parseInt(params.pageSize) : defaultPageSize,
                })),
                tap(({ page, pageSize }) => {
                    currentPageParam = page;
                    currentPageSizeParam = pageSize;
                }),
                filter(
                    ({ page, pageSize }) => page !== this.pageIndex || pageSize !== this.pageSize,
                ),
            )
            .subscribe(({ page, pageSize }) => {
                this.pageIndex = page;
                this.pageSize = pageSize;
                this._emitPageEvent();
            });
        this.pageChange
            .pipe(
                filter(
                    (event) =>
                        currentPageParam !== event.pageIndex ||
                        currentPageSizeParam !== event.pageSize,
                ),
            )
            .subscribe((event) => {
                const page = event.pageIndex > 0 ? event.pageIndex + 1 : null;
                const pageSize = event.pageSize !== defaultPageSize ? event.pageSize : null;
                void router.navigate([], {
                    queryParams: { page, pageSize },
                    queryParamsHandling: 'merge',
                });
            });
    }

    private _initPaginator(paginator: MatTableDataSourcePaginator): void {
        this._paginatorReset.next();
        if (paginator) {
            paginator.pageIndex = this.pageIndex;
            paginator.pageSize = this.pageSize;
            paginator.length = this.length;
            paginator.page.pipe(takeUntil(this._paginatorReset)).subscribe((pageEvent) => {
                this.pageIndex = pageEvent.pageIndex;
                this.pageSize = pageEvent.pageSize;
                this.length = pageEvent.length;
                this._emitPageEvent();
            });
        }
    }

    private _emitPageEvent(): void {
        this._pageChange.next({
            pageIndex: this.pageIndex,
            pageSize: this.pageSize,
            length: this.length,
        });
    }
}

class SortHandler {
    private readonly _sortChange = new Subject<Sort>();
    readonly sortChange = this._sortChange.asObservable();
    private readonly _initialized = new ReplaySubject<void>();
    readonly initialized = this._initialized.asObservable();

    private _currentSort: Sort;
    get currentSort(): Sort {
        return this._currentSort;
    }

    init(sort: Sort): void {
        this._currentSort = sort;
        this._initialized.next();
        this._initialized.complete();
    }

    changeSort(sort: Sort): void {
        this._currentSort = sort;
        this._sortChange.next(sort);
    }

    registerQueryParameters(route: ActivatedRoute, router: Router) {
        // throw new Error('Method not implemented.');
    }
}
