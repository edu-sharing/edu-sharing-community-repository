import { Injectable } from '@angular/core';
import * as rxjs from 'rxjs';
import { BehaviorSubject, Observable } from 'rxjs';
import { filter, first, map, mapTo, scan, share, startWith, switchMap, tap } from 'rxjs/operators';
import { ApiRequestConfiguration } from '../api-request-configuration';
import * as apiModels from '../api/models';
import { AuthenticationV1Service as AuthenticationApiService } from '../api/services';
import { switchRelay } from '../utils/switch-relay';

export type LoginInfo = apiModels.Login;

type LoginAction =
    | {
          // Logs the user in with the provided credentials.
          kind: 'login';
          username: string;
          password: string;
          scope?: string;
      }
    | {
          // Logs the user out.
          kind: 'logout';
      }
    | {
          // Initially fetches login information from the backend.
          kind: 'initial';
      }
    | {
          // Triggers a refresh of login information after the state was changed from outside.
          kind: 'forceRefresh';
      };

type LoginActionResponse = {
    loginInfo: LoginInfo;
    loginAction: LoginAction;
};

@Injectable({
    providedIn: 'root',
})
export class AuthenticationService {
    /** Triggers requests concerning the login state. */
    // We funnel login actions through this subject, so rapidly requested actions will not update
    // `loginInfoSubject` uncontrolled but unfinished actions will be canceled cleanly.
    private readonly loginActionTrigger = new BehaviorSubject<LoginAction | null>(null);
    /**
     * The last login action with its returned login information..
     *
     * Becomes `null` while a request is underway, so subscribers to `loginInfo$` who subscribed
     * after triggering an action will receive the login information after the action has completed
     * (or was canceled).
     */
    private readonly loginActionResponseSubject = new BehaviorSubject<LoginActionResponse | null>(
        null,
    );
    /**
     * Whether there is currently a request underway that affects the login state.
     *
     * This being `true` means that we cannot currently provide information that depend on the login
     * state and should withhold any cached information until we learn whether they have to be
     * invalidated.
     *
     * The observable becomes `true` for all requests but the initial login-information fetch, which
     * doesn't affect the login state on the backend.
     *
     * A `forceRefresh` will also set this to `true` since we must assume the refresh was triggered
     * as a result of a state change.
     */
    private readonly loginActionInFlight$ = this.createLoginActionInFlight();
    /**
     * The currently known login information.
     *
     * The observable will replay the last state as long as there is no request in-flight which will
     * update the information once completed.
     */
    private readonly loginInfo$ = this.loginActionResponseSubject.pipe(
        filter((response): response is LoginActionResponse => response !== null),
        map((response) => response.loginInfo ?? null),
    );
    /** Emits when the logged-in user changes. */
    private readonly userChanges$ = this.createUserChanges();
    /**
     * A dictionary of observables for calls to `hasAccessToScope`.
     *
     * This functions as a cache to already fetched information. The observables inside invalidate
     * and update themselves when appropriate and stop doing work when there are no more
     * subscribers.
     */
    private readonly accessToScopeObservables: { [scope: string]: Observable<boolean> } = {};

    constructor(
        private authentication: AuthenticationApiService,
        private apiRequestConfiguration: ApiRequestConfiguration,
    ) {
        this.registerLoginActionResponseSubject();
    }

    /**
     * Logs in to the backend and returns the login information.
     *
     * The returned login information might be the result of a different action in case a different
     * action was triggered before the login could be completed.
     */
    login(username: string, password: string, scope?: string): Observable<LoginInfo> {
        this.loginActionTrigger.next({
            kind: 'login',
            username,
            password,
            scope,
        });
        return this.loginInfo$.pipe(first());
    }

    /**
     * Logs out of the backend and returns the login information.
     *
     * The returned login information might be the result of a different action in case a different
     * action was triggered before the logout could be completed.
     */
    logout(): Observable<LoginInfo> {
        this.loginActionTrigger.next({ kind: 'logout' });
        return this.loginInfo$.pipe(first());
    }

    /**
     * Returns a stream of login information.
     *
     * The observable is updated on any login action.
     */
    getLoginInfo(): Observable<LoginInfo> {
        if (this.loginActionTrigger.value === null) {
            this.loginActionTrigger.next({ kind: 'initial' });
        }
        return this.loginInfo$;
    }

    /**
     * Triggers an API request to update login information.
     *
     * This is usually not needed since `login` and `logout` will update login information
     * automatically. Use only if the login state was affected by some outside actor.
     */
    forceLoginInfoRefresh(): Observable<LoginInfo> {
        this.loginActionTrigger.next({ kind: 'forceRefresh' });
        return this.loginInfo$.pipe(first());
    }

