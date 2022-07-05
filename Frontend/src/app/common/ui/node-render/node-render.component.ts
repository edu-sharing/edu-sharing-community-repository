import {
    ChangeDetectorRef,
    Component,
    ComponentFactoryResolver,
    ElementRef,
    EventEmitter,
    HostListener,
    Input,
    NgZone,
    OnDestroy,
    Output,
    ViewChild,
    ViewContainerRef
} from '@angular/core';
import {Toast} from '../../../core-ui-module/toast';
import {ActivatedRoute, Params, Router} from '@angular/router';
import {TranslateService} from '@ngx-translate/core';
import {Translation} from '../../../core-ui-module/translation';
import {DefaultGroups, ElementType, OptionGroup, OptionItem, Scope} from '../../../core-ui-module/option-item';
import {UIAnimation} from '../../../core-module/ui/ui-animation';
import {UIHelper} from '../../../core-ui-module/ui-helper';
import {trigger} from '@angular/animations';
import {Location, PlatformLocation} from '@angular/common';
import {UIConstants} from '../../../core-module/ui/ui-constants';
import {SearchService} from '../../../modules/search/search.service';
import {ActionbarHelperService} from '../../services/actionbar-helper';
import {MainNavComponent} from '../main-nav/main-nav.component';
import {HttpClient} from '@angular/common/http';
import {
    ConfigurationHelper,
    ConfigurationService,
    EventListener,
    EventType,
    FrameEventsService,
    ListItem,
    LoginResult, Mds, Metadataset,
    Node,
    NodeList,
    RestConnectorService,
    RestConnectorsService,
    RestConstants,
    RestHelper,
    RestIamService,
    RestMdsService,
    RestNetworkService,
    RestNodeService,
    RestSearchService,
    RestToolService,
    RestUsageService,
    SessionStorageService,
    TemporaryStorageService
} from '../../../core-module/core.module';
import {MdsHelper} from '../../../core-module/rest/mds-helper';
import {ListTableComponent} from '../../../core-ui-module/components/list-table/list-table.component';
import {SpinnerComponent} from '../../../core-ui-module/components/spinner/spinner.component';
import {CommentsListComponent} from '../../../modules/management-dialogs/node-comments/comments-list/comments-list.component';
import {GlobalContainerComponent} from '../global-container/global-container.component';
import {VideoControlsComponent} from '../../../core-ui-module/components/video-controls/video-controls.component';
import {ActionbarComponent} from '../actionbar/actionbar.component';
import {
    OPTIONS_HELPER_CONFIG,
    OptionsHelperService
} from '../../../core-ui-module/options-helper.service';
import {RestTrackingService} from '../../../core-module/rest/services/rest-tracking.service';
import {NodeHelperService} from '../../../core-ui-module/node-helper.service';
import {CardComponent} from '../../../core-ui-module/components/card/card.component';
import {RouterComponent} from '../../../router/router.component';

declare var jQuery:any;
declare var window: any;

@Component({
  selector: 'node-render',
  templateUrl: 'node-render.component.html',
  styleUrls: ['node-render.component.scss'],
    providers: [OptionsHelperService, {provide: OPTIONS_HELPER_CONFIG, useValue: {
        subscribeEvents: true
    }}],
    animations: [
    trigger('fadeFast', UIAnimation.fade(UIAnimation.ANIMATION_TIME_FAST))
  ]
})


