
import {Component} from "@angular/core";
import {Translation} from "../../common/translation";
import {UIHelper} from "../../common/ui/ui-helper";
import {SessionStorageService} from "../../common/services/session-storage.service";
import {TranslateService} from "@ngx-translate/core";
import {Title} from "@angular/platform-browser";
import {ActivatedRoute} from "@angular/router";
import {Toast} from "../../common/ui/toast";
import {RestConnectorService} from "../../common/rest/services/rest-connector.service";
import {ConfigurationService} from "../../common/services/configuration.service";
import {RestIamService} from "../../common/rest/services/rest-iam.service";
import {IamUser, User} from "../../common/rest/data-object";
import {AuthorityNamePipe} from "../../common/ui/authority-name.pipe";

@Component({
  selector: 'app-profiles',
  templateUrl: 'profiles.component.html',
  styleUrls: ['profiles.component.scss'],
  animations: [

  ]
})
export class ProfilesComponent {
  private user: User;

  private globalProgress = true;
  constructor(private toast: Toast,
              private route: ActivatedRoute,
              private title: Title,
              private translate: TranslateService,
              private config: ConfigurationService,
              private storage : SessionStorageService,
              private iamService: RestIamService) {
      Translation.initialize(translate, this.config, this.storage, this.route).subscribe(() => {
        route.params.subscribe((params)=>{
          this.iamService.getUser(params['authority']).subscribe((profile:IamUser)=>{
            this.user=profile.person;
            let name=new AuthorityNamePipe().transform(this.user,null);
            UIHelper.setTitle('PROFILES.TITLE', this.title, this.translate, this.config,{name:name});
            this.globalProgress=false;
          },(error:any)=>{
            this.globalProgress=false;
            this.toast.error(null,'PROFILES.LOAD_ERROR');
          });
        });
      });
  }
}

