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
        // Strip origin and base HREF
        const cleanUrl = url.replace(window.origin + platformLocation.getBaseHrefFromDOM(), '');
        router.navigateByUrl(cleanUrl, { replaceUrl }).catch((error: any) => {
            console.warn(error);
            if (replaceUrl) {
                window.location.replace(url);
            } else {
                window.location.assign(url);
            }
        });
    }
}
