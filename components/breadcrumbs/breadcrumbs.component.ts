import {Component, Input, Output, EventEmitter, OnInit} from '@angular/core';
import {UIHelper} from "../../ui-helper"
import {NodeList, Node, RestNodeService, TemporaryStorageService, UIService} from "../../../core-module/core.module";


@Component({
  selector: 'breadcrumbs',
  templateUrl: 'breadcrumbs.component.html',
  styleUrls:['breadcrumbs.component.scss']
})
/**
 * A module that provides breadcrumbs for nodes or collections
 */
export class BreadcrumbsComponent{
  public _breadcrumbsAsNode : Node[] = [];
  public _breadcrumbsAsNodeShort : Node[] = [];
  /**
   * Caption of the home, if not set, an icon is used
   */
  @Input() home : string;
  /**
   * Attach a clickable class so the user cursor will be a hand
   * @type {boolean}
   */
  @Input() clickable = true;
  /**
   * Show a short variant (only the last item)
   * auto (default) decides via media query
   * Also possible: never, always
   */
  @Input() short = 'auto';
  /**
   * Allow Dropping of other items (nodes) on to the breadcrumb items
   * A function that should return true or false and gets the same argument object as the onDrop callback
   * @type {boolean}
   */
  @Input() canDrop:Function = ()=>{return false};
  private mainParents: Node[];

  /**
   * Set the id of the parent where all sub-nodes are currently in, e.g. SHARED_FILES
   * @param homeId
   */
  @Input() set homeId(homeId : string){
    if(!homeId)
      return;
    this.node.getChildren(homeId).subscribe((data:NodeList)=>{
      this.mainParents=data.nodes;
      this.findMainParent();
    })
  }

  /**
   * Called when an item is dropped on the breadcrumbs
   *
   * @type {EventEmitter<any>}
   */
  @Output() onDrop=new EventEmitter();

  private dragHover:Node;
  private _searchQuery : string;
  private isBuilding = false;

  /**
   * Set a search query so the breadcrumbs will show this query
   * @param searchQuery
   */
  @Input() set searchQuery(searchQuery : string){
    this._searchQuery=searchQuery;
    this.addSearch();
  }
  @Output() onClick=new EventEmitter();
  private _breadcrumbsAsId : string[];

  /**
   * Set the breadcrumb list as a @Node array
   * @param nodes
   */
  @Input() set breadcrumbsAsNode(nodes : Node[]){
    if(nodes==null)
      return;
    this._breadcrumbsAsNode=nodes;
    this.findMainParent();
    this.generateShort();
  }
  /**
   * Set the breadcrumb main id
   * The breadcrumb nodes will get async resolved via api
   * @param nodes
   */
  @Input() set breadcrumbsForId(id : string){
    if(id==null)
      return;
    this.node.getNodeParents(id).subscribe((nodes)=> {
      this._breadcrumbsAsNode = nodes.nodes.reverse();
      this.findMainParent();
      this.generateShort();
    });
  }

  /*
  @Input() set breadcrumbsAsId(path : string[]){
    if(this.isBuilding)
      return;
    this._breadcrumbsAsId=path.slice(1);
    this._breadcrumbsAsNode=[];
    if(this._breadcrumbsAsId.length<1)
      return;
    //this.loadBreadcrumbNode(0);
    this.node.getNodeParents(this._breadcrumbsAsId[this._breadcrumbsAsId.length-1]).subscribe((data : NodeList) => {
      let list=data.nodes;
      for(let node of list){
        console.log("root "+this._breadcrumbsAsId[0]+" pos "+node.ref.id);
        this._breadcrumbsAsNode.push(node);
        if(node.ref.id==this._breadcrumbsAsId[0])
          break;
      }
      this._breadcrumbsAsNode=this._breadcrumbsAsNode.reverse();
      this.findMainParent();


    });

  }
  */
  private openBreadcrumb(position : number){
    this.onClick.emit(position);
  }

  constructor(private node : RestNodeService,private storage:TemporaryStorageService,private ui:UIService){}
  private allowDrag(event:any,target:Node){
    if(UIHelper.handleAllowDragEvent(this.storage,this.ui,event,target,this.canDrop)) {
      this.dragHover = target;
    }
  }
  private drop(event:any,target:Node){
    this.dragHover=null;
    UIHelper.handleDropEvent(this.storage,this.ui,event,target,this.onDrop);
  }
  private generateShort() {
	  this.addSearch();
    if(this._breadcrumbsAsNode.length<2)
      this._breadcrumbsAsNodeShort=this._breadcrumbsAsNode.slice();
    else
      this._breadcrumbsAsNodeShort=this._breadcrumbsAsNode.slice(this._breadcrumbsAsNode.length-2);
  }

  private addSearch() {
    let add=!(this._breadcrumbsAsNode.length>0 && this._breadcrumbsAsNode[this._breadcrumbsAsNode.length-1] && this._breadcrumbsAsNode[this._breadcrumbsAsNode.length-1].type=="SEARCH");
    if(this._searchQuery){
      let search=new Node();
      search.name="'"+this._searchQuery+"'";
      search.type="SEARCH";
      if(add) {
        this._breadcrumbsAsNode.splice(this._breadcrumbsAsNode.length,0,search);
      }
      else{
        this._breadcrumbsAsNode[this._breadcrumbsAsNode.length-1]=search;
      }
    }
    else if(!add){
      this._breadcrumbsAsNode.splice(this._breadcrumbsAsNode.length,1);
    }
  }
  private parentContains(id:String){
    for(let node of this.mainParents){
      if(node.ref.id==id)
        return true;
    }
    return false;
  }
  private findMainParent() {
    // handled via api
    /*
    if(this.mainParents==null)
      return;
    let i=0;
    for(let node of this._breadcrumbsAsNode){
      if(node && node.ref && this.parentContains(node.ref.id)){
        this._breadcrumbsAsNode=this._breadcrumbsAsNode.slice(i);
        return;
      }
      i++;
    }
    */
  }
}
