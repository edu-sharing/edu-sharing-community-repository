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
    CollectionContent, Collection, NodeWrapper, ConnectorList, Connector
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
import {RestConnectorsService} from "../../common/rest/services/rest-connectors.service";
import {FrameEventsService} from "../../common/services/frame-events.service";
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
    private editorType: string;
    private file: File;
    private fileName: string;
    private text: string;
    private loading=true;
  constructor(private toast: Toast,
              private route: ActivatedRoute,
              private router: Router,
              private sanitizer: DomSanitizer,
              private node: RestNodeService,
              private connectors: RestConnectorsService,
              private events: FrameEventsService,
              private utilities: RestUtilitiesService,
              private translate: TranslateService,
              private collectionApi: RestCollectionService,
              private storage : SessionStorageService,
              private cordova : CordovaService,
              private config : ConfigurationService,
              private connector: RestConnectorService) {
      this.columns.push(new ListItem("COLLECTION", 'title'));
      this.columns.push(new ListItem("COLLECTION", 'info'));
      this.columns.push(new ListItem("COLLECTION", 'scope'));
      if (this.cordova.isRunningCordova()) {
          this.cordova.subscribeServiceReady().subscribe(() => {
              this.init();
          });
      }
      else{
          this.init();
      }

  }
    getType(){
      if(this.isLink())
          return "link";
      if(this.isTextSnippet())
          return "file-txt";
      if(this.mimetype=="application/pdf")
          return "file-pdf";
      if(this.mimetype)
          return "file-"+this.mimetype.split("/")[0];
      return "file";
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
            if(this.editorType){
                prop[RestConstants.CCM_PROP_EDITOR_TYPE]=[this.editorType];
            }
            if(this.text){
                prop[RestConstants.LOM_PROP_GENERAL_DESCRIPTION]=[this.text];
            }
            this.node.createNode(this.inbox.ref.id, RestConstants.CCM_TYPE_IO, [], prop, true).subscribe((data: NodeWrapper) => {
                this.node.uploadNodeContent(data.node.ref.id, this.file, RestConstants.COMMENT_MAIN_FILE_UPLOAD,this.mimetype).subscribe(() => {
                    callback(data.node);
                });
            });
        }
    }
    saveFile(){
        this.saveInternal((node:Node)=>{
            this.goToInbox();
            this.events.broadcastEvent(FrameEventsService.EVENT_SHARED,node);
        });
    }
    private saveToCollection(collection:Node){
      this.saveInternal((node:Node)=>{
          this.collectionApi.addNodeToCollection(collection.ref.id,node.ref.id).subscribe(()=>{
              UIHelper.goToCollection(this.router,collection,{replaceUrl:true});
              this.events.broadcastEvent(FrameEventsService.EVENT_SHARED,node);
          });
      });
    }
    private isLink() {
        if(this.hasData())
            return false;
        if(!this.uri)
            return false;
        if(this.uri.startsWith("content://") || this.uri.startsWith("file://"))
            return false;
        let pos=this.uri.indexOf("://");
        return pos>0 && pos<10;
    }
    private isTextSnippet() {
        if(this.hasData())
            return false;
        if(this.isLink())
            return false;
        if(!this.uri)
            return false;
        return !this.uri.startsWith("content://") && !this.uri.startsWith("file://");
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
                console.log("share app query params",params);
                this.uri=params['uri'];
                this.mimetype=params['mimetype'];
                this.fileName=params['file'];
                this.text=params['text']; // ios only: custom description
                this.description=null;
                console.log(this.uri);
                this.collectionApi.search("",{
                    sortBy:[RestConstants.CM_MODIFIED_DATE],
                    offset:0,
                    count:50,
                    sortAscending:false,
                }).subscribe((data:CollectionContent)=>{
                    this.collections=data.collections;
                    this.loading=false;
                },(error)=>{
                    this.toast.error(error)
                    this.loading=false;
                });
                this.node.getNodeParents(RestConstants.INBOX, false).subscribe((data: ParentList) => {
                    this.inboxPath = data.nodes;
                    this.inbox=data.nodes[0];
                });
                this.previewUrl=this.connector.getThemeMimePreview(this.getType()+'.svg');
                if(this.isLink()) {
                    this.utilities.getWebsiteInformation(this.uri).subscribe((data: any) => {
                        if(data.title) {
                            this.title = data.title + " - " + data.page;
                        }
                        else{
                            this.title = this.uri;
                        }
                        this.description = data.description;
                        this.globalProgress = false;
                    });
                }
                else if(this.isTextSnippet()){
                    this.globalProgress = false;
                    this.connectors.list().subscribe((list:ConnectorList)=>{
                        this.prepareTextSnippet(list.connectors);
                    },(error:any)=>{
                        this.prepareTextSnippet(null);
                    })
                }
                else{
                    if(this.cordova.isRunningCordova() && this.cordova.getLastIntent() && this.cordova.getLastIntent().stream){
                        let base64=this.cordova.getLastIntent().stream;
                        if(this.mimetype.startsWith("image/")) {
                            this.previewUrl = this.sanitizer.bypassSecurityTrustResourceUrl("data:" + this.mimetype + ";base64," + base64);
                        }
                        this.file = Helper.base64toBlob(base64,this.mimetype) as any;
                        this.cordova.getFileAsBlob(this.uri,this.mimetype).subscribe((data:any)=> {
                            this.globalProgress=false;
                            this.updateTitle();
                        },(error:any)=>{
                            console.warn(error);
                            this.globalProgress=false;
                            this.updateTitle();
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

    private updateTitle() {
        let split = this.fileName ? this.fileName.split("/") : this.uri.split("/");
        this.title = decodeURIComponent(split[split.length - 1]);
    }

    private hasData() {
        return this.cordova.isRunningCordova() && this.cordova.getLastIntent() && this.cordova.getLastIntent().stream!=null;
    }

    private prepareTextSnippet(connectors : Connector[]) {
        this.mimetype='text/plain';
        let filetype='txt';
        if(connectors && connectors.length){
            let i=Helper.indexOfObjectArray(connectors,"id","TINYMCE");
            if(i!=-1){
                let connector=connectors[i];
                this.mimetype=connector.filetypes[0].mimetype;
                filetype=connector.filetypes[0].filetype;
                this.editorType=connector.filetypes[0].editorType;
            }
        }
        this.title = this.translate.instant('SHARE_APP.TEXT_SNIPPET')+" "+
            DateHelper.getDateForNewFile()+"."+filetype;
        this.file = (new Blob([this.uri], {
            type: this.mimetype
        }) as any);
    }
}

