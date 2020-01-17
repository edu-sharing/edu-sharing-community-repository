
import {Component} from "@angular/core";
import {Translation} from "../../core-ui-module/translation";
import {UIHelper} from "../../core-ui-module/ui-helper";
import {SessionStorageService} from "../../core-module/core.module";
import {TranslateService} from "@ngx-translate/core";
import {Title} from "@angular/platform-browser";
import {ActivatedRoute} from "@angular/router";
import {Toast} from "../../core-ui-module/toast";
import {RestConnectorService} from "../../core-module/core.module";
import {ConfigurationService} from "../../core-module/core.module";

@Component({
  selector: 'upload-main',
  templateUrl: 'upload.component.html',
  styleUrls: ['upload.component.scss'],
  animations: [

  ]
})
export class UploadComponent {


  constructor(private toast: Toast,
              private route: ActivatedRoute,
              private title: Title,
              private translate: TranslateService,
              private config: ConfigurationService,
              private storage : SessionStorageService,
              private connector: RestConnectorService) {
      Translation.initialize(translate, this.config, this.storage, this.route).subscribe(() => {
      UIHelper.setTitle('UPLOAD.TITLE', this.title, this.translate, this.config);

      });
  }
}

