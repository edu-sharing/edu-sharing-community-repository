import {
  Component, Input, Output, EventEmitter, ChangeDetectionStrategy, ApplicationRef,
  ChangeDetectorRef, ViewChild, ElementRef, HostListener, ViewEncapsulation, ContentChild, TemplateRef
} from '@angular/core';
import {BrowserModule, DomSanitizer, SafeHtml} from '@angular/platform-browser';
import {TranslateService} from "@ngx-translate/core";
import {NodeHelper} from "../../node-helper";
import {UIAnimation} from "../../../core-module/ui/ui-animation";
import {OptionItem} from "../../option-item";
import {Toast} from "../../toast";
import {KeyEvents} from "../../../core-module/ui/key-events";
import {animate, sequence, style, transition, trigger} from "@angular/animations";
import {UIHelper} from "../../ui-helper";
import {Helper} from "../../../core-module/rest/helper";
import {ColorHelper} from '../../../core-module/ui/color-helper';
import {UIConstants} from "../../../core-module/ui/ui-constants";
import {
    ConfigurationService,
    FrameEventsService, EventListener,
    ListItem, NetworkRepositories,
    Repository, Node,
    RestConstants, RestHelper, RestLocatorService, RestNetworkService,
    TemporaryStorageService, UIService
} from '../../../core-module/core.module';
import {AddElement} from "../../add-element";
import {MatMenuTrigger} from "@angular/material";

