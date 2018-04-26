import {Component, Input, Output, EventEmitter, OnInit, NgZone, group, HostListener, ViewChild, ElementRef} from '@angular/core';
import {TranslateService} from '@ngx-translate/core';
import {RestMdsService} from '../../rest/services/rest-mds.service';
import {MdsMetadataset, View, Type, Node, NodeList, NodeWrapper, MdsValueList} from '../../rest/data-object';
import {DomSanitizer, SafeHtml} from '@angular/platform-browser';
import {RestNodeService} from '../../rest/services/rest-node.service';
import {RestConstants} from '../../rest/rest-constants';
import {Translation} from '../../translation';
import {HtmlParser} from '@angular/compiler';
import {ActivatedRoute} from '@angular/router';
import {Toast} from '../toast';
import {VCard} from '../../VCard';
import {Helper} from '../../helper';
import {ConfigurationService} from '../../services/configuration.service';
import {SessionStorageService} from '../../services/session-storage.service';
import {RestConnectorService} from '../../rest/services/rest-connector.service';
import {RestToolService} from '../../rest/services/rest-tool.service';
import {UIHelper} from '../ui-helper';
import {RestHelper} from '../../rest/rest-helper';
import {NodeHelper} from '../node-helper';
import {RestLocatorService} from '../../rest/services/rest-locator.service';
import {trigger} from '@angular/animations';
import {UIAnimation} from '../ui-animation';
import {DialogButton} from '../modal-dialog/modal-dialog.component';
import {UIService} from '../../services/ui.service';

@Component({
  selector: 'mds',
  templateUrl: 'mds.component.html',
  styleUrls: ['mds.component.scss'],
  animations: [
    trigger('fade', UIAnimation.fade()),
    trigger('cardAnimation', UIAnimation.cardAnimation())
  ]
})
export class MdsComponent{
  @ViewChild('mdsScrollContainer') mdsScrollContainer: ElementRef;

  @Input() addWidget=false;
  @Input() embedded=false;
  private activeAuthorType: number;
  private jumpmarksCount: number;
  public static TYPE_TOOLDEFINITION = 'tool_definition';
  public static TYPE_TOOLINSTANCE = 'tool_instance';
  public static TYPE_SAVED_SEARCH = 'saved_search';
  private static VCARD_FIELDS=['Surname','Givenname'];
  /**
   * Can the node content be replaced?
   */
  @Input() allowReplacing=true;
  @Input() parentNode:Node;
  private _setId=RestConstants.DEFAULT;
  private _suggestions:any;
  private _groupId: string;
  private _repository=RestConstants.HOME_REPOSITORY;
  private createType: string;
  private _currentValues: any;
  /**
   * Show extended widgets
   */
  @Input() extended=false;
  /**
   * mode, currently "search" or "default"
   * @type {string}
   */
  @Input() mode:string='default';

  @Output() extendedChange=new EventEmitter();
  private static AUTHOR_TYPE_FREETEXT = 0;
  private static AUTHOR_TYPE_PERSON = 1;
  private lastMdsQuery: string;
  dialogTitle: string;
  dialogMessage: string;
  dialogParameters: any;
  dialogButtons: DialogButton[];
  private variables: string[];
  private currentWidgetSuggestion: string;
  private static GROUP_MULTIVALUE_DELIMITER="[+]";
  @Input() set suggestions(suggestions:any){
    this._suggestions=suggestions;
  };
  @Input() set repository(repository:string){
    this.isLoading=false;
    this._repository=repository;
  }
  @Input() set currentValues(currentValues:any){
    this._currentValues=currentValues;
  }
  @Input() set setId(setId:string){
    if(!setId)
      return;
    this._setId=setId;
  }
  @Input() set invalidate(invalidate:Boolean){
    if(invalidate)
      setTimeout(()=>this.loadMds(),5);
  }

  @Input() set groupId(groupId:string){
    this._groupId=groupId;
  }
  private isSearch(){
    return this._groupId!=null;
  }

  private loadMdsFinal(callback:Function=null) {
    if(!this.mds)
      return;
    this.renderGroup(this._groupId,this.mds);
    this.isLoading=false;
    this.setValuesByProperty(this.mds,this._currentValues ? this._currentValues : {});
    this.applySuggestions();
    setTimeout(()=>{
      this.showExtended(this.extended);
      this.onMdsLoaded.emit(this.mds);
      if(callback) callback();
    },5);
  }
  private loadMds(){
    if(this.isLoading) {
      setTimeout(()=>this.loadMds(),5);
      return;
    }
    this.mds=null;
    this.rendered=null;
    this.renderedSuggestions=null;
    this.isLoading=true;
    this.mdsService.getSet(this._setId,this._repository).subscribe((data:any)=>{
      this.locator.getConfigVariables().subscribe((variables:string[])=>{
        this.mds=data;
        this.variables=variables;
        this.loadMdsFinal();
        },(error:any)=>this.toast.error(error));
      },(error:any)=>this.toast.error(error));
  }

  /**
   * Set this to a special type if you want to first create a special node (e.g. a tool definition)
   * @param {string} type
   */
  @Input() set create(type:string){
    this.createType=type;
    this.isLoading=true;
    this.mdsService.getSet().subscribe((data:any)=> {
      this.mds=data;
      this.renderGroup(this.createType, this.mds);
      this.isLoading=false;
    });
  }

  @Input() set nodeId(nodeId:string){
    this.currentNode=null;
    if(nodeId==null)
      return;
    this.isLoading=true;
    this.node.getNodeMetadata(nodeId,[RestConstants.ALL]).subscribe((node : NodeWrapper)=>{
      this._setId = node.node.metadataset ? node.node.metadataset : RestConstants.DEFAULT;
      this.mdsService.getSet(this._setId).subscribe((data:any)=>{
        // test a widget
        //data.widgets.push({caption:'Test',id:'test',type:'range',min:0,max:60});
        //data.views[0].html+="<test>";
        this.locator.getConfigVariables().subscribe((variables:string[])=> {
          this.mds = data;
          this.variables=variables;
          this.currentNode = node.node;
          for (let property in this.currentNode.properties) {
            this.properties.push(property);
          }
          this.properties.sort();
          let nodeGroup = this.currentNode.isDirectory ? 'map' : 'io';
          if (this.currentNode.aspects.indexOf(RestConstants.CCM_ASPECT_TOOL_DEFINITION) != -1) {
            nodeGroup = MdsComponent.TYPE_TOOLDEFINITION;
          }
          if (this.currentNode.type == RestConstants.CCM_TYPE_TOOL_INSTANCE) {
            nodeGroup = MdsComponent.TYPE_TOOLINSTANCE;
          }
          if (this.currentNode.type == RestConstants.CCM_TYPE_SAVED_SEARCH) {
            nodeGroup = MdsComponent.TYPE_SAVED_SEARCH;
          }
          this.renderGroup(nodeGroup, this.mds);
          this.isLoading = false;
        });
      },(error:any)=>{
        this.toast.error(error);
        this.cancel();
      });
    },(error:any)=>{
      this.toast.error(error);
      this.cancel();
    });
  };
  @Output() onCancel=new EventEmitter();
  @Output() onDone=new EventEmitter();
  @Output() openLicense=new EventEmitter();
  @Output() openContributor=new EventEmitter();
  @Output() onMdsLoaded=new EventEmitter();
  private rendered : SafeHtml;
  private renderedSuggestions : SafeHtml;
  private jumpmarks: SafeHtml;
  isLoading = false;

  private widgetName='cclom:general_keyword';
  private widgetType='multivalueFixedBadges';
  private currentNode: Node;
  public globalProgress=false;
  private properties: string[]=[];
  private currentWidgets: any[];
  private mds: any;
  private static MAX_SUGGESTIONS = 5;
  private suggestionsViaSearch = false;
  private resetValues(){
    this._currentValues=null;
    this.loadMdsFinal(()=>{
      this.onDone.emit(null);
    });
  }
  constructor(private mdsService : RestMdsService,
              private translate : TranslateService,
              private route : ActivatedRoute,
              private uiService : UIService,
              private node : RestNodeService,
              private tools : RestToolService,
              private toast : Toast,
              private locator : RestLocatorService,
              private storage : SessionStorageService,
              private connector : RestConnectorService,
              private sanitizer: DomSanitizer,
              private config : ConfigurationService,
              private _ngZone: NgZone) {
      Translation.initialize(this.translate,this.config,this.storage,this.route);
      (window as any)['mdsComponentRef'] = {component: this, zone: _ngZone};
    }

