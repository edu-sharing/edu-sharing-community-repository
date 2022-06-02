import { RestAdminService } from '../../../core-module/rest/services/rest-admin.service';
import { Component } from '@angular/core';
import {
    ConfigFilePrefix,
    DialogButton,
    RestLocatorService,
} from '../../../core-module/core.module';
import { Toast } from '../../../core-ui-module/toast';
import { ModalMessageType } from '../../../common/ui/modal-dialog-toast/modal-dialog-toast.component';
import { forkJoin, Observable } from 'rxjs';
import { ConfigService } from 'ngx-edu-sharing-api';
// Charts.js
declare var Chart: any;

@Component({
    selector: 'es-admin-config',
    templateUrl: 'config.component.html',
    styleUrls: ['config.component.scss'],
})
export class AdminConfigComponent {
    public static CONFIG_FILE_REFERENCE = 'edu-sharing.reference.conf';
    public static EXTENSION_CONFIG_FILE = 'edu-sharing.application.conf';
    public static OVERRIDE_CONFIG_FILE = 'edu-sharing.override.conf';
    public static CONFIG_DEPLOYMENT_FILE = 'edu-sharing.deployment.conf';
    public static CLIENT_CONFIG_FILE = 'client.config.xml';
    codeOptionsHocoonRO: any = {
        minimap: { enabled: false },
        language: 'json',
        readOnly: true,
        automaticLayout: true,
    };
    codeOptionsHocoonRW: any = {
        minimap: { enabled: false },
        language: 'json',
        automaticLayout: true,
    };
    clientCodeOptions: any = {
        minimap: { enabled: false },
        language: 'xml',
        automaticLayout: true,
    };
    configs: any = {
        clientConfig: null,
        reference: null,
        clusterDeployment: null,
        nodeDeployment: null,
        clusterOverride: null,
        nodeOverride: null,
        parsed: null,
    };
    size = 'medium';
    showRO = false;
    editSupported = false;

    constructor(
        private adminService: RestAdminService,
        private configService: ConfigService,
        private toast: Toast,
    ) {
        this.adminService
            .getConfigFile(AdminConfigComponent.CLIENT_CONFIG_FILE, 'DEFAULTS')
            .subscribe((data) => (this.configs.clientConfig = data));
        this.adminService
            .getConfigFile(AdminConfigComponent.CONFIG_FILE_REFERENCE, 'DEFAULTS')
            .subscribe((base) => (this.configs.reference = base));
        this.adminService
            .getConfigFile(AdminConfigComponent.CONFIG_DEPLOYMENT_FILE, 'CLUSTER')
            .subscribe((deployment) => (this.configs.clusterDeployment = deployment));
        this.adminService
            .getConfigFile(AdminConfigComponent.CONFIG_DEPLOYMENT_FILE, 'NODE')
            .subscribe((deployment) => (this.configs.nodeDeployment = deployment));
        this.adminService
            .getConfigFile(AdminConfigComponent.OVERRIDE_CONFIG_FILE, 'CLUSTER')
            .subscribe((c) => (this.configs.clusterOverride = c));
        this.adminService
            .getConfigFile(AdminConfigComponent.OVERRIDE_CONFIG_FILE, 'NODE')
            .subscribe((c) => (this.configs.nodeOverride = c));
        this.setEditSupported(false);
        this.adminService.getConfigMerged().subscribe((merged) => {
            this.configs.parsed = JSON.stringify(merged, null, 2);
            this.setEditSupported(merged?.security?.configuration?.inlineEditing);
        });
    }
    setEditSupported(status: boolean) {
        this.editSupported = status;
        this.showRO = !this.editSupported;
        this.codeOptionsHocoonRW.readOnly = !this.editSupported;
        this.clientCodeOptions.readOnly = !this.editSupported;
    }
    displayError(error: any) {
        console.warn(error);
        this.toast.closeModalDialog();
        this.toast.showConfigurableDialog({
            title: 'ADMIN.GLOBAL_CONFIG.ERROR',
            message: 'ADMIN.GLOBAL_CONFIG.PARSE_ERROR',
            messageParameters: { error: error?.error?.error },
            messageType: ModalMessageType.HTML,
            isCancelable: true,
            buttons: [
                new DialogButton('ADMIN.GLOBAL_CONFIG.CHECK', { color: 'danger' }, () =>
                    this.toast.closeModalDialog(),
                ),
            ],
        });
    }
    save() {
        this.toast.showProgressDialog();
        forkJoin([
            this.adminService.updateConfigFile(
                AdminConfigComponent.CLIENT_CONFIG_FILE,
                'DEFAULTS',
                this.configs.clientConfig,
            ),
            this.adminService.updateConfigFile(
                AdminConfigComponent.OVERRIDE_CONFIG_FILE,
                'CLUSTER',
                this.configs.clusterOverride,
            ),
            this.adminService.updateConfigFile(
                AdminConfigComponent.OVERRIDE_CONFIG_FILE,
                'NODE',
                this.configs.nodeOverride,
            ),
        ]).subscribe(() => {
            this.adminService.refreshAppInfo().subscribe(
                () => {
                    this.toast.closeModalDialog();
                    this.configService.observeConfig({ forceUpdate: true }).subscribe(
                        () => {
                            this.toast.closeModalDialog();
                            this.toast.toast('ADMIN.GLOBAL_CONFIG.SAVED');
                        },
                        (error) => this.displayError(error),
                    );
                },
                (error) => {
                    this.displayError(error);
                },
            );
        });
    }
}
