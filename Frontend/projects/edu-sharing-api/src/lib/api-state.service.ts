import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

@Injectable({
    providedIn: 'root',
})
export class ApiStateService {
    /**
     * current count of ongoing (not finished, either successfully or failed, api requests)
     * Note: Due to legacy reasons, the full sum should be obtained using the RestConnectorService
     */
    ongoingRequestsCount$ = new BehaviorSubject(0);
}
