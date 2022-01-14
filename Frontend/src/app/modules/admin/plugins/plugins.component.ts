import {RestAdminService} from "../../../core-module/rest/services/rest-admin.service";
import {Component} from "@angular/core";
import {
    ConfigFilePrefix,
    DialogButton,
    PluginStatus,
    RestLocatorService
} from '../../../core-module/core.module';
import {Toast} from "../../../core-ui-module/toast";
import {ModalMessageType} from '../../../common/ui/modal-dialog-toast/modal-dialog-toast.component';
import {Observable} from 'rxjs';

// Charts.js
declare var Chart:any;

@Component({
  selector: 'app-admin-plugins',
  templateUrl: 'plugins.component.html',
  styleUrls: ['plugins.component.scss']
})
export class AdminPluginsComponent {
  plugins: PluginStatus[];
  constructor(
      private adminService: RestAdminService,
  ) {
        this.adminService.getPlugins().subscribe((plugins) => {
            this.plugins = plugins;
        })
  }
}