    ngOnDestroy() {
      (window as any).mdsComponentRef = null;
    }
  private openSuggestions(id:string,event:any,allowCustom:boolean,widgetValues:boolean,showMore=false,search=this.suggestionsViaSearch){
    this.suggestionsViaSearch=search;
    let searchField:any=document.getElementById(id+'_suggestionsInput');
    if(allowCustom){
      if(event && event.keyCode==13 && searchField.value!=''){
        let badges=document.getElementById(id);
        let elements:any=badges.childNodes;
        let add=true;
        for(var i=0;i<elements.length;i++){
          if(elements[i].getAttribute('data-value')==searchField.value){
            add=false;
          }
        }
        badges.innerHTML+=this.getMultivalueBadge(searchField.value);
        searchField.value=null;
      }
    }
    let list=document.getElementById(id+'_suggestions');
    list.className=list.className.replace('suggestionListAll','').trim();
    if(showMore){
      list.className+=' suggestionListAll';
    }
    let elements=list.getElementsByTagName('a');
    if(event && event.keyCode==40){
      elements[elements.length>1 ? 1 : 0].focus();
      return;
    }
    let more=elements[elements.length-1];
    more.style.display='none';

    list.style.display='none';

    if(searchField.value.length<2 && search)
      return;
    this.currentWidgetSuggestion=id;
    list.style.display='';
    let hits=0;
    let moreCount=0;
    for(let i=1;i<elements.length-1;i++){
      let element=elements[i];
      element.style.display = 'none';
      let caption=element.getAttribute('data-caption');
      let pos=-1;

      if(caption && search)
        pos=caption.toLowerCase().indexOf(searchField.value.toLowerCase());
      else {
        pos = 0;
      }
      if(pos==-1) {
        continue;
      }
      if(hits>=MdsComponent.MAX_SUGGESTIONS && !showMore){
        moreCount++;
        continue;
      }
      element.style.display=pos>-1 ? '' : 'none';
      element.innerHTML=caption;
      if(search){
        element.innerHTML=this.highlightSearch(caption,searchField.value);
      }
      hits+=(pos>-1) ? 1 : 0;
    }
    if(moreCount){
      more.style.display='';
      more.innerHTML=moreCount+' '+this.translate.instant('MORE_SELECTBOX');
    }
    // Commented part fetches from repo, however this won't work for all mds properly
    if(!widgetValues)
      this.mdsUpdateSuggests(id);
    if(widgetValues /* && !this._groupId */)
      elements[0].style.display=hits || allowCustom ? 'none' : '';
  }
  /*
  private renderTabs(group:any,data:any,node:Node) : string{
    let tabs='<div class="row tab-group"><ul class="tabs">';
    let content='';
    let i=0;

    for(let viewId of group.views){
      for(let view of data.views){
        if(view.id==viewId){
          tabs+=`<li class="tab clickable" id="mds_tab_header_`+i+`" onclick="
                  for(var i=0;true;i++){
                    var element=document.getElementById('mds_tab_'+i);
                    var header=document.getElementById('mds_tab_header_'+i);
                    if(!element)
                      return;
                    var selected=i==`+i+`;
                    element.style.display=selected ? '' : 'none';
                    header.childNodes[0].className=selected ? 'active' : 'none';
                 }
                    
                    "><a class="`+(i==0 ? 'active' : '')+'">'+view.caption+'</a></li>';
          content+='<div id="mds_tab_'+i+'"'+(i>0 ? 'style="display:none;"' : '')+'>'+this.renderTemplate(view,data,null,node)+'</div>';
          i++;
          break;
        }
      }
    }
    tabs+='</ul></div>';
    return tabs+content;
  }
  */
  private renderList(group:any,data:any) : any{
    let content='';
    let i=0;
    let hasExtended=[false];
    let result:any={main:''};
    for(let viewId of group.views){
      let viewFound=false;
      for(let view of data.views){
        if(view.id==viewId) {
          viewFound = true;
          if (!this.embedded && view.caption)
            result.main += `<div class="card-title-element" id="`+view.id+`_header"><i class="material-icons">` + view.icon + `</i>` + view.caption + `</div>`;
          if (view.rel) {
          if(!result[view.rel])
            result[view.rel]='';
            result[view.rel] += this.renderTemplate(view, data, hasExtended);
          }
          else {
            result.main += this.renderTemplate(view, data, hasExtended);
          }
          i++;
          break;
        }
      }
      if(!viewFound){
        result.main+='View '+viewId+' was not found in the list of known views.';
    }

    }
    if(hasExtended[0]){
      let extended=`<div class="mdsExtended `+(this.isSearch() ? 'mdsExtendedSearch' : '')+`"><div class="label">`+this.translate.instant(this.isSearch() ? 'MDS.SHOW_EXTENDED_SEARCH' : 'MDS.SHOW_EXTENDED')+`</div><div class="switch">
            <label>
              `+this.translate.instant('OFF')+`
                <input type="checkbox" id="mdsExtendedCheckbox" onchange="window.mdsComponentRef.component.showExtended(this.checked)">
                <span class="lever"></span>
              `+this.translate.instant('ON')+`
              </label>
          </div></div>`;
        result.main=extended+result.main;
    }
    return result;
  }
  private showExtended(show:boolean){
    this.extended=show;
    this.extendedChange.emit(show);
    let checkbox:any=document.getElementById('mdsExtendedCheckbox');
    if(!checkbox)
      return;
    checkbox.checked=show;
    let display='none';
    if(show) {
      display = '';
    }
    let elements:any=document.getElementsByClassName('mdsExtendedGroup');
    for(let i=0;i<elements.length;i++){
      elements[i].style.display=display;
    }
  }

  private scrollSmooth(id:string){
    let pos=document.getElementById(id+'_header').offsetTop;
    UIHelper.scrollSmoothElement(pos,this.mdsScrollContainer.nativeElement,2);
  }
  private renderJumpmarks(group:any,data:any) : string{
    let html='';
    let i=0;
    for(let viewId of group.views){
      for(let view of data.views){
        if(view.id==viewId){
          html+=`<a class="clickable" onclick="window.mdsComponentRef.component.scrollSmooth('`+view.id+`')"><i class="material-icons">`+view.icon+`</i>`+view.caption+`</a>`;
          i++;
          break;
        }
      }
    }
    this.jumpmarksCount=i;
    setInterval(function(){
        let jump=document.getElementById("jumpmarks");
        if(!jump)
            return;
        let elements=jump.getElementsByTagName("a");
        let scroll=document.getElementsByClassName("card-title-element");
        let height=document.getElementById("mdsScrollContainer").getBoundingClientRect().bottom - document.getElementById("mdsScrollContainer").getBoundingClientRect().top;
        let pos=document.getElementById("mdsScrollContainer").scrollTop - height - 200;
        let closest=999999;
        let active=elements[0];
        for(let i=0;i<elements.length;i++){
            elements[i].className=elements[i].className.replace("active","").trim();
            if(!scroll[i])
                continue;
            let top=scroll[i].getBoundingClientRect().top;
            if(Math.abs(top-pos)<closest){
                closest=Math.abs(top-pos);
                active=elements[i];
            }
        }
        active.className+=" active";
    },200);
    return html;
  }

