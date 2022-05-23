import {Component, Input, EventEmitter, Output, ViewChild, ElementRef} from '@angular/core';
import {RestNodeService} from "../../../core-module/core.module";
import {RestConstants} from "../../../core-module/core.module";
import {NodeWrapper,NodeList, Node, IamUsers, WorkflowEntry} from "../../../core-module/core.module";
import {Toast} from "../../../core-ui-module/toast";
import {TranslateService} from "@ngx-translate/core";
import {RestSearchService} from "../../../core-module/core.module";
import {OptionItem} from "../../../core-ui-module/option-item";
import {MdsComponent} from "../../../features/mds/legacy/mds/mds.component";
import {RestToolService} from "../../../core-module/core.module";
import {UIAnimation} from "../../../core-module/ui/ui-animation";
import {trigger} from "@angular/animations";
import { MdsType } from '../../../features/mds/types/types';

@Component({
  selector: 'es-workspace-lti-tools',
  templateUrl: 'lti-tools.component.html',
  styleUrls: ['lti-tools.component.scss'],
  animations: [
    trigger('overlay', UIAnimation.openOverlay(UIAnimation.ANIMATION_TIME_FAST)),
  ]
})
export class WorkspaceLtiToolsComponent  {
  @Output() onClose=new EventEmitter();
  @Output() onCreate=new EventEmitter();
  @Output() onCreateLtiObject=new EventEmitter();
  @Output() onEdit=new EventEmitter();
  @Output() onOpenConfig=new EventEmitter();
  @Input() set refresh(boolean:Boolean){
    this.doRefresh();
  };
  public _currentTool:Node;
  @Input() set currentTool(currentTool:Node){
    this._currentTool=currentTool;
    this.doRefresh();
  }
  @Output() currentToolChange=new EventEmitter();
  public loading=true;
  public toolInstances: Node[];
  public showCreateList=false;
  public query="";
  public nodes: Node[];
  public options:OptionItem[]=[];
  public cancel(){
    this.openTool(null);
    this.onClose.emit();
  }
  public openTool(node:Node,emit=true){
    if(emit){
      // causes ui flickering
      //this.currentToolChange.emit(node);
    }
    this._currentTool=node;
    this.options=[];
    if(node){
      this.loading=true;
      this.toolService.getToolInstances(node.ref.id).subscribe((data:NodeList)=> {
        this.loading=false;
        this.toolInstances=data.nodes;
        this.options=[];
        let use=new OptionItem('WORKSPACE.LTI_TOOLS.USE_TOOL', 'open_in_new', () => {
          if(data.nodes.length==1) {
            this.createLtiObject(data.nodes[0]);
          }
          else{
            this.showCreateList=!this.showCreateList;
          }
        });
        use.isEnabled=data.nodes.length>0;
        this.options.push(use);

        if (node.access.indexOf(RestConstants.ACCESS_WRITE) != -1) {
          this.options.push(new OptionItem('WORKSPACE.LTI_TOOLS.EDIT_TOOL', 'edit', () => this.onEdit.emit(node)));
        }
        this.options.push(new OptionItem('WORKSPACE.LTI_TOOLS.MANAGE_CONFIGS', 'settings', () => this.onOpenConfig.emit(node)));
      });
    }
    else {
      this.options.push(new OptionItem('WORKSPACE.LTI_TOOLS.NEW_TOOL', 'add', () => this.onCreate.emit({type: MdsType.ToolDefinition})))
    }

  }
  public constructor(
    private nodeService:RestNodeService,
    private translate:TranslateService,
    private searchService:RestSearchService,
    private toolService:RestToolService,
    private toast:Toast,
  ){
    this.search();
    this.openTool(null);
  }
  public search(){
    this.loading=true;
    let criterias=[
      {property:"toolname",values:[this.query]}
    ];
    this.searchService.search(criterias,[],{maxItems:100},
      RestConstants.CONTENT_TYPE_FILES,RestConstants.HOME_REPOSITORY,
      RestConstants.DEFAULT,[RestConstants.ALL],"tools").subscribe((data:NodeList)=>{
      this.nodes=data.nodes;
      this.loading=false;
    });
  }

  createLtiObject(node: Node) {
    this.onCreateLtiObject.emit(node);
  }

  doRefresh() {
    this.search();
    if(this._currentTool){
      this.nodeService.getNodeMetadata(this._currentTool.ref.id,[RestConstants.ALL]).subscribe((data:NodeWrapper)=>{
        this.openTool(data.node,false);
      });
    }
  }
}
