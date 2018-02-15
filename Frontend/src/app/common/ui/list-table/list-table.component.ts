import {
  Component, Input, Output, EventEmitter, ChangeDetectionStrategy, ApplicationRef,
  ChangeDetectorRef, ViewChild, ElementRef, HostListener, ViewEncapsulation
} from '@angular/core';
import {BrowserModule, DomSanitizer} from '@angular/platform-browser';
import {TranslateService} from "ng2-translate";
import {Node} from "../../rest/data-object";
import {RestConnectorService} from "../../rest/services/rest-connector.service";
import {RestConstants} from "../../rest/rest-constants";
import {NodeHelper} from "../../ui/node-helper";
import {Translation} from "../../translation";
import {OptionItem} from "../actionbar/actionbar.component";
import {UIAnimation} from "../ui-animation";
import {TemporaryStorageService} from "../../services/temporary-storage.service";
import {Toast} from "../toast";
import {UIService} from "../../services/ui.service";
import {KeyEvents} from "../key-events";
import {FrameEventsService,EventListener} from "../../services/frame-events.service";
import {ConfigurationService} from "../../services/configuration.service";
import {RestHelper} from "../../rest/rest-helper";
import {trigger} from "@angular/animations";
import {ListItem} from "../list-item";

@Component({
  selector: 'listTable',
  templateUrl: 'list-table.component.html',
  styleUrls: ['list-table.component.scss'],
  animations: [
    trigger('openOverlay', UIAnimation.openOverlay(UIAnimation.ANIMATION_TIME_FAST)),
    trigger('openOverlayBottom', UIAnimation.openOverlayBottom(UIAnimation.ANIMATION_TIME_FAST))
  ],
  // Causes action menu not to align properly
  changeDetection: ChangeDetectionStrategy.OnPush
})
/**
 * A provider to render multiple Nodes as a list
 */
export class ListTableComponent implements EventListener{
  public static VIEW_TYPE_LIST = 0;
  public static VIEW_TYPE_GRID = 1;
  public static VIEW_TYPE_GRID_SMALL = 2;
  @ViewChild('drag') drag : ElementRef;


  private optionsAlways:OptionItem[]=[];

  private _nodes : Node[];
  private lastScroll: number;
  private static MIN_SCROLL_TIME=1000;
  private appleCmd=false;
  public shiftCmd = false;

  /**
   * Set the current list of nodes to render
   * @param nodes
   */
  @Input() set nodes(nodes : Node[]){
    this._nodes=nodes;
    /*
     if(!nodes || !nodes.length){
     this.selectedNodes=[];
     }
     */
  };

  /**
   * Set the columns to show, see @ListItem
   */
  private columnsOriginal : ListItem[];
  private columnsAll : ListItem[];
  private columnsVisible : ListItem[];
  @Input() set columns (columns : ListItem[]){
    this.columnsOriginal=JSON.parse(JSON.stringify(columns));
    this.columnsAll=columns;
    this.columnsVisible=[];
    for(let col of columns){
      if(col.visible)
        this.columnsVisible.push(col);
    }
    this.changes.detectChanges();
  }

  private _options : OptionItem[];
  /**
   * Set the options which are valid for each node, similar to the action bar options, see @OptionItem
   * @param options
   */
  @Input() set options(options : OptionItem[]){
    options=OptionItem.filterValidOptions(this.ui,options);
    this._options=[];
    if(!options)
      return;
    for(let o of options){
      if(!o.isToggle)
        this._options.push(o);
    }
    this.optionsAlways=[];
    for(let option of options){
      if(option.showAlways)
        this.optionsAlways.push(option);
    }
    console.log(this.optionsAlways);
  }

