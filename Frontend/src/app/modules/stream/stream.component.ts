import {Component, ViewChild} from '@angular/core';
import 'rxjs/add/operator/map';
import {ActivatedRoute, Params, Router, RoutesRecognized} from '@angular/router';
import {TranslateService} from "@ngx-translate/core";
import {Translation} from "../../core-ui-module/translation";
import * as EduData from "../../core-module/core.module"; //
import {RequestObject, RestCollectionService} from "../../core-module/core.module"; //
import {Toast} from "../../core-ui-module/toast"; //
import {RestSearchService} from '../../core-module/core.module';
import {RestNodeService} from '../../core-module/core.module';
import {RestConstants} from '../../core-module/core.module';
import {RestConnectorService} from "../../core-module/core.module";
import {Node, NodeList, LoginResult, STREAM_STATUS, ConnectorList} from '../../core-module/core.module';
import {OptionItem} from "../../core-ui-module/option-item";
import {TemporaryStorageService} from "../../core-module/core.module";
import {UIHelper} from "../../core-ui-module/ui-helper";
import {Title} from "@angular/platform-browser";
import {ConfigurationService} from "../../core-module/core.module";
import {SessionStorageService} from "../../core-module/core.module";
import {UIConstants} from "../../core-module/ui/ui-constants";
import {RestMdsService} from "../../core-module/core.module";
import {RestHelper} from "../../core-module/core.module";
import {NodeHelper} from "../../core-ui-module/node-helper"; //
import {Observable} from 'rxjs/Rx';
import {RestStreamService} from "../../core-module/core.module";
import {RestConnectorsService} from '../../core-module/core.module';
import {UIAnimation} from '../../core-module/ui/ui-animation';
import {trigger} from '@angular/animations';
import {Connector} from '../../core-module/core.module';
import {NodeWrapper} from '../../core-module/core.module';
import {Filetype} from '../../core-module/core.module';
import {FrameEventsService} from '../../core-module/core.module';
import {CordovaService} from '../../common/services/cordova.service';
import 'rxjs/add/operator/pairwise';
import {Subscription} from 'rxjs/Subscription';
import * as moment from 'moment';
import {ActionbarHelperService} from '../../common/services/actionbar-helper';
import {RestIamService} from '../../core-module/core.module';
import {MainNavComponent} from '../../common/ui/main-nav/main-nav.component';
import {BridgeService} from "../../core-bridge-module/bridge.service";
import {GlobalContainerComponent} from "../../common/ui/global-container/global-container.component";


@Component({
  selector: 'app-stream',
  templateUrl: 'stream.component.html',
  styleUrls: ['stream.component.scss'],
  animations:[
      trigger('overlay', UIAnimation.openOverlay(UIAnimation.ANIMATION_TIME_FAST)),
  ]
  })


export class StreamComponent {
  @ViewChild('mainNav') mainNavRef: MainNavComponent;
  connectorList: ConnectorList;
  createConnectorName: string;
  createConnectorType: Connector;
  createAllowed : boolean ;
  showCreate = false;
  public collectionNodes:EduData.Node[];
  public tabSelected:string = RestConstants.COLLECTIONSCOPE_MY;
  public mainnav = true;
  public nodeReport: Node;
  public globalProgress=false;
  menuOption = 'new';
  showMenuOptions = false;
  streams: any;
  actionOptions:OptionItem[]=[];
  pageOffset: number;
  imagesToLoad = -1;
  shouldOpen = false;
  routerSubscription: Subscription;
  dateToDisplay: string;
  amountToRandomize: number;

  moveUpOption = new OptionItem('STREAM.OBJECT.OPTION.MARK','toc',(node: any)=>{
    this.updateStatus(node.id, STREAM_STATUS.PROGRESS).subscribe( () => {
      //this.updateDataFromJSON(STREAM_STATUS.OPEN);
      this.streams = this.streams.filter((n: any) => n.id !== node.id);
      this.toast.toast("STREAM.TOAST.MARKED");
    }, error => console.log(error));
  });

