
import {Component, ViewChild, HostListener, ElementRef} from '@angular/core';
import 'rxjs/add/operator/map';
import { SearchService } from './search.service';
import {HttpModule, Http} from '@angular/http';
import { WindowRefService } from './window-ref.service';
import { Subscription } from 'rxjs/Subscription';
import { SuggestItem} from '../../common/ui/autocomplete/autocomplete.component';
import {Router, ActivatedRoute} from '@angular/router';
import {TranslateService} from "ng2-translate";
import {Translation} from "../../common/translation";
import {RestSearchService} from '../../common/rest/services/rest-search.service';
import {RestMetadataService} from '../../common/rest/services/rest-metadata.service';
import {RestNodeService} from '../../common/rest/services/rest-node.service';
import {RestConstants} from '../../common/rest/rest-constants';
import {RestConnectorService} from "../../common/rest/services/rest-connector.service";
import {
  Node, NodeList, LoginResult, NetworkRepositories, Repository, NodeWrapper,
  MdsMetadatasets, MdsInfo
} from "../../common/rest/data-object";
import {ListTableComponent} from "../../common/ui/list-table/list-table.component";
import {RouterComponent} from "../../router/router.component";
import {ActionbarComponent, OptionItem} from "../../common/ui/actionbar/actionbar.component";
import {TemporaryStorageService} from "../../common/services/temporary-storage.service";
import {NodeRenderComponent} from "../../common/ui/node-render/node-render.component";
import {Helper} from "../../common/helper";
import {UIHelper} from "../../common/ui/ui-helper";
import {Title} from "@angular/platform-browser";
import {ConfigurationService} from "../../common/services/configuration.service";
import {Toast} from "../../common/ui/toast";
import {SessionStorageService} from "../../common/services/session-storage.service";
import {RestNetworkService} from "../../common/rest/services/rest-network.service";
import {WorkspaceMainComponent} from "../workspace/workspace.component";
import {UIAnimation} from "../../common/ui/ui-animation";
import {trigger} from "@angular/animations";
import {NodeHelper} from "../../common/ui/node-helper";
import {RestCollectionService} from "../../common/rest/services/rest-collection.service";
import {RestMdsService} from "../../common/rest/services/rest-mds.service";
import {RestHelper} from "../../common/rest/rest-helper";
import {RestIamService} from "../../common/rest/services/rest-iam.service";
import {SearchNodeStoreComponent} from "./node-store/node-store.component";
import {UIConstants} from "../../common/ui/ui-constants";
import {ListItem} from "../../common/ui/list-item";
import {MdsComponent} from "../../common/ui/mds/mds.component";
import {RequestObject} from "../../common/rest/request-object";
import {DialogButton} from "../../common/ui/modal-dialog/modal-dialog.component";



@Component({
  selector: 'app-search',
  templateUrl: 'search.component.html',
  styleUrls: ['search.component.scss'],
  providers: [HttpModule, WindowRefService, RestMetadataService],
  animations: [
    trigger('fromLeft', UIAnimation.fromLeft()),
  ]
})



export class SearchComponent {
  public initalized = false;
  @ViewChild('mds') mdsRef: MdsComponent;
  public mdsSuggestions:any={}
  public mdsExtended=false;
  public sidenavTab=0;
  public resultCount:any={};
  sidenav_opened: boolean = true;
  public resultsRight=false;
  public collectionsMore=false;
  view = ListTableComponent.VIEW_TYPE_GRID;
  main_view:string = 'view_search'; // view_search | view_collection
  searchFail: boolean = false;
  showspinner: boolean = false;
  public nodeReport: Node;
  public currentRepository:string=RestConstants.HOME_REPOSITORY;

  public applyMode=false;
  public columns : ListItem[]=[];
  public collectionsColumns : ListItem[]=[];
  public hasCheckbox=false;
  public showMoreRepositories=false;
  innerWidth: number = 0;
  breakpoint: number = 800;
  invalidateNodeStore: Boolean;

  @ViewChild('toolbar') toolbar: any;

  //@ViewChildren(AutocompleteComponent) autocompletes: QueryList<AutocompleteComponent>;
  //autocompletesArray: Array<any> = [];