export class NodeRenderComponent implements EventListener, OnDestroy {
    @Input() set node(node: Node|string) {
      const id=(node as Node).ref ? (node as Node).ref.id : (node as string);
      jQuery('#nodeRenderContent').html('');
      this._nodeId=id;
      this.loadRenderData();
    }
    constructor(
      private translate : TranslateService,
      private tracking : RestTrackingService,
      private nodeHelper: NodeHelperService,
      private location: Location,
      private searchService : SearchService,
      private connector : RestConnectorService,
      private http : HttpClient,
      private connectors : RestConnectorsService,
      private iam : RestIamService,
      private mdsApi : RestMdsService,
      private nodeApi : RestNodeService,
      private searchApi : RestSearchService,
      private usageApi : RestUsageService,
      private toolService: RestToolService,
      private componentFactoryResolver: ComponentFactoryResolver,
      private viewContainerRef: ViewContainerRef,
      private frame : FrameEventsService,
      private actionbarService : ActionbarHelperService,
      private toast : Toast,
      private cd: ChangeDetectorRef,
      private config : ConfigurationService,
      private storage : SessionStorageService,
      private route : ActivatedRoute,
      private networkService : RestNetworkService,
      private _ngZone: NgZone,
      private router : Router,
      private platformLocation : PlatformLocation,
      private optionsHelper : OptionsHelperService,
      private temporaryStorageService: TemporaryStorageService) {
      (window as any).nodeRenderComponentRef = {component: this, zone: _ngZone};
      (window as any).ngRender = {setDownloadUrl:(url:string)=> {this.setDownloadUrl(url)}};
      this.frame.addListener(this);

        Translation.initialize(translate,config,storage,route).subscribe(()=> {
        this.banner = ConfigurationHelper.getBanner(this.config);
        this.connector.setRoute(this.route);
        this.networkService.prepareCache();
        this.route.queryParams.subscribe((params:Params)=> {
          this.closeOnBack=params.closeOnBack === 'true';
          this.editor=params.editor;
          this.fromLogin=params.fromLogin === 'true' || params.redirectFromSSO === 'true';
          this.repository=params.repository ? params.repository : RestConstants.HOME_REPOSITORY;
          this.queryParams=params;
          const childobject = params.childobject_id ? params.childobject_id : null;
          this.isChildobject=childobject!=null;
          this.route.params.subscribe((params: Params) => {
            if(params.node) {
              this.isRoute=true;
              this.list = this.temporaryStorageService.get(TemporaryStorageService.NODE_RENDER_PARAMETER_LIST);
              this.connector.isLoggedIn(false).subscribe((data:LoginResult)=> {
                this.isSafe=data.currentScope==RestConstants.SAFE_SCOPE;
                if(params.version) {
                    this.version = params.version;
                }
                if(childobject) {
                    setTimeout(()=>this.node = childobject,10);
                }
                else {
                    setTimeout(()=>this.node = params.node,10);
                }
              });
            }
          });
        });

      });
      this.frame.broadcastEvent(FrameEventsService.EVENT_VIEW_OPENED,'node-render');
    }


  public isLoading=true;
  public isBuildingPage=false;
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
  private list: Node[];
  private isSafe=false;
  private isOpenable: boolean;
  private closeOnBack: boolean;
  public nodeMetadata: Node[];
  public nodeShare: Node[];
  public nodeShareLink: Node;
  public nodeWorkflow: Node[];
  public addNodesStream: Node[];
  public nodeDelete: Node[];
  public nodeVariant: Node;
  public addToCollection: Node[];
  public nodeReport: Node;
  private editor: string;
  private fromLogin = false;
  public banner: any;
  private repository: string;
  private downloadButton: OptionItem;
  private downloadUrl: string;
  currentOptions: OptionItem[];
  sequence: NodeList;
  sequenceParent: Node;
  canScrollLeft = false;
  canScrollRight = false;
  private queryParams: Params;
  public similarNodes: Node[];
  mds: Mds;
  isDestroyed = false;

  @ViewChild('sequencediv') sequencediv : ElementRef;
  @ViewChild('mainNav') mainNavRef : MainNavComponent;
  @ViewChild('actionbar') actionbar: ActionbarComponent;
  isChildobject = false;
    _node : Node;
    _nodeId : string;
    @Output() onClose=new EventEmitter();
    similarNodeColumns: ListItem[]=[];


    public static close(location:Location) {
        location.back();
    }

  @HostListener('window:beforeunload', ['$event'])
  beforeunloadHandler(event:any) {
    if(this.isSafe) {
      this.connector.logoutSync();
    }
  }
  @HostListener('window:resize', ['$event'])
  onResize(event:any) {
      this.setScrollparameters();
  }