  private renderGroup(id:string,data:any){
    if(!id)
      return;
    this.currentWidgets=[];
    // add the default widgets
    data.widgets.push({id:'preview'});
    data.widgets.push({id:'version'});
    data.widgets.push({id:'author',caption:this.translate.instant('MDS.AUTHOR_LABEL')});
    data.widgets.push({id:RestConstants.CCM_PROP_LIFECYCLECONTRIBUTER_AUTHOR,type:'vcard'});
    data.widgets.push({id:RestConstants.CCM_PROP_AUTHOR_FREETEXT,type:'textarea'});
    if(this.getWidget('license',null,data.widgets)==null) {
      data.widgets.push({id: 'license', caption: this.translate.instant('MDS.LICENSE')});
    }
    for(let group of data.groups){
      if(group.id==id){
        let result=this.renderList(group,data);
        this.setRenderedHtml(result.main);
        if(result.suggestions)
          this.renderedSuggestions=this.sanitizer.bypassSecurityTrustHtml(result.suggestions);
        let jumpHtml=this.renderJumpmarks(group,data);
        this.jumpmarks=this.sanitizer.bypassSecurityTrustHtml(jumpHtml);
        this.readValues(data);
        //setTimeout(()=>UIHelper.materializeSelect(),15);
        return;
      }
    }
    let html='Group \''+id+'\' was not found in the mds';
    this.setRenderedHtml(html);
  }
  public getValues(propertiesIn:any={},showError=true,widgets=this.currentWidgets){
    let properties:any={};
    // add author data
    this.addAuthorValue(properties);
    if(!widgets)
      return properties;
    for(let widget of widgets){
      if(widget.id=='preview' || widget.id=='author'){
        continue;
      }
      if(widget.type=='vcard'){
        if(!propertiesIn[widget.id])
          propertiesIn[widget.id]=[null];

        let vcard=new VCard(propertiesIn[widget.id][0]);
        for(let field of MdsComponent.VCARD_FIELDS){
          let element=(document.getElementById(widget.id+'_'+field) as any);
          if(!element)
            continue;
          vcard.set(field,element.value);
        }
        propertiesIn[widget.id][0]=vcard.toVCardString();
        properties[widget.id]=propertiesIn[widget.id];
        continue;
      }
      if(widget.type=='radioVertical' || widget.type=='radioHorizontal'){
        properties[widget.id]=[];
        let list:any=document.getElementsByName(widget.id);
        for(let i=0;i<list.length;i++) {
          if(list.item(i).checked){
            properties[widget.id].push(list.item(i).value);
          }
        }
        continue;
      }
      let element=(document.getElementById(widget.id) as any);
      if(!element)
        continue;
      if(widget.type=='checkboxVertical' || widget.type=='checkboxHorizontal'){
        let inputs=element.getElementsByTagName('input');
        properties[widget.id]=[];
        for(let input of inputs){
          if(input.checked){
            properties[widget.id].push(input.value);
          }
        }
        continue;
      }
      element.className=element.className.replace('invalid','').trim();
      let v=element.value;
      if(element.getAttribute('data-value') && !this.isPrimitiveWidget(widget)){
        v=element.getAttribute('data-value');
      }
      let props=[v];
      if(this.isSliderWidget(widget)) {
        let value = element.noUiSlider.get();
        let valueClean = [];
        for (let v of Array.isArray(value) ? value : [value]) {
            let split = (v+'').split('</label>');
            v = split[split.length - 1].trim();
            if(widget.unit)
              v = v.replace(widget.unit,'').trim();
            valueClean.push(v);
          }

        if(widget.type=='duration') {
          valueClean[0] *= 60000;
        }
        if(widget.type=='range'){
          properties[widget.id+'_from']=[valueClean[0]];
          properties[widget.id+'_to']=[valueClean[1]];
          continue;
        }
          if (Array.isArray(valueClean))
            props = valueClean;
          else
            props = [valueClean];
      }
      else if(this.isMultivalueWidget(widget)){
        props=[];
        for(let i=0;i<element.childNodes.length;i++){
          var e=(element.childNodes.item(i) as HTMLElement);
          props.push(e.getAttribute('data-value'));
        }
      }
      else if(widget.type=='checkbox'){
        props=[(element as any).checked];
      }
      if(this.isRequiredWidget(widget) && (!props.length || props[0]=='')){
        if(showError) {
          let inputField=element;
          if(this.isMultivalueWidget(widget)){
            inputField=document.getElementById(widget.id+'_suggestionsInput');
          }
          if(inputField)
            inputField.className += 'invalid';
          this.toast.error(null, 'TOAST.FIELD_REQUIRED', {name: widget.caption});
        }
        return;
      }
      if(this.isSearch()){
        // don't send empty values to search -> this may not work with defaultvalues, so keep it
        if(!props || props.length==1 && !props[0] && !widget.defaultvalue)
          continue;
        if(props.length==1 && props[0]=='')
          props=[];
      }
      properties[widget.id]=props;
    }
    if(!properties[RestConstants.CM_NAME]){
      properties[RestConstants.CM_NAME]=properties[RestConstants.LOM_PROP_TITLE];
    }
    return properties;
  }
  private checkFileExtension(callback:Function=null,values:any){
    let ext1=this.currentNode.name.split(".");
    let ext2=values[RestConstants.CM_NAME][0].split(".");
    let extV1=ext1[ext1.length-1];
    let extV2=ext2[ext2.length-1];
    if(ext1.length==1 && ext2.length==1)
      return true;
    if(extV1!=extV2){
      this.dialogTitle='EXTENSION_NOT_MATCH';
      this.dialogMessage='EXTENSION_NOT_MATCH_INFO';
      if(ext1.length==1){
          this.dialogMessage='EXTENSION_NOT_MATCH_INFO_NEW';
      }
      if(ext2.length==1){
          this.dialogMessage='EXTENSION_NOT_MATCH_INFO_OLD';
      }
      this.dialogParameters={
        extensionOld:extV1,
        extensionNew:extV2,
        warning:this.translate.instant('EXTENSION_NOT_MATCH_WARNING')
      };
      this.dialogButtons=[
          new DialogButton('CANCEL',DialogButton.TYPE_CANCEL,()=>{
              this.dialogTitle=null;
          }),
          new DialogButton('SAVE',DialogButton.TYPE_PRIMARY,()=>{
            this.dialogTitle=null;
            this.saveValues(callback,true);
          }),
      ];
      return false;
    }
    return true;
  }
  public saveValues(callback:Function=null,force=false){
    if(this.embedded){
      this.onDone.emit(this.getValues());
      return this.getValues();
    }
    let properties:any={};
    if(this.currentNode)
      properties=this.currentNode.properties;
    let values=this.getValues(properties);
    if(values==null)
      return;
    if(!force){
      if(this.currentNode && this.currentNode.type==RestConstants.CCM_TYPE_IO && !this.checkFileExtension(callback,values)){
        return;
      }
    }
    for(var key in values){
      properties[key]=values[key];
    }
    if(this.currentNode)
      this.currentNode.properties=properties;
    let version='';
    let files:File[]=[];
    try {
      let comment=(document.getElementById('comment') as any);
      version = comment.value;
      files = (document.getElementById('fileSelect') as any).files;
      let display = document.getElementById('versionGroup').style.display;
      if (version && display == 'none')
        version = '';
      if(display!='none' && !version){
        comment.className+=' invalid';
        this.toast.error(null,'TOAST.FIELD_REQUIRED',{name:this.translate.instant('VERSION_COMMENT')});
        return;
      }
    }catch (e){}

    this.globalProgress=true;
    if(version){
      if(files.length){
        this.node.uploadNodeContent(this.currentNode.ref.id,files[0],version).subscribe(()=>{
          this.node.editNodeMetadata(this.currentNode.ref.id,this.currentNode.properties).subscribe(()=>{
            this.onUpdatePreview(callback);
          },(error:any)=>{
            this.toast.error(error);
            this.globalProgress=false;
          });
        },(error:any)=>{
          this.toast.error(error)
          this.globalProgress=false;
        });
      }
      else{
        this.node.editNodeMetadataNewVersion(this.currentNode.ref.id,version,this.currentNode.properties).subscribe(()=>{
          this.onUpdatePreview(callback);
        },(error:any)=>{
          this.toast.error(error);
          this.globalProgress=false;
        });
      }
    }
    else {
      if(this.createType==MdsComponent.TYPE_TOOLDEFINITION){
        this.tools.createToolDefinition(properties).subscribe((data:NodeWrapper)=>{
          this.currentNode=data.node;
          this.onUpdatePreview(callback);
        }, (error: any) => {
          this.toast.error(error);
          this.globalProgress = false;
        });
      }
      else if(this.createType==MdsComponent.TYPE_TOOLINSTANCE){
        this.tools.createToolInstance(this.parentNode.ref.id,properties).subscribe((data:NodeWrapper)=>{
          this.currentNode=data.node;
          this.onUpdatePreview(callback);
        }, (error: any) => {
          this.toast.error(error);
          this.globalProgress = false;
        });
      }
      else {
        this.node.editNodeMetadata(this.currentNode.ref.id, this.currentNode.properties).subscribe(() => {
          this.onUpdatePreview(callback);
        }, (error: any) => {
          this.toast.error(error);
          this.globalProgress = false;
        });
      }
    }
  }
  public setValuesByProperty(data:any,properties:any){
    setTimeout(()=>{
      for(let widget of data.widgets) {
        if(widget.template)
          continue;
        let props=properties[widget.id];
        let element=(document.getElementById(widget.id) as any);
        // try to resolve proper template widget if exists to exchange valuespace
        try {
          let template = element.parentNode.getAttribute('data-template');
          if(template!=null) {
            let tplWidget = this.getWidget(widget.id, template);
            if(tplWidget)
              widget=tplWidget;
          }
        }catch(e){}
        if(widget.id=='author'){
          /*if(properties[RestConstants.CCM_PROP_LIFECYCLECONTRIBUTER_AUTHOR]){
            this.setActiveAuthor(MdsComponent.AUTHOR_TYPE_PERSON);
          }
          else
            this.setActiveAuthor(MdsComponent.AUTHOR_TYPE_FREETEXT);
            */
          this.setActiveAuthor(MdsComponent.AUTHOR_TYPE_FREETEXT);
        }
        if(widget.type=='vcard'){
          if(!props)
            continue;

          let vcard=new VCard(props[0]);
          for(let field of MdsComponent.VCARD_FIELDS){
            let element=(document.getElementById(widget.id+'_'+field) as any);
            if(element){
              element.value=vcard.get(field);
            }
          }
        }
        else if(element) {
          if(this.isSliderWidget(widget)){
            if(widget.type=='range' && properties[widget.id+'_from'] && properties[widget.id+'_to']){
              let from=properties[widget.id+'_from'][0];
              let to=properties[widget.id+'_to'][0];
              element.noUiSlider.set([from,to]);
            }
            if(!props)
              continue;
            if(widget.type=='duration') {
              props[0] /= 60000;
            }
            element.noUiSlider.set(props);
          }
          if(!props)
            continue;

          if(widget.type=='multivalueGroup'){
              for(let v of props){
                  if(v!="") {
                    let caption=this.getGroupValueCaption(v,widget);
                    element.innerHTML += this.getMultivalueBadge(v, caption);
                  }
              }
          }
          else if (this.isMultivalueWidget(widget)){
            for(let v of props){
              if(v!='') {
                let caption=this.getValueCaption(widget, v);
                element.innerHTML += this.getMultivalueBadge(v, caption);
              }
            }
          }
          else if(widget.type=='checkbox'){
            element.checked=props[0];
          }
          else if(widget.type=='checkboxVertical' || widget.type=='checkboxHorizontal'){
            for(let input of element.getElementsByTagName('input')){
              input.checked=props.indexOf(input.value)!=-1;
            }
          }
          else if(widget.type=='singleoption'){
            element.value=props[0];
          }
          else {
            let caption=props[0];
            if(widget.values){
              for(let value of widget.values){
                if(value.id==caption){
                  caption=value.caption ? value.caption : caption;
                  break;
                }
              }
            }
            element.value=caption;
            try {
                let event = new KeyboardEvent('keyup', {
                    'view': window,
                    'bubbles': true,
                    'cancelable': true
                });
                // simulate event for materialize
                element.dispatchEvent(event);
            }catch(e){
              // fails in ie11
            }
            if(element.value!=props[0]) {
              element.setAttribute('data-value', props[0]);
            }
          }
        }
        else{
          if(!props)
            continue;
          for(let v of props){
            let element=(document.getElementById(widget.id+'_'+v) as any);
            if(element) {
              if (element.type == 'checkbox' || element.type=='radio')
                element.checked = true;
            }
          }
        }
      }
    },10);
  }