  autocompleteSuggestions:any = [];
  public options : OptionItem[]=[];
  public savedSearchOptions : OptionItem[]=[];
  private render_options: OptionItem[]=[];
  private renderedNode: Node;
  public isGuest = false;
  public mainnav = true;
  public hasMoreCollections=false;
  public queryId=RestConstants.DEFAULT_QUERY_NAME;
  private viewToggle: OptionItem;
  public actionOptions:OptionItem[]=[];
  public repositories: Repository[];
  public globalProgress = false;
  // Max items to fetch at all (afterwards no more infinite scroll)
  private static MAX_ITEMS_COUNT = 250;
  private repositoryIds: any[];
  public addNodesToCollection: Node[];
  private mdsSets: MdsInfo[];
  public mdsId: string;
  public selection: Node[];
  private currentValues: any;
  private reloadMds: Boolean;
  private currentMdsSet: any;
  private sidenavSet=false;
  public extendedRepositorySelected = false;
  public banner: any;
  public savedSearch : Node[]=[];
  public savedSearchColumns : ListItem[]=[];
  private mdsActions: OptionItem[];
  public saveSearchDialog = false;
  private currentSavedSearch: Node;
  private login: LoginResult;
  public dialogTitle: string;
  private dialogMessage: string;
  private dialogButtons: DialogButton[];
  private savedSearchOwn = true;
  public savedSearchLoading = false;
  public savedSearchQuery:string = null;
  public savedSearchQueryModel:string = null;

  @HostListener('window:scroll', ['$event'])
  handleScroll(event: Event) {
    this.searchService.offset = (window.pageYOffset || document.documentElement.scrollTop);
  }

  private queryParamsSubscription: Subscription;
  private nodeDisplayed : Node;


  constructor(
    private router : Router,
    private http : Http,
    private connector:RestConnectorService,
    private RestNodeService: RestNodeService,
    private mds:RestMdsService,
    private iam:RestIamService,
    private search: RestSearchService,
    private collectionApi : RestCollectionService,
    private nodeApi: RestNodeService,
    private toast : Toast,
    private translate : TranslateService,
    private activatedRoute: ActivatedRoute,
    private winRef:WindowRefService,
    public searchService:SearchService,
    private title:Title,
    private config:ConfigurationService,
    private storage : SessionStorageService,
    private network : RestNetworkService,
    private temporaryStorageService: TemporaryStorageService
  ) {
    this.savedSearchColumns.push(new ListItem("NODE",RestConstants.CM_PROP_TITLE));
    this.connector.setRoute(this.activatedRoute).subscribe(()=> {
      Translation.initialize(translate, this.config, this.storage, this.activatedRoute).subscribe(() => {
        UIHelper.setTitle('SEARCH.TITLE', title, translate, config);
        this.initalized = true;
        this.banner = this.config.instant('bannerSearch');
        this.printListener();
        this.view = this.config.instant('searchViewType',temporaryStorageService.get('view', '1'));
        this.setViewType(this.view);

        this.collectionsColumns.push(new ListItem("NODE", RestConstants.CM_NAME));
        this.collectionsColumns.push(new ListItem("COLLECTION", 'info'));
        this.collectionsColumns.push(new ListItem("COLLECTION",'scope'));
        this.updateActionbar(null);
        setInterval(() => this.updateHasMore(), 1000);

        this.network.getRepositories().subscribe((data: NetworkRepositories) => {
          this.repositories = data.repositories;
          if (this.repositories.length < 2) {
            this.repositories = null;
            this.repositoryIds = [RestConstants.HOME_REPOSITORY];
            return;
          }
          let all = new Repository();
          all.id = RestConstants.ALL;
          all.title = this.translate.instant('SEARCH.REPOSITORY_ALL');
          all.repositoryType = 'ALL';
          this.repositories.splice(0, 0, all);
          this.updateRepositoryOrder();
        }, (error: any) => {
          this.repositories = null;
          this.repositoryIds = [];
        });
      });
    });
  }
  public setRepository(repository:string){
    this.routeSearch(this.searchService.searchTerm,repository,null,null);
    //this.currentRepository=repository;
    //this.getSearch(null,true);
  }

  applyParameters(props:any){
    this.currentValues=props;
    console.log(this.searchService.searchTerm);
    console.log(this.currentValues);
    this.routeSearchParameters(props);
    //this.getSearch(null,true,props);
  }
  downloadNode() {
    window.open(this.renderedNode.downloadUrl);
  }
  updateSelection(selection:Node[]){
    this.selection=selection;
    this.updateActionbar(selection);
  }
  ngOnInit() {
    Translation.initialize(this.translate,this.config,this.storage,this.activatedRoute).subscribe(()=>{
      this.queryParamsSubscription = this.activatedRoute.queryParams.subscribe(
        (param: any) => {
          console.log("query params");
          this.mainnav=param['mainnav']=='false' ? false : true;
          if(param['reurl']) {
            this.searchService.reurl = param['reurl'];
            this.applyMode=true;
          }

          if(param['query'])
            this.searchService.searchTerm=param['query'];
          if(param['repository']){
            this.mdsSets=null;
            if(this.currentRepository!=param['repository'])
              this.mdsId=RestConstants.DEFAULT;
            this.currentRepository=param['repository'];
            this.currentValues=null;
            this.updateRepositoryOrder();
          }
          if(param['savedQuery']){
            this.nodeApi.getNodeMetadata(param['savedQuery'],[RestConstants.ALL]).subscribe((data:NodeWrapper)=>{
              this.currentSavedSearch=data.node;
            });
          }
          this.updateSelection([]);
          this.mds.getSets(this.currentRepository).subscribe((data:MdsMetadatasets)=>{
            console.log(data.metadatasets);
            this.mdsSets=RestHelper.filterValidMds(data.metadatasets,this.config);
            if(this.mdsSets){
              RestHelper.prepareMetadatasets(this.translate,this.mdsSets);
              console.log(this.mdsSets);
              this.mdsId = this.mdsSets[0].id;
              if(param['mds'])
                this.mdsId=param['mds'];
              this.searchService.init();
              this.prepare(param);
            }
          },(error:any)=>{
            this.mdsId=RestConstants.DEFAULT;
            this.searchService.init();
            this.prepare(param);
          });
        });
    });
  }

