import { HttpInterceptorFn } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { environment } from '../environments/environment';

/**
 * Header for the rendering token, see `ApiAuthenticationFilter.HEADER_RENDER_TOKEN` of the repository.
 * The repository only evaluates it on the few endpoints which accept it and ignores it everywhere
 * else, so it can be sent with every request to the repository api.
 */
export const RENDER_TOKEN_HEADER = 'X-Edu-Render-Token';

/**
 * Holds the rendering token of the embedded material.
 *
 * The web component has no repository session (credentials are not sent), so without the token
 * e.g. the metadataset of the inline metadata viewer could only be loaded with an activated guest.
 * All `edu-sharing-render` elements of a page share one injector; the endpoints accepting the token
 * are not node specific, so any valid token of the page is fine.
 */
@Injectable({ providedIn: 'root' })
export class RenderTokenService {
    private token: string | null = null;

    setToken(token: string | null | undefined): void {
        if (token) {
            this.token = token;
        }
    }

    /** the token, if it is still valid (an expired one would be rejected by the repository) */
    getValidToken(): string | null {
        if (!this.token) {
            return null;
        }
        const expiresAt = RenderTokenService.getExpiration(this.token);
        if (expiresAt !== null && expiresAt <= Date.now()) {
            return null;
        }
        return this.token;
    }

    private static getExpiration(jwt: string): number | null {
        try {
            const payload = jwt.split('.')[1].replace(/-/g, '+').replace(/_/g, '/');
            const exp = JSON.parse(atob(payload)).exp;
            return typeof exp === 'number' ? exp * 1000 : null;
        } catch (e) {
            return null;
        }
    }
}

/** adds the rendering token to the requests to the repository api (not to the rendering service) */
export const renderTokenInterceptor: HttpInterceptorFn = (req, next) => {
    const apiUrl = environment.eduSharingApiUrl;
    if (!apiUrl || !req.url.startsWith(apiUrl.replace(/\/$/, '') + '/')) {
        return next(req);
    }
    const token = inject(RenderTokenService).getValidToken();
    if (!token) {
        return next(req);
    }
    return next(req.clone({ setHeaders: { [RENDER_TOKEN_HEADER]: token } }));
};
