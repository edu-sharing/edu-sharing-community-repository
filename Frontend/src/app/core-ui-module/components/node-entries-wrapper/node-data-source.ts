import {DataSource} from '@angular/cdk/collections';
import {BehaviorSubject, Observable, ReplaySubject} from 'rxjs';
import {Node, Pagination} from '../../../core-module/rest/data-object';

export class NodeDataSource<T extends Node> extends DataSource<T> {
    private dataStream = new BehaviorSubject<T[]>([]);
    private pagination: Pagination;
    public isLoading: boolean;

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
        let data = await this.dataStream.asObservable().toPromise();
        data = data.concat(appendData);
        this.dataStream.next(data);
    }

    private setPagination(pagination: Pagination) {
        this.pagination = pagination;
    }

    reset() {
        this.setData([]);
    }

    async hasMore() {
        return this.pagination?.total < (await this.dataStream.asObservable().toPromise()).length;
    }

    getData() {
        return this.dataStream.value;
    }

    isEmpty(): boolean {
        return this.getData()?.length === 0;
    }
}
