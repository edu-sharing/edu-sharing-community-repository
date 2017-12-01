import {Component, Input, EventEmitter, Output, ViewChild, ElementRef} from '@angular/core';
import {RestNodeService} from "../../../common/rest/services/rest-node.service";
import {RestConstants} from "../../../common/rest/rest-constants";
import {NodeWrapper,NodeList, Node, IamUsers, WorkflowEntry} from "../../../common/rest/data-object";
import {VCard} from "../../../common/VCard";
import {Toast} from "../../../common/ui/toast";
import {ModalDialogComponent, DialogButton} from "../../../common/ui/modal-dialog/modal-dialog.component";
import {DateModel, DatePickerOptions} from "ng2-datepicker";
import {Translation} from "../../../common/translation";
import {TranslateService} from "ng2-translate";
import any = jasmine.any;
import {SuggestItem} from "../../../common/ui/autocomplete/autocomplete.component";
import {RestIamService} from "../../../common/rest/services/rest-iam.service";
import {NodeHelper} from "../../../common/ui/node-helper";
import {RestSearchService} from "../../../common/rest/services/rest-search.service";
import {OptionItem} from "../../../common/ui/actionbar/actionbar.component";
import {MdsComponent} from "../../../common/ui/mds/mds.component";
import {RestToolService} from "../../../common/rest/services/rest-tool.service";
import {UIAnimation} from "../../../common/ui/ui-animation";
import {trigger} from "@angular/animations";

@Component({
  selector: 'workspace-lti-tools',
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
      this.options.push(new OptionItem('WORKSPACE.LTI_TOOLS.NEW_TOOL', 'add', () => this.onCreate.emit({type: MdsComponent.TYPE_TOOLDEFINITION})))
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
      console.log(data);
      this.nodes=data.nodes;
      this.loading=false;
    });
  }

  private createLtiObject(node: Node) {
    this.onCreateLtiObject.emit(node);
  }

  private doRefresh() {
    this.search();
    if(this._currentTool){
      this.nodeService.getNodeMetadata(this._currentTool.ref.id,[RestConstants.ALL]).subscribe((data:NodeWrapper)=>{
        this.openTool(data.node,false);
      });
    }
  }
}
