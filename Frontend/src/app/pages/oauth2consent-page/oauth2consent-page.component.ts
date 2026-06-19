import { Component, inject } from '@angular/core';
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
    private authentication = inject(AuthenticationService);
    private translate = inject(TranslateService);
    private http = inject(HttpClient);
    private toast = inject(Toast);

    scopes: ScopeEntry[] = [];

    oauth2Consent: OAuth2Consent;
    constructor() {
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

        const url = '/edu-sharing/oauth2server/authorize';

        const form = document.createElement('form');
        form.method = 'POST';
        form.action = url;

        form.appendChild(
            Object.assign(document.createElement('input'), {
                type: 'hidden',
                name: 'client_id',
                value: this.oauth2Consent.clientId,
            }),
        );
        form.appendChild(
            Object.assign(document.createElement('input'), {
                type: 'hidden',
                name: 'state',
                value: this.oauth2Consent.state,
            }),
        );

        this.oauth2Consent.scopes.forEach((s) => {
            form.appendChild(
                Object.assign(document.createElement('input'), {
                    type: 'hidden',
                    name: 'scope',
                    value: s,
                }),
            );
        });

        form.appendChild(
            Object.assign(document.createElement('input'), {
                type: 'hidden',
                name: 'consent_action',
                value: 'approve',
            }),
        );

        document.body.appendChild(form);
        form.submit();
    }
}