  @HostListener('document:keydown', ['$event'])
  handleKeyboardEvent(event: KeyboardEvent) {
    if(this.nodeMetadata!=null) {
      return;
    }
    if(CardComponent.getNumberOfOpenCards() > 0){
        return;
    }
    if (event.code == 'ArrowLeft' && this.canSwitchBack()) {
      this.switchPosition(this.getPosition() - 1);
      event.preventDefault();
      event.stopPropagation();
      return;
    }
    if (event.code == 'ArrowRight' && this.canSwitchForward()) {
        this.switchPosition(this.getPosition() + 1);
      event.preventDefault();
      event.stopPropagation();
      return;
    }

  }
    close() {
      if(this.isRoute) {
        if(this.closeOnBack) {
          window.close();
        }
        else {
          if(this.fromLogin && !RouterComponent.isRedirectedFromLogin()) {
            UIHelper.goToDefaultLocation(this.router, this.platformLocation, this.config, false);
          }
          else {
            if(window.history.state?.scope === Scope.Search) {
                this.searchService.reinit = false;
            }
            NodeRenderComponent.close(this.location);
            // use a timeout to let the browser try to go back in history first
            setTimeout(()=> {
                if(!this.isDestroyed) {
                    this.mainNavRef.toggleMenuSidebar();
                }
            },250);
          }
        }
      }
      else
        this.onClose.emit();
    }


    showDetails() {
      const rect=document.getElementById('edusharing_rendering_metadata').getBoundingClientRect();
      if(window.scrollY<rect.top) {
          UIHelper.scrollSmooth(rect.top, 1.5);
      }
    }
    public getPosition() {
      if(!this._node || !this.list)
        return -1;
      let i=0;
      for(const node of this.list) {
        if(node.ref.id==this._node.ref.id || node.ref.id==this.sequenceParent.ref.id)
          return i;
        i++;
      }
      return -1;
    }
    onEvent(event: string, data: any): void {
        if(event==FrameEventsService.EVENT_REFRESH) {
            this.refresh();
        }
    }
    ngOnDestroy() {
        (window as any).ngRender = null;
        this.optionsHelper.setListener(null);
        this.isDestroyed = true;
    }

