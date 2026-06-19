import { Component, NgZone, DOCUMENT, inject } from '@angular/core';
import { AuthenticationService, PrimaryLogin, RestConstants } from 'ngx-edu-sharing-api';
import { UIHelper } from '../../core-ui-module/ui-helper';
import { PlatformLocation } from '@angular/common';

@Component({
    selector: 'es-global-login',
    templateUrl: 'global-login.component.html',
})
/**
 * handle global login triggers (i.e. google one tap)
 */
export class GlobalLoginComponent {
    private platformLocation = inject(PlatformLocation);
    private authenticationService = inject(AuthenticationService);
    private ngZone = inject(NgZone);
    private document = inject<Document>(DOCUMENT);

    constructor() {
        this.authenticationService.observeLoginInfo().subscribe((login) => {
            if (login.statusCode !== RestConstants.STATUS_CODE_OK) {
                const googleEntry = (login as PrimaryLogin).oauthEntries.find(
                    (e) => e.name === 'google',
                );
                if (googleEntry?.clientId && googleEntry?.allowThirdPartyLoginPlugin) {
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
            UIHelper.waitForComponent(this.ngZone, window, 'google').subscribe(() => {
                (window as any).handleCredentialResponse = (response: any) => {
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
        void fetch(this.platformLocation.getBaseHrefFromDOM() + 'login/google', {
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
