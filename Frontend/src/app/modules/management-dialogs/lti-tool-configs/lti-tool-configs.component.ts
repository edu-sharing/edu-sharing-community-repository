import {Component, Input, EventEmitter, Output, ViewChild, ElementRef} from '@angular/core';
import {DialogButton, ListItem, RestNodeService} from "../../../core-module/core.module";
import {RestConstants} from "../../../core-module/core.module";
import {NodeWrapper,NodeList, Node, IamUsers, WorkflowEntry} from "../../../core-module/core.module";
import {Toast} from "../../../core-ui-module/toast";
import {TranslateService} from "@ngx-translate/core";
import {MdsComponent} from "../../../features/mds/legacy/mds/mds.component";
import {RestToolService} from "../../../core-module/core.module";
import {CustomOptions, OptionItem} from "../../../core-ui-module/option-item";
import { MdsType } from '../../../features/mds/types/types';

@Component({
  selector: 'es-workspace-lti-tool-configs',
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
  public explorerOptions: CustomOptions={
    useDefaultOptions: false
  };
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
      this.onCreate.emit({ type: MdsType.ToolInstance, node:this._tool });
    }));
    this.explorerOptions.addOptions.push(new OptionItem('WORKSPACE.LTI_TOOLS.EDIT_CONFIG','edit',(node:Node)=>{
      this.onEdit.emit(node);
      this.onClose.emit();
    }));
    this.explorerOptions.addOptions.push(new OptionItem('WORKSPACE.LTI_TOOLS.DELETE_CONFIG','delete',(node:Node)=>this.deleteInfo(node)));
    this.columns.push(new ListItem("NODE",RestConstants.CM_NAME));
  }
  public search(){
    this.loading=true;
    this.toolsApi.getToolInstances(this._tool.ref.id).subscribe((data:NodeList)=>{
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