  public switchPosition(pos:number) {
    // this.router.navigate([UIConstants.ROUTER_PREFIX+"render",this.list[pos].ref.id]);
    this.isLoading=true;
    this.sequence = null;
    this.node=this.list[pos];
    // this.options=[];
  }
  public canSwitchBack() {
    return this.list && this.getPosition()>0 && !this.list[this.getPosition()-1].isDirectory;
  }
  public canSwitchForward() {
    return this.list && this.getPosition()<this.list.length-1 && !this.list[this.getPosition()+1].isDirectory;
  }
  public closeMetadata() {
    this.nodeMetadata=null;
  }
  public refresh() {
    if(this.isLoading) {
        return;
    }
    this.optionsHelper.clearComponents(this.mainNavRef, this.actionbar);
    this.isLoading=true;
    this.node=this._nodeId;
  }
  viewParent() {
      this.isChildobject=false;
      this.router.navigate([], {relativeTo: this.route, queryParamsHandling: 'merge', queryParams: {
              childobject_id: null
          },
          replaceUrl: true});
  }
  viewChildobject(node:Node,pos:number) {
        this.isChildobject=true;
        this.router.navigate([], {relativeTo: this.route, queryParamsHandling: 'merge', queryParams: {
                childobject_id: node.ref.id
            },
            replaceUrl: true});

  }
  private loadNode() {
    if(!this._node) {
        this.isBuildingPage = false;
        return;
    }

    const download=new OptionItem('OPTIONS.DOWNLOAD','cloud_download',()=>this.downloadCurrentNode());
    download.elementType = [ElementType.Node, ElementType.NodeChild, ElementType.NodePublishedCopy];
    // declare explicitly so that callback will be overriden
    download.customEnabledCallback = null;
    download.group = DefaultGroups.View;
    download.priority = 25;
    download.isEnabled=this._node.downloadUrl!=null &&  (
        !this._node.properties[RestConstants.CCM_PROP_IO_WWWURL] ||
        !RestNetworkService.isFromHomeRepo(this._node)
    );    download.showAsAction=true;
    if(this.isCollectionRef()) {
      this.nodeApi.getNodeMetadata(this._node.properties[RestConstants.CCM_PROP_IO_ORIGINAL]).subscribe((node) => {
        this.addDownloadButton(download);
      },(error:any)=> {
        if(error.status==RestConstants.HTTP_NOT_FOUND) {
          download.isEnabled = false;
        }
        this.addDownloadButton(download);
      });
      return;
    }
    this.addDownloadButton(download);
  }
  private loadRenderData() {
      this.isLoading=true;
      this.optionsHelper.clearComponents(this.mainNavRef, this.actionbar);
    if(this.isBuildingPage) {
        setTimeout(()=>this.loadRenderData(),50);
        return;
    }
    const parameters= {
      showDownloadButton:this.config.instant('rendering.showDownloadButton',false),
      showDownloadAdvice:!this.isOpenable
    };
    this._node=null;
    this.isBuildingPage=true;
      // we only fetching versions for the primary parent (child objects don't have versions)
      this.nodeApi.getNodeRenderSnippet(this._nodeId,this.version && !this.isChildobject ? this.version : '-1',parameters,this.repository)
        .subscribe((data:any)=> {
            if (!data.detailsSnippet) {
                console.error(data);
                this.toast.error(null,'RENDERSERVICE_API_ERROR');
            }
            else {
                this._node=data.node;
                this.isOpenable = this.connectors.connectorSupportsEdit(this._node) != null;
                const finish = (set:Mds = null) => {
                    this.similarNodeColumns = MdsHelper.getColumns(this.translate, set, 'search');
                    this.mds = set;

                    jQuery('#nodeRenderContent').html(data.detailsSnippet);
                    this.postprocessHtml();
                    this.addCollections();
                    this.addVideoControls();
                    this.linkSearchableWidgets();
                    this.addComments();
                    this.loadNode();
                    this.loadSimilarNodes();
                    this.isLoading = false;
                };
                this.getSequence(()=> {
                    this.mdsApi.getSet(this.getMdsId(), this.repository).subscribe((set) => {
                        finish(set);
                    },(error) => {
                        console.warn('mds fetch error', error);
                        finish();
                    });
                });
            }
            this.isLoading = false;
            GlobalContainerComponent.finishPreloading();
        },(error)=> {
            console.log(error.error.error);
            if(error?.error?.error === 'org.edu_sharing.restservices.DAOMissingException') {
                this.toast.error(null, 'TOAST.RENDER_NOT_FOUND', null, null, null, {
                    link: {
                        caption: 'BACK',
                        callback: () => this.close()
                    }
                })
            } else {
                this.toast.error(error);
            }
            this.isLoading = false;
            GlobalContainerComponent.finishPreloading();
        })
  }
    onDelete(event:any) {
        if(event.error)
            return;
        this.close();
    }
    addVideoControls() {
        let videoElement: HTMLVideoElement;
        let target: Element;
        if(!this.isCollectionRef()) {
            return;
        }
        try {
            videoElement = document.querySelector('.edusharing_rendering_content_wrapper video');
            if(!videoElement){
                throw new Error();
            }
            const listener = () => {
                this.tracking.trackEvent(EventType.VIEW_MATERIAL_PLAY_MEDIA, this._node.ref.id).subscribe(() => {})
                videoElement.removeEventListener('play', listener);
            };
            videoElement.addEventListener('play', listener);
            target = document.createElement('div');
            videoElement.parentElement.appendChild(target);
        } catch (e) {
            // console.log("did not find video element, skipping controls",e);
            setTimeout(()=>this.addVideoControls(),1000/30);
            return;
        }
        const data= {
            video:videoElement,
            node:this._node
        }
        UIHelper.injectAngularComponent(this.componentFactoryResolver,this.viewContainerRef,VideoControlsComponent,target,data);
    }

