
import {Component, ViewChild, HostListener, ElementRef} from '@angular/core';
import 'rxjs/add/operator/map';
import {Router, ActivatedRoute, Params} from '@angular/router';
import {TranslateService} from "@ngx-translate/core";
import {Translation} from "../../common/translation";
import {RestSearchService} from '../../common/rest/services/rest-search.service';
import {RestNodeService} from '../../common/rest/services/rest-node.service';
import {RestConstants} from '../../common/rest/rest-constants';
import {RestConnectorService} from "../../common/rest/services/rest-connector.service";
import {Node, NodeList, LoginResult, SharingInfo, Person} from "../../common/rest/data-object";
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
import {RestSharingService} from "../../common/rest/services/rest-sharing.service";
import {Toast} from "../../common/ui/toast";
import {ConfigurationHelper} from "../../common/rest/configuration-helper";
import {Helper} from "../../common/helper";
import {NodeHelper} from "../../common/ui/node-helper";



@Component({
  selector: 'app-sharing',
  templateUrl: 'sharing.component.html',
  styleUrls: ['sharing.component.scss'],
  })



export class SharingComponent {
  loading=true;
  loadingChildren=true;
  passwordInput:string;
  private params: Params;
  sharingInfo: SharingInfo;
  childs: Node[];
  columns:ListItem[]=[];
  sort= {
      sortBy: RestConstants.CM_NAME,
      sortAscending: true
  };
  options:OptionItem[]=[];
  constructor(
    private router : Router,
    private route : ActivatedRoute,
    private connector:RestConnectorService,
    private nodeService: RestNodeService,
    private sharingService:RestSharingService,
    private storage : TemporaryStorageService,
    private session : SessionStorageService,
    private title : Title,
    private toast : Toast,
    private config : ConfigurationService,
    private translate : TranslateService) {
      this.columns.push(new ListItem('NODE',RestConstants.CM_NAME));
      this.columns.push(new ListItem('NODE',RestConstants.SIZE));
      this.options.push(new OptionItem('SHARING.DOWNLOAD','cloud_download',(node:Node)=>this.download(node)));
      Translation.initialize(translate,this.config,this.session,this.route).subscribe(()=> {
          UIHelper.setTitle('SHARING.TITLE', title, translate, config);
          this.route.queryParams.subscribe((params)=>{
             this.params=params;
             this.sharingService.getInfo(params['nodeId'],params['token']).subscribe((result)=>{
                 this.loading=false;
                 this.sharingInfo=result;
                 if(result.expired){
                     this.router.navigate([UIConstants.ROUTER_PREFIX,'messages','share_expired']);
                     return;
                 }
                 this.loadChildren();
             },(error)=>{
                 console.warn(error);
                 this.router.navigate([UIConstants.ROUTER_PREFIX,'messages','share_expired']);
                 this.loading=false;
             })
          });
      });
   }
    validatePassword() {
        this.sharingService.getInfo(this.params['nodeId'], this.params['token'], this.passwordInput).subscribe((result) => {
            if(!result.passwordMatches){
                this.toast.error(null,'SHARING.ERROR_INVALID_PASSWORD');
            }
            this.sharingInfo=result;
            this.loadChildren();
        });
    }
    download(child : Node = null){
      let node=this.params['nodeId'];
      let token=this.params['token'];
      let url = this.connector.getAbsoluteEndpointUrl() + "../share?mode=download&token=" + encodeURIComponent(token) + "&password=" + encodeURIComponent(this.passwordInput) + "&nodeId=" + encodeURIComponent(node);
      if(child==null && this.sharingInfo.node.isDirectory){
          let ids = RestHelper.getNodeIds(this.childs).join(",");
          url+= "&childIds=" + encodeURIComponent(ids);
      }
      else {
          if (child != null) {
              url += "&childIds=" + encodeURIComponent(child.ref.id);
          }
      }
        window.open(url);

    }
    private changeSort(sort:any){
        this.sort=sort;
        this.loadChildren();
    }
    private loadChildren() {
        if(this.sharingInfo.password && !this.sharingInfo.passwordMatches)
            return;
        this.loadingChildren=true;
        this.childs=[];
        let request={count:RestConstants.COUNT_UNLIMITED,sortBy:[this.sort.sortBy],sortAscending:[this.sort.sortAscending]};
        this.sharingService.getChildren(this.params['nodeId'], this.params['token'], this.passwordInput,request).subscribe((nodes)=>{
            this.childs=nodes.nodes;
            this.loadingChildren=false;
        });
    }
    inviterIsAuthor(){
      return Helper.objectEquals(this.sharingInfo.invitedBy,this.sharingInfo.node.createdBy);
    }
    getPersonName(person:Person){
        return ConfigurationHelper.getPersonWithConfigDisplayName(person,this.config);
    }
}
