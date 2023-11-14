import { Injectable, Injector } from '@angular/core';
import { TranslationsService } from 'ngx-edu-sharing-ui';
import { CordovaService } from '../services/cordova.service';
import { MessageType } from '../core-module/ui/message-type';
import { Toast } from '../core-ui-module/toast';
import { CardDialogRef } from '../features/dialogs/card-dialog/card-dialog-ref';
import {
    GenericDialogConfig,
    GenericDialogData,
} from '../features/dialogs/dialog-modules/generic-dialog/generic-dialog-data';
import { DialogsService } from '../features/dialogs/dialogs.service';

@Injectable({ providedIn: 'root' })
export class BridgeService {
    constructor(private injector: Injector, private cordova: CordovaService) {}
    showTemporaryMessage(
        type: MessageType,
        message: string | any,
        messageParameters: any = null,
        additional: any = null,
        error: any = null,
    ) {
        if (type === MessageType.info) {
            this.injector.get(Toast).toast(message, messageParameters, null, null, additional);
        } else if (type === MessageType.error) {
            this.injector
                .get(Toast)
                .error(
                    error,
                    message ? message : 'COMMON_API_ERROR',
                    messageParameters,
                    null,
                    null,
                    additional,
                );
        }
    }
    closeModalDialog() {
        this.injector.get(Toast).closeProgressSpinner();
    }
    openGenericDialog<R extends string>(
        config: GenericDialogConfig<R>,
    ): Promise<CardDialogRef<GenericDialogData<R>, R>> {
        return this.injector.get(DialogsService).openGenericDialog(config);
    }
    showError(errorObject: any) {
        this.injector.get(Toast).error(errorObject);
    }
    showProgressSpinner() {
        this.injector.get(Toast).showProgressSpinner();
    }
    isRunningCordova() {
        return this.cordova.isRunningCordova();
    }
    getCordova() {
        return this.cordova;
    }

    getISOLanguage() {
        return this.injector.get(TranslationsService).getISOLanguage();
    }
}