  private getValueCaption(widget:any, id:string) {
    if (widget.values) {
      for (let value of widget.values) {
        if (value.id == id) {
          return value.caption ? value.caption : id;
        }
      }
    }
    return id;
  }
  private readValues(data:any){
    this.setGeneralNodeData();
    this.setValuesByProperty(data,this.currentNode ? this.currentNode.properties : []);
  }
  private renderTemplate(template : any,data:any,extended:boolean[]) {
    if(!template.html || !template.html.trim()){
      return '';
    }
    let html='<div class="mdsGroup'+(this.embedded?' mdsEmbedded':'')+'">'+template.html;
    let removeWidgets:any=[];
    for(let widget of data.widgets){
      if(!widget.template){
        for(let w2 of data.widgets){
          if(w2.id==widget.id && w2.template==template.id) {
            widget = w2;
            break;
          }
        }
      }
      else{
        continue; // already processed!
      }
      let search='<'+widget.id+'>';
      let start=html.indexOf(search);
      let end=start+search.length;
      let attr="";
      if(start<0){
        search='<'+widget.id+' ';
        start=html.indexOf(search);
        end=html.indexOf('>',start)+1;
        attr=html.substring(start+search.length,end-1);
        let attributes=this.getAttributes(html.substring(start,end-1));
        for(var k in attributes){
            widget[k]=attributes[k];
        }
      }
      if(start<0)
        continue;

      let first=html.substring(0,start);
      let second=html.substring(end);

      if(this.isExtendedWidget(widget))
        extended[0]=true;
      this.replaceVariables(widget);
      this.currentWidgets.push(widget);
      let widgetData=this.renderWidget(widget,attr,template);
      if(!widgetData) {
        removeWidgets.push(widget);
        continue;
      }
      html=first+widgetData+second;
    }
    for(let remove of removeWidgets){
      html=html.replace(new RegExp("<"+remove.id+"*>"),'');
    }
    html+='</div>';
    return html;

    }
  private renderPrimitiveWidget(widget:any,attr:string,type:string,css=''){
    let html='<div class="inputTable">';
    if(widget.icon){
      html+='<i class="inputIcon material-icons">'+widget.icon+'</i>';
    }
    html+='<input type="'+type+'" id="'+widget.id+'" placeholder="'+(widget.placeholder ? widget.placeholder : '')+'" class="'+css+'">';
    if(widget.type=='checkbox'){
      html+=this.getCaption(widget);
    }
    html+='</div>'
    html+=this.addBottomCaption(widget);
    return html;
  }
  private getSuggestBadge(value:string,caption:string,id:string){
    return `<div class="badge" data-value="`+value+`"><span>`+caption+`</span>
            <i class="material-icons clickable" onclick="
            this.parentNode.parentNode.removeChild(this.parentNode);
            var caption='`+caption+`';
            var value='`+value+`';
            document.getElementById('`+id+`').innerHTML+='`+this.getMultivalueBadgeEmbedded('caption','value')+`';
            ">add_circle</i></div>`;
  }
  private getMultivalueBadge(value:string,caption:string=value){
    return '<div class="badge" data-value="'+value+'"><span>'+caption+`</span><i class="material-icons clickable" tabindex="0" onkeyup="if(event.keyCode==13){this.click()}" onclick="
    this.parentNode.parentNode.removeChild(this.parentNode);
    window.mdsComponentRef.component.applySuggestions();
    ">cancel</i></div>`;
  }
  private getMultivalueBadgeEmbedded(label='this.value',value='this.value'){
    return `<div class=\\'badge\\' data-value=\\''+`+value+`+'\\'><span>'+`+label+`+'</span><i class=\\'material-icons clickable\\' tabindex=\\'0\\' onkeyup=\\'if(event.keyCode==13){this.click()}\\' onclick=\\'this.parentNode.parentNode.removeChild(this.parentNode);window.mdsComponentRef.component.applySuggestions();\\'>cancel</i></div>`;
  }
  private renderVCardWidget(widget: any, attr: string) {
    let html='';
    let i=0;
    for(let field of [MdsComponent.VCARD_FIELDS[1],MdsComponent.VCARD_FIELDS[0]]) {
      let id = widget.id + '_' + field;
      let caption = this.translate.instant('VCARD.' + field);
      html += `<div class="vcardGroup"><label for="` + id + `">` + caption + `</label>
               <input type="text" class="vcard_`+field+(i==0?' vcardFirstInput':'')+`" id="` + id + `">`;
      if(i==0){
        html += `<i class="material-icons">person</i>`;
      }
      html += `</div>`;
      i++;
    }
    return html;
  }
  private renderMultivalueBadgesWidget(widget:any,attr:string){
    let html=`<input type="text" class="multivalueBadgesText" aria-label="`+widget.caption+`" placeholder="`+(widget.placeholder ? widget.placeholder : '')+`" onkeyup="if(event.keyCode==13){
        var elements=document.getElementById('`+widget.id+`').childNodes;
        for(var i=0;i<elements.length;i++){
            if(elements[i].getAttribute('data-value')==this.value){
                return;
            }
        }
        document.getElementById('`+widget.id+`').innerHTML+='`+this.getMultivalueBadgeEmbedded()+`';
        this.value='';
      }
      ">`;
      html+=this.addBottomCaption(widget);
      html+=`<div id="`+widget.id+`" class="multivalueBadges"></div>`;
    return html;
  }
  private renderSuggestBadgesWidget(widget:any, attr:string, allowCustom:boolean){
    let html=this.autoSuggestField(widget,'',allowCustom,true)+`<div id="`+widget.id+`" class="multivalueBadges"></div>`;
    return html;
  }
  private renderSubTree(widget:any,parent:string=null){
    let html='<div id="'+widget.id+'_group_'+parent+'" class="treeGroup"';
    if(parent!=null){
      html+=' style="display:none;"';
    }
    html+='>';
    let count=0;
    if(widget.values) {
      for (let value of widget.values) {
        if (value.parent != parent)
          continue;
        let id = widget.id + '_' + value.id;
        let sub = this.renderSubTree(widget, value.id);
        html += '<div><div id="'+id+'_bg"><div class="treeIcon">';
        if (sub) {
          html += `<i class="material-icons clickable" onclick="
                  var element=document.getElementById('` + widget.id + `_group_` + value.id + `');
                  var enable=element.style.display=='none';
                  element.style.display=enable ? '' : 'none';
                  this.innerHTML=enable ? 'keyboard_arrow_down' : 'keyboard_arrow_right';
                  ">keyboard_arrow_right</i>`;
        }
        else
          html += '&nbsp;';
        html += '</div>'
        html += `<input type="checkbox" id="` + id + `" class="filled-in" onchange="window.mdsComponentRef.component.changeTreeItem(this,'`+widget.id+`')"`;
        if (value.disabled) {
          html += ' disabled="true"';
        }
        html += ' value="' + value.id + '"><label for="' + id + '">' + value.caption + '</label></div>';
        if (sub) {
          html += sub;
        }
        html += '</div>'
        count++;
      }
    }
    if(!count)
      return null;

    html+='</div>';
    return html;
  }
  private changeTreeItem(element:any,widgetId:string){
    let widget=this.getWidget(widgetId);
    let multivalue=widget.type=='multivalueTree';
    document.getElementById(element.id+'_bg').className=element.checked ? 'treeSelected' : '';
    if(!multivalue) {
      let inputs = document.getElementById('`+widget.id+`_tree').getElementsByTagName('input');
      for (let i = 0; i < inputs.length; i++) {
        if (inputs[i].id == element.id)
          continue;
        inputs[i].checked = false;
        document.getElementById(inputs[i].id + '_bg').className = '';
      }
    }
    if(this.mode=='search'){
      // disable all sub-elements if the element is checked, because they will all be found as well
      let subElements=element.parentElement.parentElement.getElementsByTagName('input');
      for(let i=0;i<subElements.length;i++){
        if(subElements.item(i)==element)
          continue;
        subElements.item(i).disabled=element.checked;
        subElements.item(i).checked=element.checked;
        document.getElementById(subElements.item(i).id+'_bg').className=element.checked ? 'treeSelected' : '';
      }
    }
  }
  public handleKeyboardEvent(event: KeyboardEvent) {
    if(event.code=='Escape'){

      for(let widget of this.currentWidgets){
        if(widget.type=='multivalueTree'){
          let element=document.getElementById(widget.id+'_tree');
          if(element && element.style.display==''){
            element.style.display='none';
            event.preventDefault();
            event.stopPropagation();
            return true;
          }
        }
      }
    }
    return false;
  }
  private mdsUpdateSuggests(id:string,showMore=false){
    let list=document.getElementById(id+'_suggestions');
    let element:any=document.getElementById(id+'_suggestionsInput');
    let elements=list.getElementsByTagName('a');
    let widget=this.getWidget(id);
    if(showMore){
      list.className+=' suggestionListAll';
    }
    elements.item(0).style.display='none';
    list.style.display='none';
    let values=this.getValues([],false);
    let group=this._groupId;
    if(!group){
      group=RestConstants.DEFAULT_QUERY_NAME;
    }
    this.lastMdsQuery=element.value;
    this.mdsService.getValues({query:group,property:id,pattern:element.value},this._setId).subscribe((data:MdsValueList)=>{
      if(this.lastMdsQuery!=element.value)
        return;

      for(let i=1;i<elements.length;){
        list.removeChild(elements.item(i));
      }
      list.className=list.className.replace('suggestionListAll','').trim();

      list.style.display='';
      let i=0;
      let moreCount=0;
      for(let value of data.values){
        if(i>=MdsComponent.MAX_SUGGESTIONS && !showMore){
          moreCount++;
          continue;
        }
        let key=value.key ? value.key : value.displayString;
        let caption=this.getKeyCaption(key,widget.values);
        if(!widget.values && value.displayString){
          caption=value.displayString;
        }
        list.innerHTML+=this.getListEntry(id,key,caption,false,element.value);
        i++;
      }
      if(i==0){
        list.style.display='none';
      }
      if(moreCount){
        list.innerHTML+='<a class="collection-item suggestionMoreItems" onclick="window.mdsComponentRef.component.mdsUpdateSuggests(\''+id+'\',true)">'+moreCount+' '+this.translate.instant('MORE_SELECTBOX')+'</a>';
      }
      //elements.item(0).style.display=data.values ? 'none' : '';
    },(error:any)=>{
      console.warn('invalid suggestions result for '+this._groupId+' '+id);
    });
  }
  private getListEntry(id:string,key:string,caption:string,singleValue=false,searchString:string=null){
    let html=`<a class="collection-item" tabindex="0" data-value="` + key + `" data-caption="` + this.htmlEscape(caption) + `" onkeydown="
                if(event.keyCode==13){ 
                    this.click();
                }
                var suggestions=document.getElementById('` + id + `_suggestions');
                var elements=suggestions.childNodes;
                if(event.keyCode==40 || event.keyCode==38){
                  var direction=event.keyCode==40 ? 1 : -1;
                  for(var i=0;i<elements.length;i++){
                      if(elements[i]==this){
                          var pos=(i+direction);
                          while(pos>0 && pos<elements.length && elements[pos].style.display=='none')
                            pos+=direction;
                          if(pos==0) pos=elements.length-1;
                          if(pos==elements.length) pos=1;
                          elements[pos].focus();
                          event.preventDefault();
                          event.stopPropagation();
                          return;
                      }
                  }
                }
                " onclick="
                document.getElementById('` + id + `_suggestions').style.display='none';
                window.mdsComponentRef.component.currentWidgetSuggestion=null;`;

    if(singleValue){
      html+=`   document.getElementById('` + id + `').value=this.getAttribute('data-caption');
                document.getElementById('` + id + `').setAttribute('data-value',this.getAttribute('data-value'));`;
    }
    else {
      html += `
                document.getElementById('` + id + `_suggestionsInput').value='';`;
      if(!this.uiService.isMobile())
        html += `document.getElementById('` + id + `_suggestionsInput').focus();`;
      html += `var badges=document.getElementById('` + id + `');
                var elements=badges.childNodes;
                for(var i=0;i<elements.length;i++){
                    if(elements[i].getAttribute('data-value')==this.getAttribute('data-value')){
                        return;
                    }
                }
                badges.innerHTML+='` + this.getMultivalueBadgeEmbedded('this.getAttribute(\'data-caption\')', 'this.getAttribute(\'data-value\')') + `';`;
    }
    html+=`">` + (searchString ? this.highlightSearch(caption,searchString) : caption) + `</a>`;
    return html;
  }
  private autoSuggestField(widget:any,css='',allowCustom=false,showOpen=false,singleValue=false){
    if(widget.values==null/* || this._groupId*/)
      showOpen=false;
    if(!showOpen && widget.type!='multivalueTree' && widget.type!='singlevalueTree')
      css+=' suggestInputNoOpen';
    let postfix='_suggestionsInput';
    if(singleValue)
      postfix='';
    let html=`<input type="text" id="`+widget.id+postfix+`" `
    if(singleValue)
      html+='readonly ';
    html+=`aria-label="`+widget.caption+`" placeholder="`+(widget.placeholder ? widget.placeholder : '')+`" class="suggestInput `+css+`" 
            onkeyup="window.mdsComponentRef.component.openSuggestions('`+widget.id+`',event,`+allowCustom+`,`+(widget.values ? true  : false)+`,false,true)">`;
    if(widget.type=='singleoption' && !widget.allowempty){
      setTimeout(()=>{
        let pos=0;
        if(widget.defaultvalue){
          for(let i=0;i<widget.values.length;i++){
            if(widget.values[i].id==widget.defaultvalue){
              pos=i;
              break;
            }
          }
        }
        eval(`
          document.getElementById('`+widget.id+`').value='`+widget.values[pos].caption+`';
          document.getElementById('`+widget.id+`').setAttribute('data-value','`+widget.values[pos].id+`');
        `)
      },5);
    }
    html+=this.addBottomCaption(widget);
    if(showOpen){
      html+=`<a class="btn-flat suggestOpen" 
              onclick="window.mdsComponentRef.component.openSuggestions('`+widget.id+`',null,false,`+(widget.values ? true : false)+`,false,false)"
              `;
      /*
              var list=document.getElementById('`+widget.id+`_suggestions');
              var dialog=document.getElementById('`+widget.id+`_dialog');
              list.style.display='';
              dialog.style.display='';
              var elements=list.getElementsByTagName('a');
              var more=elements[elements.length-1];
              more.style.display='none';
              var hits=0;
              var moreCount=0;
              for(var i=1;i<elements.length-1;i++){
                  var element=elements[i];
                  if(i==1) element.focus();
                  var caption=element.getAttribute('data-caption');
                  var add=true;
                  if(hits>=`+MdsComponent.MAX_SUGGESTIONS+`){
                    add=false;
                    moreCount++;
                  }
                  element.style.display=add ? '' : 'none';
                  if(!add)
                    continue;
                  element.innerHTML=caption;
                  hits+=add;
              }
              if(moreCount){
                  more.style.display='';
                  more.innerHTML=moreCount+' `+this.translate.instant("MORE_SELECTBOX")+`';
              }
              elements[0].style.display='none';*/
      html+=`"><i class="material-icons">arrow_drop_down</i></a>`;

    }
    html+=`<div id="`+widget.id+`_suggestions" class="suggestionList collection" style="display:none;">`;
    html+=`<a class="collection-item suggestionNoMatches"  onclick="
              document.getElementById('`+widget.id+`_suggestions').style.display='none';
              document.getElementById('`+widget.id+`_dialog').style.display='none';
              ">`+this.translate.instant('NO_MATCHES')+`</a>`;
    if(widget.allowempty==true){
      html += this.getListEntry(widget.id,'','',singleValue);
    }
    if(widget.values) {
      for (let value of widget.values) {
        if (value.disabled/* || value.parent*/) // find all fields, not only main nodes of a tree
          continue;
        html += this.getListEntry(widget.id,value.id,value.caption,singleValue);
      }
    }
    html+=`<a class="collection-item suggestionMoreItems"  onclick="
              //document.getElementById('`+widget.id+`_suggestions').style.display='none';
              //document.getElementById('`+widget.id+`_dialog').style.display='none';
              window.mdsComponentRef.component.openSuggestions('`+widget.id+`',null,false,`+(widget.values ? true : false)+`,true);
              ">...</a>`;
    html+=`</div>`;
    if(allowCustom && !showOpen){
      html+='<div class="hint">'+this.translate.instant('WORKSPACE.EDITOR.HINT_ENTER')+'</div>';
    }
    return html;
  }
  private closeDialog(){
    document.getElementById(this.currentWidgetSuggestion+'_suggestions').style.display='none';
    this.currentWidgetSuggestion=null;
  }
  private renderTreeWidget(widget:any,attr:string){
    let html=this.autoSuggestField(widget)+`<div class="btn-flat suggestOpen" onclick="
                  var tree=document.getElementById('`+widget.id+`_tree');
                  tree.style.display='';
                  var childs=document.getElementById('`+widget.id+`').childNodes;
                  var elements=tree.getElementsByTagName('input');
                  for(var i=0;i<elements.length;i++){
                      elements[i].checked=false;
                      document.getElementById(elements[i].id+'_bg').className='';
                  }
                  for(var i=0;i<childs.length;i++){
                     var child=childs[i];
                     var element=document.getElementById('`+widget.id+`_'+child.getAttribute('data-value'));
                     var elementBg=document.getElementById(element.id+'_bg');
                     if(element){
                      element.checked=true;
                      window.mdsComponentRef.component.changeTreeItem(element,'`+widget.id+`');
                     }
                  }
              "><i class="material-icons">arrow_forward</i></div>
              <div class="dialog darken" style="display:none;z-index:121;" id="`+widget.id+`_tree">
                <div class="card center-card card-wide card-high card-action">
                  <div class="card-content">
                  <div class="card-cancel" onclick="document.getElementById('`+widget.id+`_tree').style.display='none';"><i class="material-icons">close</i></div>
                  <div class="card-title">`+(widget.caption ? widget.caption : widget.placeholder)+`</div>
                    <div class="card-scroll">
                    `+this.renderSubTree(widget,null)+`
                    </div>
                  </div>
                  <div class="card-action">
                       <a class="waves-effect waves-light btn" onclick="window.mdsComponentRef.component.saveTree('` + widget.id + `')">`+this.translate.instant('SAVE')+`</a>
                     </div>
                </div>
              </div>
              <div id="`+widget.id+`" class="multivalueBadges"></div>`;
    // delete existing tree from document
    try{
      document.getElementsByTagName('body')[0].removeChild(document.getElementById(widget.id+'_tree'));
    }catch(e){}
    // dirty hack: In search, the tree is inside the sidebar which does not render correctly. So we need to append it to the main body and delete any existing trees
    setTimeout(()=> {
      try {
        let id = widget.id + '_tree';
        document.getElementsByTagName('body')[0].appendChild(document.getElementById(id));
      }catch(e){}
    },5);
    return html;
  }
  private saveTree(widgetId:string){
    let tree=document.getElementById(widgetId+'_tree');
    tree.style.display='none';
    let badges=document.getElementById(widgetId);
    while (badges.firstChild)
      badges.removeChild(badges.firstChild);
    let elements=tree.getElementsByTagName('input');
    let labels=tree.getElementsByTagName('label');
    for(let i=0;i<elements.length;i++){
      let element=elements[i];
      let label=labels[i];
      if(!element.checked || element.disabled)
        continue;
      badges.innerHTML+=this.getMultivalueBadge(element.value,label.innerHTML);
    }
  }
  private renderTextareaWidget(widget:any,attr:string){
    let html='<textarea class="materialize-textarea" id="'+widget.id+'"';
    if(widget.placeholder){
      html+=' placeholder="'+widget.placeholder+'"';
    }
    html += '></textarea>';
    return html;
  }
  private renderDurationWidget(widget:any,attr:string){
    let html=`
              <div class="inputField"><label for="`+widget.id+`_hours">`+this.translate.instant('INPUT_HOURS')+`</label>
              <input type="number" min="0" max="9" id="`+widget.id+`_hours" onchange="
              document.getElementById('`+widget.id+`').noUiSlider.set(
              document.getElementById('`+widget.id+`_hours').value*60+
              document.getElementById('`+widget.id+`_minutes').value*1);
              " />
              </div>
              <div class="inputField"><span>:</span></div>
              <div class="inputField">
              <label for="`+widget.id+`_minutes">`+this.translate.instant('INPUT_MINUTES')+`</label>
              <input type="number" min="0" max="60" id="`+widget.id+`_minutes" onchange="
              document.getElementById('`+widget.id+`').noUiSlider.set(
              document.getElementById('`+widget.id+`_hours').value*60+
              document.getElementById('`+widget.id+`_minutes').value*1);
              "/>
              </div>
              <div class="inputSlider" id="`+widget.id+`"></div>
    `;
    setTimeout(()=>{
      eval(`
                var slider = document.getElementById('`+widget.id+`');
                          noUiSlider.create(slider, {
                           start: [0],
                           step: 1,
                           connect: true,
                           tooltips: true,
                           format: {
                            to: function ( value ) {
                              //return Math.round(value/60)+':'+Math.round(value%60);
                              return '<label>`+this.translate.instant('INPUT_MINUTES')+`</label>'+Math.round(value);
                            },from:function(value){return value;}
                            },
                           range: {
                             'min': 0,
                             'max': 599
                           },
                          });
                          var sliderUpdate=function(values,handle,unencoded){
                    document.getElementById('`+widget.id+`_hours').value=Math.floor(unencoded/60);
                    document.getElementById('`+widget.id+`_minutes').value=Math.floor(unencoded%60);
                  };
                  slider.noUiSlider.on('slide', sliderUpdate);
                  slider.noUiSlider.on('update', sliderUpdate);                 
            `);
    },5);
    return html;
  }
  private renderRangeWidget(widget:any,attr:string){
    let html=`
              <div class="inputRange" id="`+widget.id+`"></div>
    `;
    setTimeout(()=>{
      let values=widget.defaultvalue!=null ? widget.defaultvalue : widget.min;
      if(widget.type=='range') {
        values = (widget.defaultMin != null ? widget.defaultMin : widget.min) + `,` +
                 (widget.defaultMax != null ? widget.defaultMax : widget.max);
      }
      let unit=widget.unit ? widget.unit : '';
      eval(`
                var slider = document.getElementById('`+widget.id+`');
                          noUiSlider.create(slider, {
                           start: [`+values+`],
                           step: `+(widget.step>1 ? widget.step : 1)+`,
                           connect: true,
                           tooltips: true,
                           format: {
                            to: function ( value ) {
                              return Math.round(value)+' `+unit+`';
                            },
                            from:function(value){
                              return value;
                            }
                           },
                           range: {
                             'min': `+widget.min+`,
                             'max': `+widget.max+`
                           },
                          });
            `);
    },5);
    return html;
  }