  private addDefaultNodeOptions(data: LoginResult) {
    let nodeStore = new OptionItem("SEARCH.ADD_NODE_STORE", "bookmark_border", (node: Node) => {
      this.addToStore([node]);
    });
    this.options.push(nodeStore);
    let save = new OptionItem("SAVE", "reply", (node: Node) => this.importNode(node));
    save.showCallback = ((node: Node) => {
      return RestNetworkService.supportsImport(node.ref.repo, this.repositories) && !this.isGuest;
    });
    this.options.push(save);
    if (!this.isGuest && this.currentRepository==RestConstants.HOME_REPOSITORY) {
      let collection = new OptionItem("WORKSPACE.OPTION.COLLECTION", "layers", (node: Node) => {
        this.addNodesToCollection = [node];
      });
      collection.enabledCallback = (node: Node) => {
        return node.access.indexOf(RestConstants.ACCESS_CC_PUBLISH) != -1;
      }
      this.options.push(collection);
      let openFolder = new OptionItem('SHOW_IN_FOLDER', 'folder', (node: Node) => {
        NodeHelper.goToWorkspace(this.nodeApi,this.router, data, node);
      });
      openFolder.enabledCallback = (node: Node) => {
        return node.access.indexOf(RestConstants.ACCESS_WRITE) != -1;
      };
      this.options.push(openFolder);
    }
    let download = new OptionItem("DOWNLOAD", "cloud_download", (node: Node) => {
      NodeHelper.downloadNode(node);
    });
    download.enabledCallback = (node: Node) => {
      return node.downloadUrl != null;
    }
    this.options.push(download);

    if(this.config.instant("nodeReport",false)){
      let report = new OptionItem("NODE_REPORT.OPTION", "flag", (node: Node) => this.nodeReport=this.getCurrentNode(node));
      this.options.push(report);
    }
    let custom = this.config.instant("searchNodeOptions");
  }

  /*AUTOCOMPLETE*/

  addAutocompleteValue(data: any) {
    if(typeof this.searchService.autocompleteData[data.id] == 'undefined')
      this.searchService.autocompleteData[data.id] = [];
    this.searchService.autocompleteData[data.id].push(data.item);
    this.refresh();
  }

  removeAutoCompleteValue(data: any) {
    for(var i = 0; i < this.searchService.autocompleteData[data.id].length; i++) {
      if(this.searchService.autocompleteData[data.id][i].id == data.item.id) {
        this.searchService.autocompleteData[data.id].splice(i, 1);
        this.refresh();
        return;
      }
    }

  }
  public refresh(){
    this.getSearch(null,true);
  }

  /* implement this for your autocomplete element */
  /*
      getAutocompleteSuggestions(data: any) {
          if(data.id == 'keyword') {
                  this.RestMetadataService.getMetadataValues('-default-', this.queryId, '{http://www.campuscontent.de/model/lom/1.0}general_keyword', data.input).subscribe(
                  md => {
                      var ret:SuggestItem[] = [];
                      for (var index = 0; index < md.values.length; index++) {
                          ret.push(new SuggestItem(md.values[index]["displayString"], md.values[index]["displayString"], '', '', ''));
                      }
                      this.autocompleteSuggestions['keyword'] = ret;
                  },
                  error => console.log(error));
          }

          if(data.id == 'educationallearningresourcetype') {
                  this.RestMetadataService.getMetadataValues('-default-', this.queryId, '{http://www.campuscontent.de/model/1.0}educationallearningresourcetype', data.input).subscribe(
                  md => {
                      var ret:SuggestItem[] = [];
                      for (var index = 0; index < md.values.length; index++) {
                          ret.push(new SuggestItem(md.values[index]["displayString"], md.values[index]["displayString"], '', '', ''));
                      }
                      this.autocompleteSuggestions['educationallearningresourcetype'] = ret;
                  },
                  error => console.log(error));
          }

        if(data.id == 'taxonid') {
          this.RestMetadataService.getMetadataValues('-default-', this.queryId, '{http://www.campuscontent.de/model/1.0}taxonid', data.input).subscribe(
            md => {
              var ret:SuggestItem[] = [];
              for (var index = 0; index < md.values.length; index++) {
                ret.push(new SuggestItem(md.values[index]["displayString"], md.values[index]["displayString"], '', '', md.values[index]["key"]));
              }
              this.autocompleteSuggestions['taxonid'] = ret;
            },
            error => console.log(error));
        }

        if(data.id == 'educationalcontext') {
          this.RestMetadataService.getMetadataValues('-default-', this.queryId, '{http://www.campuscontent.de/model/1.0}educationalcontext', data.input).subscribe(
            md => {
              var ret:SuggestItem[] = [];
              for (var index = 0; index < md.values.length; index++) {
                ret.push(new SuggestItem(md.values[index]["displayString"], md.values[index]["displayString"], '', '', ''));
              }
              this.autocompleteSuggestions['educationalcontext'] = ret;
            },
            error => console.log(error));
        }
      }
  */
  /*AUTOCOMPLETE*/




