import {Translation} from "../../common/translation";
import {UIHelper} from "../../common/ui/ui-helper";
import {ActivatedRoute, Router} from "@angular/router";
import {Toast} from "../../common/ui/toast";
import {ConfigurationService} from "../../common/services/configuration.service";
import {DomSanitizer, Title} from "@angular/platform-browser";
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
import {RestHelper} from "../../common/rest/rest-helper";
import {CordovaService} from "../../common/services/cordova.service";
import {DateHelper} from "../../common/ui/DateHelper";
@Component({
  selector: 'share-app',
  templateUrl: 'share-app.component.html',
  styleUrls: ['share-app.component.scss'],
  animations: [

  ]
})
export class ShareAppComponent {
    globalProgress=true;
    private uri: string;
    private type="LINK";
    private title:string;
    private description:string;
    private previewUrl:any;
    private inboxPath: Node[];
    private inbox: Node;
    private columns:ListItem[]=[];
    private collections: Collection[];
    private mimetype: string;
    private file: File;
    private fileName: string;
  constructor(private toast: Toast,
              private route: ActivatedRoute,
              private router: Router,
              private sanitizer: DomSanitizer,
              private node: RestNodeService,
              private utilities: RestUtilitiesService,
              private translate: TranslateService,
              private collectionApi: RestCollectionService,
              private storage : SessionStorageService,
              private cordova : CordovaService,
              private config : ConfigurationService,
              private connector: RestConnectorService) {
      this.columns.push(new ListItem("COLLECTION", 'title'));
      this.columns.push(new ListItem("COLLECTION", 'info'));
      this.columns.push(new ListItem("COLLECTION",'scope'));
      this.cordova.subscribeServiceReady().subscribe(()=> {

          this.init();
          // should be handled via cordova service now
          /*if (this.cordova.hasValidConfig()) {
              this.init();
          }
          else{
              console.log("no cordova config, go to start");
              this.router.navigate(['']);
          }
          */
      });
  }
    getType(){
      if(this.isLink())
          return "link";
      if(this.isTextSnippet())
          return "file-txt";
      return "file-"+this.mimetype.split("/")[0];
    }
    saveInternal(callback:Function){
        this.globalProgress=true;
        if(this.isLink()){
            let prop:any={};
            prop[RestConstants.CCM_PROP_IO_WWWURL]=[this.uri];
            this.node.createNode(this.inbox.ref.id,RestConstants.CCM_TYPE_IO,[],prop,true,RestConstants.COMMENT_MAIN_FILE_UPLOAD).subscribe((data:NodeWrapper)=>{
                callback(data.node);
            });
        }
        else {
            let prop: any = RestHelper.createNameProperty(this.title);
            this.node.createNode(this.inbox.ref.id, RestConstants.CCM_TYPE_IO, [], prop, true).subscribe((data: NodeWrapper) => {
                this.node.uploadNodeContent(data.node.ref.id, this.file, RestConstants.COMMENT_MAIN_FILE_UPLOAD,this.mimetype).subscribe(() => {
                    callback(data.node);
                });
            });
        }
    }
    saveFile(){
        this.saveInternal(()=>this.goToInbox());
    }
    private saveToCollection(collection:Node){
      this.saveInternal((node:Node)=>{
          this.collectionApi.addNodeToCollection(collection.ref.id,node.ref.id).subscribe(()=>{
              UIHelper.goToCollection(this.router,collection,{replaceUrl:true});
          });
      });
    }
    private isLink() {
        if(this.uri.startsWith("content://"))
            return false;
        let pos=this.uri.indexOf("://");
        return pos>0 && pos<10;
    }
    private isTextSnippet() {
        return !this.uri.startsWith("content://") && !this.isLink();
    }
    private goToInbox() {
        UIHelper.goToWorkspaceFolder(this.node,this.router,null,this.inbox.ref.id,{replaceUrl:true});
    }
    private hasWritePermissions(node:any){
        if(node.access.indexOf(RestConstants.ACCESS_WRITE)==-1){
            return {status:false,message:'NO_WRITE_PERMISSIONS'};
        }
        return {status:true};
    }
    private init() {
        Translation.initialize(this.translate, this.config, this.storage, this.route).subscribe(() => {
            console.log("translate");
            this.route.queryParams.subscribe((params:any)=>{
                this.uri=params['uri'];
                this.mimetype=params['mimetype'];
                this.fileName=params['file'];
                this.description=null;
                console.log(this.uri);
                this.collectionApi.search("",{
                    sortBy:[RestConstants.CM_MODIFIED_DATE],
                    offset:0,
                    count:50,
                    sortAscending:false,
                }).subscribe((data:CollectionContent)=>{
                    this.collections=data.collections;
                });
                this.node.getNodeParents(RestConstants.INBOX, false).subscribe((data: ParentList) => {
                    this.inboxPath = data.nodes;
                    this.inbox=data.nodes[0];
                });
                this.previewUrl=this.connector.getThemeMimePreview(this.getType()+'.svg');
                if(this.isLink()) {
                    this.utilities.getWebsiteInformation(this.uri).subscribe((data: any) => {
                        this.title = data.title + " - " + data.page;
                        this.description = data.description;
                        this.globalProgress = false;
                    });
                }
                else if(this.isTextSnippet()){
                    this.globalProgress = false;
                    this.title = this.translate.instant('SHARE_APP.TEXT_SNIPPET')+" "+
                        DateHelper.formatDate(this.translate,new Date().getTime(),true,false)+".txt";
                    this.mimetype='text/plain';
                    this.file = (new Blob([this.uri], {
                        type: 'text/plain'
                    }) as any);
                }
                else{
                    this.globalProgress=false;
                    if(this.cordova.getLastIntent().stream){
                        let base64=this.cordova.getLastIntent().stream;
                        this.previewUrl=this.sanitizer.bypassSecurityTrustResourceUrl("data:"+this.mimetype+";base64,"+base64);
                        this.file = Helper.base64toBlob(base64,this.mimetype) as any;
                        this.cordova.getFileAsBlob(this.uri,this.mimetype).subscribe((data:any)=> {
                            console.log(this.fileName);
                            let split = this.fileName ? this.fileName.split("/") : this.uri.split("/");
                            console.log(data);
                            this.title = decodeURIComponent(split[split.length - 1]);
                        });
                    }
                    else{
                        this.router.navigate([UIConstants.ROUTER_PREFIX,'messages','SHARING_ERROR']);
                    }
                    /*
                    this.cordova.getFileAsBlob(this.uri,this.mimetype).subscribe((data:any)=>{
                        console.log(this.fileName);
                        let split=this.fileName ? this.fileName.split("/") : this.uri.split("/");
                        console.log(data);
                        this.title=split[split.length-1];
                        this.file=data;
                        if(this.mimetype.startsWith("image/"))
                            this.previewUrl=this.sanitizer.bypassSecurityTrustUrl(data.localURL);
                        let request = new XMLHttpRequest();
                        let result=request.open('GET', data.localURL, true);
                        request.responseType = 'blob';
                        request.onload = ()=> {
                            console.log(request.response);
                            if(request.response.size<=0){
                                this.router.navigate([UIConstants.ROUTER_PREFIX,'messages','CONTENT_NOT_READABLE']);
                            }
                            this.file=request.response;
                            (this.file as any).name=this.title;
                        };
                        request.onerror=(e)=>{
                            this.router.navigate([UIConstants.ROUTER_PREFIX,'messages','CONTENT_NOT_READABLE']);
                        }
                        request.send();
                    },(error:any)=>{
                        this.router.navigate([UIConstants.ROUTER_PREFIX,'messages','CONTENT_NOT_READABLE']);
                    });*/
                }
            })
        });
    }
}