    addCollections() {
        let domContainer:Element;
        let domCollections:Element;
        try {
            domContainer = document.getElementsByClassName('node_collections_render')[0].parentElement;
            domCollections = document.getElementsByTagName('collections')[0];
        } catch(e) {
            return;
        }
        UIHelper.injectAngularComponent(this.componentFactoryResolver,this.viewContainerRef,SpinnerComponent,domCollections);
        this.usageApi.getNodeUsagesCollection(this.isCollectionRef() ? this._node.properties[RestConstants.CCM_PROP_IO_ORIGINAL] : this._node.ref.id,this._node.ref.repo).subscribe((usages)=> {
            // @TODO: This does currently ignore the "hideIfEmpty" flag of the mds template
            if(usages.length==0) {
                domContainer.parentElement.removeChild(domContainer);
                return;
            }
            const data= {
                nodes:usages.map((u)=>u.collection),
                columns:ListItem.getCollectionDefaults(),
                isClickable:true,
                clickRow:(event: {node: Node})=> {
                    UIHelper.goToCollection(this.router,event.node);
                },
                doubleClickRow:(event: Node)=> {
                    UIHelper.goToCollection(this.router,event);
                },
                viewType:ListTableComponent.VIEW_TYPE_GRID_SMALL,
            };
            UIHelper.injectAngularComponent(this.componentFactoryResolver,this.viewContainerRef,ListTableComponent,document.getElementsByTagName('collections')[0],data, { delay: 250 });
        },(error)=> {
            domContainer.parentElement.removeChild(domContainer);
        });
    }
  private addComments() {
      const data= {
          node:this._node
      };
      UIHelper.injectAngularComponent(this.componentFactoryResolver,this.viewContainerRef,CommentsListComponent,document.getElementsByTagName('comments')[0],data);
  }
  private postprocessHtml() {
    if(!this.config.instant('rendering.showPreview',true)) {
      jQuery('.edusharing_rendering_content_wrapper').hide();
      jQuery('.showDetails').hide();
    }
    if(this.isOpenable) {
      jQuery('#edusharing_downloadadvice').hide();
    }
      const element=jQuery('#edusharing_rendering_content_href');
      element.click((event:any)=> {
          if(this.connector.getBridgeService().isRunningCordova()) {
              const href=element.attr('href');
              this.connector.getBridgeService().getCordova().openBrowser(href);
              event.preventDefault();
          }
      });
  }
  private downloadSequence() {
      const nodes = [this.sequenceParent].concat(this.sequence.nodes);
      this.nodeHelper.downloadNodes(nodes, this.sequenceParent.name+'.zip');
  }

  private downloadCurrentNode() {
      if(this.downloadUrl) {
          this.nodeHelper.downloadUrl(this.downloadUrl);
      } else {
          this.nodeHelper.downloadNode(this._node, this.isChildobject ? null : this.version);
      }
  }

  private openConnector(node: Node, newWindow=true) {
    if(RestToolService.isLtiObject(node)){
      this.toolService.openLtiObject(node);
    }
    else {
      UIHelper.openConnector(this.connectors,this.iam,this.frame,this.toast, node,null,null,null,newWindow);
    }
  }

    private async initOptions() {
        this.optionsHelper.setData({
            scope: Scope.Render,
            activeObjects: [this._node],
            parent: new Node(this._node.parent.id),
            allObjects: this.list,
            customOptions: {
                useDefaultOptions: true,
                addOptions: this.currentOptions
            },
            postPrepareOptions: (options, objects) => {
                if(this.version && this.version !== RestConstants.NODE_VERSION_CURRENT) {
                    options.filter(o => o.name === 'OPTIONS.OPEN')[0].isEnabled = false;
                }
            }
        });
        await this.optionsHelper.initComponents(this.mainNavRef, this.actionbar);
        this.optionsHelper.setListener({
            onRefresh: () => this.refresh(),
            onDelete: (result) => this.onDelete(result),
        });
        this.optionsHelper.refreshComponents();
        this.postprocessHtml();
        this.isBuildingPage = false;
    }

  private isCollectionRef() {
    return this._node.aspects.indexOf(RestConstants.CCM_ASPECT_IO_REFERENCE)!=-1;
  }

  private addDownloadButton(download: OptionItem) {
      this.nodeApi.getNodeChildobjects(this.sequenceParent.ref.id,this.sequenceParent.ref.repo).subscribe((data:NodeList)=> {
          this.downloadButton=download;
          const options: OptionItem[] = [];
          options.splice(0,0,download);
          if(data.nodes.length > 0 || this._node.aspects.indexOf(RestConstants.CCM_ASPECT_IO_CHILDOBJECT) != -1) {
              const downloadAll = new OptionItem('OPTIONS.DOWNLOAD_ALL','archive',()=> {
                  this.downloadSequence();
              });
              downloadAll.elementType = [ElementType.Node, ElementType.NodeChild, ElementType.NodePublishedCopy];
              downloadAll.group = DefaultGroups.View;
              downloadAll.priority = 35;
              options.splice(1,0,downloadAll);
          }
          this.currentOptions = options;
          this.initOptions();
    });
  }
  setDownloadUrl(url:string) {
      if(this.downloadButton!=null)
        this.downloadButton.isEnabled=url!=null;
      this.downloadUrl=url;
      this.initOptions();
  }

