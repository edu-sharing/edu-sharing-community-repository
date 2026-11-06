import { Component, OnInit, inject, ChangeDetectionStrategy } from '@angular/core';
import { CordovaService } from '../services/cordova.service';
import { UIConstants } from 'ngx-edu-sharing-ui';
import { Router } from '@angular/router';
import { ConfigurationService } from '../core-module/rest/services/configuration.service';
import { firstValueFrom } from 'rxjs';
import { RouterHelper } from '../util/router.helper';
import { PlatformLocation } from '@angular/common';

/**
 * The root route (`''`). A full page load of the entry point never renders it - the
 * Frontend-Server redirects it (`Frontend-Server/src/lib/default-location.ts`, like
 * `AuthenticationFilter.redirectToDefaultLocation` did). It is reached by client side
 * navigations to the root, e.g. a login `next` pointing at `/edu-sharing/`, and then
 * does the same: go to `defaultLocation`, default `login`.
 */
@Component({
    selector: 'es-startup',
    template: '',
    changeDetection: ChangeDetectionStrategy.Eager,
    standalone: false,
})
export class StartupComponent implements OnInit {
    private cordova = inject(CordovaService);
    private router = inject(Router);
    private platformLocation = inject(PlatformLocation);
    private config = inject(ConfigurationService);

    async ngOnInit() {
        if (this.cordova.isRunningCordova()) {
            // wait until cordova device init is ready
            this.cordova.subscribeServiceReady().subscribe(() => {
                // per default go to app
                void this.router.navigate([UIConstants.ROUTER_PREFIX, 'login'], {
                    replaceUrl: true,
                });
            });
            return;
        }
        RouterHelper.navigateToAbsoluteUrl(
            this.platformLocation,
            this.router,
            await StartupComponent.defaultLocation(this.config),
            // the empty root route must not stay in the history ("back" would show a blank page)
            true,
        );
    }

    /** `defaultLocation` as a route below `components/`, or as is when it is an absolute url */
    static async defaultLocation(config: ConfigurationService): Promise<string> {
        const location =
            (await firstValueFrom(config.get('defaultLocation', 'login')))?.trim() || 'login';
        return /^https?:\/\//.test(location)
            ? location
            : UIConstants.ROUTER_PREFIX + location.replace(/^\/+/, '');
    }
}
