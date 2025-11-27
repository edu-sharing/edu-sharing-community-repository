import { Component } from '@angular/core';
import { AuthenticationService, OAuth2Consent } from 'ngx-edu-sharing-api';
import { ScopeEntry } from './scope-entry';
import { TranslateService } from '@ngx-translate/core';
import { HttpClient } from '@angular/common/http';
import { Toast, ToastMessage, ToastType } from '../../services/toast';

@Component({
    selector: 'es-oauth2consent-page',
    templateUrl: './oauth2consent-page.component.html',
    styleUrl: './oauth2consent-page.component.scss',
    standalone: false,
})
export class Oauth2consentPageComponent {
    scopes: ScopeEntry[] = [];

    oauth2Consent: OAuth2Consent;
    constructor(
        private authentication: AuthenticationService,
        private translate: TranslateService,
        private http: HttpClient,
        private toast: Toast,
    ) {
        this.authentication.getOauthConsent().subscribe((c) => {
            this.oauth2Consent = c;
            this.scopes = this.oauth2Consent.scopes.map((s) => ({
                name: s,
                checked: false,
            }));
        });
    }

    validateAndSubmit() {
        const selectedScopes = this.scopes.filter((s) => s.checked).map((s) => s.name);

        if (selectedScopes.length === 0) {
            let toastMessage: ToastMessage = {
                message: this.translate.instant('LOGIN.OAUTH2SERVER.CONSENT.VALIDATE_SCOPES'),
                html: false,
                type: 'info',
                subtype: ToastType.InfoSimple,
            };
            this.toast.show(toastMessage);
            return;
        }

        const body = new URLSearchParams();
        body.set('client_id', this.oauth2Consent.clientId);
        body.set('state', this.oauth2Consent.state);

        selectedScopes.forEach((scope) => body.append('scope', scope));
        body.set('consent_action', 'approve');

        console.log('xhr stuff....');
        this.http
            .post('/edu-sharing/oauth2server/authorize', body.toString(), {
                responseType: 'text',
                observe: 'response',
                headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            })
            .subscribe({
                next: (res) => {
                    if (res.status === 302) {
                        // extract redirect URL
                        const redirect = res.headers.get('Location');
                        window.location.href = redirect!;
                    } else {
                        console.log('OK:', res);
                    }
                },
                error: (err) => {
                    this.toast.error(
                        err,
                        this.translate.instant('LOGIN.OAUTH2SERVER.CONSENT.ERROR'),
                    );
                },
            });
    }
}