  /**
   * Shall an icon be shown?
   */
  @Input() hasIcon : boolean;
  /**
   * is it possible to load more items? (Otherwise, the icon to laod more is hidden)
   */
  @Input() hasMore : boolean;
  /**
   * Use this material icon (only applicable if it's not a node but a group or user)
   */
  @Input() icon : string;
  /**
   * Are checkboxes visible (if disabled, only single select)
   */
  @Input() hasCheckbox : boolean;
  /**
   * Is a heading in table mode shown (when disabled, no sorting possible)
   * @type {boolean}
   */
  @Input() hasHeading : boolean = true;
  /**
   * Can an individual item be clicked
   */
  @Input() isClickable : boolean;
  /**
   *  a custom function to validate if a given node has permissions or should be displayed as "disabled"
   *  Function will get the node object as a parameter and should return
   *  {status:boolean, message:string}
   */
  @Input() validatePermissions : Function;
  /**
   * Hint that the "apply node" mode is active (when reurl is used)
   */
  @Input() applyMode = false;
  /**
   * Mark/Select the row when clicking on it, isClickable must be set to true in this case
   * @type {boolean}
   */
  @Input() selectOnClick = false;
  /**
   * Hint that the parent is currently fetching nodes, in this case the view will show a loading information
   */
  @Input() isLoading : boolean;
  /**
   * The View Type, either VIEW_TYPE_LIST, VIEW_TYPE_GRID_SMALL or VIEW_TYPE_GRID, can be changed on the fly
   * @type {number}
   */
  @Input() viewType = ListTableComponent.VIEW_TYPE_LIST;
  /**
   * Are drag and drop events allowed
   * @type {boolean}
   */
  @Input() dragDrop = false;
  /**
   * Is reordering of columns via settings menu allowed
   * @type {Array}
   */
  @Input() reorderColumns = false;
  /**
   * Info to the component which nodes should currently be selected
   * @type {Array}
   */
  @Input() selectedNodes : Node[] = [];
  /**
   * Select the property to sort the list by, must be a name included in your columns
   */
  @Input() sortBy : string;
  /**
   * Sort ascending or descending
   * @type {boolean}
   */
  @Input() sortAscending=true;
  /**
   *  For Infinite Scroll, when false, also does reload when scrolling inside a div
   * @type {boolean}
   */
  @Input() scrollWindow=true;
  /**
   * For global css styling, the css class name
   * @type {string}
   */
  @Input() listClass="list";

  // Callbacks

  /**
   * Called when the user scrolled the list and it should load more data
   */
  @Output() loadMore = new EventEmitter();
  /**
   * Called when the user changed sort order, emits an object {sortBy:<property>,sortAscending:boolean}
   * @type {EventEmitter}
   */
  @Output() sortListener = new EventEmitter();
  /**
   * Called when the user clicks on a row, emits an object from the list (usually a node, but depends how you filled it)
   * @type {EventEmitter}
   */
  @Output() clickRow = new EventEmitter();
  /**
   * Called when the user double clicks on a row, emits an object from the list (usually a node, but depends how you filled it)
   * @type {EventEmitter}
   */
  @Output() doubleClickRow = new EventEmitter();
  /**
   * Called when the selection has changed, emits an array of objects from the list (usually nodes, but depends how you filled it)
   * @type {EventEmitter}
   */
  @Output() onSelectionChanged = new EventEmitter();
  /**
   * Called when the user opens an overflow menu (right side of the node), and the parent component should invalidate the options (may some are not allowed for this item)
   * @type {EventEmitter}
   */
  @Output() onUpdateOptions = new EventEmitter();
  /**
   * Called when a drop event happened, emits {target:<Node>,source:<Node[]>,event:<any>}
   * @type {EventEmitter}
   */
  @Output() onDrop=new EventEmitter();
  /**
   * Called when the user changed the order of the columns, emits ListItem[]
   * @type {EventEmitter}
   */
  @Output() columnsChanged=new EventEmitter();


  private dragHover : Node;
  private dropdownPosition = "";
  private dropdownLeft : string;
  private dropdownTop : string;
  private dropdownBottom : string;
  @ViewChild('dropdown') dropdownElement : ElementRef;



