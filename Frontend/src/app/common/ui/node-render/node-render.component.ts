import {
  Component, OnInit, OnDestroy, Input, EventEmitter, Output, ViewChild, ElementRef,
  HostListener, ChangeDetectorRef, ApplicationRef, NgZone
} from '@angular/core';
import {RestConnectorService} from "../../rest/services/rest-connector.service";
import {RestConstants} from "../../rest/rest-constants";
import {NodeList, Node, NodeWrapper, LoginResult, ConnectorList} from "../../rest/data-object";
import {Toast} from "../toast";
import {RestNodeService} from "../../rest/services/rest-node.service";
import {ActivatedRoute, Params, Router} from "@angular/router";
import {TranslateService} from "@ngx-translate/core";
import {Translation} from "../../translation";
import {TemporaryStorageService} from "../../services/temporary-storage.service";
import {OptionItem} from "../actionbar/option-item";
import {UIAnimation} from "../ui-animation";
import {FrameEventsService} from "../../services/frame-events.service";
import {UIHelper} from "../ui-helper";
import {ConfigurationService} from "../../services/configuration.service";
import {Title} from "@angular/platform-browser";
import {SessionStorageService} from "../../services/session-storage.service";
import {RestConnectorsService} from "../../rest/services/rest-connectors.service";
import {trigger} from "@angular/animations";
import {Location} from "@angular/common";
import {NodeHelper} from "../node-helper";
import {RestToolService} from "../../rest/services/rest-tool.service";
import {UIConstants} from "../ui-constants";
import {ConfigurationHelper} from "../../rest/configuration-helper";
import {SearchService} from "../../../modules/search/search.service";
import {Helper} from "../../helper";
import {EventListener} from "../../../common/services/frame-events.service";

declare var jQuery:any;
declare var window: any;

@Component({
  selector: 'node-render',
  templateUrl: 'node-render.component.html',
  styleUrls: ['node-render.component.scss'],
  animations: [
    trigger('fadeFast', UIAnimation.fade(UIAnimation.ANIMATION_TIME_FAST))
  ]
})


export class NodeRenderComponent implements EventListener{


  public isLoading=true;
  /**
   * Show a bar at the top with the node name or not
   * @type {boolean}
   */
  @Input() showTopBar=true;
  /**
   * Node version, -1 indicates the latest
   * @type {string}
   */
  @Input() version=RestConstants.NODE_VERSION_CURRENT;
  /**
   *   display metadata
   */
  @Input() metadata=true;
  private isRoute = false;
  private options: OptionItem[]=[];
  private list: Node[];
  private isSafe=false;
  private isOpenable: boolean;
  private closeOnBack: boolean;
  public nodeMetadata: Node;
  public addToCollection: Node[];
  public nodeReport: Node;
  private editor: string;
  private fromLogin = false;
  public banner: any;
  private repository: string;
  private downloadButton: OptionItem;
  private downloadUrl: string;
  public nodeComments: Node;

  @HostListener('window:beforeunload', ['$event'])
  beforeunloadHandler(event:any) {
    if(this.isSafe){
      this.connector.logoutSync();
    }
  }

  @HostListener('document:keydown', ['$event'])
  handleKeyboardEvent(event: KeyboardEvent) {
    if(this.nodeMetadata!=null) {
      return;
    }
    if(event.code=="Escape"){
      event.preventDefault();
      event.stopPropagation();
      this.close();
      return;
     }
    if (event.code == "ArrowLeft" && this.canSwitchBack()) {
      this.node = this.list[this.getPosition() - 1];
      event.preventDefault();
      event.stopPropagation();
      return;
    }
    if (event.code == "ArrowRight" && this.canSwitchForward()) {
      this.node = this.list[this.getPosition() + 1];
      event.preventDefault();
      event.stopPropagation();
      return;
    }

  }
    private _node : Node;
    private _nodeId : string;
    @Input() set node(node: Node|string){
      let id=(node as Node).ref ? (node as Node).ref.id : (node as string);
      this._nodeId=id;
      this.loadRenderData();
    }
    @Output() onClose=new EventEmitter();
    private close(){
      if(this.isRoute) {
        if(this.closeOnBack){
          window.close();
        }
        else {
          if(this.fromLogin){
            this.router.navigate([UIConstants.ROUTER_PREFIX+"workspace"]);
          }
          else {
            this.searchService.reinit=false;
            NodeRenderComponent.close(this.location);
          }
        }
      }
      else
        this.onClose.emit();
    }


