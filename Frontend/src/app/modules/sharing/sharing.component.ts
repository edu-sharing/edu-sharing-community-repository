import {Component} from '@angular/core';
import 'rxjs/add/operator/map';
import {ActivatedRoute, Params, Router} from '@angular/router';
import {TranslateService} from '@ngx-translate/core';
import {Translation} from '../../core-ui-module/translation';
import {
    ConfigurationHelper,
    ConfigurationService,
    ListItem,
    Node,
    Person,
    RestConnectorService,
    RestConstants,
    RestHelper,
    RestNodeService,
    RestSharingService,
    SessionStorageService,
    SharingInfo,
    TemporaryStorageService
} from '../../core-module/core.module';
import {CustomOptions, DefaultGroups, OptionItem} from '../../core-ui-module/option-item';
import {OPEN_URL_MODE, UIConstants} from '../../core-module/ui/ui-constants';
import {Toast} from '../../core-ui-module/toast';
import {Helper} from '../../core-module/rest/helper';
import {UIHelper} from "../../core-ui-module/ui-helper";
import {BridgeService} from "../../core-bridge-module/bridge.service";
import {NodeHelperService} from "../../core-ui-module/node-helper.service";


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
  options:CustomOptions={
      useDefaultOptions: false,
      addOptions: []
  };
  constructor(
    private router : Router,
    private route : ActivatedRoute,
    private connector:RestConnectorService,
    private nodeService: RestNodeService,
    private sharingService:RestSharingService,
    private bridge: BridgeService,
    private nodeHelperService: NodeHelperService,
    private storage : TemporaryStorageService,
    private session : SessionStorageService,
    private toast : Toast,
    private config : ConfigurationService,
    private translate : TranslateService) {
      this.columns.push(new ListItem('NODE',RestConstants.CM_NAME));
      this.columns.push(new ListItem('NODE',RestConstants.CM_MODIFIED_DATE));
      this.columns.push(new ListItem('NODE',RestConstants.SIZE));
      const download = new OptionItem('SHARING.DOWNLOAD','cloud_download',(node:Node)=>this.download(node));
      download.group = DefaultGroups.Primary;
      download.showAlways = true;
      const open = new OptionItem('SHARING.OPEN','open_in_new',(node:Node)=> {
          console.log(node);
          UIHelper.openUrl(
              node.properties[RestConstants.CCM_PROP_IO_WWWURL][0],
              this.bridge,
              OPEN_URL_MODE.BlankSystemBrowser
          )
      });
      open.group = DefaultGroups.Primary;
      open.showAlways = true;
      download.customShowCallback = ((nodes: Node[]) => nodes?.[0]?.mediatype !== 'link');
      open.customShowCallback = ((nodes: Node[]) => nodes?.[0]?.mediatype === 'link');
      this.options.addOptions.push(download);
      this.options.addOptions.push(open);
      Translation.initialize(translate,this.config,this.session,this.route).subscribe(()=> {
          this.route.queryParams.subscribe((params)=> {
             this.params=params;
             this.sharingService.getInfo(params.nodeId,params.token).subscribe((result)=> {
                 this.loading=false;
                 this.sharingInfo=result;
                 if(result.expired) {
                     this.router.navigate([UIConstants.ROUTER_PREFIX,'messages','share_expired']);
                     return;
                 }
                 this.loadChildren();
             },(error)=> {
                 console.warn(error);
                 this.router.navigate([UIConstants.ROUTER_PREFIX,'messages','share_expired']);
                 this.loading=false;
             })
          });
      });
   }
    validatePassword() {
        this.sharingService.getInfo(this.params.nodeId, this.params.token, this.passwordInput).subscribe((result) => {
            if(!result.passwordMatches) {
                this.toast.error(null,'SHARING.ERROR_INVALID_PASSWORD');
            }
            this.sharingInfo=result;
            this.loadChildren();
        });
    }
    download(child : Node = null) {
      const node=this.params.nodeId;
      const token=this.params.token;
      let url = this.connector.getAbsoluteEndpointUrl() + '../share?mode=download&token=' + encodeURIComponent(token) + '&password=' + encodeURIComponent(this.passwordInput) + '&nodeId=' + encodeURIComponent(node);
      if(child==null && this.sharingInfo.node.isDirectory) {
          const ids = RestHelper.getNodeIds(this.childs).join(',');
          url+= '&childIds=' + encodeURIComponent(ids);
      }
      else {
          if (child != null) {
              url += '&childIds=' + encodeURIComponent(child.ref.id);
          }
      }
        window.open(url);

    }
    changeSort(sort:any) {
        this.sort=sort;
        this.loadChildren();
    }
    private loadChildren() {
        if(this.sharingInfo.password && !this.sharingInfo.passwordMatches)
            return;
        this.loadingChildren=true;
        this.childs=[];
        const request= {
            count:RestConstants.COUNT_UNLIMITED,
            sortBy:[this.sort.sortBy],
            sortAscending:[this.sort.sortAscending],
            propertyFilter: [RestConstants.ALL]
        };
        this.sharingService.getChildren(this.params.nodeId, this.params.token, this.passwordInput,request).subscribe((nodes)=> {
            this.childs=nodes.nodes;
            this.loadingChildren=false;
        });
    }
    inviterIsAuthor() {
      return Helper.objectEquals(this.sharingInfo.invitedBy,this.sharingInfo.node.createdBy);
    }
    getPersonName(person:Person) {
        return ConfigurationHelper.getPersonWithConfigDisplayName(person,this.config);
    }

    childCount() {
        if(this.sharingInfo.node.type === RestConstants.CCM_TYPE_IO) {
            try {
                return parseInt(this.sharingInfo.node.properties[RestConstants.VIRTUAL_PROP_CHILDOBJECTCOUNT]?.[0], 10) || 0;
            } catch(e) {}
        }
        return 0;
    }
}