  collectionOption = new OptionItem("WORKSPACE.OPTION.COLLECTION", "layers",(node: Node) => this.addToCollection(node));

  removeOption = new OptionItem('STREAM.OBJECT.OPTION.REMOVE','delete',(node: any)=> {
    this.updateStatus(node.id, STREAM_STATUS.DONE).subscribe( () => {
      this.streams = this.streams.filter((n: any) => n.id !== node.id);
      this.toast.toast("STREAM.TOAST.REMOVED");
    });
  });

  // TODO: Store and use current search query
  searchQuery:string;
  isLoading=true;
  doSearch(query:string){
    this.searchQuery=query;
    console.log(query);
    // TODO: Search for the given query doch nicht erledigt
  }
  constructor(
    private router : Router,
    private route : ActivatedRoute,
    private connector:RestConnectorService,
    private connectors:RestConnectorsService,
    private nodeService: RestNodeService,
    private cordova: CordovaService,
    private searchService: RestSearchService,
    private event:FrameEventsService,
    private streamService:RestStreamService,
    private iam:RestIamService,
    private storage : TemporaryStorageService,
    private session : SessionStorageService,
    private title : Title,
    private toast : Toast,
    private bridge : BridgeService,
    private actionbar: ActionbarHelperService,
    private collectionService : RestCollectionService,
    private config : ConfigurationService,
    private translate : TranslateService) {
      Translation.initialize(translate,this.config,this.session,this.route).subscribe(()=>{
        UIHelper.setTitle('STREAM.TITLE',title,translate,config);
        this.connector.isLoggedIn().subscribe(data => {
            this.dateToDisplay = moment().locale(translate.currentLang).format('dddd, DD. MMMM YYYY');
            this.createAllowed=data.statusCode==RestConstants.STATUS_CODE_OK;
            GlobalContainerComponent.finishPreloading();
        });
          this.connectors.list().subscribe(list=>{
              this.connectorList=list;
          });
      });
      this.amountToRandomize = 4;
      this.setStreamMode();
      this.routerSubscription = this.router.events
      .filter(e => e instanceof RoutesRecognized)
      .pairwise()
      .subscribe((e: any[]) => {
        document.cookie = "scroll="+"noScroll";
        if (/components\/render/.test(e[0].urlAfterRedirects)) {
          this.route.queryParams.subscribe((params: Params) => {
            console.log("params.mode", params.mode);
            if (params.mode === 'seen') {
              document.cookie = "scroll="+"seen";
            }
            if (params.mode === 'new') {
              if (e[1].urlAfterRedirects === '/components/stream?mode=new'){
                document.cookie = "scroll="+"new";
                this.toast.toast('STREAM.TOAST.SEEN');
              }
            }
          });
          this.routerSubscription.unsubscribe();
        }
      });
  }

  setStreamMode() {
    this.route.queryParams.subscribe((params: Params) => {
      if (params.mode === 'new' || params.mode === 'seen' || params.mode === 'relevant' || params.mode === 'marked') {
        this.menuOptions(params.mode);
      }
      else{
        this.goToOption('new');
      }
    });
  }

  seen(id: any) {
    this.updateStatus(id, STREAM_STATUS.READ).subscribe(data => this.updateDataFromJSON(STREAM_STATUS.OPEN) , error => console.log(error));
  }

  onScroll() {
    console.log("scrolled!!");
    //this.updateDataFromJSON(STREAM_STATUS.OPEN);
    let curStat = this.menuOption === 'new' ? STREAM_STATUS.OPEN : this.menuOption=='marked'? STREAM_STATUS.PROGRESS : STREAM_STATUS.READ;
    let sortWay = this.menuOption === 'new' ? false : false;
    this.getJSON(curStat, sortWay).subscribe(data => {
      console.log("r, data: ",data['stream']);
      this.streams = this.streams.concat(data['stream']);
    }, error => console.log(error));
  }

