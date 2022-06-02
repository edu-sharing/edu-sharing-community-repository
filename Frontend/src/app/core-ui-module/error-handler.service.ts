import { Injectable, Injector } from '@angular/core';
import { Toast } from './toast';
import { Location } from '@angular/common';
import { DialogButton, RestConstants } from '../core-module/core.module';
import { ApiErrorResponse } from 'ngx-edu-sharing-api';
import { HttpRequest } from '@angular/common/http';

@Injectable({
    providedIn: 'root',
})
export class ErrorHandlerService {
    private toast: Toast;

    constructor(private injector: Injector, private location: Location) {
        // Prevent circular dependency.
        Promise.resolve().then(() => (this.toast = this.injector.get(Toast)));
    }

    handleError(error: ApiErrorResponse, req: HttpRequest<unknown>): void {
        console.log('handleError', error, req);
        if (req.method === 'GET' && req.url === this.getApiUrl('/config/v1/values')) {
            this.showConfigurationErrorNotice(error);
        } else if (
            req.method === 'GET' &&
            (req.url === this.getApiUrl('/authentication/v1/hasAccessToScope') ||
                req.url === this.getApiUrl('/authentication/v1/validateSession') ||
                req.url === this.getApiUrl('/iam/v1/people/-home-/-me-'))
        ) {
            if (
                req.url === this.getApiUrl('/iam/v1/people/-home-/-me-') &&
                error.status === RestConstants.HTTP_UNAUTHORIZED
            ) {
                // ignore -> there is no guest available
            } else {
                this.showReloadNotice();
            }
        } else {
            this.toast.error(error);
        }
    }

    private getApiUrl(path: string): string {
        return this.location.prepareExternalUrl('/rest' + path);
    }

    private showReloadNotice() {
        this.toast.showModalDialog(
            'API_ERROR.RELOAD_NOTICE_TITLE',
            'API_ERROR.RELOAD_NOTICE',
            [
                new DialogButton('API_ERROR.RELOAD_BUTTON_LABEL', { color: 'primary' }, () =>
                    location.reload(),
                ),
            ],
            false,
        );
    }

    private showConfigurationErrorNotice(error: ApiErrorResponse) {
        // Use a toast instead of a modal dialog because the configuration is fetched at an early
        // stage of the application and a loading spinner will probably occlude a modal dialog.
        this.toast.error(
            error,
            'Error fetching configuration data. Please contact administrator.\n' +
                'Fehler beim Abrufen der Konfigurationsdaten. Bitte Administrator kontaktieren.',
        );
    }
}
