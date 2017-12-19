import {Component} from '@angular/core';
import {ActivatedRoute, Params, Router} from "@angular/router";
import {RestConnectorService} from "../../rest/services/rest-connector.service";
import {RestNodeService} from "../../rest/services/rest-node.service";
import {Toast} from "../toast";
import {Node, NodeWrapper} from "../../rest/data-object";
import {RestConstants} from "../../rest/rest-constants";
import {SearchService} from "../../../modules/search/search.service";
import {FrameEventsService} from "../../services/frame-events.service";
import {NodeHelper} from "../node-helper";
import {UIConstants} from "../ui-constants";
import {Translation} from "../../translation";
import {TranslateService} from "@ngx-translate/core";
import {ConfigurationService} from "../../services/configuration.service";
import {SessionStorageService} from "../../services/session-storage.service";
import {TemporaryStorageService} from "../../services/temporary-storage.service";

@Component({
  selector: 'add-node-to-lms',
  templateUrl: 'apply-to-lms.component.html',
  styleUrls: ['apply-to-lms.component.scss']
})

export class ApplyToLmsComponent{
  private node: Node;
  private reurl: string;

  constructor(
    private connector : RestConnectorService,
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
          this.connector.locateApi().subscribe(()=>{
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
    let reurl = this.reurl;
    console.log(reurl);
    if(reurl=="IFRAME"){
      NodeHelper.appendImageData(this.connector,this.node).subscribe((data:Node)=>{
        this.events.broadcastEvent(FrameEventsService.EVENT_APPLY_NODE,data);
        window.history.back();
      });
      return;
    }
    let params = reurl.indexOf("?")==-1 ? '?' : '&';
    params += 'nodeId=ccrep://'+encodeURIComponent(this.node.ref.repo)+'/'+encodeURIComponent(this.node.ref.id);
    params += '&localId='+encodeURIComponent(this.node.ref.id);
    if(this.node.title)
      params += '&title='+encodeURIComponent(this.node.title);
    else
      params += '&title='+encodeURIComponent(this.node.name);
    params += '&mimeType='+encodeURIComponent(this.node.mimetype);
    params += '&mediatype='+encodeURIComponent(this.node.mediatype);
    params += '&h='+ApplyToLmsComponent.roundNumber(this.node.properties[RestConstants.CCM_PROP_HEIGHT]);
    params += '&w='+ApplyToLmsComponent.roundNumber(this.node.properties[RestConstants.CCM_PROP_WIDTH]);
    params += '&v='+this.node.contentVersion;
    params += '&repoType='+encodeURIComponent(this.node.repositoryType);
    // reurl + params
    let contentParams = this.node.contentUrl.indexOf("?")==-1 ? '?' : '&';
    contentParams+="LMS_URL="+encodeURIComponent(reurl);
    //console.log(reurl+params);
    console.log(this.node.contentUrl + contentParams);
    window.location.replace(this.node.contentUrl + contentParams);// + params;
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
}