  toggleMenuOptions() {
    this.showMenuOptions = !this.showMenuOptions;
    if (this.showMenuOptions) {
      this.shouldOpen = true;
    }
  }

  closeMenuOptions() {
    this.showMenuOptions = false;
    if (this.shouldOpen) {
      this.showMenuOptions = true;
      this.shouldOpen = false;
    }
  }

  checkIfEnable(nodes: Node[]) {
    this.collectionOption.isEnabled = NodeHelper.getNodesRight(nodes, RestConstants.ACCESS_CC_PUBLISH);
  }

  scrollToDown() {
    console.log(this.getCookie("jumpToScrollPosition"));
    let pos = Number(this.getCookie("jumpToScrollPosition"));
    let whichScroll = this.getCookie("scroll");
    console.log("which: ", whichScroll);
    if (whichScroll !== "noScroll"){
      setTimeout(function() {
        console.log("scroll to: ",pos);
        window.scrollTo(0,pos);
      }, 2900);
    }
    document.cookie = "scroll="+"noScroll";
  }

  getCookie(cname: any) {
    let name = cname + "=";
    let decodedCookie = decodeURIComponent(document.cookie);
    let ca = decodedCookie.split(';');
    for(let i = 0; i <ca.length; i++) {
        let c = ca[i];
        while (c.charAt(0) == ' ') {
            c = c.substring(1);
        }
        if (c.indexOf(name) == 0) {
            return c.substring(name.length, c.length);
        }
    }
    return "";
}

  menuOptions(option: any) {
    this.menuOption = option;
    this.imagesToLoad = -1;
    this.actionOptions=[];
    this.streams = [];
    if (option === 'new') {
      this.updateDataFromJSON(STREAM_STATUS.OPEN);
      this.actionOptions.push(this.moveUpOption);
      this.actionOptions.push(this.collectionOption);
      this.actionOptions.push(this.removeOption);
    }
    else if (option === 'marked') {
      this.updateDataFromJSON(STREAM_STATUS.PROGRESS);
      this.actionOptions.push(this.collectionOption);
      this.actionOptions.push(this.removeOption);
    }
    else if(option == 'relevant'){
      this.searchRelevant();
    }
    else {
      this.updateDataFromJSON(STREAM_STATUS.READ);
      this.actionOptions.push(this.collectionOption);
      this.actionOptions.push(this.removeOption);
    }
      let nodeStore = this.actionbar.createOptionIfPossible('ADD_NODE_STORE',null,(node: Node) => {
          this.addToStore(node);
      });
      this.actionOptions.push(nodeStore);
  }

  goToOption(option: string) {
    this.router.navigate(["./"],{queryParams:{mode:option},relativeTo:this.route})
  }

  updateDataFromJSON(streamStatus: any) {
    /*if (streamStatus == STREAM_STATUS.OPEN) {
      let openStreams: any[];
      let progressStreams: any[];
      let unSortedStream: any[];
      this.getSimpleJSON(STREAM_STATUS.OPEN, false).subscribe(data => {
        openStreams = data['stream'].filter( (n : any) => n.nodes.length !== 0);
        this.getSimpleJSON(STREAM_STATUS.PROGRESS, false).subscribe(data => {
          progressStreams = data['stream'].filter( (n : any) => n.nodes.length !== 0);
          console.log("streams received: ",  progressStreams.concat(openStreams));
          unSortedStream = progressStreams.concat(openStreams);
          //unSortedStream.length >= this.amountToRandomize ? this.randomizeTop(unSortedStream,this.amountToRandomize) : console.log('not big enough to randomize');
          this.streams = unSortedStream;
          console.log("objs: ",this.streams);
          this.imagesToLoad = this.streams.length;
          this.scrollToDown();
        });
      }, error => console.log(error));
    }
    else {*/
      this.streams = [];
      this.isLoading = true;
      this.getSimpleJSON(streamStatus).subscribe(data => {
        this.streams = data['stream'].filter( (n : any) => n.nodes.length !== 0);
        this.imagesToLoad = this.streams.length;
        this.isLoading = false;
        this.scrollToDown();
      }, error => console.log(error));
    //}

  }