    /**
     * Returns whether the user has access to the given scope.
     *
     * The observable is updated when the user logs in or out.
     */
    hasAccessToScope(scope: string): Observable<boolean> {
        if (!this.accessToScopeObservables[scope]) {
            this.accessToScopeObservables[scope] = this.createHasAccessToScope(scope);
        }
        return this.accessToScopeObservables[scope];
    }

    private registerLoginActionResponseSubject(): void {
        this.loginActionTrigger
            .pipe(
                filter((loginAction): loginAction is LoginAction => loginAction !== null),
                tap(() => this.loginActionResponseSubject.next(null)),
                switchMap((loginAction) =>
                    this.handleLoginAction(loginAction).pipe(
                        map((loginInfo) => ({ loginInfo, loginAction })),
                    ),
                ),
            )
            .subscribe((response) => this.loginActionResponseSubject.next(response));
    }

    private handleLoginAction(action: LoginAction) {
        switch (action.kind) {
            case 'login':
                if (action.scope) {
                    return this.loginToScope(action.username, action.password, action.scope);
                } else {
                    return this.loginWithBasicAuth(action.username, action.password);
                }
            case 'logout':
                return this.authentication.logout().pipe(switchMap(() => this.fetchLoginInfo()));
            case 'initial':
            case 'forceRefresh':
                return this.fetchLoginInfo();
        }
    }

    private loginToScope(username: string, password: string, scope: string): Observable<LoginInfo> {
        return this.authentication.loginToScope({
            body: { userName: username, password: password, scope: scope },
        });
    }

    private loginWithBasicAuth(username: string, password: string): Observable<LoginInfo> {
        return rxjs.of(void 0).pipe(
            // Make `setBasicAuthForNextRequest` part of the observable, so it is guaranteed to
            // be run together with the login request.
            tap(() =>
                this.apiRequestConfiguration.setBasicAuthForNextRequest({ username, password }),
            ),
            switchMap(() => this.authentication.login()),
        );
    }

    private fetchLoginInfo(): Observable<LoginInfo> {
        return this.authentication.login();
    }

    private createLoginActionInFlight(): Observable<boolean> {
        return rxjs.combineLatest([this.loginActionTrigger, this.loginActionResponseSubject]).pipe(
            map(
                ([actionTrigger, response]) =>
                    // An action was requested.
                    actionTrigger !== null &&
                    // We don't have a response yet.
                    response === null &&
                    // The requested action was not the initial data fetch.
                    actionTrigger.kind !== 'initial',
            ),
        );
    }

    private createUserChanges(): Observable<void> {
        return this.loginActionResponseSubject.pipe(
            filter((response): response is LoginActionResponse => response !== null),
            scan((acc, response) => {
                if (acc === null && response.loginAction.kind === 'initial') {
                    // We had no information about the logged-in user so far and just refreshed the
                    // login information. We learned about the logged-in user, but they really were
                    // already logged-in before.
                    return {
                        changed: false,
                        authorityName: response.loginInfo.authorityName,
                    };
                } else if (acc?.authorityName !== response.loginInfo.authorityName) {
                    // The logged-in user changed.
                    return {
                        changed: true,
                        authorityName: response.loginInfo.authorityName,
                    };
                } else {
                    return {
                        changed: false,
                        authorityName: response.loginInfo.authorityName,
                    };
                }
            }, null as { changed: boolean; authorityName?: string } | null),
            filter((acc) => acc?.changed ?? false),
            mapTo(void 0),
            share(),
        );
    }

    /**
     * Creates the observable for `hasAccessToScope` requests for a given scope.
     *
     * The returned observable
     *   - updates on changes,
     *   - triggers a single api call for all subscribers,
     *   - guarantees up-to-date values to new subscribers, i.e., does not replay old values after a
     *     login request is made.
     */
    private createHasAccessToScope(scope: string): Observable<boolean> {
        // We use `userChanges` as starting point, so the cached response will be refreshed when
        // needed.
        const inner$ = this.userChanges$.pipe(
            startWith(void 0 as void),
            switchRelay(() => this.authentication.hasAccessToScope({ scope })),
            map((response: { hasAccess: boolean }) => response.hasAccess),
        );
        // Do not resolve the observable for new subscribers while a login request is in-flight.
        // Instead, make sure the first value they see is the one relevant after the login request
        // has completed.
        return this.loginActionInFlight$.pipe(
            first((inFlight) => !inFlight),
            switchMap(() => inner$),
        );
    }
}