  private renderSingleoptionWidget(widget:any,attr:string){
    if(widget.values==null)
      return 'Error at '+widget.id+': No values for a singleOption widget is not possible';
    let html='<select id="'+widget.id+'">';
    if(widget.allowempty==true){
      html+='<option value=""></option>';
    }
    for(let option of widget.values){
      html+='<option value="'+option.id+'"';
      if(widget.defaultvalue && option.id==widget.defaultvalue){
       html+=' selected';
      }
      html+='>'+option.caption+'</option>';
    }
    html+='</select>';
    return html;
  }
  private renderMultioptionWidget(widget:any,attr:string){
    let html=`<select onchange="
        var elements=document.getElementById('`+widget.id+`').childNodes;
        for(var i=0;i<elements.length;i++){
            if(elements[i].getAttribute('data-value')==this.value){
                return;
            }
        }
        document.getElementById('`+widget.id+`').innerHTML+='`+this.getMultivalueBadgeEmbedded('this.options[this.selectedIndex].innerHTML')+`';
        this.value='';
      "><option></option>`;
    for(let option of widget.values){
      html+='<option value="'+option.id+'">'+option.caption+'</option>';
    }
    html+='</select><div id="'+widget.id+'" class="multivalueBadges"></div>';
    return html;
  }
  private renderRadioWidget(widget:any,attr:string,vertical:boolean){
    let html='<fieldset class="'+(vertical ? 'radioVertical' : 'radioHorizontal')+'">';

    for(let option of widget.values){
      let id=widget.id+'_'+option.id;
      html+='<input type="radio" name="'+widget.id+'" id="'+id+'" value="'+option.id+'"'+(option.id==widget.defaultvalue ? ' checked' : '')+(option.disabled ? ' disabled' : '')+'> <label for="'+id+'">'+option.caption+'</label>';
    }
    html+='</fieldset>';
    return html;
  }
  private renderCheckboxWidget(widget:any,attr:string,vertical:boolean){
    let html='<fieldset id="'+widget.id+'" class="'+(vertical ? 'checkboxVertical' : 'checkboxHorizontal')+'">';

    for(let option of widget.values){
      let id=widget.id+'_'+option.id;
      html+='<input type="checkbox" class="filled-in" name="'+widget.id+'" id="'+id+'" value="'+option.id+'"'+(option.disabled ? ' disabled' : '')
        +'> <label for="'+id+'">'+(option.imageSrc ? '<img src="'+option.imageSrc+'">' : '')+(option.caption ? '<span class="caption">'+option.caption+'</span>' : '')
        +(option.description ? '<span class="description">'+option.description+'</span>' : '')
        +'</label>';
    }
    html+='</fieldset>';
    return html;
  }
  private isWidgetConditionTrue(widget:any){
    if(!widget.condition)
      return true;
    let condition=widget.condition;
    console.log('condition:');
    console.log(condition);
    if(condition.type=='PROPERTY' && this.currentNode) {
        if (!this.currentNode.properties[condition.value] && !condition.negate || this.currentNode.properties[condition.value] && condition.negate) {
            return false;
        }
    }
    if(condition.type=='TOOLPERMISSION'){
        let tp=this.connector.hasToolPermissionInstant(condition.value);
        if(tp==condition.negate){
            return false;
        }
    }
    console.log("condition is true, will display widget");
    return true;
  }
  private renderWidget(widget: any,attr:string,template:any) : string{
    let id=widget.id;
    let hasCaption=widget.caption;
    let html='';
    let caption='';
    if(!this.isWidgetConditionTrue(widget))
      return null;

    if(hasCaption) {
      caption=this.getCaption(widget);
    }
    html+='<div id="'+widget.id+(template.rel ? '_'+template.rel : '')+'_container"';
    if(this.isExtendedWidget(widget)){
      html+=' class="mdsExtendedGroup" style="display:none"';
    }
    html+='>';
    if(widget.type!='checkbox')
      html+=caption;

    html+='<div class="mdsWidget widget_'+widget.type+' '+id.replace(':','_')+'"'+attr+' data-template="'+template.id+'">';
    if(template.rel=='suggestions'){
      html+=`<div id="`+widget.id+`_badgeSuggestions" style="display:none" class="multivalueBadges"></div>`;
    }
    else if(this.isPrimitiveWidget(widget)){
      html+=this.renderPrimitiveWidget(widget,attr,widget.type);
    }
    else if(widget.type=='textarea'){
      html+=this.renderTextareaWidget(widget,attr);
    }
    else if(widget.type=='duration'){
      html+=this.renderDurationWidget(widget,attr);
    }
    else if(widget.type=='range' || widget.type=='slider'){
      html+=this.renderRangeWidget(widget,attr);
    }
    else if(widget.type=='singleoption'){
      html+=this.renderSingleoptionWidget(widget,attr);
      //html+=this.autoSuggestField(widget,'',false,true,true);
    }
    else if(widget.type=='multioption'){
      html+=this.renderMultioptionWidget(widget,attr);
    }
    else if(widget.type=='radioHorizontal' || widget.type=='radioVertical'){
      html+=this.renderRadioWidget(widget,attr,widget.type=='radioVertical');
    }
    else if(widget.type=='checkboxHorizontal' || widget.type=='checkboxVertical'){
      html+=this.renderCheckboxWidget(widget,attr,widget.type=='checkboxVertical');
    }
    else if(widget.type=='multivalueBadges'){
      //html+=this.renderMultivalueBadgesWidget(widget,attr);
      html+=this.renderSuggestBadgesWidget(widget,attr,true);
    }
    else if(widget.type=='multivalueSuggestBadges' || widget.type=='multivalueFixedBadges'){
      html+=this.renderSuggestBadgesWidget(widget,attr,widget.type=='multivalueSuggestBadges');
    }
    else if(widget.type=='multivalueTree' || widget.type=='singlevalueTree') {
      html += this.renderTreeWidget(widget, attr);
    }
    else if(widget.type=='checkbox') {
      html+=this.renderPrimitiveWidget(widget,attr,widget.type,'filled-in');
    }
    else if(widget.type=='vcard') {
      html+=this.renderVCardWidget(widget,attr);
    }
    else if(widget.type=='multivalueGroup'){
        html+=this.renderGroupWidget(widget,attr,template);
    }
    else if(widget.type=='defaultvalue'){
        // hide this widget, it's used in backend
        return '';
    }
    else if(widget.id=='preview'){
      html+=this.renderPreview(widget,attr);
    }
    else if(widget.id=='author'){
      html+=this.renderAuthor(widget);
    }
    else if(widget.id=='version'){
      html+=this.renderVersion(widget);
    }
    else if(widget.id=='license'){
      html+=this.renderLicense(widget);
    }
    else{
      html+='Unknown widget type \''+widget.type+'\' at id \''+widget.id+'\'';
    }

    html+='</div></div>';
    return html;
  }

