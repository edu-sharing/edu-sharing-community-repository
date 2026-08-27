import {
    HttpEvent,
    HttpHandler,
    HttpInterceptor,
    HttpRequest,
    HttpResponseBase,
} from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiRequestConfiguration } from './api-request-configuration';
import { EduSharingApiConfiguration } from './edu-sharing-api-configuration';
import { handleError } from './utils/rxjs-operators/handle-error';
import { finalize, tap } from 'rxjs/operators';
import { ApiStateService } from './api-state.service';

@Injectable()
export class ApiInterceptor implements HttpInterceptor {
    private apiRequestConfiguration = inject(ApiRequestConfiguration);
    private configuration = inject(EduSharingApiConfiguration);
    private apiStateService = inject(ApiStateService);

    /**
     * Response header telling whether the request was answered for a real authenticated user
     * ("true") or for a guest / anonymous user ("false"). Set by the backend on all rest responses.
     */
    static readonly HEADER_AUTHENTICATED = 'X-Edu-Authenticated';

    /**
     * proxy target, only non-null in dev mode, will be set via interceptor
     */
    static proxyTarget: string | null;

    intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
        // We filter for requests that actually target the API since this interceptor will be called
        // on all HTTP requests by the application, not limited to this library. (See notes in
        // `edu-sharing-api.module.ts`.)
        const isApiRequest = req.url.startsWith(this.configuration.rootUrl);
        // console.log('intercept', req, isApiRequest)
        if (isApiRequest) {
            // Apply the headers
            req = this.apiRequestConfiguration.apply(req);
            this.apiStateService.ongoingRequestsCount$.next(
                this.apiStateService.ongoingRequestsCount$.value + 1,
            );
            return next.handle(req).pipe(
                tap((event) => {
                    if (event instanceof HttpResponseBase) {
                        ApiInterceptor.proxyTarget = (event as HttpResponseBase).headers.get(
                            'X-Edu-Sharing-Proxy-Target',
                        );
                        this.trackAuthenticationState(event, req);
                    }
                }),
                // Handle errors globally
                handleError((err) => this.configuration.onError?.(err, req)),
                finalize(() =>
                    this.apiStateService.ongoingRequestsCount$.next(
                        this.apiStateService.ongoingRequestsCount$.value - 1,
                    ),
                ),
            );
        } else {
            return next.handle(req);
        }
    }

    /**
     * Reports the authentication state announced by the backend to the `ApiStateService`, so a
     * session that silently expired (the user is degraded to guest and still gets a 200 response)
     * can be detected.
     */
    private trackAuthenticationState(response: HttpResponseBase, req: HttpRequest<unknown>): void {
        // Requests handling the login state itself may legitimately change the state (login,
        // logout, 2fa) and must not be interpreted as an unexpected session loss.
        if (req.url.includes('/authentication/')) {
            return;
        }
        let authenticated = response.headers.get(ApiInterceptor.HEADER_AUTHENTICATED);
        // Header is missing on backends that do not support it yet -> keep the last known state.
        if (authenticated === null) {
            return;
        }
        this.apiStateService.updateAuthenticated(authenticated === 'true');
    }
}
