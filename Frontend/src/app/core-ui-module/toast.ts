import { Injectable, Injector, OnDestroy } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Router } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { BehaviorSubject, Observable, Subject } from 'rxjs';
import { ModalDialogOptions } from '../common/ui/modal-dialog-toast/modal-dialog-toast.component';
import { ProgressType } from '../shared/components/modal-dialog/modal-dialog.component';
import { RestConstants } from '../core-module/rest/rest-constants';
import { TemporaryStorageService } from '../core-module/rest/services/temporary-storage.service';
import { DialogButton } from '../core-module/ui/dialog-button';
import { UIConstants } from '../core-module/ui/ui-constants';
import { DateHelper } from './DateHelper';
import { ToastMessageComponent } from './components/toast-message/toast-message.component';
import { RestConnectorService } from '../core-module/core.module';
import { AccessibilityService } from '../services/accessibility.service';
import { takeUntil } from 'rxjs/operators';
import { HttpErrorResponse } from '@angular/common/http';

interface CustomAction {
    link: {
        caption: string;
        callback: () => void;
    };
}

interface Action {
    /** Translated button label. */
    label: string;
    callback: () => void;
}
export enum ToastDuration {
    Seconds_3 = 3,
    Seconds_5 = 5,
    Seconds_8 = 8,
    Seconds_15 = 15,
    Seconds_30 = 30,
    Seconds_60 = 60,
    Infinite = null,
}
export enum ToastType {
    InfoSimple, // A simple info just confirming an action without providing useful data
    InfoData, // A info including some additional data to the previous action
    InfoAction, // A info including some interactive feature, e.g. navigating to a newly created item
    ErrorGeneric, // A generic error without any more specific data available
    ErrorSpecific, // a specific error may also including some details on how to solve it
}
export type ToastMessage = {
    message: string;
    html?: boolean;
    type: 'error' | 'info';
    subtype: ToastType;
    action?: Action;
};
@Injectable()
export class Toast implements OnDestroy {
    private static MIN_TIME_BETWEEN_TOAST = 2000;

    dialogInputValue: string;
    isModalDialogOpen: Observable<boolean>;

    private isInstanceVisible = false;
    private messageQueue = new BehaviorSubject<ToastMessage[]>([]);
    private onShowModal: (params: ModalDialogOptions) => void;
    private lastToastMessage: string;
    private lastToastMessageTime: number;
    private lastToastError: string;
    private lastToastErrorTime: number;
    private isModalDialogOpenSubject = new BehaviorSubject<boolean>(false);
    private translate: TranslateService;
    private destroyed = new Subject<void>();
    mode: 'important' | 'all' = null;
    duration: ToastDuration = null;
    static convertDuration(duration: ToastDuration) {
        switch (duration) {
            case ToastDuration.Infinite:
                return null;
        }
        return duration.valueOf();
    }

    constructor(
        private injector: Injector,
        private router: Router,
        private snackBar: MatSnackBar,
        private storage: TemporaryStorageService,
        private accessibility: AccessibilityService,
    ) {
        this.messageQueue.pipe(takeUntil(this.destroyed)).subscribe((message) => {
            if (this.isInstanceVisible) {
                return;
            }
            this.showNext();
        });
        this.isModalDialogOpen = this.isModalDialogOpenSubject.asObservable();
        this.accessibility
            .observe(['toastDuration', 'toastMode'])
            .pipe(takeUntil(this.destroyed))
            .subscribe(({ toastDuration, toastMode }) => {
                // TODO(Christopher, 2022-03-01): Do we need to clear existing toasts here?
                this.messageQueue.next([]);
                this.snackBar.dismiss();
                this.mode = toastMode;
                this.duration = toastDuration;
            });
        // Avoid cyclic-dependency error at runtime.
        setTimeout(() => {
            this.translate = this.injector.get(TranslateService);
        });
    }
    ngOnDestroy() {
        this.destroyed.next();
        this.destroyed.complete();
    }
    show(message: ToastMessage) {
        if (message.type === 'info') {
            this.toast(
                message.message,
                null,
                null,
                null,
                message.action
                    ? {
                          link: {
                              caption: message.action.label,
                              callback: message.action.callback,
                          },
                      }
                    : null,
                message,
            );
        } else {
            this.error(
                null,
                message.message,
                null,
                null,
                null,
                message.action
                    ? {
                          link: {
                              caption: message.action.label,
                              callback: message.action.callback,
                          },
                      }
                    : null,
                message,
            );
        }
    }