  private getCaption(widget: any) {
    let caption = '<label for="' + widget.id + '"> ' + widget.caption;
    if(this.isRequiredWidget(widget))
      caption+= ' <span class="required">('+this.translate.instant('FIELD_REQUIRED')+')</span>';
    caption +=  '</label>';
    return caption;
  }

  private getAttributes(element: string) {
    let attributes:any={};
    let str=element;
    while(true){
      str=str.substring(str.indexOf(' ')+1);
      let pos=str.indexOf('=');
      if(pos==-1) {
        return attributes;
      }
      let name=str.substring(0,pos).trim();
      str=str.substring(pos+1);
      let search=' ';
      if(str.startsWith('\'')){
        search='\'';
      }
      if(str.startsWith('"')){
        search='"';
      }
      if(search!=' ')
        str=str.substring(1);
      let end=str.indexOf(search);
      let value=str.substring(0,end);
      str=str.substring(end+1);
      attributes[name]=value;
    }
 }

  private isMultivalueWidget(widget: any) {
    return widget.type == "multivalueBadges"
    || widget.type=="multioption"
    || widget.type=="multivalueFixedBadges"
    || widget.type=="multivalueSuggestBadges"
    || widget.type=="singlevalueTree" // it basically uses the tree so all functions relay on multivalue stuff
    || widget.type=="multivalueTree"
    || widget.type=="multivalueGroup"
  }
  private isSliderWidget(widget: any) {
    return widget.type == 'duration'
      || widget.type == 'range'
      || widget.type == 'slider';
  }
  private addBottomCaption(widget: any) {
    if(widget.bottomCaption){
      return '<div class="input-hint-bottom">'+widget.bottomCaption+'</div>';
    }
    return '';
  }
  private renderAuthor(widget: any) {
    let authorWidget={id:RestConstants.CCM_PROP_LIFECYCLECONTRIBUTER_AUTHOR};
    let freetextWidget={id:RestConstants.CCM_PROP_AUTHOR_FREETEXT,placeholder:this.translate.instant('MDS.AUTHOR_FREETEXT_PLACEHOLDER')};
    let author=`
      <div class="mdsAuthor">
        <div class="row">
          <div class="col s12">
          <ul class="tabs" id="mdsAuthorTabs">
            <li class="clickable tab col s6" onclick="window.mdsComponentRef.component.setActiveAuthor(`+MdsComponent.AUTHOR_TYPE_FREETEXT+`)">
              <a>`+this.translate.instant('MDS.AUTHOR_FREETEXT')+`</a>
            </li>
            <li class="clickable tab col s6" onclick="window.mdsComponentRef.component.setActiveAuthor(`+MdsComponent.AUTHOR_TYPE_PERSON+`)">
              <a>`+this.translate.instant('MDS.AUTHOR_PERSON')+`</a>
            </li>
          </ul>
         </div>
         <div id="mdsAuthorFreetext">`+this.renderTextareaWidget(freetextWidget,null)+`</div>
          <div id="mdsAuthorPerson">`+this.renderVCardWidget(authorWidget,null)+`
            <div class="mdsContributors">
            <a class="clickable contributorsLink" onclick="window.mdsComponentRef.component.openContributorsDialog();">`+
            this.translate.instant('MDS.CONTRIBUTOR_LINK')+` <i class="material-icons">arrow_forward</i></a>
          </div>
         </div>
        </div>
      </div>
    `;
    return author;
  }
  private renderPreview(widget: any,attr:string) {
    let preview=`<div class="mdsPreview">`;

    preview+=`<input type="file" style="display:none" id="previewSelect" accept="image/*" onchange="
            var valid=this.files.length;
            if(valid){
                document.getElementById('preview').setAttribute('data-custom',true);
                document.getElementById('preview').src=window.URL.createObjectURL(this.files[0]);
            }
          " />
            <label>`+this.translate.instant('WORKSPACE.EDITOR.PREVIEW')+`</label>`;
    if(this.connector.getApiVersion()>=RestConstants.API_VERSION_4_0) {
      preview += `<div onclick="document.getElementById('previewSelect').click()" class="changePreview clickable">` + this.translate.instant('WORKSPACE.EDITOR.REPLACE_PREVIEW') + `</div>`;
    }
    preview+=`<div class="previewImage"><img id="preview" `+attr+`></div>
            </div>`;
    return preview;
  }
  private renderVersion(widget: any) {
    if(!this.allowReplacing)
      return '';
    let html=`<div class="mdsVersion">
          <input type="file" style="display:none" id="fileSelect" onchange="
            var valid=this.files.length;
            if(valid){
              document.getElementById('selectedFileContent').innerHTML=this.files[0].name;
            }
            document.getElementById('selectedFile').style.display=valid ? '' : 'none';
            document.getElementById('versionChooser').style.display=valid ? 'none' : '';
            document.getElementById('versionGroup').style.display=valid ? '' : 'none';
            document.getElementById('versionCheckbox').checked=false;
            document.getElementById('selectFileBtn').style.display=valid ? 'none' : '';
          " />
            <label for="comment">`+this.translate.instant('WORKSPACE.EDITOR.VERSION')+`</label>
            <div class="version">`;
    if(this.isContentEditable())
      html+=`<div class="btn-flat btn-shadow" id="selectFileBtn" onclick="document.getElementById('fileSelect').click()">`+this.translate.instant('WORKSPACE.EDITOR.REPLACE_MATERIAL')+`</div>`;
    html+=`
              <div id="selectedFile" class="badge" style="display:none;"><span id="selectedFileContent"></span>
              <i class="material-icons clickable" onclick="
              document.getElementById('selectedFile').style.display='none';
              document.getElementById('versionChooser').style.display='';
              document.getElementById('versionGroup').style.display='none';
              document.getElementById('selectFileBtn').style.display='';
              ">cancel</i></div>
              <span id="versionChooser"><input type="checkbox" id="versionCheckbox" onchange="
                document.getElementById('versionGroup').style.display=this.checked ? '' : 'none';
              " class="filled-in"> <label for="versionCheckbox">`+this.translate.instant('WORKSPACE.EDITOR.AS_VERSION')+`</label></span>

            </div>
            <div id="versionGroup" style="display:none;">
            <input type="text" class="comment" id="comment" placeholder="`+this.translate.instant('WORKSPACE.EDITOR.VERSION_COMMENT')+`" required />
              <div class="input-hint-bottom"`+this.translate.instant('FIELD_MUST_BE_FILLED')+`</div>
            </div>
          </div>
         </div>`;
    return html;
  }
  private openLicenseDialog(){
    this.saveValues(()=>{
      this.openLicense.emit();
    })
  }
  private openContributorsDialog(){
    this.saveValues(()=>{
      this.openContributor.emit();
    })
  }
  private getGroupValueCaption(value:string,widget:any){
    let values=value.split(MdsComponent.GROUP_MULTIVALUE_DELIMITER);
    let caption="";
    let i=0;
    for(let sub of widget.subwidgets){
      let v=values[i++];
      if(!v)
        continue;
      if(caption!=""){
        caption+=", ";
      }
      caption+=this.getValueCaption(this.getWidget(sub.id),v);
    }
    return caption;
  }
  private addGroupValues(id:string){
    let widget=this.getWidget(id);
    let widgets=[];
    for(let sub of widget.subwidgets){
      widgets.push(this.getWidget(sub.id));
    }
    let values=this.getValues([],true,widgets);
    if(!values)
      return;
    let result="";
    let i=0;
    let hasValue=false;
    for(let sub of widget.subwidgets){
        if(values[sub.id] && values[sub.id][0]){
          hasValue=true;
          result+=values[sub.id][0];
        }
        if(i++<widget.subwidgets.length-1)
          result+=MdsComponent.GROUP_MULTIVALUE_DELIMITER;
    }
    if(!hasValue){
      return;
    }
    let badges=document.getElementById(widget.id);
    let elements:any=badges.childNodes;
    let add=true;
    for(let i=0;i<elements.length;i++){
        if(elements[i].getAttribute('data-value')==result){
            return;
        }
    }
    let caption=this.getGroupValueCaption(result,widget);
    document.getElementById(id).innerHTML+=this.getMultivalueBadge(result,caption);
  }
  private renderGroupWidget(widget: any,attr:string,template:any){
    if(!widget.subwidgets || !widget.subwidgets.length){
      return "Widget "+widget.id+" is a group widget, but has no subwidgets attached";
    }
    let html='<div class="widgetGroup">'
    for(let sub of widget.subwidgets){
      let subwidget=this.getWidget(sub.id);
      if(subwidget==null){
          html+='Widget '+sub.id+" was not found. Check the widget id";
      }
      else if(this.isMultivalueWidget(subwidget)){
        html+='Widget '+subwidget.id+" is a multivalue widget. This is not supported for groups";
      }
      else {
        console.log(subwidget);
        let render=this.renderWidget(subwidget, null, template);
        html += render ? render : "";
      }
    }
    html+=`<div class="widgetGroupAdd"><div class="btn waves-effect waves-light" onclick="window.mdsComponentRef.component.addGroupValues('`+widget.id+`')">`+this.translate.instant('ADD')+`</div></div></div>
            <div id="`+widget.id+`" class="multivalueBadges"></div>`;
    return html;
  }
  private renderLicense(widget: any) {
    if(this.mode=='search'){
      if(!widget.values){
        return 'widget \'license\' does not have values connected, can\'t render it.';
      }
      for(let value of widget.values){
        let image=NodeHelper.getLicenseIconByString(value.id, this.connector);
        if(image)
          value.imageSrc = image;
      }
      widget.type='checkboxVertical';
      let html = this.renderCheckboxWidget(widget,null,true);
      return html;
    }
    else {
        let html=`<div class="mdsLicense">`
        let isSafe=this.connector.getCurrentLogin() && this.connector.getCurrentLogin().currentScope!=null;
        if(isSafe || !this.connector.hasToolPermissionInstant(RestConstants.TOOLPERMISSION_LICENSE)){
            html+=`<div class="mdsNoPermissions">`+this.translate.instant('MDS.LICENSE_NO_PERMISSIONS'+(isSafe ? '_SAFE' : ''))+`</div>`;
        }
        else {
            html += `<a class="clickable licenseLink" onclick="window.mdsComponentRef.component.openLicenseDialog();">` +
                this.translate.instant('MDS.LICENSE_LINK') + ` <i class="material-icons">arrow_forward</i></a>`;
        }
        html+=`</div>`;
        return html;
    }
  }

