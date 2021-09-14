import {
  Component,
  Input,
  ElementRef,
  Sanitizer, ViewContainerRef, ComponentFactoryResolver, QueryList, ViewChildren
} from '@angular/core';
import {MdsWidgetComponent} from "./widget/mds-widget.component";
import {RestConstants, RestMdsService} from "../../../core-module/core.module";
import {DomSanitizer} from "@angular/platform-browser";
import {UIHelper} from "../../../core-ui-module/ui-helper";
import {MdsEditorViewComponent} from '../mds-editor/mds-editor-view/mds-editor-view.component';

@Component({
  selector: 'mds-viewer',
  templateUrl: 'mds-viewer.component.html',
  styleUrls: ['mds-viewer.component.scss'],
})
export class MdsViewerComponent{
  @ViewChildren('container') container : QueryList<ElementRef>;
  _groupId:string;
  _setId:string;
  _data: any;
  mds: any;
  templates: any[];
  @Input() set groupId(groupId:string){
    this._groupId=groupId;
    this.inflate();
  }
  @Input() set setId(setId:string){
    this._setId=setId;
    this.mdsService.getSet(setId).subscribe((mds)=>{
      this.mds=mds;
      this.inflate();
    });
  }
  @Input() set data(data: any) {
    this._data=data;
    if(this._data[RestConstants.CM_PROP_METADATASET_EDU_METADATASET] != null) {
      this.mdsService.getSet(this._data[RestConstants.CM_PROP_METADATASET_EDU_METADATASET][0]).subscribe((mds)=>{
        this.mds=mds;
        this.inflate();
      });
    } else {
      this.mdsService.getSet().subscribe((mds)=>{
        this.mds=mds;
        this.inflate();
      });
    }
  }
  /**
   * show group headings (+ icons) for the individual templates
   */
  @Input() showGroupHeadings=true;
  getGroup(){
    return this.mds.groups.find((g:any)=>g.id==this._groupId);
  }
  getView(id:string){
    return this.mds.views.find((v:any)=>v.id==id);
  }
  private inflate() {
    if(!this.mds) {
      setTimeout(() => this.inflate(), 1000 / 60);
      return;
    }
    this.templates=[];
    for(let view of this.getGroup().views){
        let v=this.getView(view);
        this.templates.push({view:v,html:this.sanitizer.bypassSecurityTrustHtml(this.prepareHTML(v.html))});
    }
    // wait for angular to inflate the new binding
    setTimeout(()=>{
      for(let w of this.mds.widgets) {
        // @TODO: it would be better to filter by widgets based on template and condition, should be implemented in 5.1
        this.container.toArray().forEach((c) => {
          let element = c.nativeElement.getElementsByTagName(w.id);
          if (element && element[0]) {
            MdsEditorViewComponent.updateWidgetWithHTMLAttributes(element[0], w);
            UIHelper.injectAngularComponent(this.factoryResolver, this.containerRef, MdsWidgetComponent, element[0], {
              widget: w,
              data: w.type === 'range' ? [this._data[w.id + '_from'], this._data[w.id + '_to']] :
                  this._data[w.id]
            });
          }
        });
      }
    });
  }
  constructor(private mdsService : RestMdsService,
              private factoryResolver:ComponentFactoryResolver,
              private containerRef:ViewContainerRef,
              private sanitizer:DomSanitizer){
  }

  /**
   * close all custom tags inside the html which are not closed
   * e.g. <cm:name>
   *     -> <cm:name></cm:name>
   * @param html
   */
  private prepareHTML(html:string) {
    for(let w of this.mds.widgets){
      let start=html.indexOf('<'+w.id);
      if(start==-1) continue;
      let end=html.indexOf('>',start)+1;
      html=html.substring(0,end)+'</'+w.id+'>'+html.substring(end);
    }
    return html;
  }
}