    /**
     * Generates a toast message
     * @param message Translation-String of message
     * @param translationParameters Parameter bindings for translation
     * @param customAction: additional parameter objects {link:{caption:string,callback:Function}}
     * @DEPRECATED: Prefer using show instead
     */
    toast(
        message: string,
        translationParameters: any = null,
        dialogTitle: string = null,
        dialogMessage: string = null,
        customAction: CustomAction = null,
        toastMessage: ToastMessage = null,
    ): void {
        if (
            this.lastToastMessage === message &&
            Date.now() - this.lastToastMessageTime < Toast.MIN_TIME_BETWEEN_TOAST
        ) {
            return;
        }
        toastMessage = toastMessage || {
            message: null,
            type: null,
            subtype: null,
        };
        if (toastMessage.subtype == null) {
            toastMessage.subtype = customAction ? ToastType.InfoAction : ToastType.InfoSimple;
        }
        this.lastToastMessage = message;
        this.lastToastMessageTime = Date.now();
        this.showToast({
            message,
            type: 'info',
            toastMessage,
            translationParameters,
            dialogTitle,
            dialogMessage,
            customAction,
        });
    }

    /**
     * Generate a message dialog that a given toolpermission is missing
     * @param toolpermission
     */
    toolpermissionError(permission: string) {
        this.showConfigurableDialog({
            title: 'TOOLPERMISSION_ERROR_TITLE',
            message: this.getToolpermissionMessage(permission),
            isCancelable: true,
        });
    }
    /**
     * Generates a toast error message
     */
    error(
        errorObject: any,
        message = 'COMMON_API_ERROR',
        translationParameters: any = null,
        dialogTitle: string = null,
        dialogMessage: string = null,
        customAction: CustomAction = null,
        toastMessage: ToastMessage = null,
    ): void {
        // If this is called by the default error handler given to ngx-edu-sharing-api und you want
        // to prevent the toast message, provide an error handler when subscribing and call
        // `error.preventDefault()`.
        const parsingResult = this.parseErrorObject({
            errorObject,
            message,
            translationParameters,
            dialogTitle,
            dialogMessage,
        });
        if (parsingResult) {
            ({ message, translationParameters, dialogTitle, dialogMessage } = parsingResult);
        } else {
            // `parseErrorObject()` took care of showing a message to the user.
            return;
        }
        if (
            this.lastToastError === message + JSON.stringify(translationParameters) &&
            Date.now() - this.lastToastErrorTime < Toast.MIN_TIME_BETWEEN_TOAST
        ) {
            return;
        }
        this.lastToastError = message + JSON.stringify(translationParameters);
        this.lastToastErrorTime = Date.now();
        toastMessage = toastMessage || {
            message: null,
            type: null,
            subtype: null,
        };
        if (!toastMessage.subtype) {
            toastMessage.subtype =
                message === 'COMMON_API_ERROR' ? ToastType.ErrorGeneric : ToastType.ErrorSpecific;
        }
        this.showToast({
            message,
            type: 'error',
            toastMessage,
            translationParameters,
            dialogTitle,
            dialogMessage,
            customAction,
        });
    }

    goToLogin() {
        this.router.navigate([UIConstants.ROUTER_PREFIX + 'login'], {
            queryParams: { next: window.location },
        });
    }

    onShowModalDialog(param: (params: any) => void) {
        this.onShowModal = param;
    }
    hasOpenDialog() {
        return this.isModalDialogOpenSubject.value;
    }
    closeModalDialog() {
        this.onShowModal({ title: null, message: null });
        this.isModalDialogOpenSubject.next(false);
    }

    showConfigurableDialog(options: ModalDialogOptions) {
        this.onShowModal(options);
        this.isModalDialogOpenSubject.next(true);
    }

    showModalDialog(
        title: string,
        message: string,
        buttons: DialogButton[],
        isCancelable = true,
        onCancel: () => void = null,
        messageParameters: any = null,
    ) {
        this.showConfigurableDialog({
            title,
            message,
            isCancelable,
            messageParameters,
            onCancel,
            buttons,
        });
    }

    showInputDialog(
        title: string,
        message: string,
        label: string,
        buttons: DialogButton[],
        isCancelable = true,
        onCancel: () => void = null,
        messageParameters: any = null,
    ) {
        this.showConfigurableDialog({
            title,
            message,
            input: label,
            isCancelable,
            messageParameters,
            onCancel,
            buttons,
        });
    }

    showProgressDialog(
        title = 'PROGRESS_DIALOG_DEFAULT_TITLE',
        message = 'PROGRESS_DIALOG_DEFAULT_MESSAGE',
        type = ProgressType.Indeterminate,
    ) {
        this.showConfigurableDialog({ title, message, progressType: type });
    }

    clientConfigError(configKey: string, details: string = null) {
        this.error(
            null,
            'client.config.xml configuration for key (' +
                configKey +
                ')' +
                (details ? ': ' + details : ''),
        );
    }

