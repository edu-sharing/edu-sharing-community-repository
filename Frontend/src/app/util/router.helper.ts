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
        const windowRedirect = () => {
            if (!(url.startsWith('http://') || url.startsWith('https://'))) {
                if (url.startsWith('/')) {
                    url = url.substring(1);
                }
                url = window.origin + platformLocation.getBaseHrefFromDOM() + url;
            }
            if (replaceUrl) {
                window.location.replace(url);
            } else {
                window.location.assign(url);
            }
        };
        // Strip origin and base HREF
        const cleanUrl = url.replace(window.origin + platformLocation.getBaseHrefFromDOM(), '');
        // navigate to servlet
        if (['/shibboleth'].some((s) => url.startsWith(s))) {
            windowRedirect();
            return;
        }
        router.navigateByUrl(cleanUrl, { replaceUrl }).catch((error: any) => {
            console.warn(error);
            windowRedirect();
        });
    }
}