    private showDetails() {
      let rect=document.getElementById('edusharing_rendering_metadata').getBoundingClientRect();
      if(window.scrollY<rect.top) {
          UIHelper.scrollSmooth(rect.top, 1.5);
      }
    }
    public getPosition(){
      if(!this._node || !this.list)
        return -1;
      let i=0;
      for(let node of this.list){
        if(node.ref.id==this._node.ref.id)
          return i;
        i++;
      }
      return -1;
    }
    onEvent(event: string, data: any): void {
        if(event==FrameEventsService.EVENT_REFRESH){
            this.refresh();
        }
    }
    constructor(
      private translate : TranslateService,
      private location: Location,
      private searchService : SearchService,
      private connector : RestConnectorService,
      private connectors : RestConnectorsService,
      private nodeApi : RestNodeService,
      private toolService: RestToolService,
      private frame : FrameEventsService,
      private toast : Toast,
      private cd: ChangeDetectorRef,
      private title : Title,
      private config : ConfigurationService,
      private storage : SessionStorageService,
      private route : ActivatedRoute,
      private router : Router,
      private temporaryStorageService: TemporaryStorageService) {
      (window as any).ngRender = {setDownloadUrl:(url:string)=>{this.setDownloadUrl(url)}};
      this.frame.addListener(this);
        Translation.initialize(translate,config,storage,route).subscribe(()=>{
        this.banner = ConfigurationHelper.getBanner(this.config);
        this.connector.setRoute(this.route);
        this.route.queryParams.subscribe((params:Params)=>{
          this.closeOnBack=params['closeOnBack']=='true';
          this.editor=params['editor'];
          this.fromLogin=params['fromLogin']=='true';
          this.repository=params['repository'] ? params['repository'] : RestConstants.HOME_REPOSITORY;
          this.route.params.subscribe((params: Params) => {
            if(params['node']) {
              this.isRoute=true;
              this.list = this.temporaryStorageService.get(TemporaryStorageService.NODE_RENDER_PARAMETER_LIST);
              this.connector.isLoggedIn().subscribe((data:LoginResult)=>{
                this.isSafe=data.currentScope==RestConstants.SAFE_SCOPE;
                if(params['version'])
                  this.version=params['version'];
                setTimeout(()=>this.node = params['node'],10);
              });
            }
          });
        });

      });
      this.frame.broadcastEvent(FrameEventsService.EVENT_VIEW_OPENED,'node-render');
    }
    ngOnDestroy() {
        (window as any).ngRender = null;
    }

  public static close(location:Location) {
    location.back();
  }
  public switchPosition(pos:number){
    //this.router.navigate([UIConstants.ROUTER_PREFIX+"render",this.list[pos].ref.id]);
    this.isLoading=true;
    this.node=this.list[pos];
    //this.options=[];
  }
  public canSwitchBack(){
    return this.list && this.getPosition()>0 && !this.list[this.getPosition()-1].isDirectory;
  }
  public canSwitchForward(){
    return this.list && this.getPosition()<this.list.length-1 && !this.list[this.getPosition()+1].isDirectory;
  }
  public closeMetadata(){
    this.nodeMetadata=null;
  }
  public refresh(){
    this.options=[];
    this.isLoading=true;
    this.node=this._nodeId;
  }
  private loadNode() {
    if(!this._node)
      return;

    let input=this.temporaryStorageService.get(TemporaryStorageService.NODE_RENDER_PARAMETER_OPTIONS);
    if(!input) input=[];
    let opt=[];
    for(let o of input){
      opt.push(o);
    }
    this.options=opt;
    let download=new OptionItem('DOWNLOAD','cloud_download',()=>this.downloadCurrentNode());
    download.isEnabled=this._node.downloadUrl!=null;
    if(this.isCollectionRef()){
      console.log("is ref");
      this.nodeApi.getNodeMetadata(this._node.properties[RestConstants.CCM_PROP_IO_ORIGINAL]).subscribe((node:NodeWrapper)=>{
        this.addDownloadButton(download);
      },(error:any)=>{
        if(error.status==RestConstants.HTTP_NOT_FOUND) {
          console.log("original missing");
          download.isEnabled = false;
        }
        this.addDownloadButton(download);
      });
      return;
    }
    this.addDownloadButton(download);
  }
  private loadRenderData(){
    let parameters={
      showDownloadButton:false,
      showDownloadAdvice:!this.isOpenable
    };
    this.nodeApi.getNodeRenderSnippet(this._nodeId,this.version ? this.version : "-1",parameters)
        .subscribe((data:any)=>{
            if (!data.detailsSnippet) {
                console.error(data);
                this.toast.error(null,"RENDERSERVICE_API_ERROR");
            }
            else {
                this._node=data.node;
                jQuery('#nodeRenderContent').html(data.detailsSnippet);
                this.postprocessHtml();
                this.loadNode();
                this.isLoading = false;
            }
            this.isLoading = false;
        },(error:any)=>{
            console.log(error);
            this.toast.error(error);
            this.isLoading = false;
        })
  }

