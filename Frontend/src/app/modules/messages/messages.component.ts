import {Translation} from "../../common/translation";
import {UIHelper} from "../../common/ui/ui-helper";
import {ActivatedRoute, Router} from "@angular/router";
import {Toast} from "../../common/ui/toast";
import {ConfigurationService} from "../../common/services/configuration.service";
import {Title} from "@angular/platform-browser";
import {TranslateService} from "@ngx-translate/core";
import {SessionStorageService} from "../../common/services/session-storage.service";
import {RestConnectorService} from "../../common/rest/services/rest-connector.service";
import {Component, ViewChild, ElementRef} from "@angular/core";
import {RouterComponent} from "../../router/router.component";
import {UIConstants} from "../../common/ui/ui-constants";
@Component({
  selector: 'messages-main',
  templateUrl: 'messages.component.html',
  styleUrls: ['messages.component.scss'],
})
export class MessagesComponent {
  public message : string;
  public messageDetail : string;
  constructor(private toast: Toast,
              private route: ActivatedRoute,
              private router: Router,
              private config: ConfigurationService,
              private titleService: Title,
              private translate: TranslateService,
              private storage : SessionStorageService) {
    Translation.initialize(translate, this.config, this.storage, this.route).subscribe(() => {
      this.route.params.subscribe((data:any)=>{
        this.message="MESSAGES."+data['message'];
        this.messageDetail="MESSAGES.DETAILS."+data['message'];
        if(this.translate.instant(this.message)==this.message){
          this.message="MESSAGES.INVALID";
          this.messageDetail="MESSAGES.DETAILS.INVALID";
        }
        UIHelper.setTitle(this.message, this.titleService, this.translate, this.config);
      })
    });
  }
  public openSearch(){
    this.router.navigate([UIConstants.ROUTER_PREFIX+"search"]);
  }
  public closeWindow(){
    window.close();
  }
}

