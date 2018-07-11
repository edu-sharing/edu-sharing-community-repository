
import {Component, ViewChild, HostListener, ElementRef} from '@angular/core';
import 'rxjs/add/operator/map';
import { HttpModule } from '@angular/http';
import {Router, ActivatedRoute, Params} from '@angular/router';
import {TranslateService} from "@ngx-translate/core";
import {Translation} from "../../common/translation";
import {RestSearchService} from '../../common/rest/services/rest-search.service';
import {RestMetadataService} from '../../common/rest/services/rest-metadata.service';
import {RestNodeService} from '../../common/rest/services/rest-node.service';
import {RestConstants} from '../../common/rest/rest-constants';
import {RestConnectorService} from "../../common/rest/services/rest-connector.service";
import {Node, NodeList, LoginResult} from "../../common/rest/data-object";
import {OptionItem} from "../../common/ui/actionbar/option-item";
import {TemporaryStorageService} from "../../common/services/temporary-storage.service";
import {UIHelper} from "../../common/ui/ui-helper";
import {Title} from "@angular/platform-browser";
import {ConfigurationService} from "../../common/services/configuration.service";
import {SessionStorageService} from "../../common/services/session-storage.service";
import {UIConstants} from "../../common/ui/ui-constants";
import {RestMdsService} from "../../common/rest/services/rest-mds.service";
import {RestHelper} from "../../common/rest/rest-helper";
import {ListItem} from "../../common/ui/list-item";
import {MdsHelper} from "../../common/rest/mds-helper";



@Component({
  selector: 'app-link-share',
  templateUrl: 'link-share.component.html',
  styleUrls: ['link-share.component.scss'],
  })



export class LinkShareComponent {
  constructor(
    private router : Router,
    private route : ActivatedRoute,
    private connector:RestConnectorService,
    private nodeService: RestNodeService,
    private searchService: RestSearchService,
    private metadataService:RestMetadataService,
    private mdsService:RestMdsService,
    private storage : TemporaryStorageService,
    private session : SessionStorageService,
    private title : Title,
    private config : ConfigurationService,
    private translate : TranslateService) {
      Translation.initialize(translate,this.config,this.session,this.route).subscribe(()=> {
          UIHelper.setTitle('LINK_SHARE.TITLE', title, translate, config);
      });

   }

}
