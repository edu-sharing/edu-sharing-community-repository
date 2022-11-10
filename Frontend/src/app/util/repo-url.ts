import { environment } from '../../environments/environment';
import { Node, RestNetworkService } from '../core-module/core.module';

/**
 * Replaces absolute backend URLs with proxied URLs when in dev environment.
 *
 * This is needed for content that requires authentication, so the browser provides the required
 * session cookie with the request, or when accessing the dev server from another device.
 */
export function getRepoUrl(url: string, node: Node): string {
    if (
        !environment.production &&
        window.location.hostname === 'localhost' &&
        RestNetworkService.isFromHomeRepo(node)
    ) {
        return withCurrentOrigin(url);
    } else {
        return url;
    }
}

function withCurrentOrigin(url: string): string {
    const urlObject = new URL(url);
    urlObject.host = window.location.host;
    urlObject.protocol = window.location.protocol;
    return urlObject.href;
}