@Component({
  selector: 'listTable',
  templateUrl: 'list-table.component.html',
  styleUrls: ['list-table.component.scss'],
  animations: [
    trigger('openOverlay', UIAnimation.openOverlay(UIAnimation.ANIMATION_TIME_FAST)),
    trigger('openOverlayBottom', UIAnimation.openOverlayBottom(UIAnimation.ANIMATION_TIME_FAST)),
    trigger('orderAnimation', [
        transition(':enter', [
          sequence([
            animate(UIAnimation.ANIMATION_TIME_SLOW+"ms ease", style({ opacity: 0 }))
          ])
        ]),
        transition(':leave', [
          sequence([
            animate(UIAnimation.ANIMATION_TIME_SLOW+"ms ease", style({ opacity: 1 }))
          ])
        ])
      ])
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
  @ViewChild('menuTrigger') menuTrigger : MatMenuTrigger;
  @ViewChild('addElementRef') addElementRef : ElementRef;

  @ContentChild('itemContent') itemContentRef: TemplateRef<any>;


  private optionsAlways:OptionItem[]=[];

  private _nodes : any[];
  private animateNode: Node;
  private repositories: Repository[];
  private sortMenu = false;

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

  @Output() nodesChange=new EventEmitter();

  /**
   * Set the columns to show, see @ListItem
   */
  private columnsOriginal : ListItem[];
  private columnsAll : ListItem[];
  private columnsVisible : ListItem[];
  @Input() set columns (columns : ListItem[]){
    this.columnsOriginal=Helper.deepCopy(columns);
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
    options=UIHelper.filterValidOptions(this.ui,options);
    if(this.selectedNodes && this.selectedNodes.length==1)
      options=this.filterCallbacks(options,this.selectedNodes[0]);
    console.log(options);
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
  }

  /**
   * Shall an icon be shown?
   */
  @Input() hasIcon : boolean;
  /**
   * total item count, when used, the header of the table will display it
   */
  @Input() totalCount : number;
  /**
   * is it possible to load more items? (Otherwise, the icon to load more is hidden)
   */
  @Input() hasMore : boolean;
  /**
   * Use this material icon (only applicable if it's not a node but a group or user)
   */
  @Input() icon : string;
  /**
   * Are checkboxes visible (if disabled, only single select)
   */
  /**
   * If not null, shows a "Add Element" option as a first element (used for collections)
   * The AddElement defines label, icon and other details
   * The event onAddElement will be called when the user selects this element
   */
  @Input() addElement : AddElement;

  private _hasCheckbox:boolean;
  @Input() set hasCheckbox(hasCheckbox : boolean){
    this._hasCheckbox=hasCheckbox;
    if(!hasCheckbox){
      // use a timeout to prevent a ExpressionChangedAfterItHasBeenCheckedError in the parent component
      setTimeout(()=> {
          this.selectedNodes = [];
          this.onSelectionChanged.emit([]);
      });
    }
  }
  get hasCheckbox(){
    return this._hasCheckbox;
  }
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
   *  {status:boolean, message:string[,button:{click:function,caption:string,icon:string]}
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
   * Can the content be re-ordered via drag and drop? (requires dragDrop to be enabled)
   * onOrderElements will be emitted containing the new array of items as they are sorted
   * @type {boolean}
   */
  @Input() orderElements = false;
  /**
   * May changes when the user starts ordering elements. Disable it to stop the order animation
   * @type {boolean}
   */
  @Input() orderElementsActive = false;
  @Output() orderElementsActiveChange = new EventEmitter();
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
     * set the allowed list of possible sort by fields
     */
  @Input() possibleSortByFields = RestConstants.POSSIBLE_SORT_BY_FIELDS;
    /**
     * Show the sort by dialog when sort is triggered in mobile view
     */
    @Input() sortByMobile = true;
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
  /**
   * Should the list table fetch the repo list (useful to detect remote repo nodes)
   * Must be set at initial loading!
   * @type {boolean}
   */
  @Input() loadRepositories=true;
  /**
   *  For collection elements only, tells if the current user can delete the given item
   *  Function should return a boolean
   * @type {boolean}
   */
  @Input() canDelete:Function
  /**
   *  Can an element be dropped on the element
   *  Called with same parameters as onDrop event
   */
  @Input() canDrop:Function=()=>{return true};
  /**
   *  Prevent key events (like when the parent has open windows)
   */
  @Input() preventKeyevents=false;

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
   * Called when the user clicks on a row, emits the following: {
   *        node: <clicked object from list, depends on what you filled in>,
   *        source: <source click information, may null, e.g. 'preview', 'comments', 'dropdown'>
   *     }
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
   * Called when a drop event happened, emits {target:<Node>,source:<Node[]>,event:<any>,type:'default'|'copy'}
   * @type {EventEmitter}
   */
  @Output() onDrop=new EventEmitter();
  /**
   * Called when the user changed the order of the columns, emits ListItem[]
   * @type {EventEmitter}
   */
  @Output() columnsChanged=new EventEmitter();
  /**
   * Called when the user clicked the "addElement" item
   * @type {EventEmitter}
   */
  @Output() onAddElement=new EventEmitter();
  /**
   * Called when the user clicked the delete for a missing reference object
   * @type {EventEmitter}
   */
  @Output() onDelete=new EventEmitter();
  /**
   * Called when the user performed a custom order of items
   * @type {EventEmitter}
   */
  @Output() onOrderElements=new EventEmitter();

  private dragHover : Node;
  private dropdownLeft : string;
  private dropdownTop : string;
  @ViewChild('dropdown') dropdownElement : ElementRef;
  @ViewChild('dropdownContainer') dropdownContainerElement : ElementRef;

  public id : number;

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
              private network : RestNetworkService,
              private locator : RestLocatorService,
              private toast : Toast,
              private frame : FrameEventsService,
              private sanitizer: DomSanitizer) {
    this.id=Math.random();
    frame.addListener(this);
    setTimeout(()=>this.loadRepos());
  }
  private filterCallbacks(options: OptionItem[],node:Node) {
      return options.filter((option)=>!option.showCallback || option.showCallback(node));
  }
  loadRepos(){
    if(!this.loadRepositories)
      return;
    this.locator.locateApi().subscribe(()=>{
        this.network.getRepositories().subscribe((data:NetworkRepositories)=>{
          this.repositories=data.repositories;
          this.cd.detectChanges();
        });
    });
  }
  onEvent(event:string,data:any){
    if(event==FrameEventsService.EVENT_PARENT_SCROLL){
      this.scroll(false);
    }
  }
  //@HostListener('document:keydown', ['$event'])
  handleKeyboardEvent(event: KeyboardEvent) {
    if(event.code=="KeyA" && (event.ctrlKey || this.ui.isAppleCmd()) && !KeyEvents.eventFromInputField(event) && !this.preventKeyevents){
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
  public delete(node:any){
    this.onDelete.emit(node);
  }
    public isBrightColorCollection(color : string){
        if(!color)
          return true;
        return ColorHelper.getColorBrightness(color)>ColorHelper.BRIGHTNESS_THRESHOLD_COLLECTIONS;
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
  private move(array:any,i1:number,i2:number){
    //let i1=RestHelper.getRestObjectPositionInArray(node1,this._nodes);
    //let i2=RestHelper.getRestObjectPositionInArray(node2,this._nodes);
    let node1=array[i1];
    let node2=array[i2];
    array.splice(i1,1);
    array.splice(i2,0,node1);
  }
  private allowDrag(event:any,target:Node){
    if(this.orderElements){
      event.preventDefault();
      let source=this.storage.get(TemporaryStorageService.LIST_DRAG_DATA);
      if(source.view==this.id && source.node.ref.id!=target.ref.id){
        this.orderElementsActive=true;
        this.orderElementsActiveChange.emit(true);
        let targetPos=this._nodes.indexOf(target);
        this._nodes=Helper.deepCopy(source.list);
        this.move(this._nodes,source.offset,targetPos);
        // inform the outer component's variable about the new order
        this.nodesChange.emit(this._nodes);
        return;
      }
    }
    if(UIHelper.handleAllowDragEvent(this.storage,this.ui,event,target,this.canDrop)) {
      event.preventDefault();
      this.dragHover = target;
    }
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
    event.preventDefault();
    event.stopPropagation();
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
    if(this.orderElements){
      let source=this.storage.get(TemporaryStorageService.LIST_DRAG_DATA);
      if(source.view==this.id && source.nodes.length==1){
        this.onOrderElements.emit(this._nodes);
        event.preventDefault();
        event.stopPropagation();
        return;
      }
    }
    UIHelper.handleDropEvent(this.storage,this.ui,event,target,this.onDrop);
  }
  private animateIcon(node:Node,animate:boolean){
    if(animate){
      if(NodeHelper.hasAnimatedPreview(node)){
        this.animateNode=node;
      }
    }
    else {
      this.animateNode = null;
    }
  }
  private dragStart(event:any,node : Node){

    if(!this.dragDrop)
      return;
    if(this.getSelectedPos(node)==-1) {
      if(this.hasCheckbox)
        this.selectedNodes.push(node);
      else
        this.selectedNodes=[node];
    }
    let nodes=this.selectedNodes.length ? this.selectedNodes : [node];

    event.dataTransfer.setData("text",JSON.stringify(nodes));
    event.dataTransfer.effectAllowed = 'all';
    let name="";
    for(let node of nodes){
      if(name)
        name+=", ";
      name+=RestHelper.getName(node);
    }
    this.currentDrag=name;
    this.currentDragCount=this.selectedNodes.length ? this.selectedNodes.length : 1;
    try {
      event.dataTransfer.setDragImage(this.drag.nativeElement, 100, 20);
    }catch(e){}
    this.storage.set(TemporaryStorageService.LIST_DRAG_DATA,{
      list:Helper.deepCopy(this._nodes),
        offset:this._nodes.indexOf(node),
        node:node,
        nodes:nodes,
        view:this.id
    });
    this.onSelectionChanged.emit(this.selectedNodes);
  }
  private dragStartColumn(event:any,index:number,column : ListItem){
    if(!this.allowDragColumn || index==0)
      return;
    event.dataTransfer.setData("text",index);
    event.dataTransfer.effectAllowed = 'all';
    this.currentDragColumn=column;
  }

  private clickRowSender(node : Node,source:string){
    this.clickRow.emit({node:node,source:source});
  }
  private canBeSorted(sortBy : any){
    return this.possibleSortByFields && this.possibleSortByFields.filter((p)=>p.name==sortBy.name).length;
  }
  private getSortableColumns(){
    let result:ListItem[]=[];
    for(let col of this.columnsAll){
      if(this.canBeSorted(col))
        result.push(col);
    }
    return result;
  }
  private setSortingIntern(sortBy : ListItem,isPrimaryElement : boolean){
    if(isPrimaryElement && window.innerWidth<UIConstants.MOBILE_WIDTH+UIConstants.MOBILE_STAGE*4){
      if(this.sortByMobile)
        this.sortMenu=true;
      return;
    }
    let ascending=this.sortAscending;
    if(this.sortBy==sortBy.name)
        ascending=!ascending;
    (sortBy as any).ascending=ascending;
    this.setSorting(sortBy);
  }
  private setSorting(sortBy : any){
      if(!this.canBeSorted(sortBy))
          return;
      this.sortListener.emit({sortBy: sortBy.name,sortAscending: sortBy.ascending });
  }
  public getTitle(node:Node){
    return RestHelper.getTitle(node);
  }
  private callOption(option : OptionItem,node:Node){
    if(!this.optionIsValid(option,node)) {
      if(option.disabledCallback) {
        option.disabledCallback(node);
      }
        return;
    }
    option.callback(node);
  }
  public scroll(fromUser:boolean){
    if(!fromUser){
      // check if there is a footer
      let elements=document.getElementsByTagName('footer');
      console.log(elements);
      if(elements.length && elements.item(0).innerHTML.trim())
        return;
    }
    this.loadMore.emit();
  }
  private contextMenu(event:any,node : Node){
    event.preventDefault();
    event.stopPropagation();

    if(!this._options || this._options.length<2)
      return;
    this.dropdownLeft=event.clientX+"px";
    this.dropdownTop=event.clientY+"px";
      this.showDropdown(node,true);
  }
  public getCollectionColor(node : any){
    return node.collection ? node.collection.color : node.color;
  }
  public getCollection(node : any){
    return node.collection ? node.collection : node
  }
  private getReference(node: any) {
      return node.reference ? node.reference : node;
  }
  public isHomeNode(node : any){
    // repos not loaded or not availale. assume true so that small images are loaded
    if(!this.repositories)
        return true;
    return RestNetworkService.isFromHomeRepo(node,this.repositories);
  }
  public getOriginalNode(node : any){
    if(node.reference)
      return node.reference;
    return node;
  }
  public getIconUrl(node : any){
    return this.getReference(node).iconURL;
  }
  public isCollection(node : any){
    return node.collection || node.hasOwnProperty('childCollectionsCount');
  }
  public isReference(node : any){
    return node.reference!=null;
  }
  public isDeleted(node:any){
    return this.isReference(node) && !node.originalId;
  }
  private showDropdown(node : Node,openMenu = false,event:any=null){
    //if(this._options==null || this._options.length<1)
    //  return;
    this.select(node,"dropdown",false,false);
    this.onUpdateOptions.emit(node);
    if(openMenu)
        if(event){
            console.log(event);
            if(event.clientX+event.clientY) {
                this.dropdownLeft = event.clientX + "px";
                this.dropdownTop = event.clientY + "px";
            }
            else{
                let rect=event.srcElement.getBoundingClientRect();
                this.dropdownLeft = rect.left+rect.width/2 + "px";
                this.dropdownTop = rect.top+rect.height/2 + "px";
            }
        }
      // short delay to let onUpdateOptions handler run and angular menu get the correct data from start
      setTimeout(()=>this.menuTrigger.openMenu());
      /*
    if(this.dropdown==node)
      this.dropdown=null;
    else {
      this.dropdown = node;
      this.onUpdateOptions.emit(node);
      setTimeout(()=>{
        UIHelper.setFocusOnDropdown(this.dropdownElement);
        UIHelper.scrollSmoothElement(this.dropdownContainerElement.nativeElement.scrollHeight,this.dropdownContainerElement.nativeElement);
      });
    }
    */
  }
  private doubleClick(node : Node){
    this.doubleClickRow.emit(node);
  }
  private select(node : Node,from : string=null,fireEvent=true,unselect=true){
    if(from!="checkbox" && !this.isClickable)
      return;
    if(from!="checkbox" && !this.selectOnClick && fireEvent){
      this.clickRowSender(node,from);
      return;
    }

    if(!this.hasCheckbox || from!="checkbox"){ // Single value select
      if(this.selectedNodes.length && this.selectedNodes[0]==node && unselect)
        this.selectedNodes=[];
      else
        this.selectedNodes=[node];
      this.onSelectionChanged.emit(this.selectedNodes);
      return;
    }
    let pos=this.getSelectedPos(node);
    // select from-to range via shift key
    if(from=="checkbox" && pos==-1 && this.ui.isShiftCmd() && this.selectedNodes.length==1){
      let pos1=RestHelper.getRestObjectPositionInArray(node,this._nodes);
      let pos2=RestHelper.getRestObjectPositionInArray(this.selectedNodes[0],this._nodes);
      let start=pos1<pos2 ? pos1 : pos2;
      let end=pos1<pos2 ? pos2 : pos1;
      for(let i=start;i<=end;i++){
        if(this.getSelectedPos(this._nodes[i])==-1)
          this.selectedNodes.push(this._nodes[i]);
      }
    }
    else {
      if (pos != -1 && unselect)
        this.selectedNodes.splice(pos, 1);
      if (pos == -1) {
        this.selectedNodes.push(node);
      }
    }
    this.onSelectionChanged.emit(this.selectedNodes);
    this.changes.detectChanges();

  }
  private getAttribute(data : any,item : ListItem) : SafeHtml{

    let attribute=NodeHelper.getAttribute(this.translate,this.config,data,item);
    // sanitizer is much slower but required when attributes inject styles, so keep it in these cases
    if(attribute!=null && attribute.indexOf("style=")!=-1){
        return this.sanitizer.bypassSecurityTrustHtml(attribute);
    }
    return attribute;
  }
  private getAttributeText(data : any,item : ListItem) : string{
      return NodeHelper.getAttribute(this.translate,this.config,data,item);
  }
  private getLRMIAttribute(data : any,item : ListItem) : string{
    return NodeHelper.getLRMIAttribute(this.translate,this.config,data,item);
  }
  private getLRMIProperty(data : any,item : ListItem) : string{
    return NodeHelper.getLRMIProperty(data,item);
  }
  private getSelectedPos(selected : Node) : number{
    if(!this.selectedNodes || !this.selectedNodes.length)
      return -1;
    return RestHelper.getRestObjectPositionInArray(selected,this.selectedNodes);
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
  public addElementClicked(){
    this.onAddElement.emit();
  }
  public askCCPublish(event:any,node : Node){
    NodeHelper.askCCPublish(this.translate,node);
    event.stopPropagation();
  }
  public getItemCssClass(item:ListItem){
    return item.type.toLowerCase()+"_"+item.name.toLowerCase().replace(":","_").replace(".","_");
  }

    handleKeyboard(event:any) {
        //will break ng menu. May add a check whether menu is open
        /*
        if(this.viewType==ListTableComponent.VIEW_TYPE_LIST && (event.key=="ArrowUp" || event.key=="ArrowDown")){
            let next=event.key=="ArrowDown";
            let elements:any=document.getElementsByClassName("node-row");
            for(let i=0;i<elements.length;i++){
                let element=elements.item(i);
                if(element==event.srcElement){
                    if(next && i<elements.length-1)
                        elements.item(i+1).focus();
                    if(!next && i>0){
                        elements.item(i-1).focus();
                    }
                }
            }
            event.preventDefault();
            event.stopPropagation();
        }
        */
    }

}
