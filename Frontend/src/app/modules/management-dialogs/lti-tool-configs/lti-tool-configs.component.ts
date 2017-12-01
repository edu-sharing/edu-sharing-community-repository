import {Component, Input, EventEmitter, Output, ViewChild, ElementRef} from '@angular/core';
import {RestNodeService} from "../../../common/rest/services/rest-node.service";
import {RestConstants} from "../../../common/rest/rest-constants";
import {NodeWrapper,NodeList, Node, IamUsers, WorkflowEntry} from "../../../common/rest/data-object";
import {Toast} from "../../../common/ui/toast";
import {ModalDialogComponent, DialogButton} from "../../../common/ui/modal-dialog/modal-dialog.component";
import {TranslateService} from "ng2-translate";
import {OptionItem} from "../../../common/ui/actionbar/option-item";
import {MdsComponent} from "../../../common/ui/mds/mds.component";
import {RestToolService} from "../../../common/rest/services/rest-tool.service";
import {ListItem} from "../../../common/ui/list-item";

@Component({
  selector: 'workspace-lti-tool-configs',
  templateUrl: 'lti-tool-configs.component.html',
  styleUrls: ['lti-tool-configs.component.scss']
})
export class WorkspaceLtiToolConfigsComponent  {
  @Output() onClose=new EventEmitter();
  @Output() onCreate=new EventEmitter();
  @Output() onEdit=new EventEmitter();
  public _tool : Node;
  @Input() set tool(tool:Node){
    this._tool=tool;
    if(this._tool)
      this.search();
  }
  public loading=true;
  public query="";
  public dialogTitle : string;
  public dialogMessage : string;
  public dialogButtons : DialogButton[];
  public configs: Node[];
  public actionOptions:OptionItem[]=[];
  public explorerOptions:OptionItem[]=[];
  public columns : ListItem[]=[];

  public cancel(){
    this.onClose.emit();
  }
  public constructor(
    private nodeService:RestNodeService,
    private translate:TranslateService,
    private toolsApi:RestToolService,
    private toast:Toast,
  ){
    this.actionOptions.push(new OptionItem('WORKSPACE.LTI_TOOLS.NEW_CONFIG', 'add', () => {
      this.onClose.emit();
      this.onCreate.emit({type:MdsComponent.TYPE_TOOLINSTANCE,node:this._tool});
    }));
    this.explorerOptions.push(new OptionItem('WORKSPACE.LTI_TOOLS.EDIT_CONFIG','edit',(node:Node)=>{
      this.onEdit.emit(node);
      this.onClose.emit();
    }));
    this.explorerOptions.push(new OptionItem('WORKSPACE.LTI_TOOLS.DELETE_CONFIG','delete',(node:Node)=>this.deleteInfo(node)));
    this.columns.push(new ListItem("NODE",RestConstants.CM_NAME));
  }
  public search(){
    this.loading=true;
    this.toolsApi.getToolInstances(this._tool.ref.id).subscribe((data:NodeList)=>{
      console.log(data);
      this.configs=data.nodes;
      this.loading=false;
    });
  }

  private deleteInfo(node: Node) {
    this.dialogTitle='WORKSPACE.LTI_TOOLS.DELETE_CONFIG_TITLE';
    this.dialogMessage='WORKSPACE.LTI_TOOLS.DELETE_CONFIG_MESSAGE';
    this.dialogButtons=DialogButton.getYesNo(()=>{this.dialogTitle=null;},()=>{
      this.dialogTitle=null;
      this.loading=true;
      this.nodeService.deleteNode(node.ref.id).subscribe(()=>{
        this.search();
      })
    });
  }
}