  private postprocessHtml() {
    if(!this.config.instant("rendering.showPreview",true)){
      jQuery('.edusharing_rendering_content_wrapper').hide();
      jQuery('.showDetails').hide();
    }
      jQuery('.edusharing_rendering_metadata_header').append(`
      <div class="nodeDetails">
        <div class="item" onclick="window.nodeRenderComponentRef.component.showComments()">
          <div>`+this._node.commentCount+`</div><i class="material-icons">message</i>
        </div>
      </div>
    `);
      let element=jQuery('#edusharing_rendering_content_href');
      console.log(element);
      element.click((event:any)=>{
          if(this.connector.getCordovaService().isRunningCordova()){
              let href=element.attr('href');
              console.log(href);
              this.connector.getCordovaService().openBrowser(href);
              event.preventDefault();
          }
      });
  }
  private openLink(href:string){

  }
  private showComments(){
      this.nodeComments=this._node;
  }
  private downloadCurrentNode() {
      if(this.downloadUrl)
        NodeHelper.downloadUrl(this.toast,this.connector.getCordovaService(),this.downloadUrl);
      else
        NodeHelper.downloadNode(this.toast,this.connector.getCordovaService(),this._node,this.version);
  }

  private openConnector(list:ConnectorList,newWindow=true) {
    if(RestToolService.isLtiObject(this._node)){
      this.toolService.openLtiObject(this._node);
    }
    else {
      UIHelper.openConnector(this.connectors,this.frame,this.toast,list, this._node,null,null,null,newWindow);
    }
  }

  private checkConnector() {
    this.connector.isLoggedIn().subscribe((login:LoginResult)=>{
      if(!this.isCollectionRef()){
        let openFolder = new OptionItem('SHOW_IN_FOLDER', 'folder', () => this.goToWorkspace(login,this._node));
        openFolder.isEnabled=false;
        this.nodeApi.getNodeParents(this._node.parent.id,false,[],this._node.parent.repo).subscribe((data:NodeList)=>{
          openFolder.isEnabled = true;
        });
        if(this._node.type!=RestConstants.CCM_TYPE_REMOTEOBJECT && ConfigurationHelper.hasMenuButton(this.config,"workspace"))
          this.options.push(openFolder);

        let edit=new OptionItem('WORKSPACE.OPTION.EDIT','info_outline',()=>this.nodeMetadata=this._node);
        edit.isEnabled=this._node.access.indexOf(RestConstants.ACCESS_WRITE)!=-1 && this._node.type!=RestConstants.CCM_TYPE_REMOTEOBJECT;
        if(this.version==RestConstants.NODE_VERSION_CURRENT)
          this.options.push(edit);
        this.isOpenable=false;
      }
      else{
        let openFolder = new OptionItem('SHOW_IN_FOLDER', 'folder', () => this.goToWorkspace(login,this._node));
        openFolder.isEnabled = false;
        this.nodeApi.getNodeMetadata(this._node.properties[RestConstants.CCM_PROP_IO_ORIGINAL]).subscribe((node:NodeWrapper)=>{

          this.nodeApi.getNodeParents( node.node.parent.id,false,[], node.node.parent.repo).subscribe((data:NodeList)=>{
            openFolder.isEnabled = true;
            //.isEnabled = data.node.access.indexOf(RestConstants.ACCESS_WRITE) != -1;
          });
        },(error:any)=>{
        });
        this.options.push(openFolder);
      }
      let addCollection=new OptionItem('WORKSPACE.OPTION.COLLECTION','layers',()=>this.addToCollection=[this._node]);
      addCollection.isEnabled=this._node.access.indexOf(RestConstants.ACCESS_CC_PUBLISH)!=-1 && this._node.type!=RestConstants.CCM_TYPE_REMOTEOBJECT;
      this.options.push(addCollection);

      if (this.config.instant("nodeReport", false)) {
        let nodeReport = new OptionItem('NODE_REPORT.OPTION', 'flag', () => this.nodeReport = this._node);
        this.options.push(nodeReport);
      }

      this.isOpenable=false;
      this.connectors.list().subscribe((data:ConnectorList)=>{
        if(this.version==RestConstants.NODE_VERSION_CURRENT && RestConnectorsService.connectorSupportsEdit(data,this._node) || RestToolService.isLtiObject(this._node)){
          let view=new OptionItem("WORKSPACE.OPTION.VIEW", "launch",()=>this.openConnector(data,true));
          //view.isEnabled = this._node.access.indexOf(RestConstants.ACCESS_WRITE)!=-1;
          this.options.splice(0,0,view);
          this.options=Helper.deepCopyArray(this.options);
          this.isOpenable=true;
          if(this.editor && RestConnectorsService.connectorSupportsEdit(data,this._node).id==this.editor){
            this.openConnector(data,false);
          }
        }
      },(error:any)=>{
      });
      this.options=Helper.deepCopyArray(this.options);
    });
  }

  private goToWorkspace(login:LoginResult,node:Node) {
      UIHelper.goToWorkspace(this.nodeApi,this.router,login,node);
  }

  private isCollectionRef() {
    return this._node.aspects.indexOf(RestConstants.CCM_ASPECT_IO_REFERENCE)!=-1;
  }

  private addDownloadButton(download: OptionItem) {
    this.downloadButton=download;
    this.options.splice(0,0,download);
    this.checkConnector();

    UIHelper.setTitleNoTranslation(this._node.name,this.title,this.config);
  }
  setDownloadUrl(url:string){
      if(this.downloadButton!=null)
        this.downloadButton.isEnabled=url!=null;
      this.downloadUrl=url;
  }
}
