import {Translation} from "../../core-ui-module/translation";
import {UIHelper} from "../../core-ui-module/ui-helper";
import {ActivatedRoute, Router} from "@angular/router";
import {Toast} from "../../core-ui-module/toast";
import {ConfigurationService} from "../../core-module/core.module";
import {Title} from "@angular/platform-browser";
import {TranslateService} from "@ngx-translate/core";
import {SessionStorageService} from "../../core-module/core.module";
import {RestConnectorService} from "../../core-module/core.module";
import {Component, ViewChild, ElementRef} from "@angular/core";
import {RouterComponent} from "../../router/router.component";
import {UIConstants} from "../../core-module/ui/ui-constants";
@Component({
  selector: 'messages-main',
  templateUrl: 'messages.component.html',
  styleUrls: ['messages.component.scss'],
})
export class MessagesComponent {
  public message : string;
  public messageDetail : string;
  public error : string;
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
        this.error = data['message'];
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