  ngOnDestroy() {
    this.queryParamsSubscription.unsubscribe();
  }


  handleFocus(event: Event) {
    if(this.innerWidth < this.breakpoint) {
      this.winRef.getNativeWindow().scrollTo(0,0);
    }
  }

  ngAfterViewInit() {

    this.winRef.getNativeWindow().scrollTo(0, this.searchService.offset);
    this.innerWidth = this.winRef.getNativeWindow().innerWidth;
    this.setSidenavSettings();
    //this.autocompletesArray = this.autocompletes.toArray();
  }

  setFilterIndicator(status: string) {
    if(status == 'opened') {
      this.resultsRight=false;
    }
    else {
      this.resultsRight=true;
    }
  }



  getMoreResults() {
    if(this.searchService.complete == false) {
      //console.log("getMoreResults() "+this.searchService.skipcount+" "+this.searchService.searchResult.length);
      this.searchService.skipcount += this.connector.numberPerRequest;
      this.getSearch();
    }
  }

  onResize() {
    this.innerWidth = this.winRef.getNativeWindow().innerWidth;
    this.setSidenavSettings();
  }


  setSidenavSettings() {
    if(this.sidenavSet)
      return;
    this.sidenavSet=true;
    if(this.innerWidth < this.breakpoint) {
      this.sidenav_opened = false;
      this.resultsRight=true;
    } else {
      this.sidenav_opened = true;
      this.resultsRight=false;
    }
  }
  public routeSearchParameters(parameters:any){
    console.log(JSON.stringify(parameters));
    this.routeSearch(this.searchService.searchTerm,this.currentRepository,this.mdsId,parameters);
  }
  public routeSearch(query:string,repository=this.currentRepository,mds=this.mdsId,parameters:any=this.currentValues){
    this.router.navigate([UIConstants.ROUTER_PREFIX+"search"],{queryParams:{query:query,parameters:parameters ? JSON.stringify(parameters) : null,mds:mds,repository:repository,mdsExtended:this.mdsExtended,reurl:this.searchService.reurl}});
  }
  getSearch(searchString:string = null, init = false,properties:any=this.currentValues) {
    if(this.showspinner && init || this.repositoryIds==null){
      setTimeout(()=>this.getSearch(searchString,init,properties),100);
      return;
    }
    if(this.showspinner && !init){
      return;
    }
    this.showspinner = true;
    if(searchString==null)
      searchString = this.searchService.searchTerm;
    if(searchString==null)
      searchString='';
    this.searchService.searchTerm = searchString;
    if(init) {
      this.searchService.init();
    }
    else if(this.searchService.searchResult.length>SearchComponent.MAX_ITEMS_COUNT){
      this.showspinner=false;
      return;
    }

    /*
    if(typeof this.searchService.autocompleteData['keyword'] != 'undefined') {
        let values:string[]=[];
        for(let i = 0; i<this.searchService.autocompleteData['keyword'].length; i++) {
            values.push(this.searchService.autocompleteData['keyword'][i].title);
        }
        criterias.push({'property': '{http://www.campuscontent.de/model/lom/1.0}general_keyword', 'values': values});
    }

    if(typeof this.searchService.autocompleteData['educationallearningresourcetype'] != 'undefined') {
        let values:string[]=[];
        for(let i = 0; i<this.searchService.autocompleteData['educationallearningresourcetype'].length; i++) {
            values.push(this.searchService.autocompleteData['educationallearningresourcetype'][i].title);
        }
        criterias.push({'property': '{http://www.campuscontent.de/model/1.0}educationallearningresourcetype', 'values': values});
    }

    if(typeof this.searchService.autocompleteData['taxonid'] != 'undefined') {
      let values:string[]=[];
      for(let i = 0; i<this.searchService.autocompleteData['taxonid'].length; i++) {
        values.push(this.searchService.autocompleteData['taxonid'][i].key);
      }
      criterias.push({'property': '{http://www.campuscontent.de/model/1.0}taxonid', 'values': values});
    }

    if(typeof this.searchService.autocompleteData['educationalcontext'] != 'undefined') {
      let values:string[]=[];
      for(let i = 0; i<this.searchService.autocompleteData['educationalcontext'].length; i++) {
        values.push(this.searchService.autocompleteData['educationalcontext'][i].title);
      }
      criterias.push({'property': '{http://www.campuscontent.de/model/1.0}educationalcontext', 'values': values});
    }
    */
    let criterias:any[] = this.getCriterias(properties,searchString);


    let repos=(this.currentRepository==RestConstants.ALL ? this.repositoryIds : [{id:this.currentRepository,enabled:true}]);
    this.searchRepository(repos,criterias,init);

    if(init) {
      this.searchService.searchResultCollections = [];
      if(this.currentRepository==RestConstants.HOME_REPOSITORY || this.currentRepository==RestConstants.ALL) {
        this.search.search(criterias, [], {
          sortBy: [
            RestConstants.CCM_PROP_COLLECTION_PINNED_STATUS,
            RestConstants.CCM_PROP_COLLECTION_PINNED_ORDER,
            RestConstants.CM_MODIFIED_DATE
          ],
          sortAscending: [false,true,false]
        }, RestConstants.CONTENT_TYPE_COLLECTIONS,RestConstants.HOME_REPOSITORY,this.mdsId).subscribe(
          (data: NodeList) => {
            this.searchService.searchResultCollections = data.nodes
            this.resultCount.collections = data.pagination.total;
            this.checkFail();
          },
          (error: any) => {
            this.toast.error(error);
          }
        );
      }
    }
  }

