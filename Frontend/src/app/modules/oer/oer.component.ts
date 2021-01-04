
import {Component, ViewChild, HostListener, ElementRef} from '@angular/core';
import 'rxjs/add/operator/map';
import {Router, ActivatedRoute, Params} from '@angular/router';
import {TranslateService} from '@ngx-translate/core';
import {Translation} from '../../core-ui-module/translation';
import {ListItem, RestSearchService} from '../../core-module/core.module';
import {RestNodeService} from '../../core-module/core.module';
import {RestConstants} from '../../core-module/core.module';
import {RestConnectorService} from '../../core-module/core.module';
import {Node, NodeList, LoginResult} from '../../core-module/core.module';
import {OptionItem, Scope} from '../../core-ui-module/option-item';
import {TemporaryStorageService} from '../../core-module/core.module';
import {UIHelper} from '../../core-ui-module/ui-helper';
import {Title} from '@angular/platform-browser';
import {ConfigurationService} from '../../core-module/core.module';
import {SessionStorageService} from '../../core-module/core.module';
import {UIConstants} from '../../core-module/ui/ui-constants';
import {RestMdsService} from '../../core-module/core.module';
import {RestHelper} from '../../core-module/core.module';
import {MainNavComponent} from '../../common/ui/main-nav/main-nav.component';
import {MdsHelper} from '../../core-module/rest/mds-helper';
import {GlobalContainerComponent} from '../../common/ui/global-container/global-container.component';
import {Helper} from '../../core-module/rest/helper';
import {NodeUrlComponent} from "../../core-ui-module/components/node-url/node-url.component";
import {NodeHelperService} from '../../core-ui-module/node-helper.service';



@Component({
  selector: 'app-oer',
  templateUrl: 'oer.component.html',
  styleUrls: ['oer.component.scss'],
  })



