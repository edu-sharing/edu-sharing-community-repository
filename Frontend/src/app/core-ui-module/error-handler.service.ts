import { Injectable, Injector } from '@angular/core';
import { Toast } from './toast';
import { Location } from '@angular/common';
import { DialogButton, RestConstants } from '../core-module/core.module';
import { ApiErrorResponse } from 'ngx-edu-sharing-api';
import { HttpRequest } from '@angular/common/http';
import { DialogsService } from '../features/dialogs/dialogs.service';

@Injectable({
    providedIn: 'root',
})
export class ErrorHandlerService {
    private toast: Toast;
    private dialogs: DialogsService;

    constructor(private injector: Injector, private location: Location) {
        // Prevent circular dependency.
        Promise.resolve().then(() => {
            this.toast = this.injector.get(Toast);
            this.dialogs = this.injector.get(DialogsService);
        });
    }

    handleError(error: ApiErrorResponse, req: HttpRequest<unknown>): void {
        // console.log('handleError', error, req);
        if (req.method === 'GET' && req.url === this.getApiUrl('/config/v1/values')) {
            this.showConfigurationErrorNotice(error);
        } else if (
            req.method === 'GET' &&
            (req.url === this.getApiUrl('/authentication/v1/hasAccessToScope') ||
                req.url === this.getApiUrl('/authentication/v1/validateSession') ||
                req.url === this.getApiUrl('/iam/v1/people/-home-/-me-'))
        ) {
            this.showReloadNotice();
        } else {
            this.toast.error(error);
        }
    }

    private getApiUrl(path: string): string {
        return this.location.prepareExternalUrl('/rest' + path);
    }

    private async showReloadNotice() {
        const dialogRef = await this.dialogs.openGenericDialog({
            title: 'API_ERROR.RELOAD_NOTICE_TITLE',
            message: 'API_ERROR.RELOAD_NOTICE',
            buttons: [{ label: 'API_ERROR.RELOAD_BUTTON_LABEL', config: { color: 'primary' } }],
        });
        dialogRef.afterClosed().subscribe((response) => {
            if (response === 'API_ERROR.RELOAD_BUTTON_LABEL') {
                location.reload();
            }
        });
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
