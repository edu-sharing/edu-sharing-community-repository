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
import {
    LoginResult, ServerUpdate, CacheInfo, Application, Node, ParentList,
    CollectionContent, Collection, NodeWrapper
} from "../../common/rest/data-object";
import {RestAdminService} from "../../common/rest/services/rest-admin.service";
import {DialogButton} from "../../common/ui/modal-dialog/modal-dialog.component";
import {Helper} from "../../common/helper";
import {RestConstants} from "../../common/rest/rest-constants";
import {UIConstants} from "../../common/ui/ui-constants";
import {RestUtilitiesService} from "../../common/rest/services/rest-utilities.service";
import {RestNodeService} from "../../common/rest/services/rest-node.service";
import {ListItem} from "../../common/ui/list-item";
import {RestCollectionService} from "../../common/rest/services/rest-collection.service";
@Component({
  selector: 'share-app',
  templateUrl: 'share-app.component.html',
  styleUrls: ['share-app.component.scss'],
  animations: [

  ]
})
export class ShareAppComponent {
    private uri: any;
    private type="LINK";
    private title:string;
    private description:string;
    private previewUrl:string;
    private globalProgress=true;
    private inboxPath: Node[];
    private inbox: Node;
    private columns:ListItem[]=[];
    private collections: Collection[];
  constructor(private toast: Toast,
              private route: ActivatedRoute,
              private router: Router,
              private node: RestNodeService,
              private utilities: RestUtilitiesService,
              private translate: TranslateService,
              private collectionApi: RestCollectionService,
              private storage : SessionStorageService,
              private config : ConfigurationService,
              private connector: RestConnectorService) {
      this.columns.push(new ListItem("COLLECTION", 'title'));
      this.columns.push(new ListItem("COLLECTION", 'info'));
      this.columns.push(new ListItem("COLLECTION",'scope'));
      Translation.initialize(translate, this.config, this.storage, this.route).subscribe(() => {
          this.previewUrl=this.connector.getThemeMimePreview('link.svg');
          this.route.queryParams.subscribe((params:any)=>{
              this.uri=params['uri'];
              console.log(this.uri);
              this.collectionApi.search("",{
                  sortBy:[RestConstants.CM_MODIFIED_DATE],
                  offset:0,
                  count:50,
                  sortAscending:false,
              }).subscribe((data:CollectionContent)=>{
                  this.collections=data.collections;
              });
              if(this.isLink()) {
                  this.node.getNodeParents(RestConstants.INBOX, false).subscribe((data: ParentList) => {
                      this.inboxPath = data.nodes;
                      this.inbox=data.nodes[0];
                      this.utilities.getWebsiteInformation(this.uri).subscribe((data: any) => {
                          this.title = data.title;
                          this.description = data.description;
                          this.globalProgress=false;
                      });
                  });
              }
          })
      });
  }
    saveInternal(callback:Function){
        if(this.isLink()){
            let prop:any={};
            prop[RestConstants.CCM_PROP_IO_WWWURL]=[this.uri];
            this.globalProgress=true;
            this.node.createNode(this.inbox.ref.id,RestConstants.CCM_TYPE_IO,[],prop,true,RestConstants.COMMENT_MAIN_FILE_UPLOAD).subscribe((data:NodeWrapper)=>{
                callback(data.node);
            });
        }
    }
    saveFile(){
        this.saveInternal(()=>this.goToInbox());
    }
    private saveToCollection(collection:Node){
      this.saveInternal((node:Node)=>{
          this.collectionApi.addNodeToCollection(collection.ref.id,node.ref.id).subscribe(()=>{
              UIHelper.goToCollection(this.router,collection);
          });
      });
    }
    private isLink() {
        return !this.uri.startsWith("content://");
    }

    private goToInbox() {
        UIHelper.goToWorkspaceFolder(this.node,this.router,null,this.inbox.ref.id);
    }
    private hasWritePermissions(node:any){
        if(node.access.indexOf(RestConstants.ACCESS_WRITE)==-1){
            return {status:false,message:'NO_WRITE_PERMISSIONS'};
        }
        return {status:true};
    }
}

