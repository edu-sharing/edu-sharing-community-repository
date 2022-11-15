import { MatTableDataSourcePageEvent, MatTableDataSourcePaginator } from '@angular/material/table';
import * as rxjs from 'rxjs';
import { Subject } from 'rxjs';

export class InfiniteScrollPaginator implements MatTableDataSourcePaginator {
    readonly page = new Subject<MatTableDataSourcePageEvent>();
    private _pageIndex = 0;
    get pageIndex() {
        return this._pageIndex;
    }
    set pageIndex(value) {
        this.pageSize = this._chunkSize;
        this._pageIndex = value;
    }
    initialized = rxjs.of<void>(void 0);
    pageSize = this._chunkSize;
    length = 0;
    firstPage = () => {};
    lastPage = () => {};

    constructor(private _chunkSize = 25) {}

    /**
     *
     * @returns Whether there is more data to load
     */
    loadMore(): boolean {
        if (this.length > this.pageSize) {
            this.pageSize = Math.min(this.pageSize + this._chunkSize, this.length);
            this.page.next(this._getPage());
            return true;
        } else {
            return false;
        }
    }

    private _getPage(): MatTableDataSourcePageEvent {
        return {
            pageIndex: 0,
            pageSize: this.pageSize,
            length: this.length,
        };
    }
}
