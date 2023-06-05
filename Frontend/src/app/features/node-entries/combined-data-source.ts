import { Observable, of } from 'rxjs';
import { GenericAuthority, Node } from '../../core-module/core.module';
import { NodeDataSource } from './node-data-source';

/**
 * data source which joins multiple underlying data sources
 * used for the "all" search
 */
export class CombinedDataSource<T extends Node | GenericAuthority> extends NodeDataSource<T> {
    constructor(private dataSources: NodeDataSource<T>[]) {
        super();
    }

    connect(): Observable<T[]> {
        // @TODO: Using a forkJoin and real connect would be better
        return of(this.getData());
        /*
        return forkJoin(this.dataSources.map(ds => ds.connect())).pipe(
            switchMap(d => d)
        ) as Observable<T[]>;
         */
    }

    disconnect() {}
    hasMore() {
        return this.dataSources.filter((ds) => ds.hasMore()).length > 0;
    }

    getData() {
        return [].concat.apply(
            [],
            this.dataSources.map((ds) => ds.getData()),
        );
    }

    isEmpty(): boolean {
        return this.getData()?.length === 0;
    }

    getTotal() {
        return this.dataSources.map((ds) => ds.getTotal()).reduce((a, b) => a + b);
    }

    isFullyLoaded() {
        return this.getTotal() <= this.getData()?.length;
    }

    getDatasource(position: number) {
        return this.dataSources[position];
    }
}
