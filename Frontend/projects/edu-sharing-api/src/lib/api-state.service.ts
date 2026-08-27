import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable, Subject } from 'rxjs';

@Injectable({
    providedIn: 'root',
})
export class ApiStateService {
    /**
     * current count of ongoing (not finished, either successfully or failed, api requests)
     * Note: Due to legacy reasons, the full sum should be obtained using the RestConnectorService
     */
    ongoingRequestsCount$ = new BehaviorSubject(0);

    /**
     * Last authentication state reported by the backend via the `X-Edu-Authenticated` response
     * header. `null` means it is not known yet (no response carrying the header was received).
     */
    readonly authenticated$ = new BehaviorSubject<boolean | null>(null);

    private readonly authenticationLostSubject = new Subject<void>();

    /**
     * Fires when the backend reports a previously authenticated session as no longer authenticated,
     * e.g. after a session timeout or a restart of the repository.
     */
    observeAuthenticationLost(): Observable<void> {
        return this.authenticationLostSubject;
    }

    /**
     * Updates the authentication state as reported by the backend. Called by the `ApiInterceptor`
     * for each response carrying the `X-Edu-Authenticated` header.
     */
    updateAuthenticated(authenticated: boolean, triggerLost: boolean): void {
        const previous = this.authenticated$.value;
        if (previous === authenticated) {
            return;
        }
        this.authenticated$.next(authenticated);
        if (previous === true && !authenticated && triggerLost) {
            this.authenticationLostSubject.next();
        }
    }
}
