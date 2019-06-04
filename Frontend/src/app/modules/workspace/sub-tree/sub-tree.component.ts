import {Component, Input, EventEmitter, Output} from '@angular/core';
import {RestNodeService} from '../../../core-module/core.module';
import {TranslateService} from '@ngx-translate/core';
import {Translation} from '../../../common/translation';
import {RestConstants} from '../../../core-module/core.module';
import {Node,NodeList} from '../../../core-module/core.module';
import {TemporaryStorageService} from '../../../core-module/core.module';
import {OptionItem} from '../../../common/ui/actionbar/option-item';
import {UIService} from '../../../core-module/core.module';
import {UIAnimation} from '../../../common/ui/ui-animation';
import {trigger} from '@angular/animations';
import {Helper} from '../../../core-module/rest/helper';
import {UIHelper} from "../../../common/ui/ui-helper";

@Component({
  selector: 'workspace-sub-tree',
  templateUrl: 'sub-tree.component.html',
  styleUrls: ['sub-tree.component.scss'],
  animations: [
    trigger('openOverlay', UIAnimation.openOverlay(UIAnimation.ANIMATION_TIME_FAST)),
    trigger('open', UIAnimation.openOverlay()),
  ]
})
export class WorkspaceSubTreeComponent  {
  public _node : string;
  public loading = true;
  public _nodes : Node[];
  private dragHover : Node;

  private _options : OptionItem[];
  private loadingStates:boolean[] = [];
  private dropdownPosition: string;
  private dropdownLeft: string;
  private dropdownBottom: string;
  private dropdownTop: string;
  private dropdown: Node;
  public _hasChilds:boolean[]=[];
  /**
   * Set the options which are valid for each node, similar to the action bar options, see @OptionItem
   * @param options
   */
  @Input() set options(options : OptionItem[]){
    options=UIHelper.filterValidOptions(this.ui,options);
    options=UIHelper.filterToggleOptions(options,false);
    this._options=options;
  }
  @Input() openPath : string[][]=[];
  @Input() set reload(reload:Boolean){
    if(reload){
      this.refresh();
    }
  }
  @Input() selectedPath : string[]=[];
  @Input() parentPath : string[]=[];
  @Input() depth = 1;
  @Input() selectedNode:string;
  @Output() onClick = new EventEmitter();
  @Output() onToggleTree = new EventEmitter();
  @Output() onLoading = new EventEmitter();
  @Output() onDrop = new EventEmitter();
  @Output() hasChilds = new EventEmitter();
  @Output() onUpdateOptions = new EventEmitter();
  @Input() set node(node : string){
    this._node=node;
    if(node==null) {
        return;
    }
    this.refresh();
  }
  constructor(private ui : UIService,private nodeApi : RestNodeService,private storage : TemporaryStorageService) {
  }
  setLoadingState(state:boolean,pos:number){
    this.loadingStates[pos]=state;
  }
  private contextMenu(event:any,node : Node){
    event.preventDefault();
    event.stopPropagation();

    if(!this._options.length)
      return;
    this.showDropdown(node);
    this.dropdownPosition='fixed';
    //this.dropdownLeft=Math.min(100,event.clientX)+"px";
    if(event.clientY>window.innerHeight/2){
      this.dropdownBottom=window.innerHeight-event.clientY+'px';
      this.dropdownTop='auto';
    }
    else{
      this.dropdownTop=event.clientY+'px';
    }
  }
  private callOption(option : OptionItem,node:Node){
    if(!option.isEnabled)
      return;
    option.callback(node);
    this.dropdown=null;
  }
  public optionIsShown(optionItem: OptionItem, node: Node) {
    if(optionItem.showCallback) {
      return optionItem.showCallback(node);
    }
    return true;
  }
  private updateOptions(event: Node){
    this.onUpdateOptions.emit(event);
  }
  private showDropdown(node : Node){
    //if(this._options==null || this._options.length<1)
    //  return;
    this.dropdownPosition='';
    this.dropdownLeft=null;
    this.dropdownTop=null;
    this.dropdownBottom=null;
    if(this.dropdown==node)
      this.dropdown=null;
    else {
      this.dropdown = node;
      this.onUpdateOptions.emit(node);
    }
  }
  private allowDrop(event : any,target:Node){
    if(!this.storage.get(TemporaryStorageService.LIST_DRAG_DATA)) {
      return;
    }
    if(event.altKey)
      event.dataTransfer.dropEffect='link';
    if(event.ctrlKey)
      event.dataTransfer.dropEffect='copy';
    event.preventDefault();
    event.stopPropagation();
    this.dragHover=target;
  }
  private dropToParent(event : any){
    this.onDrop.emit(event);
  }
  private dropEvent(event : any,target : Node){
    this.dragHover=null;
    this.storage.remove(TemporaryStorageService.LIST_DRAG_DATA);
    if(!event.dataTransfer.getData('text'))
      return;
    let data=(JSON.parse(event.dataTransfer.getData('text')) as Node[]);
    event.preventDefault();
    event.stopPropagation();
    if(!data) {
      return;
    }
    UIHelper.handleDropEvent(this.storage,this.ui,event,target,this.onDrop);
  }
  private isSelected(node : Node){
    return this.selectedNode==node.ref.id || (this.isOpen(node) && this.selectedPath[this.selectedPath.length-1]==node.ref.id && this.selectedNode==null);
  }
  private getFullPath(node : Node) : string[]{
    let path=this.parentPath.slice();
    path.push(node.ref.id);
    return path;

  }
  private openPathEvent(event : string[]) : void{
    this.onClick.emit(event);
  }
  private toggleTreeEvent(event : string[]) : void{
    this.onToggleTree.emit(event);
  }
  private getPathOpen(node:Node){
    for(let i=0;i<this.openPath.length;i++){
      if(this.openPath[i].indexOf(node.ref.id)!=-1)
        return i;
    }
    return -1;
  }
  private isOpen(node : Node) : boolean{
    return this.getPathOpen(node)!=-1;
  }
  private openOrCloseNode(node : Node) : void {
    /*
    let pos = this.openPath.indexOf(node.ref.id);

    let path =  this.parentPath.slice();
    if (pos == -1 || pos!=this.openPath.length-1) {
      path.push(node.ref.id);
    }
    this.onClick.emit(path);
    */
    this.onClick.emit(node);

  }
  private openOrCloseTree(node : Node) : void {
    /*
     let pos = this.openPath.indexOf(node.ref.id);

     let path =  this.parentPath.slice();
     if (pos == -1 || pos!=this.openPath.length-1) {
     path.push(node.ref.id);
     }
     this.onClick.emit(path);
     */
    this.onToggleTree.emit({node:node,parent:this.parentPath});

  }

    private refresh() {
      if(!this._node)
        return;
        this.nodeApi.getChildren(this._node,[RestConstants.FILTER_FOLDERS],{count:RestConstants.COUNT_UNLIMITED}).subscribe((data : NodeList) => {
            this._nodes=data.nodes;
            this.loadingStates=Helper.initArray(this._nodes.length,true);
            this.hasChilds.emit(this._nodes && this._nodes.length);
            this.onLoading.emit(false);
            this.loading=false;
        });
    }
}
