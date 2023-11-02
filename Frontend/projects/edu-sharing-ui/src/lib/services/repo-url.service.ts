import { Injectable } from '@angular/core';
import { NetworkService, Node } from 'ngx-edu-sharing-api';
import { take } from 'rxjs/operators';
import { EduSharingUiConfiguration } from '../edu-sharing-ui-configuration';

/**
 * Replaces absolute backend URLs with proxied URLs when in dev environment.
 *
 * This is needed for content that requires authentication, so the browser provides the required
 * session cookie with the request, or when accessing the dev server from another device.
 */
@Injectable({
    providedIn: 'root',
})
export class RepoUrlService {
    private constructor(
        private networkService: NetworkService,
        private configuration: EduSharingUiConfiguration,
    ) {}

    /**
     * Replaces the given URL if the given node belongs to the home repository.
     */
    async getRepoUrl(url: string, node: Node) {
        if (
            !this.configuration.production &&
            (await this.networkService.isFromHomeRepository(node).pipe(take(1)).toPromise())
        ) {
            return this.withCurrentOrigin(url);
        } else {
            return url;
        }
    }

    /**
     * Replaces the given URL.
     *
     * The caller needs to make sure that the given URL belongs to the home repository.
     */
    withCurrentOrigin(url: string): string {
        const urlObject = new URL(url);
        urlObject.host = window.location.host;
        urlObject.protocol = window.location.protocol;
        return urlObject.href;
    }
}