  public dropdown : Node;
  private id : number;

  public currentDrag : string;
  private currentDragColumn : ListItem;
  public currentDragCount = 0;
  public reorderDialog = false;
  constructor(private ui : UIService,
              private translate : TranslateService,
              private cd: ChangeDetectorRef,
              private config : ConfigurationService,
              private changes : ChangeDetectorRef,
              private storage : TemporaryStorageService,
              private toast : Toast,
              private frame : FrameEventsService,
              private sanitizer: DomSanitizer) {
    this.id=Math.random();
    frame.addListener(this);
  }
  onEvent(event:string,data:any){
    if(event==FrameEventsService.EVENT_PARENT_SCROLL){
      this.scroll();
    }
  }
  @HostListener('document:keyup', ['$event'])
  handleKeyboardEventUp(event: KeyboardEvent) {
    if(event.keyCode==91 || event.keyCode==93)
      this.appleCmd=false;
    if(event.key=='Shift'){
      this.shiftCmd=false;
    }
  }
  @HostListener('document:keydown', ['$event'])
  handleKeyboardEvent(event: KeyboardEvent) {
    if(event.key=='Shift'){
      this.shiftCmd=true;
    }
    if(event.keyCode==91 || event.keyCode==93){
      this.appleCmd=true;
      event.preventDefault();
      event.stopPropagation();
      return;
    }
    if(event.code=="KeyA" && (event.ctrlKey || this.appleCmd) && !KeyEvents.eventFromInputField(event)){
      this.toggleAll();
      event.preventDefault();
      event.stopPropagation();
    }
    if(event.key=="Escape"){
      if(this.reorderDialog) {
        this.closeReorder(false);
        event.preventDefault();
        event.stopPropagation();
      }
    }
  }

  private selectAll() {
    this.selectedNodes=[];
    for(let node of this._nodes){
      this.selectedNodes.push(node);
    }
    this.onSelectionChanged.emit(this.selectedNodes);

  }
  public toggleAll(){
    if(this.selectedNodes.length==this._nodes.length){
      this.selectedNodes=[];
      this.onSelectionChanged.emit(this.selectedNodes);
    }
    else{
      this.selectAll();
    }
  }
  private allowDrag(event:any,target:Node){

    if(!target.isDirectory || !this.storage.get("list_drag")) {
      return;
    }
    if(event.altKey)
      event.dataTransfer.dropEffect='link';
    if(event.ctrlKey)
      event.dataTransfer.dropEffect='copy';
    //if(event.dataTransfer.getData("node"))
    event.preventDefault();
    event.stopPropagation();
    this.dragHover=target;
  }
  private noPermissions(node:any){
    return this.validatePermissions!=null && this.validatePermissions(node).status==false;
  }
  private closeReorder(save:boolean){
    this.reorderDialog=false;
    if(save) {
      this.columns = this.columnsAll;
      this.columnsChanged.emit(this.columnsAll);
    }
    else{
      this.columns=this.columnsOriginal;
    }
  }
  private allowDragColumn(event:any,index:number,target:ListItem){
    if(!this.reorderColumns || index==0 || !this.currentDragColumn)
      return;
    event.preventDefault();
    event.stopPropagation();
    if(this.currentDragColumn==target) {
      return;
    }
    let posOld=this.columnsAll.indexOf(this.currentDragColumn);
    let posNew=this.columnsAll.indexOf(target);
    let old=this.columnsAll[posOld];
    this.columnsAll[posOld]=this.columnsAll[posNew];
    this.columnsAll[posNew]=old;
  }
  private dropColumn(event:any,index:number,target:ListItem){
    if(!this.reorderColumns || index==0)
      return;
    this.currentDragColumn=null;
  }
  private allowDeleteColumn(event:any){
    if(!this.reorderColumns || !this.currentDragColumn)
      return;
    event.preventDefault();
    event.stopPropagation();
  }
  private deleteColumn(event:any){
    if(!this.currentDragColumn)
      return;
    event.preventDefault();
    event.stopPropagation();
    this.columnsAll[this.columnsAll.indexOf(this.currentDragColumn)].visible=false;
    this.columns=this.columnsAll;
    this.currentDragColumn=null;
  }
  private drop(event:any,target:Node){
    this.dragHover=null;
    this.storage.remove("list_drag");
    if(!event.dataTransfer.getData("node"))
      return;
    let data=(JSON.parse(event.dataTransfer.getData("node")) as Node[]);
    event.preventDefault();
    event.stopPropagation();
    if(!data) {
      return;
    }
    this.onDrop.emit({target:target,source:data,event:event});
  }

