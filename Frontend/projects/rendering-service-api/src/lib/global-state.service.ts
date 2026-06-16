import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

@Injectable({
    providedIn: 'root',
})
export class GlobalStateService {
    downloadUrl$ = new BehaviorSubject<string | undefined>(undefined);

    constructor() {}

    /**
     * used by rs: set a custom download url for the current service
     * i.e. if a special, temporary download url is available
     */
    setDownloadUrl(downloadUrl?: string) {
        this.downloadUrl$.next(downloadUrl);
    }
}
