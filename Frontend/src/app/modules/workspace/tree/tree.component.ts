import {Component, Input, Output, EventEmitter} from '@angular/core';
import {NodeList,Node} from "../../../core-module/core.module";
import {RestNodeService} from "../../../core-module/core.module";
import {OptionItem} from "../../../core-ui-module/option-item";
import {TemporaryStorageService} from "../../../core-module/core.module";
import {RestConnectorService} from "../../../core-module/core.module";
import {RestConstants} from "../../../core-module/core.module";
import {Helper} from "../../../core-module/rest/helper";

@Component({
  selector: 'workspace-tree',
  templateUrl: 'tree.component.html',
  styleUrls: ['tree.component.scss']
})
export class WorkspaceTreeComponent  {
  public MY_FILES="MY_FILES";
  public SHARED_FILES="SHARED_FILES";
  public MY_SHARED_FILES="MY_SHARED_FILES";
  public TO_ME_SHARED_FILES="TO_ME_SHARED_FILES";
  public WORKFLOW_RECEIVE="WORKFLOW_RECEIVE";
  public RECYCLE="RECYCLE";

  @Input() root : string;
  @Input() isSafe : boolean;

  private _path : string[][]=[];
  // just for highlighting, does not open nodes!
  private _selectedPath : string[]=[];
  private _current : string;
  public dragHover: string;
  reload: Boolean;
  @Input() selectedNode:string;
  @Input() set path (path : Node[]){
    if(path.length==0) {
        this.reload=new Boolean(true);
        return;
    }
    this._path[0]=[];

    for (let node of path) {
        if (node && node.ref)
          this._path[0].push(node.ref.id);
    }
    this._selectedPath=this._path[0];
  }

  @Input() set current (current : string){
    // TODO: Using this fixes bug for AddDirectory, but constantly refreshes
    //this.homeDirectory=new String(this.homeDirectory);
    if(!current)
      return;
    this._current=current;
  }
  @Input() options : OptionItem[]=[];
  @Output() onOpenNode = new EventEmitter();
  @Output() onUpdateOptions = new EventEmitter();
  @Output() onSetRoot = new EventEmitter();
  @Output() onDrop = new EventEmitter();
  @Output() onDeleteNodes = new EventEmitter();

  private drop(event : any){
    this.onDrop.emit(event);
  }
  private updateOptions(event : Node){
    this.onUpdateOptions.emit(event);
  }
  private openNode(event : Node){
    this._path.splice(1,this._path.length-1);
    this.onOpenNode.emit(event);
  }
  public setRoot(root : string){
    this.onSetRoot.emit(root);
    this._path=[];
    /*
    if(root==this.MY_FILES) {
      this.onOpenPath.emit([this.homeDirectory]) ;
    }
    */
  }
  public toggleTree(event:any){
    console.log(this._path);
    let id=event.node.ref.id;
    console.log(id);
    let create=true;
    for(let i=0;i<this._path.length;i++){
      let pos=this._path[i].indexOf(id);
      if(pos!=-1){
        //this._path[i].splice(pos,this._path[i].length-pos);
        this._path.splice(i,1);
        console.log("close path id "+i);
        create=false;
        i--;
      }
      /*
      if(event.parent.length){
        let pos=this._path[i].indexOf(event.parent[event.parent.length-1]);
      }
      */
    }
    if(create) {
      let path = Helper.deepCopy(event.parent);
      path.push(id);
      this._path.push(path);
    }
    console.log(this._path);
  }
  public allowDrop(event : any,target:string){
    if(!this.storage.get(TemporaryStorageService.LIST_DRAG_DATA)) {
      return;
    }
    event.preventDefault();
    event.stopPropagation();
    this.dragHover=target;
  }
  public dropEvent(event : any,target:string){
    if(!this.storage.get(TemporaryStorageService.LIST_DRAG_DATA)) {
      return;
    }
    event.preventDefault();
    event.stopPropagation();
    this.dragHover=null;
    if(target==this.RECYCLE) {
      this.onDeleteNodes.emit(this.storage.get(TemporaryStorageService.LIST_DRAG_DATA).nodes);
    }
  }
  constructor(private node:RestNodeService,
              private connector:RestConnectorService,
    private storage:TemporaryStorageService){}
}