    private async showToast({
        message,
        type,
        toastMessage,
        ...options
    }: {
        message: string;
        type: 'error' | 'info';
        toastMessage: ToastMessage;
        translationParameters?: any;
        dialogTitle?: string;
        dialogMessage?: string;
        customAction?: CustomAction;
    }): Promise<void> {
        const translatedMessage = await this.injector
            .get(TranslateService)
            .get(message ?? toastMessage?.message, options.translationParameters)
            .toPromise();
        const action = this.getAction(options);
        toastMessage.message = translatedMessage;
        toastMessage.type = toastMessage.type ?? type;
        toastMessage.action = toastMessage.action ?? action;
        return this.matSnackbarShowToast(toastMessage);
    }

    private getAction({
        dialogTitle,
        dialogMessage,
        translationParameters,
        customAction,
    }: {
        dialogTitle?: string;
        dialogMessage?: string;
        translationParameters?: any;
        customAction?: CustomAction;
    }): Action | null {
        if (customAction) {
            return {
                label: this.translate.instant(customAction.link.caption),
                callback: customAction.link.callback,
            };
        } else if (dialogTitle) {
            return {
                label: this.translate.instant('DETAILS'),
                callback: () => this.openDialog(dialogTitle, dialogMessage, translationParameters),
            };
        } else {
            return null;
        }
    }

    private openDialog(title: string, message: string, translationParameters: any) {
        this.onShowModal({
            title,
            message,
            messageParameters: translationParameters,
            buttons: null,
            isCancelable: true,
        });
        this.isModalDialogOpenSubject.next(true);
    }

    private matSnackbarShowToast(message: ToastMessage): void {
        this.messageQueue.next(this.messageQueue.value.concat([message]));
    }

    private getToolpermissionMessage(permission: string) {
        let tpDescription = this.injector
            .get(TranslateService)
            .instant('TOOLPERMISSION.' + permission);
        if (permission.startsWith(RestConstants.TOOLPERMISSION_REPOSITORY_PREFIX)) {
            tpDescription = this.injector
                .get(TranslateService)
                .instant('TOOLPERMISSION.TOOLPERMISSION_REPOSITORY', {
                    repository: permission.substring(
                        RestConstants.TOOLPERMISSION_REPOSITORY_PREFIX.length,
                    ),
                });
        }
        return (
            this.translate.instant('TOOLPERMISSION_ERROR_HEADER') +
            '\n- ' +
            tpDescription +
            '\n\n' +
            this.translate.instant('TOOLPERMISSION_ERROR_FOOTER', {
                permission,
            })
        );
    }

