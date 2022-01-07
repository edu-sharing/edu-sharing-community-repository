import {RestAdminService} from "../../../core-module/rest/services/rest-admin.service";
import {Component} from "@angular/core";
import {DialogButton, RestLocatorService} from "../../../core-module/core.module";
import {Toast} from "../../../core-ui-module/toast";
import {ModalMessageType} from '../../../common/ui/modal-dialog-toast/modal-dialog-toast.component';
import {Observable} from 'rxjs';

// Charts.js
declare var Chart:any;

@Component({
  selector: 'app-admin-config',
  templateUrl: 'config.component.html',
  styleUrls: ['config.component.scss']
})
export class AdminConfigComponent {
  public static CONFIG_FILE_BASE="edu-sharing.base.conf";
  public static EXTENSION_CONFIG_FILE="edu-sharing.conf";
  public static SERVER_CONFIG_FILE="edu-sharing.server-{{hostname}}.conf";
  public static CONFIG_DEPLOYMENT_FILE="edu-sharing.deployment.conf";
  public static CLIENT_CONFIG_FILE="client.config.xml";
  codeOptionsGlobal = {minimap: {enabled: false}, language: 'json', readOnly: true, automaticLayout: true};
  codeOptions = {minimap: {enabled: false}, language: 'json', automaticLayout: true};
  clientCodeOptions = {minimap: {enabled: false}, language: 'xml', automaticLayout: true};
  configClient = '';
  configGlobal = '';
  configDeployment = '';
  configParsed = '';
  serverConfig = '';
  extensionConfig = '';
  size = 'medium';

  constructor(
      private adminService: RestAdminService,
      private locator: RestLocatorService,
      private toast: Toast,
  ) {
      this.adminService.getConfigFile(AdminConfigComponent.CLIENT_CONFIG_FILE).subscribe((data)=>{
          this.configClient = data;
          this.adminService.getConfigFile(AdminConfigComponent.CONFIG_FILE_BASE).subscribe((base) => {
              this.configGlobal = base;
              this.adminService.getConfigFile(AdminConfigComponent.CONFIG_DEPLOYMENT_FILE).subscribe((deployment) => {
                  this.configDeployment = deployment;
              });
              this.adminService.getConfigMerged().subscribe((merged) => {
                  this.configParsed = JSON.stringify(merged, null, 2);
              });
              this.adminService.getConfigFile(AdminConfigComponent.EXTENSION_CONFIG_FILE).subscribe((extension) => {
                  this.extensionConfig = extension;
              });
              this.adminService.getConfigFile(AdminConfigComponent.SERVER_CONFIG_FILE).subscribe((server) => {
                  this.serverConfig = server;
              });
          });
      });
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
      buttons: [new DialogButton('ADMIN.GLOBAL_CONFIG.CHECK', DialogButton.TYPE_DANGER, () => this.toast.closeModalDialog())],
    });
  }
  save() {
    this.toast.showProgressDialog();
    Observable.forkJoin(
        this.adminService.updateConfigFile(AdminConfigComponent.EXTENSION_CONFIG_FILE,this.extensionConfig),
        this.adminService.updateConfigFile(AdminConfigComponent.CLIENT_CONFIG_FILE, this.configClient),
        this.adminService.updateConfigFile(AdminConfigComponent.SERVER_CONFIG_FILE, this.serverConfig),
    ).subscribe(() => {
      this.adminService.refreshAppInfo().subscribe(() => {
        this.toast.closeModalDialog();
        this.locator.getConfig().subscribe(() => {
              this.toast.closeModalDialog();
              this.toast.toast('ADMIN.GLOBAL_CONFIG.SAVED');
            },error =>
                this.displayError(error)
        );
      }, error => {
        this.displayError(error);
      })
    });
  }
}