export class OerComponent {
  readonly SCOPES = Scope;
  @ViewChild('mainNav') mainNavRef: MainNavComponent;
  public COLLECTIONS=0;
  public MATERIALS=1;
  public TOOLS=2;
  private TYPE_COUNT=3;
  private columns:ListItem[][]=[];
  private options: OptionItem[][]=[];
  private displayedNode: Node;
  public currentQuery: string;
  public loading:boolean[]=[];
  private showMore:boolean[]=[];
  public hasMore:boolean[]=[];
  private offsets:number[]=[];
  public nodes:Node[][]=[];
  constructor(
    private router : Router,
    private route : ActivatedRoute,
    private connector:RestConnectorService,
    private nodeService: RestNodeService,
    private nodeHelper: NodeHelperService,
    private searchService: RestSearchService,
    private mdsService:RestMdsService,
    private storage : TemporaryStorageService,
    private session : SessionStorageService,
    private title : Title,
    private config : ConfigurationService,
    private translate : TranslateService) {
      Translation.initialize(translate,this.config,this.session,this.route).subscribe(()=> {
        UIHelper.setTitle('SEARCH.TITLE',title,translate,config);
        GlobalContainerComponent.finishPreloading();
          for(let i=0;i<this.TYPE_COUNT;i++) {
              this.columns.push([]);
              this.updateOptions(i)
              this.nodes.push([]);
          }

          this.columns[this.COLLECTIONS].push(new ListItem('NODE',RestConstants.CM_NAME));
          this.columns[this.COLLECTIONS].push(new ListItem('COLLECTION','info'));
          this.columns[this.COLLECTIONS].push(new ListItem('COLLECTION','scope'));
          this.mdsService.getSet().subscribe((mds:any)=> {
              this.columns[this.MATERIALS]=MdsHelper.getColumns(this.translate, mds,'search');
          });
          /*
          this.config.get("searchColumns").subscribe((data:any)=>{
            this.columns[this.MATERIALS]=[];
            if(data && data.length){
              for(let item of data){
                this.columns[this.MATERIALS].push(new ListItem("NODE",item));
              }
            }
            else{
              this.columns[this.MATERIALS].push(new ListItem("NODE",RestConstants.CM_NAME));
              this.columns[this.MATERIALS].push(new ListItem("NODE",RestConstants.CM_MODIFIED_DATE));
              this.columns[this.MATERIALS].push(new ListItem("NODE",RestConstants.CCM_PROP_LICENSE));
              this.columns[this.MATERIALS].push(new ListItem("NODE",RestConstants.CCM_PROP_REPLICATIONSOURCE));
            }
          });
          //this.columns[this.MATERIALS].push(new ListItem("NODE",RestConstants.CCM_PROP_REPLICATIONSOURCE));
          */
          this.columns[this.TOOLS].push(new ListItem('NODE',RestConstants.CM_NAME));
          this.columns[this.TOOLS].push(new ListItem('NODE',RestConstants.LOM_PROP_DESCRIPTION));

          this.connector.numberPerRequest=20;
          for(let i=0;i<this.TYPE_COUNT;i++) {
              this.offsets[i]=0;
              this.nodes[i]=[];
              this.showMore[i]=false;
              this.hasMore[i]=false;
          }

          this.route.queryParams.forEach((params: Params) => {
              for (let i = 0; i < this.TYPE_COUNT; i++) {
                  this.showMore[i] = params['showMore' + i]=='true';
              }
              this.search(params.query?params.query:'');
          });
      });

    setInterval(()=>this.updateHasMore(),1000);
   }
   private goToCollections() {
    this.router.navigate([UIConstants.ROUTER_PREFIX+'collections'],{queryParams:{mainnav:true}});
   }
  private goToSearch() {
    this.router.navigate([UIConstants.ROUTER_PREFIX+'search']);
  }
  public routeSearch(query=this.currentQuery) {
     if(query) {
       this.router.navigate([UIConstants.ROUTER_PREFIX+'search'],{queryParams:{query}});
       return;
     }
    const queryParams:any= {query};
     for (let i = 0; i < this.TYPE_COUNT; i++) {
        queryParams['showMore' + i]=this.showMore[i];
     }
    this.router.navigate(['./'],{queryParams,relativeTo:this.route});
   }
   private checkMore() {
   }
   private search(string: string) {
     if(this.currentQuery === string)
       return;
     for(let i=0;i<this.TYPE_COUNT;i++) {
       this.offsets[i]=0;
       this.nodes[i]=[];
       this.loading[i]=true;

     }

     const criterias:any[] = [];
     this.currentQuery=string;
     const originalQuery=string;
     if(string === '')
       string = '*';

     criterias.push({property: 'ngsearchword', values: [string]});


     this.searchService.search(criterias,[], {sortBy:[
                RestConstants.CCM_PROP_COLLECTION_PINNED_STATUS,
                RestConstants.CCM_PROP_COLLECTION_PINNED_ORDER,
                RestConstants.CM_MODIFIED_DATE],
                sortAscending:[false,true,false],
                offset:this.offsets[this.COLLECTIONS],
                propertyFilter:[RestConstants.ALL]},
        RestConstants.CONTENT_TYPE_COLLECTIONS,
        RestConstants.HOME_REPOSITORY,
        RestConstants.DEFAULT,
        [RestConstants.ALL],
         'collections'
     ).subscribe(
       (data : NodeList) => {
         if(this.currentQuery !== originalQuery)
           return;
         for(const node of data.nodes) {
           this.nodes[this.COLLECTIONS].push(node);
         }
         this.offsets[this.COLLECTIONS]+=this.connector.numberPerRequest;
         this.loading[this.COLLECTIONS]=false;
       }
     );

     this.searchService.search(criterias,[], {sortBy:[RestConstants.CM_MODIFIED_DATE],sortAscending:false,offset:this.offsets[this.MATERIALS],propertyFilter:[RestConstants.ALL]}).subscribe(
       (data : NodeList) => {
         if(this.currentQuery !== originalQuery)
           return;
         for(const node of data.nodes) {
           this.nodes[this.MATERIALS].push(node);
         }
         // force an update to allow change detection
         this.nodes[this.MATERIALS] = Helper.deepCopyArray(this.nodes[this.MATERIALS]);
         this.offsets[this.MATERIALS]+=this.connector.numberPerRequest;
         this.loading[this.MATERIALS]=false;
       }
     );

     this.searchService.search(criterias,[], {sortBy:[RestConstants.CM_MODIFIED_DATE],sortAscending:false,offset:this.offsets[this.TOOLS],propertyFilter:[RestConstants.LOM_PROP_DESCRIPTION]}).subscribe(
       (data : NodeList) => {
         if(this.currentQuery !== originalQuery)
           return;
         for(const node of data.nodes) {
           this.nodes[this.TOOLS].push(node);
         }
         this.nodes[this.TOOLS] = Helper.deepCopyArray(this.nodes[this.TOOLS]);
         this.offsets[this.TOOLS]+=this.connector.numberPerRequest;
         this.loading[this.TOOLS]=false;
       }
     );

   }
   private toggleMore(mode:number) {
    this.showMore[mode]=!this.showMore[mode];
    this.routeSearch();
   }
   private loadMore(mode:number) {

   }
   private openNode(node: Node){
      this.router.navigate([this.nodeHelper.getNodeLink('routerLink', node)],
          {queryParams: (this.nodeHelper.getNodeLink('queryParams', node) as any)}
      );
   }
   private updateOptions(mode:number,node:Node=null) {
     this.options[mode]=[];
     if(mode==this.MATERIALS) {
       this.options[mode].push(new OptionItem('INFORMATION', 'info_outline', (node: Node) => this.openNode(node)));
       const download = new OptionItem('DOWNLOAD', 'cloud_download', (node: Node) => this.downloadNode(node));
       if (node && node.mediatype == 'link')
         download.isEnabled = false;
       this.options[mode].push(download);
     }
   }
  private downloadNode(node:Node=this.displayedNode) {
    window.open(node.downloadUrl);
  }

  private updateHasMore() {
     try {
       this.hasMore[this.COLLECTIONS] = document.getElementById('collections').scrollHeight > 90 + 15;
     } catch(e) {}
     try {
       this.hasMore[this.MATERIALS] = document.getElementById('materials').scrollHeight > 300 + 15;
     } catch(e) {}
    try {
      this.hasMore[this.TOOLS] = document.getElementById('tools').scrollHeight > 300 + 15;
    } catch(e) {}

  }
}