    /**
     * Parses the errorObject and adjusts the message and other options accordingly.
     *
     * @returns false if the error toast should not be shown
     */
    public parseErrorObject({
        errorObject,
        message,
        translationParameters,
        dialogTitle,
        dialogMessage,
    }: {
        errorObject: any;
        message: string;
        translationParameters: any | null;
        dialogTitle: string;
        dialogMessage: string;
    }):
        | {
              message: string;
              translationParameters: any;
              dialogTitle: string;
              dialogMessage: string;
          }
        | false {
        let error = errorObject;
        let errorInfo = '';
        let json: any = null;
        if (errorObject) {
            json = errorObject.error;
        }
        try {
            if (json.error || json.message) {
                error = json.error + ': ' + json.message;
            } else if (json) {
                error = JSON.stringify(json, null, 2);
            }
            // add url of endpoint (if available) to the message
            if (errorObject.url) {
                error += '\n\n' + errorObject.url;
            }
        } catch (e) {
            if (errorObject) {
                console.error(errorObject);
            }
            error = errorObject?.toString();
        }
        if (message === 'COMMON_API_ERROR') {
            dialogMessage = '';
            dialogTitle = 'COMMON_API_ERROR_TITLE';
            try {
                if (json.error.stacktraceArray) {
                    errorInfo = json.stacktraceArray.join('\n');
                }
                if (json.error.indexOf(RestConstants.CONTENT_QUOTA_EXCEPTION) !== -1) {
                    message = 'GENERIC_QUOTA_ERROR_TITLE';
                    dialogTitle = '';
                }
                if (json.error.indexOf(RestConstants.CONTENT_VIRUS_EXCEPTION) != -1) {
                    message = 'GENERIC_VIRUS_ERROR_TITLE';
                    dialogTitle = '';
                } else if (json.error.indexOf('DAOToolPermissionException') !== -1) {
                    dialogTitle = 'TOOLPERMISSION_ERROR_TITLE';
                    message = 'TOOLPERMISSION_ERROR';
                    const permission = (json ? json.message : error).split(' ')[0];
                    dialogMessage = this.getToolpermissionMessage(permission);
                } else if (json.error.indexOf('DAORestrictedAccessException') !== -1) {
                    dialogTitle = 'RESTRICTED_ACCESS_ERROR_TITLE';
                    dialogMessage = 'RESTRICTED_ACCESS_ERROR_MESSAGE';
                } else if (json.error.indexOf('SystemFolderDeleteDeniedException') !== -1) {
                    message = 'SYSTEM_FOLDER_DELETE_ERROR';
                    dialogTitle = '';
                } else {
                    dialogMessage = '';
                    dialogTitle = 'COMMON_API_ERROR_TITLE';
                    if (errorObject) {
                        errorInfo = JSON.stringify(json);
                    }
                    try {
                        if (json.stacktraceArray) {
                            errorInfo = json.stacktraceArray.join('\n');
                        }
                        if (json.error.indexOf('DAOToolPermissionException') !== -1) {
                            dialogTitle = 'TOOLPERMISSION_ERROR_TITLE';
                            message = 'TOOLPERMISSION_ERROR';
                            const permission = error.split(' ')[0];
                            dialogMessage =
                                this.injector
                                    .get(TranslateService)
                                    .instant('TOOLPERMISSION_ERROR_HEADER') +
                                '\n- ' +
                                this.injector
                                    .get(TranslateService)
                                    .instant('TOOLPERMISSION.' + permission) +
                                '\n\n' +
                                this.injector
                                    .get(TranslateService)
                                    .instant('TOOLPERMISSION_ERROR_FOOTER', {
                                        permission,
                                    });
                        } else if (json.error.indexOf('SystemFolderDeleteDeniedException') !== -1) {
                            message = 'SYSTEM_FOLDER_DELETE_ERROR';
                            dialogTitle = '';
                        } else if (json.message.indexOf('InvalidLogLevel') !== -1) {
                            errorInfo = json.message;
                        }
                    } catch (e) {}
                }
            } catch (e) {
                if (json && !error) {
                    error = json;
                }
            }
            if (errorInfo === undefined) {
                errorInfo = '';
            }
            if (errorObject.status === RestConstants.DUPLICATE_NODE_RESPONSE) {
                message = 'WORKSPACE.TOAST.DUPLICATE_NAME';
                translationParameters = { name };
            } else if (errorObject.status === RestConstants.HTTP_FORBIDDEN) {
                message = 'TOAST.API_FORBIDDEN';
                dialogTitle = null;

                const login = this.injector.get(RestConnectorService).getCurrentLogin();
                if (login && login.isGuest) {
                    this.toast('TOAST.API_FORBIDDEN_LOGIN');
                    this.goToLogin();
                    return false;
                }
            } else if (errorObject.status === RestConstants.HTTP_UNAUTHORIZED) {
                this.toast('TOAST.API_FORBIDDEN_LOGIN');
                return false;
            } else {
                if (!dialogMessage) {
                    dialogMessage = error + '\n\n' + errorInfo;
                    if (errorObject instanceof HttpErrorResponse && errorObject.url) {
                        dialogMessage += '\n\nBackend URL: ' + errorObject.url;
                    }
                }
                if (!translationParameters) {
                    translationParameters = {};
                }
                translationParameters.date = DateHelper.formatDate(
                    this.translate,
                    new Date().getTime(),
                    {
                        useRelativeLabels: false,
                        showAlwaysTime: true,
                        showSeconds: true,
                    },
                );
                translationParameters.error = error;
            }
        }
        if (
            error &&
            error.status === 0 /* && this.injector.get(BridgeService).isRunningCordova()*/
        ) {
            message = 'TOAST.NO_CONNECTION';
            dialogTitle = null;
        }
        if (errorObject?.traceId) {
            dialogTitle =
                this.translate.instant(dialogTitle, translationParameters) +
                ' (' +
                errorObject.traceId +
                ')';
            message =
                this.translate.instant(message, translationParameters) +
                '\n(' +
                errorObject.traceId +
                ')';
        }
        return {
            message,
            translationParameters,
            dialogTitle,
            dialogMessage,
        };
    }

    private showNext() {
        if (this.messageQueue.value.length === 0) {
            return;
        }
        const message = this.messageQueue.value[0];
        this.isInstanceVisible = true;
        this.messageQueue.next(this.messageQueue.value.slice(1));
        // check if the specific message can be ignored
        if (this.mode === 'important') {
            if (message.subtype === ToastType.InfoSimple) {
                this.isInstanceVisible = false;
                return;
            }
        }
        const snackBarRef = this.snackBar.openFromComponent(ToastMessageComponent, {
            data: message,
            duration: Toast.convertDuration(this.duration)
                ? Toast.convertDuration(this.duration) * 1000
                : null,
            panelClass: ['app-mat-snackbar-toast', `app-mat-snackbar-toast-${message.type}`],
        });
        if (message.action) {
            snackBarRef.onAction().subscribe(message.action.callback);
        }
        snackBarRef.afterDismissed().subscribe(() => {
            this.isInstanceVisible = false;
            this.showNext();
        });
    }
}
