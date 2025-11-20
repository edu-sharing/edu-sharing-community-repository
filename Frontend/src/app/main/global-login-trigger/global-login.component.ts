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
        this.authenticationService.observeLoginInfo().subscribe((login) => {
            if (login.statusCode !== RestConstants.STATUS_CODE_OK) {
                console.log(login);
                const googleEntry = (login as PrimaryLogin).oauthEntries.find(
                    (e) => e.name === 'google',
                );
                console.log('Test ge:' + JSON.stringify(googleEntry));
                if (googleEntry?.clientId && googleEntry?.allowThirdPartyLoginPlugin) {
                    console.log('clientID:' + googleEntry.clientId);
                    this.loadGoogleScript(googleEntry.clientId);
                }
            }
        });
    }

    private loadGoogleScript(clientId: string) {
        const script = document.createElement('script');
        script.src = 'https://accounts.google.com/gsi/client';
        script.async = true;
        script.defer = true;
        script.onload = () => {
            console.log('GLOBAL google script loaded');
            UIHelper.waitForComponent(this.ngZone, window, 'google').subscribe(() => {
                console.log('google loaded');
                (window as any).handleCredentialResponse = (response: any) => {
                    console.log('Received token: ', response);
                    this.sendToBackend(response.credential);
                };

                (window as any).google.accounts.id.initialize({
                    client_id: clientId,
                    callback: (window as any).handleCredentialResponse,
                });

                (window as any).google.accounts.id.prompt();
            });
        };
        document.body.appendChild(script);
    }

    sendToBackend(token: string) {
        fetch(this.platformLocation.getBaseHrefFromDOM() + 'login/google', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ credential: token }),
        }).then((r) => {
            if (r.ok)
                window.location.href = this.platformLocation.getBaseHrefFromDOM() + 'shibboleth';
            else console.error('Login failed:', r.status);
        });
    }
}