  private setPreview(counter=1) {
    let preview:any=document.getElementById('preview');
    if(preview){
      if(!this.currentNode){
        if(this.createType==MdsComponent.TYPE_TOOLDEFINITION){
          preview.src = this.connector.getThemeMimePreview('tool_definition.svg');
        }
        else {
          preview.src = this.connector.getThemeMimePreview('file.svg');
        }
        return;
      }
      if(preview.src && !preview.src.startsWith(this.currentNode.preview.url))
        return;
      preview.src=this.currentNode.preview.url+'&crop=true&width=400&height=300&dontcache='+new Date().getMilliseconds();
      //if(node.preview.isIcon){
        setTimeout(()=>{
          //this.node.getNodeMetadata(node.ref.id).subscribe((data:NodeWrapper)=>{this.setPreview(data.node)});
          this.setPreview(counter*2);
        },Math.min(10000,500*counter));
      //}
    }
  }

  private setGeneralNodeData() {
    setTimeout(()=>{
      this.setPreview();
    },10);
  }

  private onUpdatePreview(callback:Function=null) {
    let preview=null;
    try{
      preview = (document.getElementById('previewSelect') as any).files[0];
    }catch(e){}
    if(preview){
      this.node.uploadNodePreview(this.currentNode.ref.id,preview).subscribe(()=>{
        this.toast.toast('WORKSPACE.EDITOR.UPDATED');
        if(callback)
          callback();
        this.onDone.emit(this.currentNode);
        this.globalProgress=false;

      },(error:any)=>{
        this.toast.error(error);
        this.globalProgress=false;
      });
    }
    else {
      this.globalProgress=false;
      this.toast.toast('WORKSPACE.EDITOR.UPDATED');
      if(callback)
        callback();
      this.onDone.emit(this.currentNode);
    }
  }
  private cancel(){
    this.onCancel.emit();
  }

