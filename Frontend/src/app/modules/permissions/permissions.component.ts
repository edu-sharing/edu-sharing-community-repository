import {Component, ViewChild, ElementRef, style, animate, transition, trigger} from '@angular/core';
import {TranslateService} from "ng2-translate";
import {Translation} from "../../common/translation";
import {
  NodeRef, IamUser, NodeWrapper, Node, Version, NodeVersions, LoginResult,
  IamGroups, Group, OrganizationOrganizations
} from "../../common/rest/data-object";
import {Router, Params, ActivatedRoute, Routes} from "@angular/router";
import {Toast} from "../../common/ui/toast";
import {RestConnectorService} from "../../common/rest/services/rest-connector.service";
import {RestOrganizationService} from "../../common/rest/services/rest-organization.service";
import {ConfigurationService} from "../../common/services/configuration.service";
import {Title} from "@angular/platform-browser";
import {UIHelper} from "../../common/ui/ui-helper";
import {SessionStorageService} from "../../common/services/session-storage.service";

@Component({
  selector: 'permissions-main',
  templateUrl: 'permissions.component.html',
  styleUrls: ['permissions.component.scss'],
  animations: [

  ]
})
export class PermissionsMainComponent {
  public tab : string;
  public searchQuery: string;
  private selectedOrg: Group;
  public isAdmin = false;
  public disabled = false;
  constructor(private toast: Toast,
              private route: ActivatedRoute,
              private router: Router,
              private config: ConfigurationService,
              private title: Title,
              private translate: TranslateService,
              private storage : SessionStorageService,
              private organization: RestOrganizationService,
              private connector: RestConnectorService) {
    Translation.initialize(translate,this.config,this.storage,this.route).subscribe(()=>{
      UIHelper.setTitle('PERMISSIONS.TITLE',this.title,this.translate,this.config);
    });
    this.connector.isLoggedIn().subscribe((data: LoginResult) => {
      if(data.isValidLogin && !data.isGuest && !data.currentScope){
        this.organization.getOrganizations().subscribe((data: OrganizationOrganizations) => {
          this.isAdmin = data.canCreate;
        });
        this.tab='ORG';
      }
      else{
        this.goToLogin();
      }
    }, (error: any) => this.goToLogin());
    this.config.get("hideMainMenu").subscribe((data:string[])=>{
      if(data && data.indexOf("permissions")!=-1){
        //this.router.navigate([UIConstants.ROUTER_PREFIX+"workspace"]);
        this.disabled=true;
      }
    });
  }

  public doSearch(event: string) {
    this.searchQuery = event;
  }

  setTab(tab: string) {
    if (tab != 'ORG' && !this.selectedOrg && !this.isAdmin) {
      this.toast.error(null, "PERMISSIONS.SELECT_ORGANIZATION");
      return;
    }
    if (tab == this.tab)
      return;
    if (tab == 'ORG') {
      this.selectedOrg = null;
    }
    this.searchQuery = null;
    this.tab = tab;
  }

  private goToLogin() {
    UIHelper.goToLogin(this.router,this.config);
  }
}
