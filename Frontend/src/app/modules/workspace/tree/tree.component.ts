import {Component, Input, Output, EventEmitter} from '@angular/core';
import {NodeList,Node} from "../../../common/rest/data-object";
import {RestNodeService} from "../../../common/rest/services/rest-node.service";
import {OptionItem} from "../../../common/ui/actionbar/option-item";
import {TemporaryStorageService} from "../../../common/services/temporary-storage.service";
import {RestConnectorService} from "../../../common/rest/services/rest-connector.service";
import {RestConstants} from "../../../common/rest/rest-constants";
import {Helper} from "../../../common/helper";

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
  @Input() set path (path : Node[]){
    if(path.length==0)
      return;
    this._path[0]=[];

    for (let node of path) {
        if (node && node.ref)
          this._path[0].push(node.ref.id);
    }
    this._selectedPath=this._path[0];
  }

  @Input() set current (current : Node){
    // TODO: Using this fixes bug for AddDirectory, but constantly refreshes
    //this.homeDirectory=new String(this.homeDirectory);
    if(!current)
      return;
    this._current=current.ref.id;
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
  public supportsWorkflow(){
    return this.connector.getApiVersion()>=RestConstants.API_VERSION_4_0;
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
    this.dragHover=null;
    if(target==this.RECYCLE) {
      this.onDeleteNodes.emit(this.storage.get(TemporaryStorageService.LIST_DRAG_DATA).nodes);
    }
  }
  constructor(private node:RestNodeService,
              private connector:RestConnectorService,
    private storage:TemporaryStorageService){}
}

