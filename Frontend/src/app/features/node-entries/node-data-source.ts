import { DataSource } from '@angular/cdk/collections';
import { BehaviorSubject, Observable } from 'rxjs';
import * as rxjs from 'rxjs';
import { distinctUntilChanged, map, shareReplay } from 'rxjs/operators';
import { GenericAuthority, Pagination, Node } from 'src/app/core-module/core.module';
import { Helper } from '../../core-module/rest/helper';

export class NodeDataSource<T extends Node | GenericAuthority> extends DataSource<T> {
    private dataStream = new BehaviorSubject<T[]>([]);
    private pagination: Pagination;
    public isLoadingSubject = new BehaviorSubject<boolean>(false);
    get isLoading() {
        return this.isLoadingSubject.value;
    }
    set isLoading(isLoading: boolean) {
        this.isLoadingSubject.next(isLoading);
    }
    private displayCountSubject = new BehaviorSubject<number | null>(null);
    private areAllDisplayed$ = rxjs.combineLatest([this.dataStream, this.displayCountSubject]).pipe(
        map(([data, displayCount]) => this.getAreAllDisplayed(displayCount, data)),
        distinctUntilChanged(),
        shareReplay(1),
    );

    constructor(initialData: T[] = []) {
        super();
        this.setData(initialData);
    }

    connect(): Observable<T[]> {
        return this.dataStream;
    }

    disconnect() {}

    setData(data: T[], pagination: Pagination = null) {
        this.dataStream.next(data);
        this.setPagination(pagination);
    }

    appendData(appendData: T[], location: 'before' | 'after' = 'after') {
        let data = this.getData();
        if (location === 'after') {
            data = data.concat(appendData);
        } else {
            data = appendData.concat(data);
        }
        this.dataStream.next(data);
    }

    /**
     * Removes elements from the visible data.
     */
    removeData(removeData: T[]): void {
        const data = this.getData().filter(
            (value) =>
                !removeData.find((d) => Helper.objectEquals((d as Node).ref, (value as Node).ref)),
        );
        this.dataStream.next(data);
        if (this.pagination) {
            this.setPagination({
                count: this.pagination.count - removeData.length,
                from: this.pagination.from,
                total: this.pagination.total - removeData.length,
            });
        }
    }

    setPagination(pagination: Pagination) {
        this.pagination = pagination;
    }

    reset() {
        this.setData([]);
    }

    hasMore() {
        if (!this.pagination) {
            return undefined;
        }
        return this.pagination.total > this.getData()?.length;
    }

    getData() {
        return this.dataStream.value;
    }

    isEmpty(): boolean {
        return this.getData()?.length === 0;
    }

    getTotal() {
        return this.pagination?.total ?? this.getData()?.length ?? 0;
    }

    /**
     * true if the underlying rendering component is currently displaying all data
     * false otherwise
     * useful to trigger visibility of "show/hide more" elements
     */
    areAllDisplayed(): Observable<boolean> {
        return this.areAllDisplayed$;
    }

    private getAreAllDisplayed(displayCount: number | null, data?: T[]): boolean {
        return displayCount === null || displayCount === data?.length;
    }

    /**
     * get the actual visible count
     * will return null if no visiblity constrain limit was set to the underlying rendering component
     */
    getDisplayCount() {
        return this.displayCountSubject.value;
    }
    setDisplayCount(displayCount: number | null = null) {
        if (displayCount === null) {
            this.displayCountSubject.next(null);
        } else {
            this.displayCountSubject.next(Math.min(this.getData()?.length, displayCount));
        }
    }

    isFullyLoaded() {
        return this.getTotal() <= this.getData()?.length;
    }

    /**
     * force a refresh of all elements in the current data stream
     * trigger this to enforce a rebuild of the nodes in all sub-components
     * i.e. if data from some nodes has changed
     */
    refresh() {
        this.dataStream.next(Helper.deepCopy(this.dataStream.value));
    }
}
