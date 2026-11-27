import { inject, Injectable } from '@angular/core';
import { OPEN_URL_MODE } from 'ngx-edu-sharing-ui';
import { RenderingLinkService as RenderingLinkServiceAbstract } from 'ngx-rendering-service-lib';
import { UIHelper } from '../core-ui-module/ui-helper';
import { BridgeService } from './bridge.service';

/**
 * App implementation of the rendering lib's new-tab bridge. In the mobile app, links to the own
 * platform (same origin as the web app, e.g. the html module's "open in new tab") go to the
 * in-app browser (`UIHelper.openUrl` with `OPEN_URL_MODE.Blank`), which shares the session; a
 * plain new window would leave the app for the system browser. External urls keep the default
 * behavior and so open in the system browser, as everywhere in the app. In a regular browser
 * nothing changes.
 */
@Injectable({ providedIn: 'root' })
export class RenderingLinkService extends RenderingLinkServiceAbstract {
    private bridge = inject(BridgeService);

    openInNewTab(url: string): boolean {
        if (!this.bridge.isRunningCordova() || !this.isOwnPlatform(url)) {
            return false;
        }
        UIHelper.openUrl(url, this.bridge, OPEN_URL_MODE.Blank);
        return true;
    }

    private isOwnPlatform(url: string): boolean {
        try {
            return new URL(url, window.location.href).origin === window.location.origin;
        } catch {
            return false;
        }
    }
}
