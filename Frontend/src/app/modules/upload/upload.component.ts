
import {Component} from "@angular/core";
import {Translation} from "../../common/translation";
import {UIHelper} from "../../common/ui/ui-helper";
import {SessionStorageService} from "../../common/services/session-storage.service";
import {TranslateService} from "ng2-translate";
import {Title} from "@angular/platform-browser";
import {ActivatedRoute} from "@angular/router";
import {Toast} from "../../common/ui/toast";
import {RestConnectorService} from "../../common/rest/services/rest-connector.service";
import {ConfigurationService} from "../../common/services/configuration.service";

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

