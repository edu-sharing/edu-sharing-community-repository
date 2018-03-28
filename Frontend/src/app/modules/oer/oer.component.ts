
import {Component, ViewChild, HostListener, ElementRef} from '@angular/core';
import 'rxjs/add/operator/map';
import { HttpModule } from '@angular/http';
import {Router, ActivatedRoute, Params} from '@angular/router';
import {TranslateService} from "@ngx-translate/core";
import {Translation} from "../../common/translation";
import {RestSearchService} from '../../common/rest/services/rest-search.service';
import {RestMetadataService} from '../../common/rest/services/rest-metadata.service';
import {RestNodeService} from '../../common/rest/services/rest-node.service';
import {RestConstants} from '../../common/rest/rest-constants';
import {RestConnectorService} from "../../common/rest/services/rest-connector.service";
import {Node, NodeList, LoginResult} from "../../common/rest/data-object";
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



@Component({
  selector: 'app-oer',
  templateUrl: 'oer.component.html',
  styleUrls: ['oer.component.scss'],
  providers: [HttpModule]
  })



export class OerComponent {
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
  private nodesCollections:Node[]=[];
  private render_options: OptionItem[];
  constructor(
    private router : Router,
    private route : ActivatedRoute,
    private connector:RestConnectorService,
    private nodeService: RestNodeService,
    private searchService: RestSearchService,
    private metadataService:RestMetadataService,
    private mdsService:RestMdsService,
    private storage : TemporaryStorageService,
    private session : SessionStorageService,
    private title : Title,
    private config : ConfigurationService,
    private translate : TranslateService) {
      Translation.initialize(translate,this.config,this.session,this.route).subscribe(()=>{
        UIHelper.setTitle('SEARCH.TITLE',title,translate,config);
          for(let i=0;i<this.TYPE_COUNT;i++) {
              this.columns.push([]);
              this.updateOptions(i)
              this.nodes.push([]);
          }

          this.columns[this.COLLECTIONS].push(new ListItem("NODE",RestConstants.CM_NAME));
          this.columns[this.COLLECTIONS].push(new ListItem("COLLECTION",'info'));
          this.columns[this.COLLECTIONS].push(new ListItem("COLLECTION",'scope'));
          this.mdsService.getSet().subscribe((mds:any)=>{
              this.columns[this.MATERIALS]=MdsHelper.getColumns(mds,'search');
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
          this.columns[this.TOOLS].push(new ListItem("NODE",RestConstants.CM_NAME));
          this.columns[this.TOOLS].push(new ListItem("NODE",RestConstants.LOM_PROP_DESCRIPTION));

          this.connector.numberPerRequest=20;
          for(let i=0;i<this.TYPE_COUNT;i++){
              this.offsets[i]=0;
              this.nodes[i]=[];
              this.showMore[i]=false;
              this.hasMore[i]=false;
          }

          this.route.queryParams.forEach((params: Params) => {
              for (let i = 0; i < this.TYPE_COUNT; i++) {
                  this.showMore[i] = params['showMore' + i]=='true';
              }
              this.search(params["query"]?params["query"]:"");
          });
      });

    setInterval(()=>this.updateHasMore(),1000);
   }
   private goToCollections(){
    this.router.navigate([UIConstants.ROUTER_PREFIX+"collections"],{queryParams:{mainnav:true}});
   }
  private goToSearch(){
    this.router.navigate([UIConstants.ROUTER_PREFIX+"search"]);
  }
  public routeSearch(query=this.currentQuery){
     if(query){
       this.router.navigate([UIConstants.ROUTER_PREFIX+"search"],{queryParams:{query:query}});
       return;
     }
    let queryParams:any={query:query};
     for (let i = 0; i < this.TYPE_COUNT; i++) {
        queryParams['showMore' + i]=this.showMore[i];
     }
    this.router.navigate(["./"],{queryParams:queryParams,relativeTo:this.route});
   }
   private checkMore(){
   }
   private search(string:any){
     if(this.currentQuery==string)
       return;
     for(let i=0;i<this.TYPE_COUNT;i++){
       this.offsets[i]=0;
       this.nodes[i]=[];
       this.loading[i]=true;

     }

     let criterias:any[] = [];
     this.currentQuery=string;
     let originalQuery=string;
     if(string === '')
       string = '*';

     criterias.push({'property': 'ngsearchword', 'values': [string]});


     this.searchService.search(criterias,[], {sortBy:[RestConstants.CM_MODIFIED_DATE],sortAscending:false,offset:this.offsets[this.COLLECTIONS],propertyFilter:[RestConstants.ALL]},RestConstants.CONTENT_TYPE_COLLECTIONS).subscribe(
       (data : NodeList) => {
         if(this.currentQuery!=originalQuery)
           return;
         for(let node of data.nodes){
           this.nodes[this.COLLECTIONS].push(node);
         }
         this.offsets[this.COLLECTIONS]+=this.connector.numberPerRequest;
         this.loading[this.COLLECTIONS]=false;
       }
     );

     this.searchService.search(criterias,[], {sortBy:[RestConstants.CM_MODIFIED_DATE],sortAscending:false,offset:this.offsets[this.MATERIALS],propertyFilter:[RestConstants.ALL]}).subscribe(
       (data : NodeList) => {
         if(this.currentQuery!=originalQuery)
           return;
         for(let node of data.nodes){
           this.nodes[this.MATERIALS].push(node);
         }
         this.offsets[this.MATERIALS]+=this.connector.numberPerRequest;
         this.loading[this.MATERIALS]=false;
       }
     );

     this.searchService.search(criterias,[], {sortBy:[RestConstants.CM_MODIFIED_DATE],sortAscending:false,offset:this.offsets[this.TOOLS],propertyFilter:[RestConstants.LOM_PROP_DESCRIPTION]}).subscribe(
       (data : NodeList) => {
         if(this.currentQuery!=originalQuery)
           return;
         for(let node of data.nodes){
           this.nodes[this.TOOLS].push(node);
         }
         this.offsets[this.TOOLS]+=this.connector.numberPerRequest;
         this.loading[this.TOOLS]=false;
       }
     );

   }
   private toggleMore(mode:number){
    this.showMore[mode]=!this.showMore[mode];
    this.routeSearch();
   }
   private loadMore(mode:number){

   }
   private updateOptions(mode:number,node:Node=null){
     this.options[mode]=[];
     if(mode==this.MATERIALS) {
       this.options[mode].push(new OptionItem("INFORMATION", "info_outline", (node: Node) => this.click(node)));
       let download = new OptionItem("DOWNLOAD", "cloud_download", (node: Node) => this.downloadNode(node));
       if (node && node.mediatype == 'link')
         download.isEnabled = false;
       this.options[mode].push(download);
     }
   }
   private click(node:Node){
    console.log(node);
    this.displayedNode=node;
    if(node.collection){
      this.router.navigate([UIConstants.ROUTER_PREFIX+"collections"],{queryParams:{id:node.ref.id,mainnav:true}});
    }
    else{
      this.render_options=[];
      this.storage.set(TemporaryStorageService.NODE_RENDER_PARAMETER_OPTIONS, this.render_options);
      this.storage.set(TemporaryStorageService.NODE_RENDER_PARAMETER_LIST, this.nodes[this.MATERIALS]);
      this.router.navigate([UIConstants.ROUTER_PREFIX+"render", node.ref.id]);
    }
   }
  private downloadNode(node:Node=this.displayedNode) {
    window.open(node.downloadUrl);
  }

  private updateHasMore() {
     try{
       this.hasMore[this.COLLECTIONS] = document.getElementById("collections").scrollHeight > 90 + 15;
     }catch(e){}
     try {
       this.hasMore[this.MATERIALS] = document.getElementById("materials").scrollHeight > 300 + 15;
     }catch(e){}
    try {
      this.hasMore[this.TOOLS] = document.getElementById("tools").scrollHeight > 300 + 15;
    }catch(e){}

  }
}