  render(node: Node) {
    if(node.collection){
      this.switchToCollections(node.ref.id);
      return;
    }
    if(!RestNetworkService.isFromHomeRepo(node)){
      window.open(node.contentUrl);
      return;
    }
    this.renderedNode = node;
    this.render_options=[];
    this.temporaryStorageService.set(TemporaryStorageService.NODE_RENDER_PARAMETER_OPTIONS, this.render_options);
    this.temporaryStorageService.set(TemporaryStorageService.NODE_RENDER_PARAMETER_LIST, this.searchService.searchResult);
    this.router.navigate([UIConstants.ROUTER_PREFIX+"render", node.ref.id]);
  }
  switchToCollections(id=""){
    this.router.navigate([UIConstants.ROUTER_PREFIX+"collections"],{queryParams:{mainnav:this.mainnav,id:id}});
  }
  setViewType(type:number){
    this.view = type;
    this.temporaryStorageService.set('view', type);
    if(this.viewToggle)
      this.viewToggle.icon=type==ListTableComponent.VIEW_TYPE_GRID ? 'list' : 'view_module';
  }
  toggleView() {
    if(this.view == ListTableComponent.VIEW_TYPE_LIST) {
      this.setViewType(ListTableComponent.VIEW_TYPE_GRID);
    } else {
      this.setViewType(ListTableComponent.VIEW_TYPE_LIST);
    }
  }

  processSearchResult(data: NodeList,init:boolean) {
    this.searchFail = false;
    this.searchService.searchResult = this.searchService.searchResult.concat(data.nodes);
    this.checkFail();
    this.updateActionbar(this.selection);
    if(this.searchService.searchResult.length < 1 && this.currentRepository!=RestConstants.ALL){
      this.showspinner = false;
      this.searchService.init();
      this.searchService.complete = true;
      return;
    }
    this.searchService.numberofresults = data.pagination.count;
    if(init) {
      this.searchService.facettes = data.facettes;
      this.mdsSuggestions = {};
      if (data.facettes) {
        for (let facette of data.facettes) {
          facette.values = facette.values.slice(0, 5);
          this.mdsSuggestions[facette.property] = [];
          for (let value of facette.values) {
            this.mdsSuggestions[facette.property].push({id: value.value, caption: value.value});
          }
        }
      }
      if (this.searchService.facettes && this.searchService.facettes[0]) {
        if (this.searchService.autocompleteData.keyword && this.searchService.facettes[0].values) {
          for (let i = 0; i < this.searchService.autocompleteData.keyword.length; i++) {
            let index = Helper.indexOfObjectArray(this.searchService.facettes[0].values, 'value', this.searchService.autocompleteData.keyword[i].title);
            if (index > -1)
              this.searchService.facettes[0].values.splice(index, 1);
          }
        }
        this.searchService.facettes[0].values = this.searchService.facettes[0].values.slice(0, 20);
      }
    }
    if(data.nodes.length < this.connector.numberPerRequest)
      this.searchService.complete = true;
  }
  private updateHasMore() {
    try {
      this.hasMoreCollections = document.getElementById("collections").scrollHeight > 90 + 40;
    }catch(e){}
  }
  public updateMds(){
    this.currentValues=null;
    this.routeSearch(this.searchService.searchTerm);
  }

