import { Component, Inject, NgZone } from '@angular/core';
import { AuthenticationService, PrimaryLogin, RestConstants } from 'ngx-edu-sharing-api';
import { UIHelper } from '../../core-ui-module/ui-helper';
import { DOCUMENT, PlatformLocation } from '@angular/common';

@Component({
    selector: 'es-global-login',
    templateUrl: 'global-login.component.html',
})
/**
 * handle global login triggers (i.e. google one tap)
 */
export class GlobalLoginComponent {
    constructor(
        private platformLocation: PlatformLocation,
        private authenticationService: AuthenticationService,
        private ngZone: NgZone,
        @Inject(DOCUMENT) private document: Document,
    ) {
        this.ngZone.runOutsideAngular(() => {
            this.authenticationService.observeLoginInfo().subscribe((login) => {
                console.log(login);
                if (login.statusCode !== RestConstants.STATUS_CODE_OK) {
                    const googleEntry = (login as PrimaryLogin).oauthEntries.find(
                        (e) => e.name === 'google',
                    );
                    if (googleEntry?.clientId) {
                        const newScriptElement = document.createElement('script');
                        newScriptElement.src = 'https://accounts.google.com/gsi/client';
                        newScriptElement.async = true;
                        newScriptElement.onload = () => {
                            UIHelper.waitForComponent(this.ngZone, window, 'google').subscribe(
                                () => {
                                    (window as any).google.accounts.id.initialize({
                                        client_id: googleEntry.clientId,
                                        color_scheme: 'light',
                                        ux_mode: 'redirect',
                                        login_uri:
                                            this.platformLocation.getBaseHrefFromDOM() +
                                            'login/oauth2/code/' +
                                            encodeURI(googleEntry.registrationId),
                                        callback: (data: {
                                            credential: string;
                                            select_by: string;
                                        }) => {
                                            console.log('got data', data);
                                            // @TODO: An endpoint or api endpoint is required to handle the credential
                                            /*window.open(
                                            this.platformLocation.getBaseHrefFromDOM() + 'login/oauth2/code/' + encodeURI(googleEntry.registrationId) + '?state=' + encodeURIComponent(data.credential),
                                            '_SELF'
                                        );*/
                                        },
                                    });
                                    (window as any).google.accounts.id.prompt();
                                },
                            );
                        };
                        this.document.head.appendChild(newScriptElement);
                    }
                }
            });
        });
    }
}
