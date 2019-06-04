import {Component} from '@angular/core';
import {ActivatedRoute, Params, Router} from "@angular/router";
import {
    ConfigurationService,
    FrameEventsService, NodeRemoteWrapper, NodeWrapper,
    Node,
    RestConnectorService, RestConstants,
    RestNodeService, SessionStorageService,
    TemporaryStorageService
} from "../../../core-module/core.module";
import {Toast} from "../toast";
import {SearchService} from "../../../modules/search/search.service";
import {NodeHelper} from "../node-helper";
import {UIConstants} from "../../../core-module/ui/ui-constants";
import {Translation} from "../../translation";
import {TranslateService} from "@ngx-translate/core";
import {RestLocatorService} from "../../../core-module/core.module";

@Component({
  selector: 'add-node-to-lms',
  templateUrl: 'apply-to-lms.component.html',
  styleUrls: ['apply-to-lms.component.scss']
})

export class ApplyToLmsComponent{
  node: Node;
  reurl: string;

  constructor(
    private connector : RestConnectorService,
    private locator  : RestLocatorService,
    private nodeApi : RestNodeService,
    private toast : Toast,
    private events : FrameEventsService,
    private translate : TranslateService,
    private config : ConfigurationService,
    private temporaryStorage : TemporaryStorageService,
    private storage : SessionStorageService,
    private route : ActivatedRoute,
    private searchService:SearchService) {
    this.route.queryParams.subscribe((params:Params)=>{
      if(params['reurl']) {
        this.reurl=params['reurl'];
      }
      this.route.params.subscribe((params: Params) => {
        if(temporaryStorage.get(TemporaryStorageService.APPLY_TO_LMS_PARAMETER_NODE)){
          this.node = temporaryStorage.get(TemporaryStorageService.APPLY_TO_LMS_PARAMETER_NODE);
          this.forward();
        }
        else if(params['node']) {
          this.locator.locateApi().subscribe(()=>{
            this.nodeApi.getNodeMetadata(params['node'], [RestConstants.ALL],params['repo']).subscribe(
              (data : NodeWrapper) => {
                this.node = data.node;
                this.forward();
              },(error:any)=>{
                Translation.initialize(this.translate,this.config,this.storage,this.route).subscribe(()=>{
                  this.toast.error(error);
                });
              }
            )
          });
        }
      });
    });

  }

  forward() {
      this.nodeApi.prepareUsage(this.node.ref.id,this.node.ref.repo).subscribe((nodeWrapper)=> {
          this.applyNode(nodeWrapper);
      },(error)=>{
          this.toast.error(error);
      });
  }

  public static navigateToSearchUsingReurl(router:Router,url=window.location.href) {
    router.navigate(["./"+UIConstants.ROUTER_PREFIX+"search"],{queryParams:{reurl:url}});
  }

  private static roundNumber(number: number) {
      number=Math.round(number);
      if(Number.isNaN(number))
        return 0;
      return number;
  }

    private applyNode(wrapper: NodeRemoteWrapper) {
        let node=wrapper.node;
        // copy the main object to remote (in this case, it's simply a regular, local object)
        if(!wrapper.remote)
            wrapper.remote=wrapper.node;
        let reurl = this.reurl;
        console.log(reurl);
        // the ccrep should always point to the local object (relevant if it's from a remote repo)
        let ccrepUrl = 'ccrep://' + encodeURIComponent(wrapper.remote.ref.repo) + '/' + encodeURIComponent(wrapper.remote.ref.id);
        if (reurl == "IFRAME" || reurl=="WINDOW") {
            (node as any).objectUrl = ccrepUrl;
            NodeHelper.appendImageData(this.connector, node).subscribe((data: Node) => {
                this.events.broadcastEvent(FrameEventsService.EVENT_APPLY_NODE, data);
                window.history.back();
            },(error)=>{
                console.warn("failed to fetch image data",error);
                this.events.broadcastEvent(FrameEventsService.EVENT_APPLY_NODE,node);
                window.history.back();
            });
            return;
        }
        let params = reurl.indexOf("?") == -1 ? '?' : '&';
        params += 'nodeId=' + ccrepUrl;
        params += '&localId=' + encodeURIComponent(node.ref.id);
        if (node.title)
            params += '&title=' + encodeURIComponent(node.title);
        else
            params += '&title=' + encodeURIComponent(node.name);
        params += '&mimeType=' + encodeURIComponent(node.mimetype);
        params += '&mediatype=' + encodeURIComponent(node.mediatype);
        params += '&h=' + ApplyToLmsComponent.roundNumber(node.properties[RestConstants.CCM_PROP_HEIGHT]);
        params += '&w=' + ApplyToLmsComponent.roundNumber(node.properties[RestConstants.CCM_PROP_WIDTH]);
        if(node.contentVersion)
            params += '&v=' + node.contentVersion;
        if(node.properties[RestConstants.CCM_PROP_CCRESSOURCETYPE])
            params += '&resourceType=' + encodeURIComponent(node.properties[RestConstants.CCM_PROP_CCRESSOURCETYPE]);
        if(node.properties[RestConstants.CCM_PROP_CCRESSOURCEVERSION])
            params += '&resourceVersion=' + encodeURIComponent(node.properties[RestConstants.CCM_PROP_CCRESSOURCEVERSION]);
        params += '&isDirectory=' + node.isDirectory;
        params += '&iconURL=' + encodeURIComponent(node.iconURL);
        params += '&previewURL=' + encodeURIComponent(node.preview.url);
        params += '&repoType=' + encodeURIComponent(node.repositoryType);
        // reurl + params
        //let contentParams = node.contentUrl.indexOf("?") == -1 ? '?' : '&';
        //contentParams += "LMS_URL=" + encodeURIComponent(reurl);
        console.log(reurl+params);
        //console.log(node.contentUrl + contentParams);
        window.location.replace(reurl + params);// + params;
    }
}