  private checkFail() {
    this.searchFail=this.searchService.searchResult.length<1 && this.searchService.searchResultCollections.length<1;
  }

  private updateColumns() {
    /*
    this.config.get("searchColumns").subscribe((data:any)=>{
      this.columns=[];
      if(data && data.length){
        for(let item of data){
          this.columns.push(new ListItem("NODE",item));
        }
      }
      else{
        this.columns.push(new ListItem("NODE",RestConstants.CM_PROP_TITLE));
        this.columns.push(new ListItem("NODE",RestConstants.CM_MODIFIED_DATE));
        this.columns.push(new ListItem("NODE",RestConstants.CCM_PROP_LICENSE));
        this.columns.push(new ListItem("NODE",RestConstants.CCM_PROP_REPLICATIONSOURCE));
      }
    });
    */
    this.columns=RestHelper.getColumns(this.currentMdsSet,'search');

    console.warn("mds set is missing list 'search' for column rendering");
  }

  private importNode(node: Node) {
    this.globalProgress=true;
    this.nodeApi.importNode(node.ref.repo,node.ref.id,RestConstants.INBOX).subscribe((data:NodeWrapper)=>{
      this.globalProgress=false;
      this.toast.toast('SEARCH.NODE_IMPORTED',{link:this.getWorkspaceUrl(data.node)});
    },(error:any)=>{
      this.toast.error(error);
      this.globalProgress=false;
    });
  }

  private getWorkspaceUrl(node: Node) {
    return UIConstants.ROUTER_PREFIX+"workspace/files?root=MY_FILES&id="+node.parent.id+"&file="+node.ref.id;
  }

  private updateActionbar(nodes:Node[]) {
    this.actionOptions=[];
    if(nodes && nodes.length) {
      let nodeStore = new OptionItem("SEARCH.ADD_NODE_STORE", "bookmark_border", (node: Node) => {
        this.addToStore(this.selection);
      });
      if(this.currentRepository==RestConstants.HOME_REPOSITORY)
        this.actionOptions.push(nodeStore);

      if(nodes.length==1){
        if(RestNetworkService.supportsImport(nodes[0].ref.repo, this.repositories) && !this.isGuest) {
          let save = new OptionItem("SAVE", "reply", (node: Node) => this.importNode(this.getCurrentNode(node)));
          this.actionOptions.push(save);
        }
      }
      let collection = new OptionItem("WORKSPACE.OPTION.COLLECTION", "layers", (node: Node) => {
        this.addNodesToCollection = nodes;
      });
      collection.isEnabled = NodeHelper.getNodesRight(nodes, RestConstants.ACCESS_CC_PUBLISH);
      collection.isSeperate = true;
      if(!this.isGuest && this.currentRepository==RestConstants.HOME_REPOSITORY)
        this.actionOptions.push(collection);
      if(nodes.length==1 && this.config.instant("nodeReport",false)){
        let report = new OptionItem("NODE_REPORT.OPTION", "flag", (node: Node) => this.nodeReport=this.getCurrentNode(node));
        this.actionOptions.push(report);
      }
    }
    let custom=this.config.instant("searchNodeOptions");
    NodeHelper.applyCustomNodeOptions(this.toast,this.http,this.connector,custom,this.searchService.searchResult, nodes, this.actionOptions,(load:boolean)=>this.globalProgress=load);
    this.viewToggle = new OptionItem("", "", (node: Node) => this.toggleView());
    this.viewToggle.isToggle = true;
    this.actionOptions.push(this.viewToggle);
    this.setViewType(this.view);
  }

  private addToCollectionList(collection:Node[],nodes=this.addNodesToCollection,position=0,error=false){
    if(position>=nodes.length){
      if(!error)
        this.toast.toast("WORKSPACE.TOAST.ADDED_TO_COLLECTION",{count:nodes.length,collection:collection[0].title});
      this.globalProgress=false;
      return;
    }
    this.addNodesToCollection=null;
    this.globalProgress=true;
    this.collectionApi.addNodeToCollection(collection[0].ref.id,nodes[position].ref.id).subscribe(()=>{
        this.addToCollectionList(collection,nodes,position+1,error);
      },
      (error:any)=>{
        if(error.status==RestConstants.DUPLICATE_NODE_RESPONSE){
          this.toast.error(null,"WORKSPACE.TOAST.NODE_EXISTS_IN_COLLECTION",{name:nodes[position].name});
        }
        else
          NodeHelper.handleNodeError(this.toast,nodes[position].name,error);
        this.addToCollectionList(collection,nodes,position+1,true);
      });
  }

  private printListener() {
    // not working properly
    /*
    let mediaQueryList = window.matchMedia('print');
    mediaQueryList.addListener((mql)=> {
      let lastType=-1;
      if (mql.matches) {
        console.log("print match");
        lastType=this.view;
        this.view=ListTableComponent.VIEW_TYPE_LIST;
        console.log(this.view);
      } else if(lastType!=-1) {
        this.view=lastType;
        lastType=-1;
      }
    });
    */
  }