  private applySuggestions() {
    setTimeout(() => {
        if (!this.currentWidgets)
          return;
        let values=this.getValues([],false);
        for (var property in this._suggestions) {
          let widget: any = null;
          for (let w of this.currentWidgets) {
            if (w.id == property)
              widget = w;
          }
          let element = document.getElementById(property + '_badgeSuggestions');
          if (element) {
            element.style.display='';
            element.innerHTML = '';
            for(let item of this._suggestions[property]) {
                if (Helper.indexOfNoCase(values[property], item.id) == -1) {
                  element.innerHTML += this.getSuggestBadge(item.id, item.caption, property);
                }
              }
            }
            else {
              //console.log("no suggestion area found for widget " + property);
            }
          if(!widget){
            //console.warn("no widget found for " + property);
          }
        }
      }
      , 10);
  }

  private getWidget(id: string,template:string=null,widgets=this.mds.widgets) {
    for(let w of widgets){
      if(w.id==id){
        if((template==null || w.template==template) && this.isWidgetConditionTrue(w)){
          return w;
        }
      }
    }
    return null;
  }

  private getKeyCaption(key: string, values:any[]) {
    if(!values)
      return key;

    for(let value of values){
      if(value.id==key)
        return value.caption;
    }
    return key;
  }

  private isContentEditable() {
    let value=this.currentNode && this.currentNode.properties[RestConstants.CCM_PROP_EDITOR_TYPE];
    return value!='tinymce';
  }

  private isExtendedWidget(widget: any) {
    return widget.isExtended==true || widget.extended==true || widget.isExtended=='true' || widget.extended=='true';
  }
  private isRequiredWidget(widget: any) {
        return widget.isRequired==true || widget.required==true || widget.isRequired=='true' || widget.required=='true';
  }
  private highlightSearch(caption: string, searchString: string) :string {
    let pos=caption.toLowerCase().indexOf(searchString.toLowerCase());
    if(pos==-1)
      return caption;
    return caption.substring(0,pos)+'<span class=suggestHighlight>'+
      caption.substring(pos,pos+searchString.length)+'</span>'+
      caption.substring(pos+searchString.length);
  }

  private setActiveAuthor(type: number) {
    this.activeAuthorType=type;
    let freetext=document.getElementById('mdsAuthorFreetext');
    let person=document.getElementById('mdsAuthorPerson');
    if(!freetext || !person)
      return;
    let tabs=document.getElementById('mdsAuthorTabs').getElementsByTagName('li');
    freetext.style.display='none';
    person.style.display='none';
    for(let i=0;i<tabs.length;i++){
      tabs[i].getElementsByTagName('a')[0].className=tabs[i].getElementsByTagName('a')[0].className.replace('active','').trim();
    }
    tabs[type].getElementsByTagName('a')[0].className+=' active';
    if(type==MdsComponent.AUTHOR_TYPE_FREETEXT){
      freetext.style.display='';
    }
    if(type==MdsComponent.AUTHOR_TYPE_PERSON){
      person.style.display='';
    }
  }

  private addAuthorValue(properties: any) {
    if(document.getElementById(RestConstants.CCM_PROP_AUTHOR_FREETEXT) || document.getElementById(RestConstants.CCM_PROP_LIFECYCLECONTRIBUTER_AUTHOR)) {
      //if(this.activeAuthorType==MdsComponent.AUTHOR_TYPE_FREETEXT)
      if(Helper.indexOfObjectArray(this.currentWidgets,'id',RestConstants.CCM_PROP_AUTHOR_FREETEXT)==-1) {
          this.currentWidgets.push({id: RestConstants.CCM_PROP_AUTHOR_FREETEXT, type: 'textarea'});
      }
      //if(this.activeAuthorType==MdsComponent.AUTHOR_TYPE_PERSON)
      if(Helper.indexOfObjectArray(this.currentWidgets,'id',RestConstants.CCM_PROP_LIFECYCLECONTRIBUTER_AUTHOR)==-1) {
          this.currentWidgets.push({id: RestConstants.CCM_PROP_LIFECYCLECONTRIBUTER_AUTHOR, type: 'vcard'});
      }
    }
  }

  private setRenderedHtml(html: string) {
    if(!html.trim())
      this.rendered=null;
    else
      this.rendered=this.sanitizer.bypassSecurityTrustHtml(html);
  }

  private isPrimitiveWidget(widget: any) {
    return widget.type=='text' || widget.type=='number' || widget.type=='email' || widget.type=='date' || widget.type=='month' || widget.type=='color'
  }

  private htmlEscape(caption: string) {
    return caption.split('"').join('&quot;');
  }

  private replaceVariables(widget: any) {
    if(this.variables==null)
      return;
    widget.caption=this.replaceVariableString(widget.caption,this.variables);
    widget.placeholder=this.replaceVariableString(widget.placeholder,this.variables);
    widget.icon=this.replaceVariableString(widget.icon,this.variables);
    widget.defaultvalue=this.replaceVariableString(widget.defaultvalue,this.variables);
  }

  private replaceVariableString(string:string, variables: string[]) {
    if(!string)
      return string;
    if(!string.match('\\${.+}')) {
      return string;
    }
    for(let key in variables){
      if('${'+key+'}'==string){
        return variables[key];
      }
    }
    console.warn('mds declared variable '+string+', but it was not found in the config variables. List of known variables below');
    console.warn(variables);
    return string;
  }
}
