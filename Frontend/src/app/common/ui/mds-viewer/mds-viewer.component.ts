import {
  Component,
  Input,
  ElementRef,
  Sanitizer, ViewContainerRef, ComponentFactoryResolver, QueryList, ViewChildren
} from '@angular/core';
import {TranslateService} from '@ngx-translate/core';
import {MdsWidgetComponent} from "./widget/mds-widget.component";
import {RestMdsService} from "../../../core-module/core.module";
import {DomSanitizer} from "@angular/platform-browser";
import {UIHelper} from "../../../core-ui-module/ui-helper";

@Component({
  selector: 'mds-viewer',
  templateUrl: 'mds-viewer.component.html',
  styleUrls: ['mds-viewer.component.scss'],
})
export class MdsViewerComponent{
  @ViewChildren('container') container : QueryList<ElementRef>;
  _groupId:string;
  _data: string;
  private mds: any;
  private templates: any[];
  @Input() set groupId(groupId:string){
    this._groupId=groupId;
    this.inflate();
  }
  @Input() set data(data:string){
    this._data=data;
    this.inflate();
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
            UIHelper.injectAngularComponent(this.factoryResolver, this.containerRef, MdsWidgetComponent, element[0], {
              widget: w,
              data: this._data[w.id]
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
    this.mdsService.getSet().subscribe((mds)=>{
      this.mds=mds;
    });
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