  private addToStore(selection: Node[],position=0,errors=0) {
    if(position==selection.length){
      if(errors==0)
        this.toast.toast('SEARCH.ADDED_TO_NODE_STORE',{count:position,errors:errors});
      this.globalProgress=false;
      this.updateSelection([]);
      this.invalidateNodeStore=new Boolean(true);
      return;
    }
    this.globalProgress=true;
    this.iam.addNodeList(SearchNodeStoreComponent.LIST,selection[position].ref.id).subscribe(()=>{
      this.addToStore(selection,position+1,errors);
    },(error:any)=>{
      if(RestHelper.errorMessageContains(error,"Node is already in list"))
        this.toast.error(null,'SEARCH.ADDED_TO_NODE_STORE_EXISTS',{name:RestHelper.getTitle(selection[position])});
      this.addToStore(selection,position+1,errors+1)
    });
  }

  private prepare(param:any) {
    this.connector.isLoggedIn().subscribe((data:LoginResult)=> {
      this.login=data;
      //if (data.isValidLogin && data.currentScope == null) {
      this.mds.getSet(this.mdsId,this.currentRepository==RestConstants.ALL ? RestConstants.HOME_REPOSITORY : this.currentRepository).subscribe((data:any)=>{
        this.currentMdsSet=data;
        this.updateColumns();
      });
      this.updateMdsActions();
      this.isGuest = data.isGuest;
      this.hasCheckbox=true;
      this.options=[];
      this.currentValues=null;
      this.mdsExtended=false;
      this.loadSavedSearch();
      if(param['mdsExtended'])
        this.mdsExtended=param['mdsExtended']=='true';
      if(param['parameters']){
        this.currentValues=JSON.parse(param['parameters']);
        this.reloadMds=new Boolean(true);
      }
      if(param['reurl']) {
        this.hasCheckbox=false;
        let apply=new OptionItem("APPLY", "redo", (node: Node) => NodeHelper.addNodeToLms(this.router,this.temporaryStorageService,node,this.searchService.reurl));
        apply.enabledCallback=((node:Node)=> {
          return node.access.indexOf(RestConstants.ACCESS_CC_PUBLISH) != -1;
        });
        this.options.push(apply);
      }
      else {
        this.addDefaultNodeOptions(data);
      }
      console.log("init "+this.searchService.searchResult.length);
      console.log(this.currentValues);
      if (this.searchService.searchResult.length < 1) {
        this.getSearch(param['query'] ? param['query'] : '', true,this.currentValues);
      }
    });
  }
  private getSourceIcon(repo:Repository){
    return NodeHelper.getSourceIconRepoPath(repo);
  }

  private getCurrentNode(node: Node) {
    return node ? node : this.selection[0];
  }

  private searchRepository(repos: Repository[],criterias:any,init:boolean,position=0) {
    if(position>0 && position>=repos.length)
      return;

    let repo=repos[position];
    /*
    let properties=[RestConstants.CM_MODIFIED_DATE,
      RestConstants.CM_CREATOR,
      RestConstants.CCM_PROP_WIDTH,
      RestConstants.CCM_PROP_HEIGHT,
      RestConstants.CCM_PROP_AUTHOR_FREETEXT,
      RestConstants.CCM_PROP_LIFECYCLECONTRIBUTER_AUTHOR_FN,
      RestConstants.CCM_PROP_METADATACONTRIBUTER_CREATOR_FN,
      RestConstants.CCM_PROP_LICENSE,
      RestConstants.CCM_PROP_REPLICATIONSOURCE,
      RestConstants.CCM_PROP_QUESTIONSALLOWED];*/
    let properties=[RestConstants.ALL];
    this.search.search(criterias,
      [RestConstants.LOM_PROP_GENERAL_KEYWORD],
      {
        sortBy: [RestConstants.LUCENE_SCORE,RestConstants.CM_MODIFIED_DATE],
        sortAscending: false,
        offset: this.searchService.skipcount,
        propertyFilter: [
          properties]
      },
      RestConstants.CONTENT_TYPE_FILES,
      repo ? repo.id : RestConstants.HOME_REPOSITORY,
      this.mdsId
    ).subscribe(
      (data: NodeList) => {
        this.resultCount.materials = data.pagination.total;
        this.processSearchResult(data,init);
        this.showspinner = false;
        this.searchService.showchosenfilters = true;
        this.searchRepository(repos,criterias,init,position+1);
      },
      (error: any) => {
        this.showspinner = false;
        this.toast.error(error);
        this.searchRepository(repos,criterias,init,position+1);
      }
    );

  }
  private getSourceIconPath(path:string){
    return NodeHelper.getSourceIconPath(path);
  }
  private updateRepositoryOrder() {
    if(!this.repositories)
      return;
    console.log("check swap "+this.repositories.length);
    if(this.repositories.length>4){
      console.log("check swap "+this.currentRepository);
      let hit=false;
      for(let i=3;i<this.repositories.length;i++){
        if(this.currentRepository==this.repositories[i].id){
          Helper.arraySwap(this.repositories,i,3);
          this.extendedRepositorySelected=true;
          break;
        }
      }
    }
    this.repositoryIds=[];
    for(let repo of this.repositories){
      if(repo.id==RestConstants.ALL || repo.id=='MORE')
        continue;
      this.repositoryIds.push({id:repo.id,title:repo.title,enabled:true});
    }
  }

