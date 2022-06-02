import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';

import { RestLtiService } from '../../../core-module/rest/services/rest-lti.service';
import { LTIRegistrationToken, LTIRegistrationTokens } from '../../../core-module/rest/data-object';
import { Toast } from '../../../core-ui-module/toast';
import { AdminComponent } from '../admin.component';
import { UIHelper } from '../../../core-ui-module/ui-helper';
import { DialogButton } from '../../../core-module/ui/dialog-button';

@Component({
    selector: 'es-lti-admin',
    templateUrl: './lti-admin.component.html',
    styleUrls: ['./lti-admin.component.scss'],
})
export class LtiAdminComponent implements OnInit {
    @Output() onRefreshAppList = new EventEmitter<void>();

    /**
     * dynamic
     */
    tokens: LTIRegistrationTokens;
    displayedColumns: string[] = ['url', 'tsExpiry', 'registeredAppId', 'copy', 'delete'];

    /**
     * advanced
     *
     */
    platformId: string;
    clientId: string;
    deploymentId: string;
    authenticationRequestUrl: string;
    keysetUrl: string;
    keyId: string;
    authTokenUrl: string;

    constructor(private ltiService: RestLtiService, private toast: Toast) {}

    ngOnInit(): void {
        this.refresh();
    }

    remove(element: LTIRegistrationToken) {
        this.toast.showConfigurableDialog({
            title: 'ADMIN.LTI.REMOVE_TITLE',
            message: 'ADMIN.LTI.REMOVE_MESSAGE',
            messageParameters: element,
            buttons: [
                new DialogButton('CANCEL', { color: 'standard' }, () =>
                    this.toast.closeModalDialog(),
                ),
                new DialogButton('ADMIN.APPLICATIONS.REMOVE', { color: 'danger' }, () => {
                    this.ltiService.removeToken(element.token).subscribe((t: void) => {
                        this.refresh();
                    });
                    this.toast.closeModalDialog();
                }),
            ],
            isCancelable: true,
        });
    }

    refresh() {
        this.ltiService.getTokensCall(false).subscribe((t: LTIRegistrationTokens) => {
            this.tokens = t;
        });
    }

    generate() {
        this.ltiService.getTokensCall(true).subscribe((t: LTIRegistrationTokens) => {
            this.tokens = t;
            this.copyUrl(this.tokens.registrationLinks[this.tokens.registrationLinks.length - 1]);
        });
    }

    saveAdvanced() {
        this.ltiService
            .registrationAdvanced(
                this.platformId,
                this.clientId,
                this.deploymentId,
                this.authenticationRequestUrl,
                this.keysetUrl,
                this.keyId,
                this.authTokenUrl,
            )
            .subscribe(
                (t: void) => {
                    this.toast.toast('ADMIN.LTI.DATA.CREATED', null);
                    this.toast.closeModalDialog();
                    this.onRefreshAppList.emit();
                },
                (error: any) => {
                    this.toast.error(error);
                    this.toast.closeModalDialog();
                },
            );
    }

    copyUrl(element: LTIRegistrationToken) {
        UIHelper.copyToClipboard(element.url);
        this.toast.toast('ADMIN.APPLICATIONS.COPIED_CLIPBOARD');
    }
}
