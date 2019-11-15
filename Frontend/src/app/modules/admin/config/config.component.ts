import {RestAdminService} from "../../../core-module/rest/services/rest-admin.service";
import {Component, EventEmitter, Output} from "@angular/core";
import {TranslateService} from "@ngx-translate/core";
import {NodeStatistics, Node, Statistics, IamGroup, Group, Collection} from "../../../core-module/rest/data-object";
import {ListItem} from "../../../core-module/ui/list-item";
import {RestConstants} from "../../../core-module/rest/rest-constants";
import {RestHelper} from "../../../core-module/rest/rest-helper";
import {NodeHelper} from "../../../core-ui-module/node-helper";
import {ConfigurationService} from "../../../core-module/rest/services/configuration.service";
import {DialogButton, RestCollectionService, RestConnectorService, RestIamService, RestMdsService, RestMediacenterService, RestNodeService} from "../../../core-module/core.module";
import {Helper} from "../../../core-module/rest/helper";
import {Toast} from "../../../core-ui-module/toast";
import {OptionItem} from "../../../core-ui-module/option-item";
import {AbstractControl, FormBuilder, FormControl, FormGroup, Validators, ValidatorFn} from "@angular/forms";
import {MdsHelper} from "../../../core-module/rest/mds-helper";
import {UIHelper} from "../../../core-ui-module/ui-helper";

// Charts.js
declare var Chart:any;

@Component({
  selector: 'app-admin-config',
  templateUrl: 'config.component.html',
  styleUrls: ['config.component.scss']
})
export class AdminConfigComponent {
  public static CONFIG_FILE_BASE="edu-sharing.base.conf";
  public static CONFIG_FILE="edu-sharing.conf";
  codeOptionsGlobal = {minimap: {enabled: false}, language: 'json', readOnly: true, automaticLayout: true};
  codeOptions = {minimap: {enabled: false}, language: 'json', automaticLayout: true};
  configGlobal = '';
  config = '';
  size = 'medium';

  constructor(
      private adminService: RestAdminService,
      private toast: Toast,
  ) {
    this.adminService.getConfigFile(AdminConfigComponent.CONFIG_FILE_BASE).subscribe((data) => {
      this.configGlobal = data;
      this.adminService.getConfigFile(AdminConfigComponent.CONFIG_FILE).subscribe((data) => {
        this.config = data;
      });
    });
  }

  save() {
    this.toast.showProgressDialog();
    this.adminService.updateConfigFile(AdminConfigComponent.CONFIG_FILE,this.config).subscribe(()=>{
      this.adminService.refreshAppInfo().subscribe(()=>{
        this.toast.closeModalDialog();
      },(error)=>{
        this.toast.error(error,"ADMIN.GLOBAL_CONFIG.PARSE_ERROR");
        this.toast.closeModalDialog();
      })
    },(error)=>{
      this.toast.error(error);
      this.toast.closeModalDialog();
    });
  }
}