  randomizeTop(array: any, quantity: number) {
      quantity = (quantity > 0) ? quantity - 1 : 0;
      for (let i = quantity; i > 0; i--) {
          const j = Math.floor(Math.random() * (i + 1));
          [array[i], array[j]] = [array[j], array[i]];
      }
  }

  onStreamObjectClick(node: any) {
    if(node.nodes) {
      console.log(node.nodes[0].ref.id);
      this.seen(node.id);
      document.cookie = "jumpToScrollPosition="+window.pageYOffset;
      this.router.navigate([UIConstants.ROUTER_PREFIX+"render", node.nodes[0].ref.id])
    }
    else{
      this.router.navigate([UIConstants.ROUTER_PREFIX+"render", node.ref.id])
    }

  }

  private addToCollection(nodes: any) {
    /*
    let result = this.streams.filter( (n: any) => (n.id == node) ).map( (n: any) => { return n.nodes } );
    this.collectionNodes = [].concat.apply([], result);
    */
    this.collectionNodes=nodes.nodes;

  }
  private addToStore(nodes:any) {
    this.globalProgress=true;
    RestHelper.addToStore(nodes.nodes,this.bridge,this.iam,()=>{
        this.globalProgress=false;
        this.mainNavRef.refreshNodeStore();
    });
  }
  public getJSON(streamStatus: any, sortAscendingCreated: boolean = false): Observable<any> {
    console.log(this.streams.length);
    let request:any={offset: (this.streams ? this.streams.length : 0), sortBy:["priority","created"],sortAscending:[false,sortAscendingCreated]};
    return this.streamService.getStream(streamStatus,this.searchQuery,{},request);
  }

  public getSimpleJSON(streamStatus: any, sortAscendingCreated: boolean = false): Observable<any> {
    let request:any={offset: 0, sortBy:["priority","created"],sortAscending:[false,sortAscendingCreated]};
    return this.streamService.getStream(streamStatus,this.searchQuery,{},request);
  }

  public updateStatus(idToUpdate: any, status: any): Observable<any> {
    return this.streamService.updateStatus(idToUpdate, this.connector.getCurrentLogin().authorityName, status)
  }
    private create(){
        if(!this.createAllowed)
            return;
        this.showCreate = true;
    }
    private createConnector(event : any){
        this.createConnectorName=null;
        let prop=NodeHelper.propertiesFromConnector(event);
        let win:any;
        if(!this.cordova.isRunningCordova())
            win=window.open("");
        this.nodeService.createNode(RestConstants.INBOX,RestConstants.CCM_TYPE_IO,[],prop,false).subscribe(
            (data : NodeWrapper)=>{
                this.editConnector(data.node,event.type,win,this.createConnectorType);
                UIHelper.goToWorkspaceFolder(this.nodeService,this.router,null,RestConstants.INBOX);
            },
            (error : any)=>{
                win.close();
                if(NodeHelper.handleNodeError(this.toast,event.name,error)==RestConstants.DUPLICATE_NODE_RESPONSE){
                    this.createConnectorName=event.name;
                }
            }
        )

    }
    private editConnector(node:Node,type : Filetype=null,win : any = null,connectorType : Connector = null){
        UIHelper.openConnector(this.connectors,this.iam,this.event,this.toast,node,type,win,connectorType);
    }

  private searchRelevant() {
    let request:RequestObject={
      propertyFilter:[RestConstants.ALL]
    };
    this.isLoading=true;
    this.searchService.getRelevant(request).subscribe((relevant)=>{
      console.log(relevant);
      this.streams=relevant.nodes;
      this.imagesToLoad=this.streams.length;
      this.isLoading=false;
    });
  }
  public getTitle(node:Node){
    return RestHelper.getTitle(node);
  }
}
