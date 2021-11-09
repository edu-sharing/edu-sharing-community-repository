import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';
import * as rxjs from 'rxjs';
import { filter, first, switchMap, tap } from 'rxjs/operators';
import { ApiRequestConfiguration } from '../api-request-configuration';
import * as apiModels from '../api/models';
import { AuthenticationV1Service as AuthenticationApiService } from '../api/services';

export type LoginInfo = apiModels.Login;

type LoginAction =
    | {
          kind: 'login';
          username: string;
          password: string;
          scope?: string;
      }
    | {
          kind: 'logout';
      }
    | {
          kind: 'refresh';
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
     * Current login information.
     *
     * Becomes `null` while a request is underway, so subscribers to `loginInfo$` who subscribed
     * after triggering an action will receive the login information after the action has completed
     * (or was canceled).
     */
    private readonly loginInfoSubject = new BehaviorSubject<LoginInfo | null>(null);
    private readonly loginInfo$ = this.loginInfoSubject.pipe(
        filter((loginInfo): loginInfo is LoginInfo => loginInfo !== null),
    );

    constructor(
        private authentication: AuthenticationApiService,
        private apiRequestConfiguration: ApiRequestConfiguration,
    ) {
        this.registerLoginInfoSubject();
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
            this.loginActionTrigger.next({ kind: 'refresh' });
        }
        return this.loginInfo$;
    }

    /**
     * Triggers an API request to update login information.
     *
     * This is usually not needed since `login` and `logout` will update login information
     * automatically. Use only if the login state was affected by some outside actor.
     */
    updateLoginInfo(): Observable<LoginInfo> {
        this.loginActionTrigger.next({ kind: 'refresh' });
        return this.loginInfo$.pipe(first());
    }

    private registerLoginInfoSubject(): void {
        this.loginActionTrigger
            .pipe(
                filter((action): action is LoginAction => action !== null),
                tap(() => this.loginInfoSubject.next(null)),
                switchMap((action) => this.handleLoginAction(action)),
            )
            .subscribe((loginInfo) => this.loginInfoSubject.next(loginInfo));
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
            case 'refresh':
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
}