  private updateMdsActions() {
    this.savedSearchOptions=[];

    this.mdsActions=[];
    this.mdsActions.push(new OptionItem('SEARCH.APPLY_FILTER','search',()=>{
      this.applyParameters(this.mdsRef.saveValues());
    }));
    if(this.applyMode){
      let apply=new OptionItem('APPLY','redo',(node:Node)=>NodeHelper.addNodeToLms(this.router,this.temporaryStorageService,node,this.searchService.reurl));
      this.savedSearchOptions.push(apply);
    }
    else{
    }
    let save=new OptionItem(this.applyMode ? 'SEARCH.EMBED_SEARCH_ACTION' : 'SEARCH.SAVE_SEARCH_ACTION',this.applyMode ? 'redo' : 'save',()=>{
      this.saveSearchDialog=true;
    });
    save.showName=false;
    this.mdsActions.push(save);
  }
  private closeSaveSearchDialog(){
    this.saveSearchDialog=false;
  }
  private saveSearch(name:string,replace=false){
    this.search.saveSearch(name,this.queryId,this.getCriterias(),this.currentRepository,this.mdsId,replace).subscribe((data:NodeWrapper)=>{
        console.log(data);
        this.saveSearchDialog=false;
        this.toast.toast('SEARCH.SAVE_SEARCH.TOAST_SAVED');
        this.loadSavedSearch();
        if(this.applyMode){
          NodeHelper.addNodeToLms(this.router,this.temporaryStorageService,data.node,this.searchService.reurl);
        }
      },
      (error:any)=>{
        if(error.status==RestConstants.DUPLICATE_NODE_RESPONSE){
          this.dialogTitle='SEARCH.SAVE_SEARCH.SEARCH_EXISTS_TITLE';
          this.dialogMessage='SEARCH.SAVE_SEARCH.SEARCH_EXISTS_MESSAGE';
          this.dialogButtons=[
            new DialogButton('RENAME',DialogButton.TYPE_CANCEL,()=>{this.dialogTitle=null}),
            new DialogButton('REPLACE',DialogButton.TYPE_PRIMARY,()=>{
              this.dialogTitle=null;
              this.saveSearch(name,true);
            })
          ];
        }
        else {
          this.toast.error(error);
        }
      });
  }

  private getCriterias(properties=this.currentValues,searchString=this.searchService.searchTerm) {
    let criterias:any=[];
    if(searchString)
      criterias.push({'property': RestConstants.PRIMARY_SEARCH_CRITERIA, 'values': [searchString]});
    if(properties) {
      for (var property in properties) {
        criterias.push({'property':property,'values':properties[property]});
      }
    }
    return criterias;
  }
  private loadSavedSearchNode(node:Node){
    this.sidenavTab=0;
    UIHelper.routeToSearchNode(this.router,node);
  }
  private goToSaveSearchWorkspace() {
    this.nodeApi.getNodeMetadata(RestConstants.SAVED_SEARCH).subscribe((data:NodeWrapper)=>{
      NodeHelper.goToWorkspaceFolder(this.nodeApi,this.router,this.login,data.node.ref.id);
    });
  }
  private loadSavedSearch() {
    if(!this.isGuest){
      this.savedSearch=[];
      this.savedSearchLoading=true;
      let request:any={propertyFilter:[RestConstants.ALL],sortBy:[RestConstants.CM_PROP_TITLE],sortAscending:true,offset:0};
      if(this.savedSearchOwn) {
        request.count=RestConstants.COUNT_UNLIMITED;
        this.nodeApi.getChildren(RestConstants.SAVED_SEARCH, [], request).subscribe((data: NodeList) => {
          this.savedSearch = data.nodes;
          this.savedSearchLoading=false;
        });
      }
      else{
        this.search.searchSimple('saved_search',[],this.savedSearchQuery,request,RestConstants.CONTENT_TYPE_ALL).subscribe((data: NodeList) => {
          this.savedSearch = data.nodes;
          this.savedSearchLoading=false;
        });;
      }
    }
  }
  public setSavedSearchQuery(query:string){
    this.savedSearchQuery=query;
    this.loadSavedSearch();
  }
}