    private getSequence(onFinish: () => void) {
        if(this._node.aspects.indexOf(RestConstants.CCM_ASPECT_IO_CHILDOBJECT) != -1) {
           this.nodeApi.getNodeMetadata(this._node.parent.id).subscribe(data => {
             this.sequenceParent = data.node;
               this.nodeApi.getNodeChildobjects(this.sequenceParent.ref.id,this.sequenceParent.ref.repo).subscribe((data:NodeList)=> {
                   if(data.nodes.length > 0) {
                       this.sequence = data;
                   } else {
                       this.sequence = null;
                   }
                   setTimeout(()=>this.setScrollparameters(),100);
                   onFinish();
               });
            });
        } else {
            this.sequenceParent = this._node;
            this.nodeApi.getNodeChildobjects(this.sequenceParent.ref.id,this.sequenceParent.ref.repo).subscribe((data:NodeList)=> {
                if(data.nodes.length > 0) {
                    this.sequence = data;
                } else {
                    this.sequence = null;
                }
                setTimeout(()=>this.setScrollparameters(),100);
                onFinish();
            }, error => {
                    console.error('failed sequence fetching');
                    console.error(error);
                    onFinish();
            });
        }
    }

    scroll(direction: string) {
        const element = this.sequencediv.nativeElement;
        const width=window.innerWidth/2;
        UIHelper.scrollSmoothElement(element.scrollLeft + (direction=='left' ? -width : width),element,2,'x').then((limit)=> {
            this.setScrollparameters();
        });
    }

    private setScrollparameters() {
      if(!this.sequence)
        return;
      const element = this.sequencediv.nativeElement;
      if(element.scrollLeft <= 20) {
          this.canScrollLeft = false;
      } else {
          this.canScrollLeft = true;
      }
      if((element.scrollLeft + 20) >= (element.scrollWidth - window.innerWidth)) {
          this.canScrollRight = false;
      } else {
          this.canScrollRight = true;
      }
    }
    private getNodeName(node:Node) {
      return RestHelper.getName(node);
    }
    getName(): string {
      if (this._node) {
        return this.getNodeName(this._node);
      } else {
        return '';
      }
    }
    getNodeTitle(node:Node) {
        return RestHelper.getTitle(node);
    }

    public switchNode(event : any) {
        UIHelper.scrollSmooth();
        this.node = event.node;
    }

    private loadSimilarNodes() {
        this.searchApi.searchFingerprint(this._nodeId).subscribe( (data: NodeList) => {
            this.similarNodes = data.nodes;
        });
    }

    private linkSearchableWidgets() {
        try {
            this.mds.widgets.filter((w: any) => w.isSearchable).forEach((w: any) => {
                try {
                    const values = document.querySelectorAll('#edusharing_rendering_metadata [data-widget-id=\'' + w.id + '\'] .mdsWidgetMultivalue .mdsValue');
                    values.forEach((v: HTMLElement) => {
                        v.classList.add('clickable', 'mdsValueClickable');
                        v.tabIndex = 0;
                        const key = v.getAttribute('data-value-key');
                        v.onclick = () => {
                            this.navigateToSearch(w.id, key);
                        }
                        v.onkeyup = (k) => {
                            if (k.key === 'Enter') {
                                this.navigateToSearch(w.id, key);
                            }
                        }
                    });
                } catch (e) {
                }
            });
            // document.getElementsByClassName("edusharing_rendering_content_wrapper")[0].ge;
        } catch (e) {
            console.warn('Could not read the widget list from the metadataset', e);
        }
    }

    private navigateToSearch(id: any, value: string) {
        UIHelper.getCommonParameters(this.route).subscribe((params) => {
            const data: any = {};
            data[id] = [value];
            params.mds = this.getMdsId();
            params.sidenav = true;
            params.repository = this.repository;
            params.parameters = JSON.stringify(data);
            this.router.navigate([UIConstants.ROUTER_PREFIX + 'search'], {queryParams: params});
        });
    }

    private getMdsId() {
        return this._node.metadataset ? this._node.metadataset : RestConstants.DEFAULT;
    }
}