  private dragStart(event:any,node : Node){
    if(!this.dragDrop)
      return;
    if(this.getSelectedPos(node)==-1)
      this.selectedNodes.push(node);
    let nodes=this.selectedNodes.length ? this.selectedNodes : [node];
    event.dataTransfer.setData("node",JSON.stringify(nodes));
    event.dataTransfer.effectAllowed = 'all';
    this.currentDrag=node.name;
    this.currentDragCount=this.selectedNodes.length ? this.selectedNodes.length : 1;
    event.dataTransfer.setDragImage(this.drag.nativeElement,100,20);
    this.storage.set("list_drag",nodes);
    this.onSelectionChanged.emit(this.selectedNodes);
  }
  private dragStartColumn(event:any,index:number,column : ListItem){
    if(!this.allowDragColumn || index==0)
      return;
    event.dataTransfer.setData("column",index);
    event.dataTransfer.effectAllowed = 'all';
    this.currentDragColumn=column;
  }

  private clickRowSender(node : Node){
    this.clickRow.emit(node);
  }
  private canBeSorted(sortBy : string){
    return RestConstants.POSSIBLE_SORT_BY_FIELDS.indexOf(sortBy)!=-1;
  }
  private setSorting(sortBy : string){
    if(!this.canBeSorted(sortBy))
      return;
    let sortAscending=true;
    if(sortBy==this.sortBy)
      sortAscending=!this.sortAscending;

    this.sortListener.emit({sortBy: sortBy,sortAscending: sortAscending });
  }
  public getTitle(node:Node){
    return RestHelper.getTitle(node);
  }
  private callOption(option : OptionItem,node:Node){
    if(!option.isEnabled || !this.optionIsValid(option,node))
      return;
    option.callback(node);
    this.dropdown=null;
  }
  public scroll(){
    if(Date.now()-this.lastScroll<ListTableComponent.MIN_SCROLL_TIME)
      return;
    this.lastScroll=Date.now();
    this.loadMore.emit();
  }
  private contextMenu(event:any,node : Node){
    event.preventDefault();
    event.stopPropagation();

    if(this._options.length<2)
      return;
    this.select(node,false,false);
    this.showDropdown(node);
    this.dropdownPosition="fixed";
    this.dropdownLeft=event.clientX+"px";
    this.dropdownTop=event.clientY+"px";
    //if(event.clientY>window.innerHeight/2){
    let interval=setInterval(()=>{
      if(!this.dropdownElement || !this.dropdownElement.nativeElement)
        return;
      let y=this.dropdownElement.nativeElement.getBoundingClientRect().bottom;
      if(y>window.innerHeight){
        //this.dropdownBottom=window.innerHeight-event.clientY+"px";
        this.dropdownBottom="0";
        this.dropdownTop="auto";
      }
      this.cd.detectChanges();
    },16);
    setTimeout(()=>clearInterval(interval),500);

  }
  private showDropdown(node : Node){
    //if(this._options==null || this._options.length<1)
    //  return;
    this.dropdownPosition="";
    this.dropdownLeft=null;
    this.dropdownTop=null;
    this.dropdownBottom=null;
    if(this.dropdown==node)
      this.dropdown=null;
    else {
      this.dropdown = node;
      this.onUpdateOptions.emit(node);
    }

    /*
    // causes issue when detail metadata panel is open
    let interval=setInterval(()=>{
      if(!this.dropdownElement || !this.dropdownElement.nativeElement)
        return;
      let y=this.dropdownElement.nativeElement.getBoundingClientRect().bottom;
      console.log(y+" "+window.innerHeight);
      if(y>window.innerHeight){
        this.dropdownPosition="fixed";
        this.dropdownBottom="0";
        this.dropdownTop="auto";
      }
      this.cd.detectChanges();
    },16);
    setTimeout(()=>clearInterval(interval),500);
    */
  }
  private doubleClick(node : Node){
    this.doubleClickRow.emit(node);
  }
  private select(node : Node,fromCheckbox : boolean,fireEvent=true){
    if(!fromCheckbox && !this.isClickable)
      return;
    if(!fromCheckbox && !this.selectOnClick && fireEvent){
      this.clickRowSender(node);
      return;
    }

    if(!this.hasCheckbox || !fromCheckbox){ // Single value select
      if(this.selectedNodes.length && this.selectedNodes[0]==node)
        this.selectedNodes=[];
      else
        this.selectedNodes=[node];
      this.onSelectionChanged.emit(this.selectedNodes);
      return;
    }
    var pos=this.getSelectedPos(node);
    // select from-to range via shift key
    if(fromCheckbox && pos==-1 && this.shiftCmd && this.selectedNodes.length==1){
      let pos1=NodeHelper.getNodePositionInArray(node,this._nodes);
      let pos2=NodeHelper.getNodePositionInArray(this.selectedNodes[0],this._nodes);
      let start=pos1<pos2 ? pos1 : pos2;
      let end=pos1<pos2 ? pos2 : pos1;
      console.log("from "+start+" to "+end);
      for(let i=start;i<=end;i++){
        if(this.getSelectedPos(this._nodes[i])==-1)
          this.selectedNodes.push(this._nodes[i]);
      }
    }
    else {
      if (pos != -1)
        this.selectedNodes.splice(pos, 1);
      else {
        this.selectedNodes.push(node);
      }
    }
    this.onSelectionChanged.emit(this.selectedNodes);
    this.changes.detectChanges();


  }
  private getAttribute(data : any,item : ListItem) : string{
    return NodeHelper.getAttribute(this.translate,this.config,data,item);
  }
  private getLRMIAttribute(data : any,item : ListItem) : string{
    return NodeHelper.getLRMIAttribute(this.translate,this.config,data,item);
  }
  private getLRMIProperty(data : any,item : ListItem) : string{
    return NodeHelper.getLRMIProperty(data,item);
  }
  private getSelectedPos(selected : Node) : number{
    if(!this.selectedNodes)
      return -1;
    return NodeHelper.getNodePositionInArray(selected,this.selectedNodes);
  }
  private optionIsValid(optionItem: OptionItem, node: Node) {
    if(optionItem.enabledCallback) {
      return optionItem.enabledCallback(node);
    }
    return optionItem.isEnabled;
  }
  private optionIsShown(optionItem: OptionItem, node: Node) {
    if(optionItem.showCallback) {
      return optionItem.showCallback(node);
    }
    return true;
  }
  public askCCPublish(event:any,node : Node){
    let mail=node.createdBy.firstName+" "+node.createdBy.lastName+"<"+node.createdBy.mailbox+">";
    let subject=this.translate.instant('ASK_CC_PUBLISH_SUBJECT',{name:RestHelper.getTitle(node)});
    window.location.href="mailto:"+mail+"?subject="+encodeURIComponent(subject);
    event.preventDefault();
    event.stopPropagation();
  }
  public getItemCssClass(item:ListItem){
    return item.type.toLowerCase()+"_"+item.name.replace(":","_");
  }
}
