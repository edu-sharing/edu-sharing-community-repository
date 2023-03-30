import { Injectable } from '@angular/core';
import { NetworkService, Node } from 'ngx-edu-sharing-api';
import { take } from 'rxjs/internal/operators';
import { EduSharingUiConfiguration } from '../edu-sharing-ui-configuration';

@Injectable({
    providedIn: 'root',
})
export class RepoUrlService {
    private constructor(
        private networkService: NetworkService,
        private configuration: EduSharingUiConfiguration,
    ) {}
    public async getRepoUrl(url: string, node: Node) {
        if (
            !this.configuration.production &&
            window.location.hostname === 'localhost' &&
            (await this.networkService.isFromHomeRepository(node).pipe(take(1)).toPromise())
        ) {
            return this.withCurrentOrigin(url);
        } else {
            return url;
        }
    }
    private withCurrentOrigin(url: string): string {
        const urlObject = new URL(url);
        urlObject.host = window.location.host;
        urlObject.protocol = window.location.protocol;
        return urlObject.href;
    }
}
/**
 * Replaces absolute backend URLs with proxied URLs when in dev environment.
 *
 * This is needed for content that requires authentication, so the browser provides the required
 * session cookie with the request, or when accessing the dev server from another device.
 */
