import {DataSource} from '@angular/cdk/collections';
import {BehaviorSubject, Observable, ReplaySubject} from 'rxjs';
import {Node, Pagination} from '../../../core-module/rest/data-object';

export class NodeDataSource<T extends Node> extends DataSource<T> {
    private dataStream = new BehaviorSubject<T[]>([]);
    private pagination: Pagination;
    public isLoading: boolean;
    private displayCount: number|null = null;

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

    async appendData(appendData: T[]) {
        console.log('append', appendData);
        let data = this.getData();
        data = data.concat(appendData);
        this.dataStream.next(data);
    }

    setPagination(pagination: Pagination) {
        this.pagination = pagination;
    }

    reset() {
        this.setData([]);
    }

    async hasMore() {
        if(!this.pagination) {
            return undefined;
        }
        return this.pagination.total < (await this.dataStream.asObservable().toPromise()).length;
    }

    getData() {
        return this.dataStream.value;
    }

    isEmpty(): boolean {
        return this.getData()?.length === 0;
    }

    getTotal() {
        return this.pagination.total ?? this.getData()?.length ?? 0;
    }

    /**
     * true if the underlying rendering component is currently displaying all data
     * false otherwise
     * useful to trigger visibility of "show/hide more" elements
     */
    areAllDisplayed() {
        return this.displayCount === null || this.displayCount === this.getData()?.length;
    }

    /**
     * get the actual visible count
     * will return null if no visiblity constrain limit was set to the underlying rendering component
     */
    getDisplayCount() {
        return this.displayCount;
    }
    setDisplayCount(displayCount: number|null = null) {
        if(displayCount === null) {
            this.displayCount = null;
        } else {
            this.displayCount = Math.min(this.getData()?.length, displayCount);
        }
    }
}
