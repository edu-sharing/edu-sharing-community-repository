import { PlatformLocation } from '@angular/common';
import { Router } from '@angular/router';

export class RouterHelper {
    /**
     * try to navigate to given url using angular routing
     */
    static navigateToAbsoluteUrl(
        platformLocation: PlatformLocation,
        router: Router,
        url: string,
        replaceUrl = false,
    ) {
        let cleanUrl = url.replace(
            (platformLocation as any).location.origin + platformLocation.getBaseHrefFromDOM(),
            '',
        );
        let parsed = router.parseUrl(cleanUrl);
        let segments: string[] = [];
        try {
            for (let segment of parsed.root.children.primary.segments) {
                segments.push(segment.path);
            }
        } catch (e) {
            // some users get a nlp if a not parsable url is given. Use default redirect in this case
            console.warn(e);
            if (replaceUrl) window.location.replace(url);
            else window.location.assign(url);
            return;
        }
        router
            .navigate(segments, { queryParams: parsed.queryParams, replaceUrl: replaceUrl })
            .catch((error: any) => {
                console.warn(error);
                if (replaceUrl) window.location.replace(url);
                else window.location.assign(url);
            });
    }
}
